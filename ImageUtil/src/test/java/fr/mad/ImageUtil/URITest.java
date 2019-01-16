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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;

import fr.mad.ImageUtil.efs.BlockHelper;

public class URITest {
	private String pass = "password";
	
	@Benchmark
	@Test
	public void blockHelper() throws IOException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		File dir = new File("test/");
		dir.mkdir();
		System.out.println(dir.getAbsolutePath());
		ByteBuffer bb = ByteBuffer.allocate(128);
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
		
		BlockHelper bh = newBlockHelper(map, dir.toPath().resolve("1.efs"), true, md.capacity(), true);
		
		md.put("metadata.metadata.metadata.metadata".getBytes());
		bh.fill(md, true);
		bb.flip();
		md.flip();
		
		for (int i = 0; i < 100000; i++) {
			//bh.setBlockData(0, bb, false);
			bh.setBlockData(i, md, true);
			bh.getBlockMetaData(i);
			md.flip();
		}
		
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
		
		URI fsLoc = new URI("efs", new File("/test.efs").toURI().toURL().toString(), "");
		
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
		
		Assumptions.assumeTrue("allo".equals(list.get(0)));
		Assumptions.assumeTrue("bye".equals(list.get(1)));
	}
	
	private FileSystem get(URI fsLoc) throws Exception {
		try {
			return FileSystems.getFileSystem(fsLoc);
		} catch (Exception e) {
			Map<String, Object> env = new HashMap<>();
			
			Crypto.removeCryptographyRestrictions();
			Security.addProvider(new BouncyCastleProvider());
			
			byte[] passB = MessageDigest.getInstance("SHA-256").digest(pass.getBytes());
			System.arraycopy(passB, 0, passB, 0, 128);
			
			env.put("password", passB);
			env.put("transformation", "AES/CTR/NoPadding");
			env.put("provider", "BC");
			
			return FileSystems.newFileSystem(fsLoc, env);
		}
	}
	
}
