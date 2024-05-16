package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DirectoryIterator implements FileIterator {

	File dir;
	List<File> files;
	int index;

	public DirectoryIterator(String path) {
		dir = new File(path);
		index = 0;

		files = listDirectory(path);
	
	}

	private List<File> listDirectory(String path) {
		List<File> files = new ArrayList<>();
		File d = new File(path);
		for(File f : d.listFiles()){
			if(f.isDirectory())
				files.addAll(listDirectory(f.getAbsolutePath()));
			else{
				if(f.getName().endsWith(".java") && !f.getAbsolutePath().contains(File.separator+"test"+File.separator))
					files.add(f);
			}
		}
		
		return files;
	}

	@Override
	public InputStream nextStream() {
		
		InputStream is = null;
		
		if(index < files.size()){
			try {
				is = new FileInputStream(files.get(index));
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			index++;
		}
		
		return is;
	}

	@Override
	public boolean close() {
		files.clear();
		files = null;
		dir = null;

		return true;
	}

    public String getCurrentFileName() {
        if (index - 1 < files.size()) {
            return files.get(index - 1).getName();
        }
        return null;
    }
}
