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

	public DirectoryIterator(String path, List<String> searchStrings, boolean shouldContain) {
		dir = new File(path);
		index = 0;

		files = listDirectory(path, searchStrings, shouldContain);
	
	}

	private List<File> listDirectory(String path, List<String> searchStrings, boolean shouldContain) {
		List<File> files = new ArrayList<>();
		File d = new File(path);
		for (File f : d.listFiles()) {
			if (f.isDirectory()) {
				files.addAll(listDirectory(f.getAbsolutePath(), searchStrings, shouldContain));
			} else if (searchStrings.isEmpty()) {
				// If no search strings are specified, all files are included
				if (f.getName().endsWith(".java")) {
					files.add(f);
				}
			} else {
				// If search strings are specified, include or exclude files based on the flag
				if (f.getName().endsWith(".java") && containsSearchString(f.getAbsolutePath(), searchStrings, shouldContain)) {
					files.add(f);
				}
			}
		}
		return files;
	}
	

    // Determine if the specified string is included in the file path (to detect test code)
	private boolean containsSearchString(String filePath, List<String> searchStrings, boolean shouldContain) {
		boolean contains = false;
		for (String searchString : searchStrings) {
			if (filePath.contains(searchString)) {
				contains = true;
				break;
			}
		}
		return shouldContain ? contains : !contains;
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

	public String getCurrentFilePath() {
		if (index - 1 < files.size()) {
			return files.get(index - 1).getAbsolutePath();
		}
		return null;
	}
}
