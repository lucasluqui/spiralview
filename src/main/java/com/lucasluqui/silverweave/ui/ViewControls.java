package com.lucasluqui.silverweave.ui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Buttons floating over the bottom of the 3D view to rotate and zoom the camera.
 */
public class ViewControls
{
  /**
   * What the buttons do to the camera.
   */
  public interface Camera
  {
    /** Rotates the camera around the knight by the given angle, in radians. */
    void rotate (float radians);

    /** Moves the camera closer (negative) or farther (positive). */
    void zoom (float amount);

    /** Returns the camera to its starting position. */
    void reset ();
  }

  /**
   * Creates the controls for a view inside the given window.
   */
  public ViewControls (Window owner, Component view, Camera camera)
  {
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 4));
    bar.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
    bar.add(new OverlayButton(Glyph.ROTATE_LEFT::paint, "Rotate left", true,
      () -> camera.rotate(ROTATE_STEP)));
    bar.add(new OverlayButton(Glyph.ZOOM_OUT::paint, "Zoom out", true,
      () -> camera.zoom(ZOOM_STEP)));
    bar.add(new OverlayButton(Glyph.RESET::paint, "Reset view", false, camera::reset));
    bar.add(new OverlayButton(Glyph.ZOOM_IN::paint, "Zoom in", true,
      () -> camera.zoom(-ZOOM_STEP)));
    bar.add(new OverlayButton(Glyph.ROTATE_RIGHT::paint, "Rotate right", true,
      () -> camera.rotate(-ROTATE_STEP)));
    _overlay = new Overlay(owner, view, Overlay.Anchor.BOTTOM_CENTER, bar, 0);
  }

  /**
   * Returns the overlay holding the controls, for others to stack on.
   */
  public Overlay getOverlay ()
  {
    return _overlay;
  }

  public void show ()
  {
    _overlay.show();
  }

  /** How far each press (or repeat) rotates and zooms the camera. */
  protected static final float ROTATE_STEP = 0.06f, ZOOM_STEP = 0.35f;

  protected final Overlay _overlay;
}
