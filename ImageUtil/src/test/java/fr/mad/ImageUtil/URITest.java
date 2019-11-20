package fr.mad.ImageUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import fr.mad.ImageUtil.efs.BlockHelper;

public class URITest {
	private String pass = "password";
	
	@Test
	public void fat() {
		long start = Runtime.getRuntime().freeMemory();
		Map<String, long[]> fat = new PatriciaTrie<>();
		long[] tmp = new long[] { 0, 1, 2, 3, 455854, 5, 6585556698L, 8, 2, 236, 4, 2, 5, 6, 8 };
		for (int i = 0; i < 50000; i++) {
			fat.put(Math.random() + "", tmp);
		}
		System.out.println(start - Runtime.getRuntime().freeMemory());
	}
	
	@Test
	public void blockHelper() throws IOException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		File dir = new File("test/");
		dir.mkdir();
		System.out.println(dir.getAbsolutePath());
		ByteBuffer bb = ByteBuffer.allocate(1024);
		ByteBuffer md = ByteBuffer.allocate(128);
		for (int i = 0; i < bb.capacity(); i++) {
			bb.put((byte) i);
		}
		
		Map<String, Object> map = new HashMap<>();
		map.put("blockSize", bb.capacity());
		byte[] bytes = digest.digest(pass.getBytes());
		byte[] pw = new byte[16];
		System.arraycopy(bytes, 0, pw, 0, 16);
		map.put("pass", new SecretKeySpec(pw, "AES"));
		
		dir.toPath().resolve("1.efs").toFile().delete();
		BlockHelper bh = newBlockHelper(map, dir.toPath().resolve("bh"), true, md.capacity() - 16, true);
		
		md.put("metadata.metadata.metadata.metadata".getBytes());
		bh.fill(md, true);
		bb.flip();
		md.flip();
		
		bh.setBlockData(0, md, true);
		
		ByteBuffer md2 = ByteBuffer.allocate(md.capacity());
		bh.getBlockMetaData(0, md2);
		
		md.position(0);
		Assertions.assertTrue(md.compareTo(md) == 0);
		
		bh.close();
	}
	
	public BlockHelper newBlockHelper(Map<String, ?> map, Path path, boolean write, int mds, boolean mde) throws IOException {
		SeekableByteChannel sbc;
		if (write)
			sbc = Files.newByteChannel(path, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		else
			sbc = Files.newByteChannel(path, StandardOpenOption.READ);
		MapObjectHelper env = new MapObjectHelper(map);
		return new BlockHelper(env, sbc, mds, mde, !write);
	}
	
	@Test
	public void fileSystem() throws Exception {
		String fragment = "path/to/fragment/*#$%$^&*/can be anything!";
		
		File file;
		URI fsLoc = new URI("efs", (file = new File("test/fs.efs")).toURI().toString(), "test");
		file.delete();
		
		FileSystem fs = get(fsLoc);
		
		Path fragmentPath = fs.getPath(fragment);
		
		BufferedWriter bo = Files.newBufferedWriter(fragmentPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		
		bo.append("allo");
		bo.newLine();
		bo.append("bye");
		bo.newLine();
		bo.close();
		
		List<String> list = Files.readAllLines(fragmentPath);
		list.forEach(System.out::println);
		
		Assertions.assertTrue("allo".equals(list.get(0)));
		Assertions.assertTrue("bye".equals(list.get(1)));
	}
	
	private FileSystem get(URI fsLoc) throws Exception {
		try {
			return FileSystems.getFileSystem(fsLoc);
		} catch (Exception e) {
			Map<String, Object> env = new HashMap<>();
			
			Crypto.removeCryptographyRestrictions();
			Security.addProvider(new BouncyCastleProvider());
			
			byte[] passB = MessageDigest.getInstance("SHA-256").digest(pass.getBytes());
			byte[] pw = new byte[16];
			System.arraycopy(passB, 0, pw, 0, 16);
			
			env.put("pass", new SecretKeySpec(pw, "AES"));
			
			return FileSystems.newFileSystem(fsLoc, env);
		}
	}
	
}
