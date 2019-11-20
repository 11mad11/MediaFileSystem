package fr.mad.ImageUtil.efs;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;

import fr.mad.ImageUtil.MapObjectHelper;

public class BlockHelper implements Closeable {
	private static final Logger log = Logger.getLogger(BlockHelper.class.getName());
	static final int METADATA_SIZE = 1024;
	static final int IV_Size = 12;
	private static final int DEFAULT_BLOCK_SIZE = 1024 * 512;
	
	final int blockMetadataSize;
	final int blockSize;
	final boolean encryptMetadata;
	private final Key pass;
	final String transformation;
	private final Provider provider;
	final short padding;
	final Function<byte[], AlgorithmParameterSpec> parameterSpec;
	private SeekableByteChannel sbc;
	
	private ThreadLocal<ByteBuffer> bufferPool = new ThreadLocal<ByteBuffer>() {
		@Override
		protected ByteBuffer initialValue() {
			return ByteBuffer.allocate(blockSize + padding);
		}
	};
	private ThreadLocal<ByteBuffer> bufferMdPool = new ThreadLocal<ByteBuffer>() {
		@Override
		protected ByteBuffer initialValue() {
			return ByteBuffer.allocate(blockMetadataSize + padding);
		}
	};
	private ThreadLocal<ByteBuffer> bufferIVPool = new ThreadLocal<ByteBuffer>() {
		@Override
		protected ByteBuffer initialValue() {
			return ByteBuffer.wrap(new byte[IV_Size]);
		}
	};
	private ThreadLocal<Cipher> cipherPool = new ThreadLocal<Cipher>() {
		@Override
		protected Cipher initialValue() {
			Cipher cipher;
			try {
				if (provider == null)
					cipher = Cipher.getInstance(transformation);
				else
					cipher = Cipher.getInstance(transformation, provider);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
				throw new RuntimeException(e);
			}
			return cipher;
		}
	};
	
	/**
	 * the blockSize,transformation and provider parameters from env will be
	 * ignored if fs is existing
	 * 
	 * @param env
	 * @param sbc
	 * @param blockMetadataSize
	 *            cannot be different for existing fs
	 * @param encryptMetadata
	 *            can be different for existing fs, will print a warning if
	 *            different
	 * @throws IOException
	 */
	public BlockHelper(MapObjectHelper env, SeekableByteChannel sbc, int blockMetadataSize, boolean encryptMetadata, boolean readOnly) throws IOException {
		pass = env.getNotNull("pass");
		this.sbc = sbc;
		if ((blockMetadataSize & 0xFFFF) != blockMetadataSize)
			throw new IllegalArgumentException("blockMetadataSize too big");
		this.blockMetadataSize = blockMetadataSize;
		
		ByteBuffer buf = ByteBuffer.allocate(METADATA_SIZE);
		buf.clear();
		sbc.position(0);
		
		int len = sbc.read(buf);
		if (len != buf.limit()) {
			this.blockSize = Math.min(0xFFFF, env.get("blockSize", DEFAULT_BLOCK_SIZE));
			this.encryptMetadata = encryptMetadata;
			this.transformation = env.get("transformation", "AES/GCM/NoPadding");
			this.provider = null;//will check installed providers instead
			this.padding = env.get("padding", (short) 16);
			this.parameterSpec = (iv) -> {//TODO how to save that
				return new GCMParameterSpec(128, iv);
			};
			
			if (!readOnly) {
				buf.clear();
				sbc.position(0);
				
				buf.put((byte) (blockSize & 0xFF));
				buf.put((byte) (blockSize >> 8 & 0xFF));
				buf.put((byte) (blockMetadataSize & 0xFF));
				buf.put((byte) (blockMetadataSize >> 8 & 0xFF));
				byte mask = 0;
				if (encryptMetadata)
					mask |= 1;
				buf.put(mask);
				buf.put((byte) transformation.length());
				buf.put(transformation.getBytes(StandardCharsets.UTF_8));
				buf.put((byte) padding);
				
				buf.flip();
				sbc.write(buf);
			} else
				throw new IOException("FileSystem do not exist and cannot be created");
			
			return;
		}
		buf.flip();
		
		blockSize = (buf.get() & 0xFF) | ((buf.get() & 0xFF) << 8);
		int tmp = (buf.get() & 0xFF) | ((buf.get() & 0xFF) << 8);//blockMetadataSize already set up here
		if (tmp != blockMetadataSize)
			throw new RuntimeException();
		byte mask = buf.get();
		this.encryptMetadata = (mask & 1) != 0;
		if (encryptMetadata != this.encryptMetadata)
			log.warning("encryptMetadata field not the same in filesystem that from spec");
		this.transformation = readString(buf);
		this.provider = null;
		this.padding = (short) (buf.get() & 0xFF);
		this.parameterSpec = (iv) -> {//TODO how to load that
			return new GCMParameterSpec(128, iv);
		};
		log.fine("Loaded BlockHelper with config: \n" + this.toString());
	}
	
	private String readString(ByteBuffer buf) {
		int len = buf.get() & 0xFF;
		byte[] bytes = new byte[len];
		buf.get(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}
	
	/**
	 * if no data was in this block return bb with position 0
	 * 
	 * @param blockID
	 * @param bb
	 *            will be ready to read with block data
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws ShortBufferException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 */
	public void getBlockData(long blockID, ByteBuffer bb) throws IOException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		bb.clear();
		if (bb.capacity() < blockSize + padding)
			throw new ShortBufferException();
		bb.limit(blockSize + padding);
		
		int len;
		synchronized (sbc) {
			sbc.position(dataStart(blockID));
			len = sbc.read(bb);
		}
		
		if (len == -1 && bb.position() == 0)
			return;
		
		if (len != bb.limit())
			throw new IOException("block incomplete or null");
		bb.flip();
		
		ByteBuffer iv = bufferIVPool.get();
		synchronized (sbc) {
			sbc.position(ivStart(blockID));
			sbc.read(iv);//already read into block so no check on len needed
		}
		Cipher cipher = cipherPool.get();
		initCipher(cipher, false, blockID, iv.array(), false);
		cipher.doFinal(bb, bb);
		
		bb.flip();
	}
	
	/**
	 * 
	 * @param blockID
	 * @param bb
	 *            will be ready to read with block metadata
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws ShortBufferException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 */
	public void getBlockMetaData(long blockID, ByteBuffer bb) throws IOException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		int size = blockMetadataSize();
		bb.clear();
		if (bb.capacity() < size)
			throw new ShortBufferException();
		bb.limit(size);
		
		int len;
		synchronized (sbc) {
			sbc.position(metadataStart(blockID));
			len = sbc.read(bb);
		}
		
		if (len != bb.limit())
			throw new IOException("block incomplete or null");
		bb.flip();
		
		if (encryptMetadata) {
			ByteBuffer iv = bufferIVPool.get();
			iv.clear();
			synchronized (sbc) {
				sbc.position(ivStart(blockID));
				sbc.read(iv);//already read into block so no check on len needed
			}
			Cipher cipher = cipherPool.get();
			initCipher(cipher, false, blockID, iv.array(), true);
			ByteBuffer buf = ByteBuffer.allocate(blockMetadataSize);//TODO pooling?
			cipher.doFinal(bb, buf);
			buf.flip();
			bb.clear();
			bb.put(buf);
			bb.flip();
		}
	}
	
	/**
	 * 
	 * @param blockID
	 * @param bb
	 * @param metaData
	 *            if metadata or data
	 * @throws IOException
	 * @throws ShortBufferException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 */
	public void setBlockData(long blockID, ByteBuffer bb, boolean metaData) throws IOException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		if (bb.remaining() != (metaData ? blockMetadataSize : blockSize))
			throw new IOException("buffer remaining is not equals to block(Metadata)Size");
		
		ByteBuffer iv = bufferIVPool.get();
		iv.clear();
		if (!metaData | encryptMetadata) {
			ByteBuffer tmp;
			if (metaData)
				tmp = bufferMdPool.get();
			else
				tmp = bufferPool.get();
			tmp.clear();
			
			int len;
			synchronized (sbc) {
				sbc.position(ivStart(blockID));
				len = sbc.read(iv);
			}
			if (len == -1) {
				if (iv.position() != 0)
					throw new IOException("should not have partial data here");
				iv.clear();
				iv.putLong(0);
				iv.putInt(0);
			}
			
			Cipher cipher = cipherPool.get();
			byte[] bytes = initCipher(cipher, true, blockID, iv.array(), metaData);
			iv.clear();
			iv.put(bytes);
			
			cipher.doFinal(bb, tmp);
			tmp.flip();
			bb = tmp;
		}
		
		synchronized (sbc) {
			if (iv.position() != 0) {
				iv.flip();
				sbc.position(ivStart(blockID));
				sbc.write(iv);
			}
			
			if (metaData)
				sbc.position(metadataStart(blockID));
			else
				sbc.position(dataStart(blockID));
			sbc.write(bb);
		}
	}
	
	private long blockStart(long blockID) {
		return blockID * (IV_Size + padding + padding + blockMetadataSize + blockSize) + METADATA_SIZE;
	}
	
	private long ivStart(long blockID) {
		return blockStart(blockID);
	}
	
	private long metadataStart(long blockID) {
		return blockStart(blockID) + IV_Size;
	}
	
	private long dataStart(long blockID) {
		return blockStart(blockID) + IV_Size + blockMetadataSize + padding;
	}
	
	public int blockMetadataSize() {
		return blockMetadataSize + (encryptMetadata ? padding : 0);
	}
	
	public int blockSize() {
		return blockMetadataSize + padding;
	}
	
	@Override
	public void close() throws IOException {
		sbc.close();
		bufferPool = null;
		bufferIVPool = null;
		bufferMdPool = null;
	}
	
	public boolean isOpen() {
		return sbc != null;
	}
	
	public void fill(ByteBuffer bb, boolean metadata) {
		//int size = metadata ? blockMetadataSize : blockSize;
		while (bb.hasRemaining()) {
			if (bb.position() == (metadata ? blockMetadataSize : blockSize))
				break;
			bb.put((byte) 0);
		}
	}
	
	private byte[] initCipher(Cipher cipher, boolean encrypt, long blockID, byte[] prev_iv, boolean metadata) throws InvalidKeyException, InvalidAlgorithmParameterException {
		byte[] iv;
		if (encrypt) {
			iv = new byte[IV_Size];//IV_Size == 12
			iv[0] = (byte) (blockID & 0xFF);//unique for each block
			iv[1] = (byte) (blockID >> 8 & 0xFF);
			iv[2] = (byte) (blockID >> 16 & 0xFF);
			iv[3] = (byte) (blockID >> 24 & 0xFF);
			iv[4] = (byte) (blockID >> 32 & 0xFF);
			iv[5] = (byte) (blockID >> 40 & 0xFF);
			iv[6] = (byte) (blockID >> 48 & 0xFF);
			iv[7] = (byte) (blockID >> 56 & 0xFF);
			
			int last = prev_iv[8] << 24 | (prev_iv[9] & 0xFF) << 16 | (prev_iv[10] & 0xFF) << 8 | (prev_iv[11] & 0xFF);
			last++;
			/*
			 * ensure that if the attacker has the old data the IV will not be
			 * the same. Permit Integer#MAX_VALUE of modification on a same
			 * block before having a little risk
			 */
			if (last == 0x7FFFFFFF)
				last = 0;
			iv[8] = (byte) (last & 0xFF);
			iv[9] = (byte) (last >> 8 & 0xFF);
			iv[10] = (byte) (last >> 16 & 0xFF);
			iv[11] = (byte) (last >> 24 & 0xFF);
			
			iv[11] = (byte) ((iv[11] & 0x80) | ((metadata ? 1 : 0) << 7));
		} else
			iv = prev_iv;
		
		cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, pass, parameterSpec.apply(iv));
		return iv;
	}
	
}
