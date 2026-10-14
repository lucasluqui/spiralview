package com.lucasluqui.silverweave.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.HierarchyListener;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicProgressBarUI;

/**
 * Knight Launcher's progress bar: a thin, rounded bar with a gradient fill and a soft glow.
 * Indeterminate bars sweep a short pill back and forth. The bar is drawn {@link #GLOW_MARGIN} in
 * from the component's bounds, so the glow isn't cut off.
 */
public class SlimProgressBarUI extends BasicProgressBarUI
{
  /**
   * Creates a progress bar in this style, sized to show a bar of the given width.
   */
  public static JProgressBar create (Color fill, int width, boolean indeterminate)
  {
    JProgressBar bar = new JProgressBar();
    bar.setUI(new SlimProgressBarUI(fill));
    bar.setOpaque(false);
    bar.setBorder(null);
    bar.setIndeterminate(indeterminate);
    Dimension size = new Dimension(width + GLOW_MARGIN * 2, BAR_HEIGHT + GLOW_MARGIN * 2);
    bar.setPreferredSize(size);
    bar.setMaximumSize(size);
    return bar;
  }

  public SlimProgressBarUI (Color fill)
  {
    _fill = fill;
    _fillEnd = blend(fill, Color.WHITE, 0.35f);
  }

  @Override
  public void installUI (JComponent c)
  {
    super.installUI(c);
    // the sweep moves by the time between frames, capped so a stall doesn't make the pill jump
    _frames = new Timer(FRAME_MS, e -> {
      long now = System.nanoTime();
      long elapsed = _lastFrame == 0 ? 0 : Math.min(now - _lastFrame, MAX_STEP_NS);
      _lastFrame = now;
      if (!progressBar.isDisplayable()) {
        _frames.stop();
        _lastFrame = 0;
      } else if (progressBar.isIndeterminate() && progressBar.isShowing()) {
        _sweep = (_sweep + elapsed / (SWEEP_MS * 1e6)) % 2.0;
        progressBar.repaint();
      }
    });
    _frames.setCoalesce(true);
    _restart = e -> {
      if (progressBar.isDisplayable() && !_frames.isRunning()) {
        _frames.start();
      }
    };
    c.addHierarchyListener(_restart);
    _frames.start();
  }

  @Override
  public void uninstallUI (JComponent c)
  {
    _frames.stop();
    c.removeHierarchyListener(_restart);
    super.uninstallUI(c);
  }

  @Override
  protected void incrementAnimationIndex ()
  {
    // the frames above do the animating; the stock timer's repaints would only add judder
  }

  @Override
  public void paint (Graphics g, JComponent c)
  {
    int w = c.getWidth(), h = c.getHeight();
    if (w <= 0 || h <= 0) {
      return;
    }
    Graphics2D g2 = Theme.smooth(g);

    // the bar is inset from the bounds and vertically centered, leaving room for the glow
    float height = Math.min(BAR_HEIGHT, h);
    float x = GLOW_MARGIN, y = (h - height) / 2f, width = w - x * 2;
    g2.setColor(new Color(255, 255, 255, 30));
    g2.fill(new RoundRectangle2D.Float(x, y, width, height, height, height));

    float start, length;
    if (progressBar.isIndeterminate()) {
      // a pill sweeping back and forth, easing in and out at either end
      float t = (float)(_sweep <= 1.0 ? _sweep : 2.0 - _sweep);
      float eased = t * t * (3f - 2f * t);
      length = Math.max(height, width * SWEEP_SHARE);
      start = x + (width - length) * eased;
    } else {
      double percent = progressBar.getPercentComplete();
      if (percent <= 0) {
        g2.dispose();
        return;
      }
      // never narrower than the bar is tall, so it's a dot rather than a sliver at the start
      start = x;
      length = Math.max(height, (float)(width * percent));
    }

    // the soft glow: a few wider and fainter copies of the fill behind it
    for (int ii = GLOW_LAYERS; ii > 0; ii--) {
      float spread = ii * 1.5f;
      g2.setColor(new Color(_fill.getRed(), _fill.getGreen(), _fill.getBlue(), 18));
      g2.fill(new RoundRectangle2D.Float(start - spread, y - spread, length + spread * 2,
        height + spread * 2, height + spread * 2, height + spread * 2));
    }
    g2.setPaint(new GradientPaint(start, 0, _fill, start + length, 0, _fillEnd));
    g2.fill(new RoundRectangle2D.Float(start, y, length, height, height, height));
    g2.dispose();
  }

  protected static Color blend (Color from, Color to, float amount)
  {
    return new Color(
      Math.round(from.getRed() + (to.getRed() - from.getRed()) * amount),
      Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
      Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * amount));
  }

  protected final Color _fill, _fillEnd;
  protected Timer _frames;

  /** How far along the sweep is: from 0 to 1 going right, then from 1 to 2 coming back. */
  protected double _sweep;

  /** When the sweep last moved, in nanoseconds, or 0 if it hasn't yet. */
  protected long _lastFrame;
  protected HierarchyListener _restart;

  protected static final int BAR_HEIGHT = 6;

  /** How many copies of the fill make up its glow. */
  protected static final int GLOW_LAYERS = 3;

  /** How long an indeterminate sweep takes, how much of the track its pill covers, and how often
   * it's redrawn, in milliseconds. */
  protected static final float SWEEP_MS = 1400f, SWEEP_SHARE = 0.3f;
  protected static final int FRAME_MS = 1000 / 60;

  /** The most the sweep moves on in one frame, however long the frame took, in nanoseconds. */
  protected static final long MAX_STEP_NS = 2_000_000_000L * FRAME_MS / 1000;

  /** The room the glow takes on each side of the bar. */
  public static final int GLOW_MARGIN = 5;
}
