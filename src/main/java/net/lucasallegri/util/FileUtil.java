package net.lucasallegri.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

  public static List<String> fileNamesInDirectory(String dir, String ext) {

    File folder = new File(dir);
    File[] fileList = folder.listFiles();
    List<String> fileNames = new ArrayList<String>();

    // The file list is null, and so we just return the empty list.
    if(fileList == null) return fileNames;

    for (File file : fileList) {
      if (!file.isDirectory() && file.toString().contains(ext)) {
        fileNames.add(file.getName());
      }
    }

    return fileNames;
  }

}
