package fr.mad;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

public class EFSFileSystemView implements FileSystemView {
	
	private FileSystem efs;
	private HomeFtpFile home;
	private User user;
	private FtpFile current;
	
	public EFSFileSystemView(FileSystem efs, User user) throws IOException {
		this.efs = efs;
		this.user = user;
		this.home = new HomeFtpFile(efs, user, list());
	}
	
	private List<FtpFile> list() throws IOException {
		DirectoryStream<Path> ds = efs.provider().newDirectoryStream(efs.getPath(null), null);
		return StreamSupport.stream(ds.spliterator(), true).map(p -> {
			return new ZipFtpFile(p, user);
		}).collect(Collectors.toList());
	}
	
	@Override
	public FtpFile getHomeDirectory() throws FtpException {
		return home;
	}
	
	@Override
	public FtpFile getWorkingDirectory() throws FtpException {
		return current;
	}
	
	@Override
	public boolean changeWorkingDirectory(String dir) throws FtpException {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public FtpFile getFile(String file) throws FtpException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isRandomAccessible() throws FtpException {
		return true;
	}
	
	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
	
}
