package com.lucasluqui.silverweave.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;

import static com.lucasluqui.silverweave.Log.log;

/**
 * A rounded panel floating over the 3D view.
 *
 * <p>The view is a native OpenGL canvas, which ordinary Swing components can't be drawn over, so
 * the panel lives in a small translucent window that follows the view around.
 */
public class Overlay
{
  /**
   * Where an overlay sits over the view.
   */
  public enum Anchor
  {
    TOP_LEFT, TOP_CENTER, BOTTOM_CENTER
  }

  /**
   * Creates an overlay for a view inside the given window.
   *
   * @param radius the corner radius of the panel, or 0 for a pill (fully rounded ends).
   */
  public Overlay (Window owner, Component view, Anchor anchor, JComponent content, int radius)
  {
    _view = view;
    _anchor = anchor;
    _window = new JWindow(owner);
    _window.setFocusableWindowState(false);
    _window.setAutoRequestFocus(false);
    _radius = radius;
    _translucent = isTranslucencySupported(owner);
    boolean translucent = _translucent;
    if (translucent) {
      _window.setBackground(new Color(0, 0, 0, 0));
    }

    JPanel surface = new JPanel(new BorderLayout()) {
      @Override protected void paintComponent (Graphics g) {
        // keep clear of the window edges, which lose a pixel to rounding on scaled displays
        Graphics2D g2 = Theme.smooth(g);
        if (!translucent) {
          // whatever of the margin the window shape leaves shows as part of the panel
          g2.setColor(Theme.BACKGROUND);
          g2.fillRect(0, 0, getWidth(), getHeight());
        }
        float w = getWidth() - MARGIN * 2 - 1, h = getHeight() - MARGIN * 2 - 1;
        float arc = radius > 0 ? radius * 2 : h;
        RoundRectangle2D shape = new RoundRectangle2D.Float(
          MARGIN + 0.5f, MARGIN + 0.5f, w, h, arc, arc);
        g2.setColor(translucent ? Theme.OVERLAY : Theme.BACKGROUND);
        g2.fill(shape);
        g2.setColor(Theme.BORDER);
        g2.draw(shape);
        g2.dispose();
      }
    };
    surface.setOpaque(false);
    surface.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
    content.setOpaque(false);
    surface.add(content, BorderLayout.CENTER);
    _window.setContentPane(surface);
    _window.pack();
    fitShape();

    ComponentAdapter follow = new ComponentAdapter() {
      @Override public void componentMoved (ComponentEvent e) {
        reposition();
      }
      @Override public void componentResized (ComponentEvent e) {
        reposition();
      }
      @Override public void componentShown (ComponentEvent e) {
        reposition();
      }
    };
    owner.addComponentListener(follow);
    view.addComponentListener(follow);
  }

  public void show ()
  {
    reposition();
    // showing a window that's already up brings it to the front, even over other applications
    if (!_window.isVisible()) {
      _window.setVisible(true);
    }
  }

  public void hide ()
  {
    _window.setVisible(false);
  }

  /**
   * Returns the height the overlay takes over the view, transparent margin included.
   */
  public int getHeight ()
  {
    return _window.getHeight();
  }

  /**
   * Stacks this overlay on top of another at the same edge of the view.
   */
  public void stackOn (Overlay below)
  {
    _below = below;
    reposition();
  }

  /**
   * Resizes the overlay to fit its contents, after they change.
   */
  public void pack ()
  {
    _window.pack();
    fitShape();
    reposition();
  }

  /**
   * Returns whether windows can have see-through parts, which on Linux takes a compositing
   * window manager. Without it, the panels over the view are opaque.
   */
  public static boolean isTranslucencySupported (Window owner)
  {
    GraphicsDevice device = owner.getGraphicsConfiguration().getDevice();
    boolean supported = device.isWindowTranslucencySupported(
      GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
    if (!_reported) {
      _reported = true;
      log.info("Window translucency.", "translucent", supported, "shaped",
        device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT));
    }
    return supported;
  }

  /**
   * Cuts a window down to the given shape, for systems without translucency, where the space
   * around a rounded panel would otherwise show as a solid block. The edges come out rougher, as
   * window shapes aren't smoothed. Does nothing if the system can't shape windows either.
   */
  public static void clip (Window window, Shape shape)
  {
    if (window.getGraphicsConfiguration().getDevice().isWindowTranslucencySupported(
        GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
      window.setShape(shape);
    }
  }

  /**
   * Without translucency, cuts the window down to the panel.
   */
  protected void fitShape ()
  {
    if (_translucent) {
      return;
    }
    float w = _window.getWidth() - MARGIN * 2, h = _window.getHeight() - MARGIN * 2;
    float arc = _radius > 0 ? _radius * 2 : h;
    clip(_window, new RoundRectangle2D.Float(MARGIN, MARGIN, w, h, arc, arc));
  }

  /**
   * Keeps the overlay at its place over the view.
   */
  protected void reposition ()
  {
    if (!_view.isShowing()) {
      return;
    }
    Point origin = _view.getLocationOnScreen();
    Dimension size = _window.getSize();
    int x = _anchor == Anchor.TOP_LEFT ? origin.x + INSET :
      origin.x + (_view.getWidth() - size.width) / 2;
    int y = _anchor == Anchor.BOTTOM_CENTER ? origin.y + _view.getHeight() - size.height - INSET :
      origin.y + INSET;
    if (_below != null) {
      // the two transparent margins count towards the space between them
      int offset = _below.getHeight() + STACK_GAP - MARGIN * 2;
      y += _anchor == Anchor.BOTTOM_CENTER ? -offset : offset;
    }
    _window.setLocation(x, y);
  }

  /** The transparent space kept around the panel inside its window. */
  protected static final int MARGIN = 3;

  /** The distance between the overlay and the edges of the view. */
  protected static final int INSET = 18;

  /** The space between stacked overlays. */
  protected static final int STACK_GAP = 6;

  protected final Component _view;
  protected final Anchor _anchor;
  protected final JWindow _window;
  protected final int _radius;

  /** Whether the window can be see-through around the panel. */
  protected final boolean _translucent;

  /** Whether the system's translucency support has been logged. */
  protected static boolean _reported;

  /** The overlay this one is stacked on, if any. */
  protected Overlay _below;
}
