package com.lucasluqui.spiralview;

import com.formdev.flatlaf.FlatDarkLaf;

import java.awt.GraphicsEnvironment;

import javax.swing.*;

public class ViewKL
{
  private static String _targetClass = null;

  public static void start (String editor, String rsrc)
  {
    resolveEditor(editor);

    ViewApp.run(ViewApp.createRuntimeCommand(
      _targetClass, "java", editor.equalsIgnoreCase("model") ? 0 : -1), true);
    System.exit(1);
  }

  public static void setupLAF ()
  {
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");

    try {
      UIManager.setLookAndFeel(new FlatDarkLaf());
    } catch (UnsupportedLookAndFeelException e) {
      Log.log.error(e);
    }

    Stylesheet.load();
  }

  /**
   * Maximizes an editor's window. Fills the screen, minus the task bar, first: those bounds also
   * keep the window on screen when it is restored from maximized.
   */
  public static void maximize (JFrame frame)
  {
    frame.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
    frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
  }

  private static void resolveEditor (String editor)
  {
    switch (editor) {
      case "model":
        _targetClass = "com.lucasluqui.spiralview.ModelViewerHook";
        break;
      case "scene":
        _targetClass = "com.lucasluqui.spiralview.SceneEditorHook";
        break;
      case "interface":
        _targetClass = "com.lucasluqui.spiralview.InterfaceTesterHook";
        break;
      case "particle":
        _targetClass = "com.lucasluqui.spiralview.ParticleEditorHook";
        break;
      case "silverweave":
        _targetClass = "com.lucasluqui.silverweave.SilverweaveTool";
        break;
    }
  }
}
