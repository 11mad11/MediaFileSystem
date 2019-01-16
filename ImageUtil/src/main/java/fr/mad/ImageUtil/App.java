package fr.mad.ImageUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Hello world!
 *
 */
public class App {
	
	public static ThreadLocal<MessageDigest> digestPool;
	public static UI ui;
	
	private static FileSystem fs;
	private static Scanner scanner;
	
	public static void main(String[] args) throws IOException {
		Crypto.removeCryptographyRestrictions();
		Security.addProvider(new BouncyCastleProvider());
		
		scanner = new Scanner(System.in);
		initPool();
		initFileSystem();
		ui = new UI();
		
		ScriptManager.classes.add(App.class);
	}
	
	private static void initFileSystem() throws IOException {
		Map<String, Object> env = new HashMap<>();
		
		System.out.println("fs pass:");
		env.put("pass", scanner.nextLine());
		
		env.put("fileSystemProvider", FileSystems.getDefault().provider());
		
		URI uri = URI.create("efs:" + new File("EncryptedFileSystem.efs").toURI());
		fs = FileSystems.newFileSystem(uri, env);
		
		System.out.println(uri.toURL());
	}
	
	private static void initPool() {
		digestPool = new ThreadLocal<MessageDigest>() {
			@Override
			protected MessageDigest initialValue() {
				try {
					return MessageDigest.getInstance("MD5");
				} catch (NoSuchAlgorithmException e) {
					throw new Error(e);
				}
			}
		};
	}
	
	/**
	 * 
	 * @param data
	 * @return id
	 */
	public String addImage(byte[] data) {
		MessageDigest digest = digestPool.get();
		byte[] md5 = digest.digest(data);//TODO
		return null;
	}
}
