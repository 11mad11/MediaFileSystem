package fr.mad;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.util.List;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

public class HomeFtpFile implements FtpFile {
	
	private FileSystem efs;
	private User user;
	private List<FtpFile> files;
	
	public HomeFtpFile(FileSystem efs, User user, List<FtpFile> files) {
		this.efs = efs;
		this.user = user;
		this.files = files;
	}
	
	@Override
	public String getAbsolutePath() {
		return "/";
	}
	
	@Override
	public String getName() {
		return "";
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
		try {
			return efs.getFileStores().iterator().next().getTotalSpace();
		} catch (Throwable e) {
			return 0;
		}
	}
	
	@Override
	public Object getPhysicalFile() {
		return "/";
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
		throw new IOException("dir");
	}
	
	@Override
	public InputStream createInputStream(long offset) throws IOException {
		throw new IOException("dir");
	}
	
}
