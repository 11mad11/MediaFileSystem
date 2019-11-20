package fr.mad.ImageUtil.efs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import org.apache.commons.collections4.trie.PatriciaTrie;

import fr.mad.ImageUtil.MapObjectHelper;

/**
 * Normal Block:</br>
 * -meta: --8 bytes = next block</br>
 * --1 byte = string len (0-255)</br>
 * --n bytes = string = name of block chain</br>
 * -data:</br>
 * --{@link BlockHelper#blockSize} bytes = data
 * </p>
 * 
 * 
 * @author marcantoine
 *
 */
public class FAT {
	
	/**
	 * storing all the block chain instead of just the first one to speed up
	 * stream creation
	 */
	private PatriciaTrie<long[]> fat;
	private BitSet blockUsed;
	private BlockHelper blockHelper;
	
	private ByteBuffer buf;
	private ByteBuffer mdBuf;
	
	public FAT(MapObjectHelper env, BlockHelper blockHelper) throws InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException {
		this.blockHelper = blockHelper;
		blockUsed = new BitSet(1024);
		fat = new PatriciaTrie<long[]>();
		
		init();
	}
	
	private void init() throws InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException {
		buf = ByteBuffer.allocate(blockHelper.blockSize + blockHelper.padding);
		mdBuf = ByteBuffer.allocate(blockHelper.blockMetadataSize());
		
		blockHelper.getBlockData(0, buf);
		if (buf.position() == 0)
			return;
		
		long id = buf.getLong();
		blockUsed.set((int) id);
		while ((id) != 0) {
			
			init(id, mdBuf);
			
			id = buf.getLong();
			if (buf.remaining() < 8) {
				blockHelper.getBlockData(id, buf);
				id = buf.getLong();
				if (blockUsed.get((int) id))
					throw new IOException("corruption!!!");//TODO Corruption exception
				blockUsed.set((int) id);
			}
		}
	}
	
	private void init(long id, ByteBuffer mdBuf) throws InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException {
		List<Long> ids = new ArrayList<>();
		blockHelper.getBlockMetaData(id, mdBuf);
		ids.add(id);
		if (blockUsed.get((int) id))
			throw new IOException("corruption!!!");
		blockUsed.set((int) id);
		id = mdBuf.getLong();
		String name = readString(mdBuf);
		if (name == null || name.isEmpty())
			throw new IOException("corruption!!!");
		while (id != 0) {
			blockHelper.getBlockMetaData(id, mdBuf);
			if (blockUsed.get((int) id))
				throw new IOException("corruption!!!");
			blockUsed.set((int) id);
			id = mdBuf.getLong();
			if (name.equals(readString(mdBuf)))
				throw new IOException("corruption!!!");
			ids.add(id);
		}
		fat.put(name, ids.stream().mapToLong(i -> i).toArray());
	}
	
	/**
	 * will use a new block (could be not empty) and register it with the key in
	 * this fat
	 * 
	 * @throws IOException
	 */
	public synchronized long[] getNextFreeBlock(String key) throws IOException {
		long[] ids = fat.get(key);
		long id = (ids == null || ids.length == 0) ? 0 : ids[ids.length - 1];
		
		long next = blockUsed.nextClearBit(0);
		if (blockUsed.get((int) next))
			throw new IOException("corruption!!!");
		
		try {
			if (id != 0) {
				blockHelper.getBlockMetaData(id, mdBuf);
				mdBuf.putLong(next);
				mdBuf.position(0);
				blockHelper.setBlockData(id, mdBuf, true);
				
				mdBuf.position(0);
				mdBuf.putLong(0);
			} else {
				mdBuf.clear();
				mdBuf.putLong(0);
				byte[] bytes = key.getBytes(StandardCharsets.UTF_16);
				if (bytes.length > 255)
					throw new IllegalArgumentException();
				mdBuf.put((byte) bytes.length);
				mdBuf.put(bytes);
				mdBuf.limit(blockHelper.blockMetadataSize);
				
				write(next);
			}
			
			mdBuf.position(0);
			blockHelper.setBlockData(next, mdBuf, true);
		} catch (InvalidKeyException | ShortBufferException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
			throw new RuntimeException(e);
		}
		
		fat.put(key, ids = add(ids, next));
		blockUsed.set((int) next);
		return ids;
	}
	
	private void write(long next) throws InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException {
		if (buf.remaining() < 16) {
			int id = blockUsed.nextClearBit(0);
			blockUsed.set(id);
			
			buf.putLong(id);
			blockHelper.getBlockData(id, buf);
		}
		buf.putLong(next);
	}
	
	private long[] add(long[] ids, long next) {
		if (ids == null || ids.length == 0)
			return new long[] { next };
		long[] tmp = new long[ids.length + 1];
		System.arraycopy(ids, 0, tmp, 0, ids.length);
		tmp[tmp.length - 1] = next;
		return tmp;
	}
	
	public long[] getChainBlock(String key, boolean create) throws IOException {
		long[] ids = fat.get(key);
		if (create && ids == null)
			synchronized (this) {
				if ((ids = fat.get(key)) != null)
					return ids;
				ids = getNextFreeBlock(key);
			}
		return ids;
	}
	
	/**
	 * 1byte for length</br>
	 * the rest for byte[] encoded in {@link StandardCharsets#UTF_16}
	 * 
	 * @param buf
	 * @return
	 */
	public String readString(ByteBuffer buf) {
		int len = (buf.get() & 0xFF);
		byte[] bytes = new byte[len];
		buf.get(bytes);
		return new String(bytes, StandardCharsets.UTF_16);
	}
	
	public synchronized void setLen(String path, long len) throws IOException {
		long[] ids = fat.get(path);
		if (ids == null || ids.length == 0)
			throw new IllegalArgumentException();
		try {
			blockHelper.getBlockMetaData(ids[0], mdBuf);
			mdBuf.getLong();
			readString(mdBuf);
			mdBuf.putLong(len);
			mdBuf.position(0);
			blockHelper.setBlockData(ids[0], mdBuf, true);
		} catch (InvalidKeyException | ShortBufferException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
			throw new IOException(e);
		}
	}
	
	public long getLen(String path) throws IOException {
		long[] ids = fat.get(path);
		if (ids == null || ids.length == 0)
			throw new IllegalArgumentException();
		try {
			blockHelper.getBlockMetaData(ids[0], mdBuf);
			mdBuf.getLong();
			readString(mdBuf);
			return mdBuf.getLong();
		} catch (InvalidKeyException | ShortBufferException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
			throw new IOException();
		} catch (BadPaddingException e) {
			return 0;
		}
	}
	
	public synchronized long[] removeLast(String path) throws IOException {
		long[] ids = fat.get(path);
		if (ids == null || ids.length <= 1)
			throw new IllegalArgumentException();
		try {
			blockHelper.getBlockMetaData(ids[ids.length - 2], mdBuf);
			mdBuf.putLong(0);
			mdBuf.position(0);
			blockHelper.setBlockData(ids[ids.length - 2], mdBuf, true);
			blockUsed.clear((int) ids[ids.length]);
		} catch (InvalidKeyException | ShortBufferException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
			throw new IOException(e);
		}
		fat.put(path, ids = removeLast(ids));
		return ids;
	}
	
	private long[] removeLast(long[] ids) {
		if (ids == null || ids.length == 0)
			throw new IllegalArgumentException();
		long[] tmp = new long[ids.length - 1];
		System.arraycopy(ids, 0, tmp, 0, tmp.length);
		return tmp;
	}
	
	public Set<String> getKeys() {
		return fat.keySet();
	}
	
}
