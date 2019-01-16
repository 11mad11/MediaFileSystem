package fr.mad.ImageUtil.efs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;

public class EFSSeekableByteChannel implements SeekableByteChannel {
	
	private EFS efs;
	private boolean read;
	private boolean write;

	public EFSSeekableByteChannel(EFSPath path, Set<? extends OpenOption> options, FileAttribute<?>[] attrs) {
		efs = (EFS) path.getFileSystem();
		
		read = options.contains(StandardOpenOption.READ);
		write = options.contains(StandardOpenOption.WRITE);
	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public int read(ByteBuffer dst) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int write(ByteBuffer src) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public long position() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public long size() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
}
