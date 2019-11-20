package fr.mad;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

public class ZipFtpFile implements FtpFile {
	
	private SeekableByteChannel sbc;
	private Path path;
	private User user;
	private List<ZipEntryFtpFile> files;
	
	@SuppressWarnings("resource")
	public ZipFtpFile(Path p, User user) throws IOException {
		this.path = p;
		this.user = user;
		try {
			sbc = Files.newByteChannel(p, StandardOpenOption.READ);
			if (sbc.size() > 0) {
				ZipFile zip = new ZipFile(sbc);
				files = EnumerationUtils.toList(zip.getEntries()).stream().map(e -> {
					return new ZipEntryFtpFile(e);
				}).collect(Collectors.toList());
			}
		} catch (FileNotFoundException e1) {
			sbc = Files.newByteChannel(p, StandardOpenOption.READ, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			new ZipArchiveOutputStream(sbc);
			
		}
	}
	
	@Override
	public String getAbsolutePath() {
		return "/" + path.toUri().getSchemeSpecificPart();
	}
	
	@Override
	public String getName() {
		return path.toUri().getSchemeSpecificPart();
	}
	
	@Override
	public boolean isHidden() {
		return false;
	}
	
	@Override
	public boolean isDirectory() {
		return true;
	}
	
	@Override
	public boolean isFile() {
		return false;
	}
	
	@Override
	public boolean doesExist() {
		return true;
	}
	
	@Override
	public boolean isReadable() {
		return true;
	}
	
	@Override
	public boolean isWritable() {
		return true;
	}
	
	@Override
	public boolean isRemovable() {
		return false;
	}
	
	@Override
	public String getOwnerName() {
		return user.getName();
	}
	
	@Override
	public String getGroupName() {
		return null;
	}
	
	@Override
	public int getLinkCount() {
		return 0;
	}
	
	@Override
	public long getLastModified() {
		return 0;
	}
	
	@Override
	public boolean setLastModified(long time) {
		return false;
	}
	
	@Override
	public long getSize() {
		if (sbc == null)
			return 0;
		try {
			return sbc.size();
		} catch (IOException e) {
			return 0;
		}
	}
	
	@Override
	public Object getPhysicalFile() {
		return path;
	}
	
	@Override
	public boolean mkdir() {
		return true;
	}
	
	@Override
	public boolean delete() {
		return false;
	}
	
	@Override
	public boolean move(FtpFile destination) {
		return false;
	}
	
	@Override
	public List<? extends FtpFile> listFiles() {
		return files;
	}
	
	@Override
	public OutputStream createOutputStream(long offset) throws IOException {
		return null;
	}
	
	@Override
	public InputStream createInputStream(long offset) throws IOException {
		return null;
	}
	
}
