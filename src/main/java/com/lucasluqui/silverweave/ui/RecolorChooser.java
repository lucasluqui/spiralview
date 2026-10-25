package com.lucasluqui.silverweave.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

import javax.swing.Icon;
import javax.swing.JComponent;

import com.threerings.media.image.ColorPository;
import com.threerings.media.image.Colorization;

import com.lucasluqui.silverweave.item.CustomItems;

/**
 * A grid of swatches for the colors a custom item can be recolored in.
 */
public class RecolorChooser extends JComponent
{
  /**
   * A color to choose from.
   */
  public static final class Choice
  {
    /** The color's id, as in the {@code armor_primary} class it's defined in. */
    public final int id;

    /** The name the game's items give the color, or its code if they have none. */
    public final String name;

    /** The color's name in the color definitions (e.g. "gld"). */
    public final String code;

    public final Color primary, secondary;

    public Choice (int id, String name, String code, Color primary, Color secondary)
    {
      this.id = id;
      this.name = name;
      this.code = code;
      this.primary = primary;
      this.secondary = secondary;
    }

    /**
     * Returns the name with the code, or just the code if they're the same.
     */
    public String describe ()
    {
      return name.equals(code) ? code : name + " (" + code + ")";
    }
  }

  /**
   * Lists the colors custom items can be recolored in, in color id order: the ones defined in the
   * {@code armor_primary} class, each shown with its {@code armor_secondary} counterpart.
   *
   * @param names returns the name the game's items give a color, or null if they give none.
   */
  public static List<Choice> createChoices (ColorPository colors, IntFunction<String> names)
  {
    List<Choice> choices = new ArrayList<>();
    ColorPository.ClassRecord primary = colors == null ? null :
      colors.getClassRecord(CustomItems.ARMOR_PRIMARY);
    if (primary == null) {
      return choices;
    }
    List<ColorPository.ColorRecord> records = new ArrayList<>(primary.colors.values());
    records.sort(Comparator.comparingInt(a -> a.colorId));
    for (ColorPository.ColorRecord record : records) {
      Color main = record.getColorization().getColorizedRoot();
      Colorization second = colors.getColorization(CustomItems.ARMOR_SECONDARY, record.colorId);
      String name = names == null ? null : names.apply(record.colorId);
      choices.add(new Choice(record.colorId, name == null ? record.name : name, record.name, main,
        second == null ? main : second.getColorizedRoot()));
    }
    return choices;
  }

  /**
   * Returns an icon showing a color's swatch, or an empty ring if the color is unknown.
   */
  public static Icon swatchIcon (Choice choice, int size)
  {
    return new Icon() {
      @Override public void paintIcon (java.awt.Component c, Graphics g, int x, int y) {
        Graphics2D g2 = Theme.smooth(g);
        if (choice != null) {
          paintSwatch(g2, choice, x, y, size);
        } else {
          g2.setColor(Theme.SIDE_DESCRIPTION);
          g2.setStroke(new BasicStroke(1.5f));
          g2.draw(new Ellipse2D.Float(x + 1, y + 1, size - 2, size - 2));
        }
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
   * Paints a color's swatch: a disc with the primary shade over its upper left half and the
   * secondary over the lower right.
   */
  public static void paintSwatch (Graphics2D g2, Choice choice, float x, float y, float size)
  {
    Ellipse2D.Float disc = new Ellipse2D.Float(x, y, size, size);
    g2.setColor(choice.secondary);
    g2.fill(disc);
    g2.setColor(choice.primary);
    g2.fill(new Arc2D.Float(disc.getBounds2D(), 45, 180, Arc2D.CHORD));
  }

  /**
   * @param onChoose notified with the id of a color the user clicks.
   */
  public RecolorChooser (IntConsumer onChoose)
  {
    _onChoose = onChoose;
    setOpaque(false);
    setFocusable(true);
    // (registers with the tooltip manager; the text comes from getToolTipText(MouseEvent))
    setToolTipText("");
    MouseAdapter mouse = new MouseAdapter() {
      @Override public void mousePressed (MouseEvent e) {
        requestFocusInWindow();
        int index = indexAt(e.getPoint());
        if (index >= 0) {
          _onChoose.accept(_choices.get(index).id);
        }
      }
      @Override public void mouseMoved (MouseEvent e) {
        setHover(indexAt(e.getPoint()));
      }
      @Override public void mouseExited (MouseEvent e) {
        setHover(-1);
      }
    };
    addMouseListener(mouse);
    addMouseMotionListener(mouse);
    addKeyListener(new KeyAdapter() {
      @Override public void keyPressed (KeyEvent e) {
        if (handleKey(e)) {
          e.consume();
        }
      }
    });
    addFocusListener(new FocusAdapter() {
      @Override public void focusGained (FocusEvent e) {
        repaint();
      }
      @Override public void focusLost (FocusEvent e) {
        repaint();
      }
    });
    // the grid wraps to the width it's given, so its height follows the width
    addComponentListener(new ComponentAdapter() {
      @Override public void componentResized (ComponentEvent e) {
        if (getPreferredSize().height != getHeight()) {
          revalidate();
        }
      }
    });
  }

  public void setChoices (List<Choice> choices)
  {
    _choices = new ArrayList<>(choices);
    revalidate();
    repaint();
  }

  public List<Choice> getChoices ()
  {
    return _choices;
  }

  /**
   * Returns the choice with the given color id, or null.
   */
  public Choice getChoice (int id)
  {
    for (Choice choice : _choices) {
      if (choice.id == id) {
        return choice;
      }
    }
    return null;
  }

  /**
   * Marks the color with the given id as the chosen one.
   */
  public void setSelected (int id)
  {
    _selected = id;
    repaint();
  }

  /**
   * Moves the choice with the arrow keys: left and right to the previous and next color, up and
   * down a row.
   *
   * @return whether the key was one of them.
   */
  public boolean handleKey (KeyEvent event)
  {
    int columns = columns(getWidth() > 0 ? getWidth() : DEFAULT_WIDTH), step;
    switch (event.getKeyCode()) {
      case KeyEvent.VK_LEFT: step = -1; break;
      case KeyEvent.VK_RIGHT: step = 1; break;
      case KeyEvent.VK_UP: step = -columns; break;
      case KeyEvent.VK_DOWN: step = columns; break;
      default: return false;
    }
    int index = -1;
    for (int ii = 0; ii < _choices.size(); ii++) {
      if (_choices.get(ii).id == _selected) {
        index = ii;
        break;
      }
    }
    int next = index < 0 ? 0 : index + step;
    if (next >= 0 && next < _choices.size() && next != index) {
      setSelected(_choices.get(next).id);
      _onChoose.accept(_selected);
    }
    return true;
  }

  @Override
  public Dimension getPreferredSize ()
  {
    int width = getWidth() > 0 ? getWidth() : DEFAULT_WIDTH;
    int rows = (_choices.size() + columns(width) - 1) / columns(width);
    return new Dimension(DEFAULT_WIDTH, Math.max(0, rows * (SWATCH + GAP) - GAP + 2 * PAD));
  }

  @Override
  public Dimension getMaximumSize ()
  {
    return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
  }

  @Override
  public String getToolTipText (MouseEvent event)
  {
    int index = indexAt(event.getPoint());
    return index < 0 ? null : _choices.get(index).describe();
  }

  @Override
  protected void paintComponent (Graphics g)
  {
    Graphics2D g2 = Theme.smooth(g);
    int columns = columns(getWidth());
    for (int ii = 0; ii < _choices.size(); ii++) {
      Choice choice = _choices.get(ii);
      int x = PAD + (ii % columns) * (SWATCH + GAP), y = PAD + (ii / columns) * (SWATCH + GAP);
      paintSwatch(g2, choice, x + 2, y + 2, SWATCH - 4);
      if (choice.id == _selected || ii == _hover) {
        // the chosen color is picked out in the accent while the arrow keys drive the grid
        g2.setColor(choice.id != _selected ? Theme.SIDE_DESCRIPTION :
          isFocusOwner() ? Theme.SIDE_ACCENT : Theme.SIDE_TEXT);
        g2.setStroke(new BasicStroke(choice.id == _selected ? 2f : 1.5f));
        g2.draw(new Ellipse2D.Float(x + 0.5f, y + 0.5f, SWATCH - 1, SWATCH - 1));
      }
    }
    g2.dispose();
  }

  protected int columns (int width)
  {
    return Math.max(1, (width - 2 * PAD + GAP) / (SWATCH + GAP));
  }

  /**
   * Returns the index of the swatch under a point, or -1.
   */
  protected int indexAt (Point point)
  {
    int columns = columns(getWidth());
    int col = (point.x - PAD) / (SWATCH + GAP), row = (point.y - PAD) / (SWATCH + GAP);
    if (point.x < PAD || point.y < PAD || col >= columns ||
        (point.x - PAD) % (SWATCH + GAP) >= SWATCH || (point.y - PAD) % (SWATCH + GAP) >= SWATCH) {
      return -1;
    }
    int index = row * columns + col;
    return index < _choices.size() ? index : -1;
  }

  protected void setHover (int index)
  {
    if (index != _hover) {
      _hover = index;
      setCursor(index < 0 ? null : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      repaint();
    }
  }

  /** The size of the swatches, the space between them and around the grid. */
  protected static final int SWATCH = 26, GAP = 4, PAD = 2;

  /** The width assumed before the grid is laid out (about that of the side panel's cards). */
  protected static final int DEFAULT_WIDTH = 320;

  protected final IntConsumer _onChoose;
  protected List<Choice> _choices = new ArrayList<>();
  protected int _selected = -1;
  protected int _hover = -1;
}
