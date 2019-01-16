package fr.mad.ImageUtil.efs;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The URI to create a FileSystem ({@link #newFileSystem(URI, Map)}) look like
 * that:</br>
 * "efs:{uri to file .efs}#{id for registering}"</br>
 * if the id is null or empty, the registration is skipped.
 * </p>
 * The URI to retrieve a FileSystem ({@link #getFileSystem(URI)}) look like
 * that:</br>
 * "efs:[ignored]#{FileSystem's id}"
 * </p>
 * The uri to get a path ({@link #getPath(URI)}) look like that:</br>
 * "efs:{name of file}#{FileSystem's id}"
 * 
 * @author marcantoine
 *
 */
public class EFSProvider extends FileSystemProvider {
	
	private static final Map<String, EFS> list = new HashMap<>();
	
	public static void get(URI uri, String pass) {
		
	}
	
	@Override
	public String getScheme() {
		return "efs";
	}
	
	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		if (list.containsKey(uri.getFragment()))
			throw new FileSystemAlreadyExistsException("\"" + uri.getFragment() + "\"");
		Path path = Paths.get(URI.create(uri.getSchemeSpecificPart()));
		
		//FileSystemProvider provider = path.getFileSystem().provider();
		
		SeekableByteChannel sbc;
		boolean readOnly = false;
		try {
			sbc = Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);
		} catch (Exception e) {
			sbc = Files.newByteChannel(path, StandardOpenOption.READ);
			readOnly = true;
		}
		
		EFS efs = new EFS(this, sbc, readOnly, uri.getFragment(), env);
		
		if (uri.getFragment() != null && !uri.getFragment().isEmpty())
			list.put(uri.getFragment(), efs);
		return efs;
	}
	
	@Override
	public FileSystem getFileSystem(URI uri) {
		EFS efs = list.get(uri.getFragment());
		if (efs == null)
			throw new FileSystemNotFoundException();
		return efs;
	}
	
	@Override
	public Path getPath(URI uri) {
		EFS efs = (EFS) getFileSystem(uri);
		return new EFSPath(efs, uri.getSchemeSpecificPart());
	}
	
	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
		return new EFSSeekableByteChannel((EFSPath)path,options,attrs);
	}
	
	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void delete(Path path) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isHidden(Path path) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public FileStore getFileStore(Path path) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
}
