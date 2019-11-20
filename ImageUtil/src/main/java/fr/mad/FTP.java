package fr.mad;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.ClientAuth;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.apache.ftpserver.ssl.impl.DefaultSslConfiguration;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;

import fr.mad.ImageUtil.Crypto;

public class FTP {
	private static final int port = 22;
	private static KeyStore keyStore;
	private static String ksp = "skjfgh";
	private static UserManager um;
	
	public static void main(String[] arg) throws Exception {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
		Crypto.removeCryptographyRestrictions();
		
		FtpServerFactory serverFactory = new FtpServerFactory();
		ListenerFactory factory = new ListenerFactory();
		
		factory.setPort(port);
		
		factory.setSslConfiguration(createKeyStore());
		factory.setImplicitSsl(false);
		
		serverFactory.addListener("default", factory.createListener());
		
		PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
		try (FileOutputStream fos = new FileOutputStream(new File("myusers.properties"))) {
		}
		userManagerFactory.setFile(new File("myusers.properties"));
		userManagerFactory.setAdminName("11mad11");
		serverFactory.setUserManager(um = userManagerFactory.createUserManager());
		
		serverFactory.setFileSystem(fsf());
		
		FtpServer server = serverFactory.createServer();
		server.start();
		
		loadUser();
	}
	
	private static FileSystemFactory fsf() throws IOException, URISyntaxException {
		Map<String, ?> map = new HashMap<String, Object>();
		FileSystem efs = FileSystems.newFileSystem(new URI("efs", "file:/ftp.efs", "ftp"), map);
		return new FileSystemFactory() {
			
			@Override
			public FileSystemView createFileSystemView(User user) throws FtpException {
				return new FileSystemView() {
					
					@Override
					public boolean isRandomAccessible() throws FtpException {
						return true;
					}
					
					@Override
					public FtpFile getWorkingDirectory() throws FtpException {
						// TODO Auto-generated method stub
						return null;
					}
					
					@Override
					public FtpFile getHomeDirectory() throws FtpException {
						// TODO Auto-generated method stub
						return null;
					}
					
					@Override
					public FtpFile getFile(String file) throws FtpException {
						// TODO Auto-generated method stub
						return null;
					}
					
					@Override
					public void dispose() {
					}
					
					@Override
					public boolean changeWorkingDirectory(String dir) throws FtpException {
						// TODO Auto-generated method stub
						return false;
					}
				};
			}
		};
	}
	
	private static void loadUser() throws FtpException {
		if (um.getUserByName("11mad11") == null) {
			BaseUser user = new BaseUser();
			user.setName("11mad11");
			user.setPassword("26002600");
			
			um.save(user);
		}
		
	}
	
	private static SslConfiguration createKeyStore() throws Exception {
		keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, ksp.toCharArray());
		
		KeyPair kp = generateKeyPair();
		KeyStore.Entry entry = new KeyStore.PrivateKeyEntry(kp.getPrivate(), new Certificate[] { Crypto.selfSign(kp, "CN=11mad11") });
		KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(ksp.toCharArray());
		keyStore.setEntry("11mad11", entry, param);
		
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, ksp.toCharArray());
		
		// initialize trust manager factory
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(keyStore);
		
		return new DefaultSslConfiguration(keyManagerFactory, trustManagerFactory, ClientAuth.NONE, "TLS", null, null);
	}
	
	public static KeyPair generateKeyPair() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048, new SecureRandom());
		KeyPair pair = generator.generateKeyPair();
		
		return pair;
	}
}
