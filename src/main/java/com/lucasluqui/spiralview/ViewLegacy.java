package com.lucasluqui.spiralview;

import com.lucasluqui.util.FileUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import static com.lucasluqui.spiralview.Log.log;

public class ViewLegacy
{
  private static String targetClass = "null";
  private static int chosen = 0;
  private static final String USER_DIR = System.getProperty("user.dir");

  public static void start ()
  {
    if (!isRunningInRootFolder())
      ViewApp.pushWarning("You need to place this .jar inside your Spiral Knights main directory. In some cases this is just a false positive and this message can be ignored.");
    if (!hasCleanConfigs())
      ViewApp.pushError("There are .xml files in your rsrc/config directory, spiralview can not proceed.");

    try {
      EventQueue.invokeAndWait((Runnable) () -> chosen = chooseEditorDialog());
    } catch (InvocationTargetException | InterruptedException e) {
      log.error(ExceptionUtils.getStackTrace(e));
    }

    resolveTargetedClass(chosen);

    // No editor was chosen (possibly forcibly closed) hence no need to keep the program alive.
    if (targetClass.equalsIgnoreCase("null")) System.exit(1);

    String[] output = ViewApp.runAndCapture(ViewApp.createRuntimeCommand(targetClass, "java", chosen));
    log.info(output[0]);
    log.info(output[1]);
  }

  private static boolean isRunningInRootFolder ()
  {
    return new File("getdown-pro.jar").exists();
  }

  private static boolean hasCleanConfigs ()
  {
    return FileUtil.fileNamesInDirectory(USER_DIR + "\\rsrc\\config", ".xml").size() <= 0;
  }

  private static int chooseEditorDialog ()
  {
    Object[] editors = {"Model Viewer", "Level Editor", "Interface Tester", "Particle Editor"};

    int n = JOptionPane.showOptionDialog(null,
      "Choose an editor to start",
      "spiralview (" + BuildConfig.getVersion() + ")",
      JOptionPane.DEFAULT_OPTION,
      JOptionPane.QUESTION_MESSAGE,
      null,
      editors,
      null);

    return n;
  }

  private static void resolveTargetedClass (int chosen)
  {
    switch (chosen) {
      case 0:
        targetClass = "com.lucasluqui.spiralview.ModelViewerHook";
        break;
      case 1:
        targetClass = "com.lucasluqui.spiralview.SceneEditorHook";
        break;
      case 2:
        targetClass = "com.lucasluqui.spiralview.InterfaceTesterHook";
        break;
      case 3:
        targetClass = "com.lucasluqui.spiralview.ParticleEditorHook";
        break;
    }
  }
}
