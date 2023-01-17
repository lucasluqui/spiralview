package net.lucasallegri.spiralview;

import com.samskivert.util.Logger;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

/**
 * Contains a reference to the {@link Logger} object used by the launcher package.
 */
@SuppressWarnings("unused")
public class Log {

  /**
   * The {@link Logger} reference.
   */
  public static Logger log = Logger.getLogger("net.lucasallegri.spiralview");

  public static void setupFileLogging() {
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
}