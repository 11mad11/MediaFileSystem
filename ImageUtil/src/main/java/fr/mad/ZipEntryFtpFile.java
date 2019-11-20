package fr.mad;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.ftpserver.ftplet.FtpFile;

public class ZipEntryFtpFile implements FtpFile {
	
	public ZipEntryFtpFile(ZipArchiveEntry e) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getAbsolutePath() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isHidden() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isDirectory() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isFile() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean doesExist() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isReadable() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isWritable() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isRemovable() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public String getOwnerName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getGroupName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int getLinkCount() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public long getLastModified() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public boolean setLastModified(long time) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public long getSize() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public Object getPhysicalFile() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean mkdir() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean delete() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean move(FtpFile destination) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public List<? extends FtpFile> listFiles() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public OutputStream createOutputStream(long offset) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public InputStream createInputStream(long offset) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
}
