package xyz.lucasallegri.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
	
	public static List<String> fileNamesInDirectory(String dir, String ext) {
		
		File folder = new File(dir);
		File[] fileList = folder.listFiles();
		List<String> fileNames = new ArrayList<String>();
		
		for(int i = 0; i < fileList.length; i++) {
			if(fileList[i].isDirectory() == false && fileList[i].toString().contains(ext)) { 
				fileNames.add(fileList[i].getName()); 
			}
		}
		
		return fileNames;
	}

}
