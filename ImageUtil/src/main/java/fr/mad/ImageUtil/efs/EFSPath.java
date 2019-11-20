package fr.mad.ImageUtil.efs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;

public class EFSPath implements Path {
	
	private EFS efs;
	String fragment;
	
	public EFSPath(EFS efs, String fragment) {
		this.efs = efs;
		this.fragment = fragment;
	}
	
	@Override
	public FileSystem getFileSystem() {
		return efs;
	}
	
	@Override
	public boolean isAbsolute() {
		return true;
	}
	
	@Override
	public Path getRoot() {
		return new EFSPath(efs, null);
	}
	
	@Override
	public Path getFileName() {
		return this;
	}
	
	@Override
	public Path getParent() {
		return new EFSPath(efs, null);
	}
	
	@Override
	public int getNameCount() {
		return 1;
	}
	
	@Override
	public Path getName(int index) {
		return index == 0 ? getFileName() : null;
	}
	
	@Override
	public Path subpath(int beginIndex, int endIndex) {
		return this;
	}
	
	@Override
	public boolean startsWith(Path other) {
		return false;
	}
	
	@Override
	public boolean startsWith(String other) {
		return false;
	}
	
	@Override
	public boolean endsWith(Path other) {
		return false;
	}
	
	@Override
	public boolean endsWith(String other) {
		return false;
	}
	
	@Override
	public Path normalize() {
		return this;
	}
	
	@Override
	public Path resolve(Path other) {
		return other == null ? this : other;
	}
	
	@Override
	public Path resolve(String other) {
		return other == null ? this : resolve(Paths.get(other));
	}
	
	@Override
	public Path resolveSibling(Path other) {
		return other == null ? getParent() : other;
	}
	
	@Override
	public Path resolveSibling(String other) {
		return other == null ? getParent() : Paths.get(other);
	}
	
	@Override
	public Path relativize(Path other) {
		return other;
	}
	
	@Override
	public URI toUri() {
		return URI.create(efs.provider().getScheme() + ":" + fragment + "#" + efs.id);
	}
	
	@Override
	public Path toAbsolutePath() {
		return this;
	}
	
	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		return this;
	}
	
	@Override
	public File toFile() {
		try {
			Path tmp = Files.createTempFile(efs.provider().getScheme(), fragment);
			InputStream in;
			Files.copy(in = efs.provider().newInputStream(this), tmp);
			in.close();
			return tmp.toFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Iterator<Path> iterator() {
		return Arrays.asList((Path) this).iterator();
	}
	
	@Override
	public int compareTo(Path other) {
		return this.toString().compareTo(other.toString());
	}
	
	@Override
	public String toString() {
		return toUri().toString();
	}
}
