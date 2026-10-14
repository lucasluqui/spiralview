package com.lucasluqui.silverweave;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.lucasluqui.util.SystemUtil;

import static com.lucasluqui.silverweave.Log.log;

/**
 * Finds the highest multisample antialiasing level the graphics driver offers for a window.
 *
 * <p>The canvas fails to start outright if asked for a sample count the driver doesn't offer, so
 * rather than guessing, a hidden window is created asking for far more samples than any driver
 * supports; GLFW settles for the closest available format, whose sample count is the maximum.
 */
public final class Antialiasing
{
  /** The level used when the maximum can't be determined. */
  public static final int FALLBACK = 4;

  /**
   * Returns the maximum supported sample count, probing the driver the first time.
   */
  public static synchronized int getMaxSamples ()
  {
    if (_maxSamples < 0) {
      _maxSamples = probe();
      log.info("Antialiasing level.", "samples", _maxSamples);
    }
    return _maxSamples;
  }

  protected static int probe ()
  {
    // GLFW must run on the main thread on macOS, where AWT already owns it
    if (SystemUtil.isMac()) {
      return FALLBACK;
    }
    try {
      if (!GLFW.glfwInit()) {
        return FALLBACK;
      }
      try {
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 64);
        long window = GLFW.glfwCreateWindow(16, 16, "", 0L, 0L);
        if (window == 0L) {
          return FALLBACK;
        }
        try {
          GLFW.glfwMakeContextCurrent(window);
          GL.createCapabilities();
          int samples = GL11.glGetInteger(GL13.GL_SAMPLES);
          return samples > 0 ? samples : 0;
        } finally {
          GL.setCapabilities(null);
          GLFW.glfwMakeContextCurrent(0L);
          GLFW.glfwDestroyWindow(window);
        }
      } finally {
        GLFW.glfwTerminate();
      }
    } catch (Throwable t) {
      log.warning("Failed to probe the antialiasing level.", t);
      return FALLBACK;
    }
  }

  protected static int _maxSamples = -1;

  private Antialiasing () { }
}
