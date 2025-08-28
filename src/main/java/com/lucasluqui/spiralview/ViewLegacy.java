package com.lucasluqui.spiralview;

import com.lucasluqui.util.FileUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class ViewLegacy {

  private static String _targetClass = "null";
  private static int _chosen = 0;

  public static void start() {
    if(!isRunningInRootFolder()) ViewApp.pushWarning("You need to place this .jar inside your Spiral Knights main directory. In some cases this is just a false positive and this message can be ignored.");
    if(!hasCleanConfigs()) ViewApp.pushError("There are .xml files in your rsrc/config directory, spiralview can not proceed.");

    try {
      EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          _chosen = chooseEditorDialog();
        }
      });
    } catch (InvocationTargetException | InterruptedException e) {
      Log.log.error(ExceptionUtils.getStackTrace(e));
    }

    resolveTargetedClass(_chosen);

    // No editor was chosen (possibly forcibly closed) hence no need to keep the program alive.
    if(_targetClass.equalsIgnoreCase("null")) System.exit(1);

    String[] output = ViewApp.runAndCapture(ViewApp.createRuntimeCommand(_targetClass, "java", _chosen));
    Log.log.info(output[0]);
    Log.log.info(output[1]);
  }

  private static boolean isRunningInRootFolder() {
    return new File("getdown-pro.jar").exists();
  }

  private static boolean hasCleanConfigs() {
    return FileUtil.fileNamesInDirectory(Globals.USER_DIR + "\\rsrc\\config", ".xml").size() <= 0;
  }

  private static int chooseEditorDialog() {
    Object[] editors = { "Model Viewer", "Level Editor", "Interface Tester", "Particle Editor" };

    int n = JOptionPane.showOptionDialog(null,
      "Choose an editor to start",
      "spiralview (" + Globals.VERSION + ")",
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
        _targetClass = "com.lucasluqui.spiralview.ModelViewerHook";
        break;
      case 1:
        _targetClass = "com.lucasluqui.spiralview.SceneEditorHook";
        break;
      case 2:
        _targetClass = "com.lucasluqui.spiralview.InterfaceTesterHook";
        break;
      case 3:
        _targetClass = "com.lucasluqui.spiralview.ParticleEditorHook";
        break;
    }
  }

}
