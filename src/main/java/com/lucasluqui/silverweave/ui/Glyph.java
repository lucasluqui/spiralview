package com.lucasluqui.silverweave.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

import javax.swing.Icon;

/**
 * Button symbols, drawn as vectors so they stay crisp at any scale. Each is centered on the origin
 * and about 12 pixels across, drawn with the current color and stroke.
 */
public enum Glyph
{
  ROTATE_LEFT, ROTATE_RIGHT, ZOOM_IN, ZOOM_OUT, RESET,
  IMPORT, SHARE, SAVE, RANDOMIZE, CLEAR,
  PLAY, STOP, COPY, CHECK, STAR, STAR_FILLED, RENAME, SEARCH, BLOCKED, WARNING, SOUND, MUTED,
  CHEVRON_DOWN, CHEVRON_UP;

  /**
   * Paints the glyph centered on the origin.
   */
  public void paint (Graphics2D g)
  {
    switch (this) {
      case ZOOM_IN:
      case ZOOM_OUT:
      case SEARCH:
        // a magnifying glass, with a plus or minus in its lens when zooming
        g.draw(new Ellipse2D.Double(-6.5, -6.5, 9.5, 9.5));
        g.draw(path(1.4, 1.4, 6, 6));
        if (this != SEARCH) {
          g.draw(path(-4, -1.75, 0.5, -1.75));
        }
        if (this == ZOOM_IN) {
          g.draw(path(-1.75, -4, -1.75, 0.5));
        }
        break;
      case RESET:
        g.drawOval(-6, -6, 12, 12);
        g.fillOval(-2, -2, 4, 4);
        break;
      case IMPORT:
      case SHARE:
        // an arrow into (or out of) a tray
        boolean in = this == IMPORT;
        g.draw(path(0, in ? -6.5 : 2, 0, in ? 2 : -6.5));
        g.draw(path(-3.5, in ? -1.5 : -3, 0, in ? 2 : -6.5, 3.5, in ? -1.5 : -3));
        g.draw(path(-6, 1.5, -6, 6, 6, 6, 6, 1.5));
        break;
      case SAVE:
        // a bookmark
        Path2D mark = path(-4.5, -6, 4.5, -6, 4.5, 6, 0, 2.5, -4.5, 6);
        mark.closePath();
        g.draw(mark);
        break;
      case RANDOMIZE:
        // a die showing three
        g.drawRoundRect(-6, -6, 12, 12, 4, 4);
        for (int ii = -1; ii <= 1; ii++) {
          g.fill(new Ellipse2D.Double(ii * 3 - 1.4, ii * 3 - 1.4, 2.8, 2.8));
        }
        break;
      case CLEAR:
        // a bin
        g.draw(path(-6, -3.5, 6, -3.5));
        g.draw(path(-2, -3.5, -2, -6, 2, -6, 2, -3.5));
        g.draw(path(-4.5, -3.5, -3.5, 6, 3.5, 6, 4.5, -3.5));
        break;
      case PLAY:
        Path2D triangle = path(-3.5, -5, 5, 0, -3.5, 5);
        triangle.closePath();
        g.fill(triangle);
        break;
      case STOP:
        g.fillRoundRect(-4, -4, 9, 9, 2, 2);
        break;
      case COPY:
        g.drawRoundRect(-3, -3, 9, 9, 2, 2);
        g.draw(path(-6, 3, -6, -6, 3, -6));
        break;
      case RENAME:
        // a pencil, writing towards the bottom left
        Path2D pencil = path(-6, 6, -5.2, 2.6, 3, -5.6, 5.6, -3, -2.6, 5.2);
        pencil.closePath();
        g.draw(pencil);
        g.draw(path(1, -3.6, 3.6, -1));
        break;
      case CHECK:
        g.draw(path(-5, 0, -1.5, 4, 5, -4));
        break;
      case CHEVRON_DOWN:
        g.draw(path(-5, -2.5, 0, 2.5, 5, -2.5));
        break;
      case CHEVRON_UP:
        g.draw(path(-5, 2.5, 0, -2.5, 5, 2.5));
        break;
      case BLOCKED:
        // a circle struck through, like a no-entry sign
        g.drawOval(-6, -6, 12, 12);
        g.draw(path(-4.2, 4.2, 4.2, -4.2));
        break;
      case WARNING:
        Path2D caution = path(0, -6, 6.5, 5.5, -6.5, 5.5);
        caution.closePath();
        g.draw(caution);
        g.draw(path(0, -2, 0, 1.5));
        g.fill(new Ellipse2D.Double(-0.9, 2.8, 1.8, 1.8));
        break;
      case SOUND:
      case MUTED:
        // a speaker, giving out sound waves or crossed out
        Path2D speaker = path(-6.5, -2, -4, -2, 0, -5.5, 0, 5.5, -4, 2, -6.5, 2);
        speaker.closePath();
        g.draw(speaker);
        if (this == SOUND) {
          g.draw(new Arc2D.Double(-2.5, -3.5, 7, 7, -50, 100, Arc2D.OPEN));
          g.draw(new Arc2D.Double(-4, -6.5, 13, 13, -50, 100, Arc2D.OPEN));
        } else {
          g.draw(path(2.5, -2.5, 6.5, 2.5));
          g.draw(path(2.5, 2.5, 6.5, -2.5));
        }
        break;
      case STAR:
      case STAR_FILLED:
        double[] points = new double[20];
        for (int ii = 0; ii < 10; ii++) {
          double angle = Math.PI * ii / 5 - Math.PI / 2, radius = (ii % 2 == 0) ? 6.5 : 2.8;
          points[ii * 2] = radius * Math.cos(angle);
          points[ii * 2 + 1] = radius * Math.sin(angle) + 0.6;
        }
        Path2D star = path(points);
        star.closePath();
        if (this == STAR_FILLED) {
          g.fill(star);
        } else {
          g.draw(star);
        }
        break;
      default:
        // most of a circle, with an arrowhead pointing along the arc at its end
        double start = this == ROTATE_LEFT ? 50 : 130, extent = this == ROTATE_LEFT ? 280 : -280;
        double radius = 6;
        g.draw(new Arc2D.Double(-radius, -radius, radius * 2, radius * 2, start, extent,
          Arc2D.OPEN));
        double end = Math.toRadians(start + extent), sign = Math.signum(extent);
        double tipX = radius * Math.cos(end), tipY = -radius * Math.sin(end);
        // direction of travel at the end of the arc, in screen coordinates
        double heading = Math.atan2(-Math.cos(end) * sign, -Math.sin(end) * sign);
        g.draw(path(tipX + 4 * Math.cos(heading + Math.PI - 0.7),
          tipY + 4 * Math.sin(heading + Math.PI - 0.7), tipX, tipY,
          tipX + 4 * Math.cos(heading + Math.PI + 0.7),
          tipY + 4 * Math.sin(heading + Math.PI + 0.7)));
        break;
    }
  }

  /**
   * Returns the glyph as an icon for ordinary Swing buttons and labels.
   */
  public Icon icon (int size, Color color)
  {
    return new Icon() {
      @Override public void paintIcon (Component c, Graphics g, int x, int y) {
        Graphics2D g2 = Theme.smooth(g);
        g2.setColor(color);
        g2.translate(x + size / 2.0, y + size / 2.0);
        double scale = size / 16.0;
        g2.scale(scale, scale);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        paint(g2);
        g2.dispose();
      }
      @Override public int getIconWidth () {
        return size;
      }
      @Override public int getIconHeight () {
        return size;
      }
    };
  }

  /**
   * Creates an open path through the given x, y pairs.
   */
  protected static Path2D path (double... coords)
  {
    Path2D path = new Path2D.Double();
    path.moveTo(coords[0], coords[1]);
    for (int ii = 2; ii < coords.length; ii += 2) {
      path.lineTo(coords[ii], coords[ii + 1]);
    }
    return path;
  }
}
