package utils;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public interface FileIterator {

	public static FileIterator getIterator(String path, List<String> searchStrings, boolean shouldContain){
		
		File f = new File(path);
		
		if(new File(path+".jar").exists())
			return new ZipIterator(path+".jar");
	
		if(new File(path+".zip").exists())
			return new ZipIterator(path+".zip");
		
		if(new File(path+".tar").exists())
			return new TarIterator(path+".tar");
		
		if(new File(path+".tar.gz").exists())
			return new TarIterator(path+".tar.gz");
		
		if(f.isDirectory())
			return new DirectoryIterator(path, searchStrings, shouldContain);
		
		return null;
	}
	
	public InputStream nextStream();
	
	public boolean close();
	
}
