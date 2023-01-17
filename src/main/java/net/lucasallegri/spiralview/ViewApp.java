package net.lucasallegri.spiralview;

import java.awt.EventQueue;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import javax.swing.JOptionPane;
import com.threerings.util.ResourceUtil;
import org.apache.commons.io.IOUtils;

import net.lucasallegri.util.*;
import org.apache.commons.lang3.exception.ExceptionUtils;

import static net.lucasallegri.spiralview.Log.log;

public class ViewApp implements Runnable {

  private static final String VERSION = "1.8.1";
  private static final String USER_DIR = System.getProperty("user.dir");
  private static String _targetClass = "net.lucasallegri.spiralview.ModelViewerHook";
  private int _chosen = 0;

  public static void main(String[] args) {
    new Thread(new ViewApp()).start();
  }

  public void run() {
    setupFileLogging();
    logVMInfo();

    if(!isRunningInRootFolder()) pushWarning("You need to place this .jar inside your Spiral Knights main directory.");
    if(!hasCleanConfigs()) pushError("There are .xml files in your rsrc/config directory, spiralview can not proceed.");

    try {
      EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          _chosen = chooseEditorDialog();
        }
      });
    } catch (InvocationTargetException | InterruptedException e) {
      log.error(ExceptionUtils.getStackTrace(e));
    }

    File rsrcDir = new File(new File(USER_DIR), "rsrc");
    ResourceUtil.setPreferredResourceDir(rsrcDir.getAbsolutePath());

    resolveTargetedClass(_chosen);
    String[] output = runAndCapture(createRuntimeCommand(_targetClass, "java"));
    log.info(output[0]);
    log.info(output[1]);
  }

  private static boolean isRunningInRootFolder() {
    return new File("getdown-pro.jar").exists();
  }

  private static boolean hasCleanConfigs() {
    return FileUtil.fileNamesInDirectory(USER_DIR + "\\rsrc\\config", ".xml").size() <= 0;
  }

  private static void pushError(String message) {
    JOptionPane.showMessageDialog(null, message);
    log.error(message);
    System.exit(1);
  }

  private static void pushWarning(String message) {
    JOptionPane.showMessageDialog(null, message);
    log.warning(message);
  }

  private static int chooseEditorDialog() {
    Object[] editors = { "Model Viewer", "Level Editor", "Interface Tester", "Particle Editor" };

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

  private String[] createRuntimeCommand(String targetClass, String javaVMPath) {
    //String javaVMPath = SystemUtil.isWindows() ? ".\\java_vm\\bin\\java.exe" : "./java/bin/java";
    String libSeparator = SystemUtil.isWindows() ? ";" : ":";

    return new String[] {
        javaVMPath,
        "-classpath",
        USER_DIR + File.separator + "./spiralview.jar" + libSeparator +
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
      log.error(ExceptionUtils.getStackTrace(e));
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
      log.error(ExceptionUtils.getStackTrace(e));
    } finally {

      // No need to keep the process active.
      if(process != null) process.destroy();
    }
    return new String[1];
  }

  private static void setupFileLogging() {
    File logFile = new File("spiralview.log");
    File oldLogFile = new File("old-spiralview.log");

    if (logFile.exists()) {
      logFile.renameTo(oldLogFile);
    }

    try {
      PrintStream printStream = new PrintStream(new BufferedOutputStream(Files.newOutputStream(logFile.toPath())), true);
      System.setOut(printStream);
      System.setErr(printStream);
    } catch (IOException e) {
      log.error(ExceptionUtils.getStackTrace(e));
    }
  }

  private void logVMInfo() {
    log.info("------------ VM Info ------------");
    log.info("OS Name: " + System.getProperty("os.name"));
    log.info("OS Arch: " + System.getProperty("os.arch"));
    log.info("OS Vers: " + System.getProperty("os.version"));
    log.info("Java Home: " + System.getProperty("java.home"));
    log.info("Java Vers: " + System.getProperty("java.version"));
    log.info("User Name: " + System.getProperty("user.name"));
    log.info("User Home: " + System.getProperty("user.home"));
    log.info("Current Directory: " + System.getProperty("user.dir"));
    log.info("---------------------------------");
  }

}
