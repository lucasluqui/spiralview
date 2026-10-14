package com.lucasluqui.silverweave.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import com.lucasluqui.silverweave.Favorites;
import com.lucasluqui.silverweave.item.KnightItem;

/**
 * A list of items with their icons. Items can be marked as favorites by clicking the star at the
 * end of their row.
 */
public class ItemList extends JList<Object>
{
  /**
   * A list entry that isn't an item, like "None".
   */
  public static final class Placeholder
  {
    public final String label;

    public Placeholder (String label)
    {
      this.label = label;
    }

    @Override
    public String toString ()
    {
      return label;
    }
  }

  /**
   * A list entry that does something other than put on an item (like taking one off), shown like
   * an item rather than like a placeholder.
   */
  public interface Command
  {
    Icon getIcon (int size);

    /**
     * Returns the note shown at the end of the entry's row, or null.
     */
    String getDetail ();
  }

  /**
   * @param favorites the favorites to show and toggle, or null to leave out the stars.
   * @param background the list's background, or null for a see-through list whose selected and
   * hovered rows are picked out with rounded highlights (for the overlays over the view).
   */
  public ItemList (IconCache icons, Favorites favorites, int iconSize, Color background)
  {
    _icons = icons;
    _favorites = favorites;
    _iconSize = iconSize;
    _background = background;
    setModel(_model);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    setFixedCellHeight(iconSize + 8);
    // the rows take the list's width; without a fixed width, Swing renders every entry to measure
    // them, which asks for all their icons and leaves the visible ones queued behind the rest
    setFixedCellWidth(1);
    setBackground(background);
    setOpaque(background != null);
    setCellRenderer(_renderer = new Renderer());
  }

  /**
   * Sets the colors and font of the rows, in place of the overlays' defaults.
   *
   * @param selection the highlight of the selected row; hovered rows get the hover color.
   * @param text the color of the items' names; entries that aren't items get the muted color.
   */
  public void setStyle (Color selection, Color hover, Color text, Color muted, Font font)
  {
    _selection = selection;
    _hover = hover;
    _text = text;
    _muted = muted;
    _renderer._label.setFont(font);
    repaint();
  }

  /**
   * Sets the note shown at the end of each item's row (such as the slot it goes in). Only lists
   * without favorite stars have room for one.
   */
  public void setDetails (Function<KnightItem, String> details)
  {
    _details = details;
    repaint();
  }

  public void setEntries (List<?> entries)
  {
    _model.clear();
    _model.addAll(entries);
  }

  @Override
  public String getToolTipText (MouseEvent event)
  {
    KnightItem item = starAt(event.getPoint());
    if (item != null) {
      return _favorites.contains(item) ? "Remove from favorites" : "Add to favorites";
    }
    return super.getToolTipText(event);
  }

  @Override
  protected void processMouseEvent (MouseEvent event)
  {
    // clicks on a star toggle the favorite without selecting (and so equipping) the item
    switch (event.getID()) {
      case MouseEvent.MOUSE_PRESSED:
        KnightItem item = starAt(event.getPoint());
        _starPress = item != null;
        if (_starPress) {
          _favorites.toggle(item);
          repaint();
          return;
        }
        break;
      case MouseEvent.MOUSE_RELEASED:
      case MouseEvent.MOUSE_CLICKED:
        if (_starPress) {
          return;
        }
        break;
      case MouseEvent.MOUSE_EXITED:
        setHover(-1, false);
        break;
    }
    super.processMouseEvent(event);
  }

  @Override
  protected void processMouseMotionEvent (MouseEvent event)
  {
    Point point = event.getPoint();
    int index = locationToIndex(point);
    Rectangle bounds = index < 0 ? null : getCellBounds(index, index);
    if (bounds == null || !bounds.contains(point)) {
      index = -1;
    }
    setHover(index, starAt(point) != null);
    if (!(_starPress && event.getID() == MouseEvent.MOUSE_DRAGGED)) {
      super.processMouseMotionEvent(event);
    }
  }

  /**
   * Returns the item whose star is under the given point, or null.
   */
  protected KnightItem starAt (Point point)
  {
    if (_favorites == null) {
      return null;
    }
    int index = locationToIndex(point);
    Rectangle bounds = index < 0 ? null : getCellBounds(index, index);
    if (bounds == null || !bounds.contains(point) ||
        point.x < bounds.x + bounds.width - STAR_WIDTH) {
      return null;
    }
    Object value = _model.get(index);
    return value instanceof KnightItem ? (KnightItem)value : null;
  }

  protected void setHover (int index, boolean star)
  {
    if (index != _hoverIndex || star != _hoverStar) {
      _hoverIndex = index;
      _hoverStar = star;
      repaint();
    }
  }

  /**
   * Shows an entry's icon and name, followed by its favorite star.
   */
  protected class Renderer extends JPanel
    implements ListCellRenderer<Object>
  {
    public Renderer ()
    {
      super(new BorderLayout());
      _label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
      _label.setFont(Theme.font(12.5f, Font.PLAIN));
      _label.setIconTextGap(10);
      add(_label, BorderLayout.CENTER);
      if (_favorites != null) {
        _star.setPreferredSize(new Dimension(STAR_WIDTH, 1));
        add(_star, BorderLayout.EAST);
      } else {
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        _detail.setFont(Theme.font(11.5f, Font.PLAIN));
        _detail.setForeground(Theme.MUTED);
        add(_detail, BorderLayout.EAST);
      }
    }

    @Override
    public Component getListCellRendererComponent (
      JList<?> list, Object value, int index, boolean selected, boolean focused)
    {
      KnightItem item = value instanceof KnightItem ? (KnightItem)value : null;
      Command command = value instanceof Command ? (Command)value : null;
      // items and commands are what can be picked; anything else is just a note
      boolean pickable = item != null || command != null;
      _label.setText(value.toString());
      _label.setIcon(command != null ? command.getIcon(_iconSize) :
        _icons.get(item, _iconSize, list));
      _label.setForeground(!pickable && !selected ? _muted : _text);
      setBackground(selected ? _selection : _background);
      _highlight = selected ? _selection : (index == _hoverIndex && pickable ?
        _hover : null);
      setOpaque(_background != null);
      _star.favorite = _favorites != null && _favorites.contains(item);
      _star.visible = item != null && (_star.favorite || selected || index == _hoverIndex);
      _star.hover = index == _hoverIndex && _hoverStar;
      _detail.setText(command != null ? command.getDetail() :
        item == null || _details == null ? null : _details.apply(item));
      return this;
    }

    @Override
    protected void paintComponent (Graphics g)
    {
      super.paintComponent(g);
      if (_background == null && _highlight != null) {
        Graphics2D g2 = Theme.smooth(g);
        g2.setColor(_highlight);
        g2.fillRoundRect(0, 1, getWidth(), getHeight() - 2, HIGHLIGHT_ARC, HIGHLIGHT_ARC);
        g2.dispose();
      }
    }

    protected final JLabel _label = new JLabel(), _detail = new JLabel();
    protected Color _highlight;
    protected final Star _star = new Star();
  }

  /**
   * The favorite marker: filled for favorites, outlined for the row under the mouse.
   */
  protected static class Star extends JComponent
  {
    public boolean favorite, visible, hover;

    @Override
    protected void paintComponent (Graphics g)
    {
      if (!visible) {
        return;
      }
      Graphics2D g2 = Theme.smooth(g);
      g2.translate(getWidth() / 2.0 - 3, getHeight() / 2.0);
      Path2D star = new Path2D.Float();
      for (int ii = 0; ii < 10; ii++) {
        double angle = Math.PI * ii / 5 - Math.PI / 2, radius = (ii % 2 == 0) ? 6.5 : 2.8;
        double x = radius * Math.cos(angle), y = radius * Math.sin(angle) + 0.6;
        if (ii == 0) {
          star.moveTo(x, y);
        } else {
          star.lineTo(x, y);
        }
      }
      star.closePath();
      if (favorite) {
        g2.setColor(Theme.FAVORITE);
        g2.fill(star);
      } else {
        g2.setColor(hover ? Theme.TEXT : Theme.MUTED);
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(star);
      }
      g2.dispose();
    }
  }

  /** The roundness of the highlights of see-through lists. */
  protected static final int HIGHLIGHT_ARC = 12;

  /** The width of the clickable star area at the end of each row. */
  protected static final int STAR_WIDTH = 30;

  protected final IconCache _icons;
  protected final Favorites _favorites;
  protected final int _iconSize;
  protected final Color _background;
  protected final DefaultListModel<Object> _model = new DefaultListModel<>();
  protected final Renderer _renderer;
  protected Color _selection = Theme.SELECTION, _hover = Theme.SURFACE_HOVER, _text = Theme.TEXT,
    _muted = Theme.MUTED;
  protected Function<KnightItem, String> _details;
  protected int _hoverIndex = -1;
  protected boolean _hoverStar, _starPress;
}
