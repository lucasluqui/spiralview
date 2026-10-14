package com.lucasluqui.silverweave.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Window;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * A short message that pops up over a component and goes away by itself.
 */
public final class Toast
{
  /**
   * Shows a message centered above a component.
   *
   * @param glyph a symbol shown before the message, or null.
   */
  public static void show (Component anchor, String message, Glyph glyph)
  {
    show(anchor, message, glyph, false);
  }

  /**
   * Shows a message centered above or below a component.
   *
   * @param glyph a symbol shown before the message, or null.
   * @param below whether the message goes below the component, as for those at the top of the
   * window.
   */
  public static void show (Component anchor, String message, Glyph glyph, boolean below)
  {
    Window owner = SwingUtilities.getWindowAncestor(anchor);
    if (owner == null || !anchor.isShowing()) {
      return;
    }
    // belong to the application window even for anchors in the overlays, which are in small
    // windows of their own: a window owned by one of those can't be see-through
    while (owner.getOwner() != null) {
      owner = owner.getOwner();
    }
    // one at a time: a new message replaces the last
    if (_current != null) {
      _current.dispose();
    }

    JWindow window = new JWindow(owner);
    window.setFocusableWindowState(false);
    boolean translucent = Overlay.isTranslucencySupported(owner);
    if (translucent) {
      window.setBackground(new Color(0, 0, 0, 0));
    }
    JPanel surface = new JPanel(new BorderLayout()) {
      @Override protected void paintComponent (Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        if (!translucent) {
          // whatever of the margin the window shape leaves shows as part of the pill
          g2.setColor(Theme.BACKGROUND);
          g2.fillRect(0, 0, getWidth(), getHeight());
        }
        // keep clear of the window edges, which lose a pixel to rounding on scaled displays
        float w = getWidth() - MARGIN * 2 - 1, h = getHeight() - MARGIN * 2 - 1;
        RoundRectangle2D pill = new RoundRectangle2D.Float(MARGIN + 0.5f, MARGIN + 0.5f, w, h, h, h);
        g2.setColor(translucent ? Theme.OVERLAY : Theme.BACKGROUND);
        g2.fill(pill);
        g2.setColor(Theme.BORDER);
        g2.draw(pill);
        g2.dispose();
      }
    };
    surface.setOpaque(false);
    surface.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
    JLabel label = new JLabel(message, glyph == null ? null : glyph.icon(14, Theme.ACCENT),
      JLabel.CENTER);
    label.setIconTextGap(8);
    label.setFont(Theme.font(12.5f, Font.PLAIN));
    label.setForeground(Theme.TEXT);
    label.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    surface.add(label, BorderLayout.CENTER);
    window.setContentPane(surface);
    window.pack();
    if (!translucent) {
      float h = window.getHeight() - MARGIN * 2;
      Overlay.clip(window, new RoundRectangle2D.Float(MARGIN, MARGIN,
        window.getWidth() - MARGIN * 2, h, h, h));
    }

    Point origin = anchor.getLocationOnScreen();
    Dimension size = window.getSize();
    // keep within the application window, as the anchor may sit right at its edge
    Point corner = owner.getLocationOnScreen();
    int x = origin.x + (anchor.getWidth() - size.width) / 2;
    x = Math.max(corner.x + GAP, Math.min(x, corner.x + owner.getWidth() - size.width - GAP));
    int y = below ? origin.y + anchor.getHeight() + GAP - MARGIN :
      origin.y - size.height - GAP + MARGIN;
    window.setLocation(x, y);
    window.setVisible(true);
    _current = window;

    Timer timer = new Timer(DURATION, e -> {
      window.dispose();
      if (_current == window) {
        _current = null;
      }
    });
    timer.setRepeats(false);
    timer.start();
  }

  /** How long a message stays up, in milliseconds. */
  protected static final int DURATION = 1800;

  /** The space between a message and the component it's about. */
  protected static final int GAP = 6;

  /** The transparent space kept around the message inside its window. */
  protected static final int MARGIN = 3;

  protected static JWindow _current;

  private Toast () { }
}
