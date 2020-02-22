package xyz.lucasallegri.spiralview;

import java.io.File;
import javax.swing.JOptionPane;
import com.threerings.util.ResourceUtil;

public class ViewApp implements Runnable {
	
	private static final String VERSION = "1.0";
	private static final String JVM_PATH = "java_vm" + File.separator + "bin" + File.separator + "java";
	private static String targetClass = "xyz.lucasallegri.spiralview.ModelViewerHook";
	
	public static void main(String[] args) throws Exception {
		new Thread(new ViewApp()).start();
	}
	
	public void run() {
		verifyRunningDirectory();
		
		File rsrcdir = new File(new File(System.getProperty("user.dir")), "rsrc");
		ResourceUtil.setPreferredResourceDir(rsrcdir.getAbsolutePath());
		
		String psep = System.getProperty("path.separator");
		String classPath = "spiralview-" + VERSION + ".jar" + psep + "code" + File.separator + "lwjgl.jar" + psep + "code" + File.separator + "projectx-config.jar" + psep + "code" + File.separator + "projectx-pcode.jar" + psep + ".";
		
		String[] command = new String[] { JVM_PATH, "-Djava.library.path=native", "-cp", classPath, targetClass, "rsrc" + File.separator + "character" + File.separator + "pc" + File.separator + "model.dat" };
	    
	    try {
			ProcessBuilder builder = new ProcessBuilder(command);
			builder.start();
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	}
	
	private static void verifyRunningDirectory() {
		if (!new File("getdown.txt").exists()) {
			JOptionPane.showMessageDialog(null, "You need to place this .jar inside your Spiral Knights main directory.");
			System.exit(1);
		}
	}

}
