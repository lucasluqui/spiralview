package com.lucasluqui.silverweave;

import java.awt.*;
import java.util.concurrent.TimeUnit;

/**
 * Paces the frames of a tool's 3D view to the display's refresh rate.
 *
 * <p>clyde draws frames back to back on the event thread. With vertical sync, the graphics driver
 * then holds each frame until the display refreshes, and some drivers (NVIDIA's on Linux, for one)
 * wait by spinning, which keeps a whole core busy. clyde's own limiter, a 16 ms sleep per frame,
 * avoids that but misses every other refresh, halving the frame rate. This sleeps instead until just
 * before the frame's refresh is due, so the driver is left almost nothing to wait for and no refresh
 * is missed. Without vertical sync, it keeps the frame rate to about the refresh rate.
 *
 * <p>While the window is hidden or minimized, frames are slowed right down, since nothing is seen.
 */
public class FrameLimiter
{
  public FrameLimiter (Window window)
  {
    _window = window;
  }

  /**
   * Notes the start of a frame, first resting a while if the window isn't being looked at. Call at
   * the start of each frame's update, whether or not the frame is drawn.
   */
  public void frameStarted ()
  {
    if (!isVisible()) {
      sleepUntil(_frameStart + HIDDEN_PERIOD);
    }
    // with vertical sync, the previous frame was held until the display refreshed, so this is
    // just after a refresh
    _frameStart = System.nanoTime();
  }

  /**
   * Waits until shortly before the display's next refresh, measured from the start of the frame.
   * Call once the frame is drawn, just before the buffers are swapped.
   */
  public void beforeSwap ()
  {
    sleepUntil(_frameStart + getPeriod() - SWAP_MARGIN);
  }

  /**
   * Checks whether the window can be seen: showing and not minimized.
   */
  protected boolean isVisible ()
  {
    return _window.isShowing() && !(_window instanceof Frame &&
      (((Frame)_window).getExtendedState() & Frame.ICONIFIED) != 0);
  }

  /**
   * Returns the time between refreshes of the display the window is on, in nanoseconds. It's
   * looked up again every so often, as the window may be moved to another display.
   */
  protected long getPeriod ()
  {
    // getDisplayMode() makes X re-probe the outputs (~100 ms), so only ask again when the
    // window moves to a different screen
    GraphicsConfiguration config = _window.getGraphicsConfiguration();
    GraphicsDevice device = (config == null) ? null : config.getDevice();
    if (_period == 0L || device != _device) {
      _device = device;
      int rate = DEFAULT_REFRESH_RATE;
      DisplayMode mode = (device == null) ? null : device.getDisplayMode();
      if (mode != null && mode.getRefreshRate() != DisplayMode.REFRESH_RATE_UNKNOWN) {
        rate = Math.max(MIN_REFRESH_RATE, Math.min(MAX_REFRESH_RATE, mode.getRefreshRate()));
      }
      _period = TimeUnit.SECONDS.toNanos(1) / rate;
    }
    return _period;
  }

  /**
   * Sleeps until the given {@link System#nanoTime} deadline, if it's still ahead.
   */
  protected static void sleepUntil (long deadline)
  {
    long wait = deadline - System.nanoTime();
    if (wait <= 0) {
      return;
    }
    try {
      Thread.sleep(wait / 1_000_000L, (int)(wait % 1_000_000L));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** How long before the refresh is due the frame is let go, leaving room for the swap and for
   * sleeping a little long, in nanoseconds. Letting it go after the refresh would miss it. */
  protected static final long SWAP_MARGIN = TimeUnit.MICROSECONDS.toNanos(2000);

  /** The time between frames while the window can't be seen, in nanoseconds. */
  protected static final long HIDDEN_PERIOD = TimeUnit.MILLISECONDS.toNanos(100);

  /** The refresh rate assumed when the display doesn't report one, and the range the reported
   * rate is kept within. */
  protected static final int DEFAULT_REFRESH_RATE = 60, MIN_REFRESH_RATE = 30,
    MAX_REFRESH_RATE = 360;

  protected final Window _window;

  /** When the current frame started, from {@link System#nanoTime}. */
  protected long _frameStart;

  /** The time between refreshes, and when to look it up again, in nanoseconds. */
  protected long _period;

  protected GraphicsDevice _device;
}
