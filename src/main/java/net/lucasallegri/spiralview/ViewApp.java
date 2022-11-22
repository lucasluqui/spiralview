package net.lucasallegri.spiralview;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;

import javax.swing.JOptionPane;
import com.threerings.util.ResourceUtil;
import org.apache.commons.io.IOUtils;

import net.lucasallegri.util.*;

public class ViewApp implements Runnable {
	
	private static final String VERSION = "1.8";
	private static final String USER_DIR = System.getProperty("user.dir");
	private static String _targetClass = "net.lucasallegri.spiralview.ModelViewerHook";
	private int _chosen = 0;
	
	public static void main(String[] args) {
		new Thread(new ViewApp()).start();
	}
	
	public void run() {
		if(!isRunningInRootFolder()) pushWarning("You need to place this .jar inside your Spiral Knights main directory.");
		if(!hasCleanConfigs()) pushError("There are .xml files in your rsrc/config directory, spiralview can not proceed.");
		
		try {
			EventQueue.invokeAndWait(new Runnable() {
				public void run() {
					_chosen = chooseEditorDialog();
				}	
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}

		File rsrcDir = new File(new File(USER_DIR), "rsrc");
		ResourceUtil.setPreferredResourceDir(rsrcDir.getAbsolutePath());

		resolveTargetedClass(_chosen);
		run(createRuntimeCommand(_targetClass), true);
	}
	
	private static boolean isRunningInRootFolder() {
		if (new File("getdown-pro.jar").exists()) {
			return true;
		}
		return false;
	}
	
	private static boolean hasCleanConfigs() {
		if(FileUtil.fileNamesInDirectory(USER_DIR + "\\rsrc\\config", ".xml").size() > 0) {
			return false;
		}
		return true;
	}
	
	private static void pushError(String message) {
		JOptionPane.showMessageDialog(null, message);
		System.exit(1);
	}

	private static void pushWarning(String message) {
		JOptionPane.showMessageDialog(null, message);
	}
	
	private static int chooseEditorDialog() {
		Object[] editors = {"Model Viewer", "Level Editor", "Interface Tester", "Particle Editor"};

		int n = JOptionPane.showOptionDialog(null,
		    "Choose an editor to start",
		    "spiralview (" + VERSION + ")",
		    JOptionPane.DEFAULT_OPTION,
		    JOptionPane.QUESTION_MESSAGE,
		    null,
		    editors,
		    null);

		return n;
	}
	
	private static void resolveTargetedClass(int clazz) {
		switch(clazz) {
		case 0:
			break;
		case 1:
			_targetClass = "net.lucasallegri.spiralview.SceneEditorHook";
			break;
		case 2:
			_targetClass = "net.lucasallegri.spiralview.InterfaceTesterHook";
			break;
		case 3:
			_targetClass = "net.lucasallegri.spiralview.ParticleEditorHook";
			break;
		}
	}

	private String[] createRuntimeCommand(String targetClass) {
		String javaVMPath = SystemUtil.isWindows() ? ".\\java_vm\\bin\\java.exe" : "./java/bin/java";
		String libSeparator = SystemUtil.isWindows() ? ";" : ":";

		return new String[] {
				javaVMPath,
				"-classpath",
				USER_DIR + File.separator + "./spiralview-" + VERSION + ".jar" + libSeparator +
						USER_DIR + File.separator + "./code/projectx-config.jar" + libSeparator +
						USER_DIR + File.separator + "./code/projectx-pcode.jar" + libSeparator +
						USER_DIR + File.separator + "./code/lwjgl.jar",
				"-Xms1G",
				"-Xmx2G",
				"-Dappdir=" + USER_DIR + File.separator + "./",
				"-Dresource_dir=" + USER_DIR + File.separator + "./rsrc",
				"-Djava.library.path=" + USER_DIR + File.separator + "./native",
				targetClass,
				_chosen == 0 ? "rsrc\\character\\pc\\model.dat" : ""
		};
	}

	private void run(String[] command, boolean keepAlive) {
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			// No need to keep the process alive.
			if (process != null && !keepAlive) process.destroy();
		}
	}

	private String[] runAndCapture(String[] command) {
		Process process = null;
		try {
			process = new ProcessBuilder(command).start();

			// Capture both streams and send them back. Ideally we'd only pass stdout but Java likes
			// sending important stuff through stderr like -version.
			String stdout = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
			String stderr = IOUtils.toString(process.getErrorStream(), Charset.defaultCharset());
			return new String[] {stdout, stderr};
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			// No need to keep the process active.
			if(process != null) process.destroy();
		}
		return new String[1];
	}

}
