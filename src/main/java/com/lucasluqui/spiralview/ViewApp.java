package com.lucasluqui.spiralview;

import com.lucasluqui.util.SystemUtil;
import com.lucasluqui.util.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

import javax.swing.JOptionPane;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import static com.lucasluqui.spiralview.Log.log;

public class ViewApp implements Runnable
{
  private String[] args = null;
  private static final String USER_DIR = System.getProperty("user.dir");

  public ViewApp (String... args)
  {
    this.args = args;
    run();
  }

  public static void main (String[] args)
  {
    new Thread(new ViewApp(args)).start();
  }

  public void run ()
  {
    setupFileLogging();
    logVMInfo();
    log.info("Running version " + BuildConfig.getVersion());

    if (this.args.length > 0 && this.args[0].equalsIgnoreCase("kl")) {
      // kl boot
      ViewKL.start(args[1], args[2]);
    } else {
      // legacy boot
      ViewLegacy.start();
    }
  }

  protected static void pushError (String message)
  {
    JOptionPane.showMessageDialog(null, message);
    log.error(message);
    System.exit(1);
  }

  protected static void pushWarning (String message)
  {
    JOptionPane.showMessageDialog(null, message);
    log.warning(message);
  }

  protected static String[] createRuntimeCommand (String targetClass, String javaVMPath, int chosen)
  {
    String gameJavaVMPath = SystemUtil.isWindows() ? USER_DIR + File.separator + "java_vm\\bin\\java.exe" : USER_DIR + File.separator + "java/bin/java";
    String gameJavaVMVersion = runAndCapture(new String[]{gameJavaVMPath, "-version"})[1];

    if (gameJavaVMVersion.contains("1.7") || gameJavaVMVersion.contains("1.8")) {
      log.info("Compatible game Java VM version found: " + gameJavaVMVersion);
      javaVMPath = gameJavaVMPath;
    } else if (System.getProperty("java.version").contains("1.7") || System.getProperty("java.version").contains("1.8")) {
      log.warning("Incompatible game Java VM version: " + gameJavaVMVersion + ". Luckily we can rely on system's (" + System.getProperty("java.version") + ")");
    } else {
      pushError("We have no compatible Java VM to work with, goodbye.");
    }

    String libSeparator = SystemUtil.isWindows() ? ";" : ":";

    return new String[]{
      javaVMPath,
      "-classpath",
      USER_DIR + File.separator + "./spiralview.jar" + libSeparator +
        USER_DIR + File.separator + "./code/projectx-config.jar" + libSeparator +
        USER_DIR + File.separator + "./code/projectx-pcode.jar" + libSeparator +
        USER_DIR + File.separator + "./code/lwjgl_util.jar" + libSeparator +
        USER_DIR + File.separator + "./code/lwjgl.jar",
      "-Xms3G",
      "-Xmx3G",
      "-Dappdir=" + USER_DIR + File.separator + "./",
      "-Dresource_dir=" + USER_DIR + File.separator + "./rsrc",
      "-Djava.library.path=" + USER_DIR + File.separator + "./native",
      targetClass,
      chosen == 0 ? "rsrc\\character\\pc\\model.dat" : ""
    };
  }

  protected static void run (String[] command, boolean keepAlive)
  {
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

  protected static String[] runAndCapture (String[] command)
  {
    Process process = null;
    try {
      process = new ProcessBuilder(command).start();

      // Capture both streams and send them back. Ideally we'd only pass stdout but Java likes
      // sending important stuff through stderr like -version.
      String stdout = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
      String stderr = IOUtils.toString(process.getErrorStream(), Charset.defaultCharset());
      return new String[]{stdout, stderr};
    } catch (IOException e) {
      log.error(ExceptionUtils.getStackTrace(e));
    } finally {

      // No need to keep the process active.
      if (process != null) process.destroy();
    }
    return new String[1];
  }

  private static void setupFileLogging ()
  {
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

  private void logVMInfo ()
  {
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
