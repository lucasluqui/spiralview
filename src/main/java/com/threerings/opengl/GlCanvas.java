//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.opengl;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

import static com.threerings.opengl.Log.log;

/**
 * A canvas that uses lwjgl3-awt to embed an OpenGL context in a Swing panel.
 * This replaces the old LWJGL 2 Display.setParent() approach.
 *
 * <p>spiralview: a copy of clyde 1.2.1's canvas that holds the drawing surface only while drawing a
 * frame. clyde's keeps it locked between frames, and on Linux that lock is AWT's toolkit lock,
 * which starves AWT's X11 thread and makes the editors crawl. The changes from clyde's are marked
 * "spiralview".
 */
public class GlCanvas extends JPanel
{
  /**
   * Creates a new canvas.
   */
  public GlCanvas (final int antialiasingLevel)
  {
    super(new BorderLayout());

    // configure the GL data for the context
    GLData data = new GLData();
    data.samples = antialiasingLevel;
    data.depthSize = 24;
    data.stencilSize = 8;
    data.alphaSize = 0; // no framebuffer alpha to avoid compositing issues on macOS
    data.doubleBuffer = true;
    // Request a 3.2+ compatibility profile for legacy OpenGL. The version must be set
    // alongside the profile: lwjgl3-awt rejects a non-null profile with version < 3.2,
    // since the OpenGL profile concept doesn't exist below 3.2. Drivers will typically
    // hand back a higher compatibility context (4.x) than the minimum we ask for.
    data.majorVersion = 3;
    data.minorVersion = 2;
    data.profile = GLData.Profile.COMPATIBILITY;

    // create the AWT GL canvas
    _awtCanvas = new LockableGLCanvas(data);

    add(_awtCanvas, BorderLayout.CENTER);

    JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    _awtCanvas.setFocusable(false);

    // Track whether the mouse is over the canvas (for the getMousePosition override,
    // which can't use the default JPanel behavior because the heavyweight child
    // covers the panel), and route focus to this panel on mouse press so it
    // receives subsequent key events.
    MouseInputAdapter listener = new MouseInputAdapter() {
      @Override public void mouseEntered (MouseEvent event) { _entered = true; }
      @Override public void mouseExited (MouseEvent event) { _entered = false; }
      @Override public void mousePressed (MouseEvent event) { requestFocusInWindow(); }
    };
    // Attach to _awtCanvas (heavyweight) since it sits on top and intercepts events
    // that would otherwise go to this JPanel.
    _awtCanvas.addMouseListener(listener);
  }

  /**
   * Returns the framebuffer width in pixels (may differ from component width on HiDPI/Retina).
   */
  public int getFramebufferWidth ()
  {
    return _awtCanvas.getFramebufferWidth();
  }

  /**
   * Returns the framebuffer height in pixels (may differ from component height on HiDPI/Retina).
   */
  public int getFramebufferHeight ()
  {
    return _awtCanvas.getFramebufferHeight();
  }

  /**
   * Enables or disables vsync.
   */
  public void setVSyncEnabled (boolean vsync)
  {
    // TODO: implement vsync for lwjgl3-awt (requires platform-specific CGL/WGL/GLX calls).
    // The GlCanvasTool frame limiter (Thread.sleep) provides adequate rate control for tools.
  }

  /**
   * Makes the canvas context current.
   */
  public void makeCurrent ()
  {
    // spiralview: the context is made current (so that GL calls can be made outside of the
    // render callback, e.g. during scene/resource loading) without keeping the surface locked
    try {
      _awtCanvas.makeContextCurrent();
    } catch (AWTException e) {
      log.warning("Failed to make the GL context current.", e);
    }
  }

  /**
   * Releases the GL context after a manual {@link #makeCurrent()} call.
   */
  public void releaseCurrent ()
  {
    // spiralview: the surface isn't held between frames, so there's only the context to let go
    try {
      _awtCanvas.clearContext();
    } catch (AWTException e) {
      log.warning("Failed to release the GL context.", e);
    }
  }

  /**
   * spiralview: releases the drawing surface in the middle of a frame, keeping the context current,
   * so that AWT can get on with things (on Linux, the surface lock is its toolkit lock) during a
   * long wait. Must be followed by {@link #resumeContextLock} before any more drawing or the buffer
   * swap, and only called while a frame is being drawn.
   */
  public void pauseContextLock ()
  {
    _awtCanvas.pauseLock();
  }

  /**
   * spiralview: takes back the drawing surface released by {@link #pauseContextLock}.
   */
  public void resumeContextLock ()
  {
    _awtCanvas.resumeLock();
  }

  /**
   * Shuts down the canvas.
   */
  public void shutdown ()
  {
    stopUpdating();
    _awtCanvas.disposeCanvas();
  }

  // Forward external mouse listener registration to the heavyweight GL canvas,
  // which sits on top and receives all mouse events.
  @Override
  public synchronized void addMouseListener (MouseListener l)
  {
    _awtCanvas.addMouseListener(l);
  }

  @Override
  public synchronized void addMouseMotionListener (MouseMotionListener l)
  {
    _awtCanvas.addMouseMotionListener(l);
  }

  @Override
  public synchronized void addMouseWheelListener (MouseWheelListener l)
  {
    _awtCanvas.addMouseWheelListener(l);
  }

  @Override
  public synchronized void removeMouseListener (MouseListener l)
  {
    _awtCanvas.removeMouseListener(l);
  }

  @Override
  public synchronized void removeMouseMotionListener (MouseMotionListener l)
  {
    _awtCanvas.removeMouseMotionListener(l);
  }

  @Override
  public synchronized void removeMouseWheelListener (MouseWheelListener l)
  {
    _awtCanvas.removeMouseWheelListener(l);
  }

  @Override
  public void addNotify ()
  {
    super.addNotify();
    // Start the render loop once the canvas has a native peer
    startUpdating();
  }

  @Override
  public void removeNotify ()
  {
    super.removeNotify();
    stopUpdating();
  }

  @Override
  public Point getMousePosition ()
  {
    return _entered ? getRelativeMouseLocation() : null;
  }

  /**
   * Override to perform custom initialization.
   * Called once when the GL context is first created.
   */
  protected void didInit ()
  {
  }

  /**
   * Starts calling {@link #updateFrame} regularly.
   */
  protected void startUpdating ()
  {
    _updater = new Runnable() {
      public void run () {
        if (_updater != null) {
          updateFrame();
          EventQueue.invokeLater(this);
        }
      }
    };
    EventQueue.invokeLater(_updater);
  }

  /**
   * Stops calling {@link #updateFrame}.
   */
  protected void stopUpdating ()
  {
    _updater = null;
    releaseCurrent();
  }

  /**
   * Updates and, if the canvas is showing, renders the scene and swaps the buffers.
   * The lwjgl3-awt render() call makes the context current, calls paintGL()
   * (which runs updateView + renderView + swapBuffers), then releases the context.
   */
  protected void updateFrame ()
  {
    // spiralview: render() handles the first frame and those after a resize, releasing the context
    // after, so it's made current again. Other frames hold the surface only while drawing. The
    // context stays current on the AWT thread, but the surface is free between frames.
    try {
      int width = _awtCanvas.getWidth(), height = _awtCanvas.getHeight();
      if (!_awtCanvas.isInitialized() || width != _renderedWidth || height != _renderedHeight) {
        _renderedWidth = width;
        _renderedHeight = height;
        _awtCanvas.render();
        _awtCanvas.makeContextCurrent();
      } else {
        _awtCanvas.renderFrame();
      }
    } catch (Exception e) {
      log.warning("Caught exception in frame loop.", e);
    }
  }

  /**
   * Override to perform any updates that are required even if not rendering.
   */
  protected void updateView ()
  {
  }

  /**
   * Override to render the contents of the canvas.
   */
  protected void renderView ()
  {
  }

  /**
   * Returns the location of the mouse pointer relative to this component.
   */
  protected Point getRelativeMouseLocation ()
  {
    java.awt.PointerInfo info = MouseInfo.getPointerInfo();
    if (info == null) {
      return new Point(0, 0);
    }
    Point pt = info.getLocation();
    SwingUtilities.convertPointFromScreen(pt, this);
    return pt;
  }

  /**
   * AWTGLCanvas subclass that exposes context lock/unlock for use outside the render callback.
   */
  protected class LockableGLCanvas extends AWTGLCanvas
  {
    public LockableGLCanvas (GLData data) { super(data); }
    @Override public void initGL () {
      GL.createCapabilities();
      GlCanvas.this.didInit();
    }
    @Override public void paintGL () {
      GlCanvas.this.updateView();
      if (isShowing()) {
        GlCanvas.this.renderView();
      }
      swapBuffers();
    }
    public void lockContext () { beforeRender(); }
    public void unlockContext () { afterRender(); }

    // spiralview: the following hold the surface only for as long as they need it

    /**
     * Checks whether the context has been created and set up by a first {@link #render}.
     */
    public boolean isInitialized ()
    {
      return initCalled && context != 0L;
    }

    /**
     * Makes the context current on this thread, if it isn't already, leaving the surface unlocked.
     */
    public void makeContextCurrent ()
      throws AWTException
    {
      if (context == 0L || !isDisplayable()) {
        return;
      }
      platformCanvas.lock();
      try {
        if (!platformCanvas.isCurrent(context) && !platformCanvas.makeCurrent(context)) {
          throw new AWTException("Failed to make the GL context current");
        }
      } finally {
        platformCanvas.unlock();
      }
    }

    /**
     * Makes the context no longer current on this thread, if it is.
     */
    public void clearContext ()
      throws AWTException
    {
      if (context == 0L || !isDisplayable()) {
        return;
      }
      platformCanvas.lock();
      try {
        if (platformCanvas.isCurrent(context)) {
          platformCanvas.makeCurrent(0L);
        }
      } finally {
        platformCanvas.unlock();
      }
    }

    /**
     * Releases the surface locked for the frame being drawn.
     */
    public void pauseLock ()
    {
      try {
        platformCanvas.unlock();
      } catch (AWTException e) {
        throw new RuntimeException("Failed to unlock Canvas", e);
      }
    }

    /**
     * Locks the surface again after {@link #pauseLock}.
     */
    public void resumeLock ()
    {
      try {
        platformCanvas.lock();
      } catch (AWTException e) {
        throw new RuntimeException("Failed to lock Canvas", e);
      }
    }

    /**
     * Draws a frame and swaps it in, holding the surface only while doing so and leaving the
     * context current after.
     */
    public void renderFrame ()
      throws AWTException
    {
      platformCanvas.lock();
      try {
        if (!platformCanvas.isCurrent(context) && !platformCanvas.makeCurrent(context)) {
          throw new AWTException("Failed to make the GL context current");
        }
        paintGL();
      } finally {
        platformCanvas.unlock();
      }
    }
  }

  /** The lwjgl3-awt GL canvas that provides the actual OpenGL surface. */
  protected LockableGLCanvas _awtCanvas;

  /** spiralview: the size of the canvas when the last frame went through {@link
   * AWTGLCanvas#render}, which picks up the framebuffer's size. */
  protected int _renderedWidth, _renderedHeight;

  /** Whether or not the mouse is over the component. */
  protected boolean _entered;

  /** The runnable that updates the frame. */
  protected Runnable _updater;
}
