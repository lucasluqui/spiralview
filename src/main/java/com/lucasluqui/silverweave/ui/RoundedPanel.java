package com.lucasluqui.silverweave.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A panel filled with a rounded rectangle, which the side panel holds its lists in.
 */
public class RoundedPanel extends JPanel
{
  /**
   * @param inset the space between the rounded edge and the content.
   */
  public RoundedPanel (JComponent content, Color fill, int arc, int inset)
  {
    super(new BorderLayout());
    _fill = fill;
    _arc = arc;
    setOpaque(false);
    setBorder(BorderFactory.createEmptyBorder(inset, inset, inset, inset));
    add(content, BorderLayout.CENTER);
  }

  @Override
  public Dimension getMaximumSize ()
  {
    // stretches across, but stays as tall as its content wants in a column of controls
    return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
  }

  @Override
  protected void paintComponent (Graphics g)
  {
    Graphics2D g2 = Theme.smooth(g);
    g2.setColor(_fill);
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), _arc, _arc);
    g2.dispose();
  }

  protected final Color _fill;
  protected final int _arc;
}
