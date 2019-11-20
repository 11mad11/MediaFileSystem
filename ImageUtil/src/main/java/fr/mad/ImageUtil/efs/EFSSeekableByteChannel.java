package fr.mad.ImageUtil.efs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

public class EFSSeekableByteChannel implements SeekableByteChannel {
	
	private EFS efs;
	private FAT fat;
	
	private boolean read;
	private boolean write;
	private boolean append;
	private String path;
	
	private long[] blocksId;
	/**
	 * pos in {@link #blocksId}
	 */
	private int pos;
	private ByteBuffer block;
	
	private byte[] buf;
	private long size;
	
	public EFSSeekableByteChannel(EFSPath path, Set<? extends OpenOption> options, FileAttribute<?>[] attrs) throws IOException {
		efs = (EFS) path.getFileSystem();
		fat = efs.fat;
		this.path = path.fragment;
		
		read = options.contains(StandardOpenOption.READ) || options.size() == 0;
		write = options.contains(StandardOpenOption.WRITE);
		append = options.contains(StandardOpenOption.APPEND);
		
		boolean create = options.contains(StandardOpenOption.CREATE);
		boolean create_new = options.contains(StandardOpenOption.CREATE_NEW);
		
		blocksId = fat.getChainBlock(path.fragment, false);
		if (create_new && blocksId != null)
			throw new FileAlreadyExistsException(path.fragment);
		if (blocksId == null && (create || create_new))
			blocksId = fat.getChainBlock(path.fragment, true);
		if (blocksId == null)
			throw new FileNotFoundException(path.fragment);
		
		block = ByteBuffer.allocate(efs.blockHelper.blockSize + efs.blockHelper.padding);
		block.limit(efs.blockHelper.blockSize);
		buf = new byte[efs.blockHelper.blockSize];
		
		fat.getLen();
		
		if (append)
			position(size());
		else
			position(0);
	}
	
	@Override
	public boolean isOpen() {
		return blocksId != null;
	}
	
	@Override
	public void close() throws IOException {
		block = null;
		blocksId = null;
	}
	
	@Override
	public int read(ByteBuffer dst) throws IOException {
		if (!read)
			throw new NonReadableChannelException();
		int len = 0;
		while (dst.hasRemaining()) {
			if (dst.remaining() >= block.remaining()) {
				len += block.remaining();
				dst.put(block);
				if (!loadBlock(pos + 1, false))
					return -1;
			} else {
				len += dst.remaining();
				block.get(buf, 0, dst.remaining());
				dst.put(buf, 0, dst.remaining());
			}
		}
		return len;
	}
	
	@Override
	public int write(ByteBuffer src) throws IOException {
		if (!write)
			throw new NonWritableChannelException();
		int len = 0;
		if (block.remaining() >= src.remaining()) {
			len = src.remaining();
			block.put(src);
			flush();
		} else {
			while (src.hasRemaining()) {
				int l = Math.min(block.remaining(), src.remaining());
				len += l;
				src.get(buf, 0, l);
				block.put(buf, 0, l);
				flush();
				loadBlock(pos + 1, true);
			}
		}
		
		if (position() > size)
			fat.setLen(path, size = position());
		return len;
	}
	
	private void flush() throws IOException {
		try {
			int p = block.position();
			block.position(0);
			efs.blockHelper.setBlockData(pos, block, false);
			block.position(p);
		} catch (InvalidKeyException | ShortBufferException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public long position() throws IOException {
		return (pos * efs.blockHelper.blockSize) + block.position();
	}
	
	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		int p = (int) Math.floorDiv(newPosition, efs.blockHelper.blockSize);
		if (pos != p)
			loadBlock(p, true);
		block.position((int) (newPosition % efs.blockHelper.blockSize));
		return this;
	}
	
	private boolean loadBlock(int p, boolean create) throws IOException {
		this.pos = p;
		if (pos >= blocksId.length)
			if (!create)
				return false;
		while (pos >= blocksId.length) {
			blocksId = fat.getNextFreeBlock(path);
		}
		long id = blocksId[p];
		try {
			efs.blockHelper.getBlockData(id, block);
		} catch (InvalidKeyException | ShortBufferException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
			throw new IOException(e);
		}
		
		return true;
	}
	
	@Override
	public long size() throws IOException {
		return size;
	}
	
	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		if (!write | !append)
			throw new NonWritableChannelException();
		if (size < 0)
			throw new IllegalArgumentException();
		while ((blocksId.length * efs.blockHelper.blockSize) - 1 > size) {
			blocksId = fat.removeLast(path);
		}
		if (position() > size)
			position(size);
		return this;
	}
	
}
