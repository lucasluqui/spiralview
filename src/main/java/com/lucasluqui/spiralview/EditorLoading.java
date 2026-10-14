package com.lucasluqui.spiralview;

import java.awt.Window;

import com.lucasluqui.silverweave.ui.LoadingScreen;
import com.lucasluqui.util.ImageUtil;

/**
 * Shows a loading screen while one of the editors starts up. Each editor runs in a
 * process of its own, so there's only ever the one screen to keep track of.
 */
public final class EditorLoading
{
  /**
   * Puts the loading screen up, before the editor is created.
   */
  public static void show (String title)
  {
    _screen = new LoadingScreen(ImageUtil.loadImageWithinJar("/img/icon.png"), title, "Loading...");
    _screen.show();
  }

  /**
   * Takes the loading screen down once the editor is up, bringing its window to the front.
   */
  public static void dismiss (Window editor)
  {
    if (_screen != null) {
      _screen.dismiss(GRACE, editor);
      _screen = null;
    }
  }

  /** How long the loading screen stays up after the editor is in, in milliseconds. */
  protected static final int GRACE = 500;

  /** Shown while the editor starts up, or null once it's gone. */
  protected static LoadingScreen _screen;

  private EditorLoading () { }
}
