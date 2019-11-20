package fr.mad;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;

public class EFSFilesSytemFactory implements FileSystemFactory {
	
	public EFSFilesSytemFactory() {
		
	}
	
	@Override
	public FileSystemView createFileSystemView(User user) throws FtpException {
		try {
			URI uri = getURI(user);
			
			FileSystem efs;
			try {
				efs = FileSystems.getFileSystem(uri);
			} catch (FileSystemNotFoundException e) {
				Map<String, Object> env = new HashMap<String, Object>();
				env.put("pass", getPass(user));
				
				efs = FileSystems.newFileSystem(uri, env);
			}
			
			return new EFSFileSystemView(efs);
		} catch (Exception e2) {
			throw new FtpException(e2);
		}
	}
	
	private SecretKeySpec getPass(User user) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] bytes = digest.digest(user.getPassword().getBytes(StandardCharsets.UTF_8));
		byte[] pw = new byte[16];
		System.arraycopy(bytes, 0, pw, 0, 16);
		return new SecretKeySpec(pw, "AES");
	}
	
	private URI getURI(User user) throws URISyntaxException {
		String id = user.getName();
		return new URI("efs", new File("ftp/" + id + ".efs").toURI().toString(), id);
	}
	
}
