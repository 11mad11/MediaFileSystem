package fr.mad.ImageUtil.efs;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import fr.mad.ImageUtil.MapObjectHelper;

public class EFS extends FileSystem {
	public final String id;
	
	final BlockHelper blockHelper;
	final MapObjectHelper env;
	final EFSProvider provider;
	final boolean readOnly;
	final FAT fat;
	
	private SeekableByteChannel sbc;
	
	public EFS(EFSProvider provider, SeekableByteChannel sbc, boolean readOnly, String id, Map<String, ?> map) throws IOException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		this.provider = provider;
		this.readOnly = readOnly;
		this.id = id;
		this.sbc = sbc;
		env = new MapObjectHelper(map);
		
		if (env.get("blockSize", 1024 * 512) < 512)
			throw new IllegalArgumentException("blockSize < 512");
		
		blockHelper = new BlockHelper(env, sbc, (short) 1024, env.get("encryptMetaData", true), readOnly);
		fat = new FAT(env, blockHelper);
	}
	
	
	
	@Override
	public FileSystemProvider provider() {
		return provider;
	}
	
	@Override
	public void close() throws IOException {
		blockHelper.close();
	}
	
	@Override
	public boolean isOpen() {
		return blockHelper.isOpen();
	}
	
	@Override
	public boolean isReadOnly() {
		return readOnly;
	}
	
	@Override
	public String getSeparator() {
		return ", ";
	}
	
	@Override
	public Iterable<Path> getRootDirectories() {
		return Arrays.asList(new EFSPath(this, null));
	}
	
	@Override
	public Iterable<FileStore> getFileStores() {
		return Arrays.asList(new FileStore() {
			
			@Override
			public String name() {
				return null;
			}
			
			@Override
			public String type() {
				return null;
			}
			
			@Override
			public boolean isReadOnly() {
				return readOnly;
			}
			
			@Override
			public long getTotalSpace() throws IOException {
				return sbc.size();
			}
			
			@Override
			public long getUsableSpace() throws IOException {
				return 0;
			}
			
			@Override
			public long getUnallocatedSpace() throws IOException {
				return 0;
			}
			
			@Override
			public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
				return false;
			}
			
			@Override
			public boolean supportsFileAttributeView(String name) {
				return false;
			}
			
			@Override
			public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
				return null;
			}
			
			@Override
			public Object getAttribute(String attribute) throws IOException {
				return null;
			}
		});
	}
	
	@Override
	public Set<String> supportedFileAttributeViews() {
		return null;
	}
	
	@Override
	public Path getPath(String first, String... more) {
		if (more == null)
			return new EFSPath(this, first);
		return new EFSPath(this, new StringBuilder(first).append(Stream.of(more).collect(Collectors.joining())).toString());
	}
	
	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		int colonIndex = syntaxAndPattern.indexOf(':');
		if (colonIndex <= 0 || colonIndex == syntaxAndPattern.length() - 1) {
			throw new IllegalArgumentException("syntaxAndPattern must have form \"syntax:pattern\" but was \"" + syntaxAndPattern + "\"");
		}
		
		String syntax = syntaxAndPattern.substring(0, colonIndex);
		String pattern = syntaxAndPattern.substring(colonIndex + 1);
		String expr;
		switch (syntax) {
			case "glob":
				expr = globToRegex(pattern);
				break;
			case "regex":
				expr = pattern;
				break;
			default:
				throw new UnsupportedOperationException("Unsupported syntax \'" + syntax + "\'");
		}
		final Pattern regex = Pattern.compile(expr);
		return new PathMatcher() {
			@Override
			public boolean matches(Path path) {
				return regex.matcher(path.toString()).matches();
			}
		};
	}
	
	private String globToRegex(String pattern) {
		StringBuilder sb = new StringBuilder(pattern.length());
		int inGroup = 0;
		int inClass = 0;
		int firstIndexInClass = -1;
		char[] arr = pattern.toCharArray();
		for (int i = 0; i < arr.length; i++) {
			char ch = arr[i];
			switch (ch) {
				case '\\':
					if (++i >= arr.length) {
						sb.append('\\');
					} else {
						char next = arr[i];
						switch (next) {
							case ',':
								// escape not needed
								break;
							case 'Q':
							case 'E':
								// extra escape needed
								sb.append('\\');
							default:
								sb.append('\\');
						}
						sb.append(next);
					}
					break;
				case '*':
					if (inClass == 0)
						sb.append(".*");
					else
						sb.append('*');
					break;
				case '?':
					if (inClass == 0)
						sb.append('.');
					else
						sb.append('?');
					break;
				case '[':
					inClass++;
					firstIndexInClass = i + 1;
					sb.append('[');
					break;
				case ']':
					inClass--;
					sb.append(']');
					break;
				case '.':
				case '(':
				case ')':
				case '+':
				case '|':
				case '^':
				case '$':
				case '@':
				case '%':
					if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
						sb.append('\\');
					sb.append(ch);
					break;
				case '!':
					if (firstIndexInClass == i)
						sb.append('^');
					else
						sb.append('!');
					break;
				case '{':
					inGroup++;
					sb.append('(');
					break;
				case '}':
					inGroup--;
					sb.append(')');
					break;
				case ',':
					if (inGroup > 0)
						sb.append('|');
					else
						sb.append(',');
					break;
				default:
					sb.append(ch);
			}
		}
		return sb.toString();
	}
	
	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public WatchService newWatchService() throws IOException {
		throw new UnsupportedOperationException();//TODO watchService
	}
	
}
