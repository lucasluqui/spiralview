package com.lucasluqui.silverweave.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * A flat button for the overlays over the 3D view, showing a vector glyph, a label or both. It
 * can repeat its action while held.
 */
public class OverlayButton extends JComponent
{
  /**
   * Creates a round button showing only a glyph.
   *
   * @param glyph paints the symbol, centered on the origin and about 12 pixels across.
   */
  public OverlayButton (Consumer<Graphics2D> glyph, String tooltip, boolean repeats, Runnable action)
  {
    this(glyph, null, tooltip, repeats, action);
  }

  /**
   * @param glyph paints the symbol, centered on the origin and about 12 pixels across; or null.
   * @param text the label, or null.
   */
  public OverlayButton (Consumer<Graphics2D> glyph, String text, String tooltip, boolean repeats,
    Runnable action)
  {
    _glyph = glyph;
    setFont(Theme.font(12.5f, Font.PLAIN));
    setText(text);
    setToolTipText(tooltip);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    _repeat = new Timer(30, e -> action.run());
    _repeat.setInitialDelay(350);
    addMouseListener(new MouseAdapter() {
      @Override public void mousePressed (MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e) || !isEnabled()) {
          return;
        }
        _pressed = true;
        if (repeats) {
          action.run();
          _repeat.restart();
        }
        repaint();
      }
      @Override public void mouseReleased (MouseEvent e) {
        boolean clicked = _pressed && !repeats && isEnabled() && contains(e.getPoint());
        _pressed = false;
        _repeat.stop();
        repaint();
        // one-shot actions go on release, as they may open a dialog that would swallow it
        if (clicked) {
          action.run();
          syncHover();
        }
      }
      @Override public void mouseEntered (MouseEvent e) {
        _hover = true;
        repaint();
      }
      @Override public void mouseExited (MouseEvent e) {
        _hover = false;
        repaint();
      }
    });
  }

  public void setText (String text)
  {
    if (Objects.equals(text, _text)) {
      return;
    }
    _text = text;
    revalidate();
    repaint();
  }

  /**
   * Sets the color the symbol and label take while hovered, for buttons that warn.
   */
  public void setHoverColor (Color color)
  {
    _hoverColor = color;
    repaint();
  }

  /**
   * Sets the color the button is filled with while not hovered or pressed, or null for none.
   */
  public void setRestingColor (Color color)
  {
    _restingColor = color;
    repaint();
  }

  public void setGlyph (Consumer<Graphics2D> glyph)
  {
    if (glyph == _glyph) {
      return;
    }
    _glyph = glyph;
    repaint();
  }

  /**
   * Checks whether the mouse is still over the button, since it may have left while a dialog
   * opened by the action held on to the mouse events.
   */
  protected void syncHover ()
  {
    PointerInfo pointer = isShowing() ? MouseInfo.getPointerInfo() : null;
    boolean hover = false;
    if (pointer != null) {
      Point point = pointer.getLocation();
      SwingUtilities.convertPointFromScreen(point, this);
      hover = contains(point);
    }
    if (hover != _hover) {
      _hover = hover;
      repaint();
    }
  }

  @Override
  public void setEnabled (boolean enabled)
  {
    super.setEnabled(enabled);
    setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    if (!enabled) {
      _pressed = false;
      _repeat.stop();
    }
    // (the superclass repaints if it changed)
  }

  @Override
  public Dimension getPreferredSize ()
  {
    if (isPreferredSizeSet() || _text == null) {
      return isPreferredSizeSet() ? super.getPreferredSize() : new Dimension(SIZE, SIZE);
    }
    int width = getFontMetrics(getFont()).stringWidth(_text) + PADDING * 2;
    if (_glyph != null) {
      width += GLYPH_WIDTH;
    }
    return new Dimension(width, SIZE);
  }

  @Override
  protected void paintComponent (Graphics g)
  {
    Graphics2D g2 = Theme.smooth(g);
    boolean active = isEnabled() && (_hover || _pressed);
    if (active || _restingColor != null) {
      g2.setColor(!active ? _restingColor : (_pressed ? Theme.SELECTION : Theme.SURFACE_HOVER));
      if (_text == null) {
        int size = Math.min(getWidth(), getHeight()) - 4;
        g2.fillOval((getWidth() - size) / 2, (getHeight() - size) / 2, size, size);
      } else {
        int height = getHeight() - 4;
        g2.fillRoundRect(2, 2, getWidth() - 4, height, height, height);
      }
    }
    g2.setColor(!isEnabled() ? Theme.MUTED : (active ? _hoverColor : IDLE));
    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    if (_text == null) {
      g2.translate(getWidth() / 2.0, getHeight() / 2.0);
      _glyph.accept(g2);
    } else {
      FontMetrics metrics = g2.getFontMetrics(getFont());
      int width = metrics.stringWidth(_text) + (_glyph == null ? 0 : GLYPH_WIDTH);
      int x = (getWidth() - width) / 2;
      if (_glyph != null) {
        Graphics2D glyph = (Graphics2D)g2.create();
        glyph.translate(x + 7, getHeight() / 2.0);
        _glyph.accept(glyph);
        glyph.dispose();
        x += GLYPH_WIDTH;
      }
      g2.setFont(getFont());
      g2.drawString(_text, x, (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
    }
    g2.dispose();
  }

  /** The height of the buttons (and width of the round ones). */
  protected static final int SIZE = 34;

  /** The space on either side of a label. */
  protected static final int PADDING = 14;

  /** The space a glyph takes in front of a label. */
  protected static final int GLYPH_WIDTH = 22;

  /** The color of the symbols and labels while not hovered. */
  protected static final Color IDLE = new Color(200, 205, 215);

  protected final Timer _repeat;
  protected Consumer<Graphics2D> _glyph;
  protected Color _hoverColor = Theme.TEXT;
  protected Color _restingColor;
  protected String _text;
  protected boolean _hover, _pressed;
}
