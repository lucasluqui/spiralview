package com.lucasluqui.silverweave.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.lucasluqui.silverweave.Favorites;
import com.lucasluqui.silverweave.SavedLoadouts;
import com.lucasluqui.silverweave.ShareCode;
import com.lucasluqui.silverweave.item.ItemCatalog;
import com.lucasluqui.silverweave.item.KnightItem;
import com.lucasluqui.silverweave.item.Slot;

/**
 * A search over every item, shown in the middle of the view over a dimmed window. Picking an item
 * holds it in the search bar so that more can be picked; enter with nothing typed puts them all on
 * at once. Entries for emptying each slot and the saved loadouts are listed too; a loadout is put
 * on as soon as it's picked.
 */
public class ItemSearch
{
  /**
   * Creates the search for a view inside the given window.
   *
   * @param onPick notified with the slot and item of each pick, in the order picked, once
   * they're applied; the item is null for slots to empty.
   * @param onLoad asked to put on the loadout with the given share code.
   */
  public ItemSearch (Window owner, Component view, IconCache icons, Favorites favorites,
    SavedLoadouts saved, BiConsumer<Slot, KnightItem> onPick, Consumer<String> onLoad)
  {
    _owner = owner;
    _view = view;
    _favorites = favorites;
    _saved = saved;
    _onPick = onPick;
    _onLoad = onLoad;
    _icons = icons;
    GraphicsDevice device = owner.getGraphicsConfiguration().getDevice();
    boolean translucent = Overlay.isTranslucencySupported(owner);

    // the dimming over the window, which closes the search when clicked
    _dim = new JWindow(owner);
    _dim.setFocusableWindowState(false);
    JPanel shade = new JPanel();
    shade.setBackground(Color.BLACK);
    // (without any translucency, it would black the window out)
    _dims = translucent || device.isWindowTranslucencySupported(
      GraphicsDevice.WindowTranslucency.TRANSLUCENT);
    if (translucent) {
      // the window's own background does the shading
      shade.setOpaque(false);
      _dim.setBackground(DIM);
    } else if (_dims) {
      _dim.setOpacity(DIM.getAlpha() / 255f);
    }
    _dim.setContentPane(shade);
    shade.addMouseListener(new MouseAdapter() {
      @Override public void mousePressed (MouseEvent e) {
        close();
      }
    });

    // the search field, in a pill like the controls over the view
    _field = new JTextField();
    _field.setFont(Theme.font(15f, Font.PLAIN));
    _field.setForeground(Theme.TEXT);
    _field.setCaretColor(Theme.TEXT);
    _field.setOpaque(false);
    _field.setBackground(new Color(0, 0, 0, 0));
    _field.setBorder(BorderFactory.createEmptyBorder());
    _field.getDocument().addDocumentListener(new DocumentListener() {
      @Override public void insertUpdate (DocumentEvent e) {
        refilter();
      }
      @Override public void removeUpdate (DocumentEvent e) {
        refilter();
      }
      @Override public void changedUpdate (DocumentEvent e) {
        refilter();
      }
    });
    // Tab steps through the results instead of moving the focus away
    _field.setFocusTraversalKeysEnabled(false);
    _field.addKeyListener(new KeyAdapter() {
      @Override public void keyPressed (KeyEvent e) {
        switch (e.getKeyCode()) {
          case KeyEvent.VK_TAB: cycle(e.isShiftDown() ? -1 : 1); break;
          case KeyEvent.VK_DOWN: move(1); break;
          case KeyEvent.VK_UP: move(-1); break;
          case KeyEvent.VK_PAGE_DOWN: move(VISIBLE_ROWS - 1); break;
          case KeyEvent.VK_PAGE_UP: move(1 - VISIBLE_ROWS); break;
          case KeyEvent.VK_ENTER: enter(e.isControlDown()); break;
          case KeyEvent.VK_ESCAPE: close(); break;
          case KeyEvent.VK_BACK_SPACE:
            // with nothing typed, takes back the last item picked
            if (_field.getText().isEmpty() && !_picked.isEmpty()) {
              unpick(_picked.get(_picked.size() - 1));
              break;
            }
            return;
          default: return;
        }
        e.consume();
      }
    });
    JLabel glyph = new JLabel(Glyph.SEARCH.icon(18, Theme.MUTED));
    // the same see-through fill as the controls over the view, where the system allows it
    Color fill = translucent ? Theme.OVERLAY : Theme.BACKGROUND;
    JPanel pill = new RoundedPanel(0, fill, new BorderLayout(10, 0));
    pill.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 22));
    pill.add(glyph, BorderLayout.WEST);
    // the items picked so far, ahead of the text
    // as tall as the text, so picks don't change the bar's height
    _chips = new JPanel() {
      @Override public Dimension getPreferredSize () {
        return new Dimension(super.getPreferredSize().width, _field.getPreferredSize().height);
      }
    };
    _chips.setLayout(new BoxLayout(_chips, BoxLayout.X_AXIS));
    _chips.setOpaque(false);
    JPanel bar = new JPanel(new BorderLayout());
    bar.setOpaque(false);
    bar.add(_chips, BorderLayout.WEST);
    bar.add(_field, BorderLayout.CENTER);
    pill.add(bar, BorderLayout.CENTER);

    // the matching items
    _list = new ItemList(icons, null, ICON_SIZE, null);
    _list.setDetails(ItemSearch::describe);
    _list.setFocusable(false);
    _list.setVisibleRowCount(VISIBLE_ROWS);
    // a press and release on the same result picks it (mouseClicked is dropped whenever the mouse
    // moves slightly in between, which made clicks seem ignored)
    _list.addMouseListener(new MouseAdapter() {
      @Override public void mousePressed (MouseEvent e) {
        _pressed = SwingUtilities.isLeftMouseButton(e) ? resultAt(e.getPoint()) : -1;
      }
      @Override public void mouseReleased (MouseEvent e) {
        int index = _pressed;
        _pressed = -1;
        if (index >= 0 && SwingUtilities.isLeftMouseButton(e) && resultAt(e.getPoint()) == index) {
          pick(_list.getModel().getElementAt(index));
        }
      }
      protected int resultAt (Point point) {
        int index = _list.locationToIndex(point);
        return index >= 0 && _list.getCellBounds(index, index).contains(point) ? index : -1;
      }
      protected int _pressed = -1;
    });
    JScrollPane scroll = new JScrollPane(_list);
    Theme.overlayScroll(scroll);
    JPanel results = new RoundedPanel(RESULTS_RADIUS, fill, new BorderLayout());
    // less room on the right while the scroll bar is up, so it sits out by the edge (without it,
    // the rows keep even margins)
    JScrollBar scrollBar = scroll.getVerticalScrollBar();
    Runnable fitBorder = () -> results.setBorder(BorderFactory.createEmptyBorder(8, 8, 8,
      scrollBar.isVisible() ? 8 - SCROLL_SHIFT : 8));
    fitBorder.run();
    scrollBar.addComponentListener(new ComponentAdapter() {
      @Override public void componentShown (ComponentEvent e) {
        fitBorder.run();
      }
      @Override public void componentHidden (ComponentEvent e) {
        fitBorder.run();
      }
    });
    results.add(scroll, BorderLayout.CENTER);

    JPanel content = new JPanel() {
      @Override public void doLayout () {
        super.doLayout();
        if (!translucent) {
          // without translucency, the window is cut down to the search bar and results
          Area shape = new Area(outline(pill, pill.getHeight()));
          shape.add(new Area(outline(results, RESULTS_RADIUS * 2)));
          Overlay.clip(_dialog, shape);
        }
      }
    };
    content.setOpaque(false);
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    pill.setAlignmentX(Component.CENTER_ALIGNMENT);
    results.setAlignmentX(Component.CENTER_ALIGNMENT);
    content.add(pill);
    content.add(Box.createVerticalStrut(8));
    content.add(results);

    _dialog = new JDialog(owner);
    _dialog.setUndecorated(true);
    if (translucent) {
      _dialog.setBackground(new Color(0, 0, 0, 0));
      _dialog.getRootPane().setOpaque(false);
    }
    JPanel root = new JPanel(new BorderLayout());
    // (without translucency, whatever of it the window shape leaves shows as part of the panels)
    root.setOpaque(!translucent);
    root.setBackground(Theme.BACKGROUND);
    root.add(content, BorderLayout.CENTER);
    _dialog.setContentPane(root);
    _dialog.setSize(WIDTH, pill.getPreferredSize().height + 8 +
      results.getPreferredSize().height);
    // clicking anywhere else puts the search away
    _dialog.addWindowFocusListener(new WindowAdapter() {
      @Override public void windowLostFocus (WindowEvent e) {
        close();
      }
    });
  }

  /**
   * Returns the rounded outline of one of the search's panels, in the coordinates of the window.
   */
  protected Shape outline (JComponent panel, float arc)
  {
    Rectangle bounds = SwingUtilities.convertRectangle(panel.getParent(), panel.getBounds(),
      _dialog);
    return new RoundRectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);
  }

  /**
   * Sets the items that can be searched.
   */
  public void setCatalog (ItemCatalog catalog)
  {
    List<KnightItem> items = new ArrayList<>();
    for (Slot slot : Slot.values()) {
      items.addAll(catalog.getItems(slot));
    }
    Collections.sort(items);
    _items = items;
    _catalog = catalog;
    _helmets.clear();
    // (the item picker shows its icons at the same size)
    _icons.preload(items, ICON_SIZE);
  }

  public boolean isOpen ()
  {
    return _dialog.isVisible();
  }

  /**
   * Shows the search, empty, in the middle of the view.
   */
  public void open ()
  {
    if (_items == null || isOpen() || !_view.isShowing()) {
      return;
    }
    Container pane = ((RootPaneContainer)_owner).getContentPane();
    _dim.setBounds(pane.getLocationOnScreen().x, pane.getLocationOnScreen().y,
      pane.getWidth(), pane.getHeight());
    Point origin = _view.getLocationOnScreen();
    Dimension size = _dialog.getSize();
    _dialog.setLocation(origin.x + (_view.getWidth() - size.width) / 2,
      origin.y + (_view.getHeight() - size.height) / 2);
    _picked.clear();
    updateChips();
    _field.setText("");
    refilter();
    if (_dims) {
      _dim.setVisible(true);
      _dim.toFront();
    }
    _dialog.setVisible(true);
    _dialog.toFront();
    _field.requestFocusInWindow();
  }

  public void close ()
  {
    _dialog.setVisible(false);
    _dim.setVisible(false);
  }

  /**
   * Rebuilds the results from the search text: the closest matches first, and among equally
   * close ones, slots to empty and then favorites. The saved loadouts that match come last, below
   * every item however closely they match. With no text, the favorites are shown.
   */
  protected void refilter ()
  {
    String query = Relevance.normalize(_field.getText());
    List<Object> entries = new ArrayList<>();
    if (query.isEmpty()) {
      for (KnightItem item : _items) {
        if (_favorites.contains(item)) {
          entries.add(item);
        }
      }
      if (entries.isEmpty()) {
        entries.add(new ItemList.Placeholder(String.format(Locale.ROOT,
          "Type to search %,d items", _items.size())));
      }
    } else {
      List<List<Removal>> removals = Relevance.tiers();
      for (Removal removal : REMOVALS) {
        int tier = removal.rank(query);
        if (tier >= 0) {
          removals.get(tier).add(removal);
        }
      }
      List<List<KnightItem>> items = Relevance.tiers();
      for (KnightItem item : _items) {
        int tier = rank(item, query);
        if (tier >= 0) {
          items.get(tier).add(item);
        }
      }
      List<List<SavedEntry>> loadouts = Relevance.tiers();
      for (SavedLoadouts.Entry entry : _saved.getEntries()) {
        SavedEntry saved = new SavedEntry(entry);
        int tier = saved.rank(query);
        if (tier >= 0) {
          loadouts.get(tier).add(saved);
        }
      }
      for (int ii = 0; ii < Relevance.TIERS; ii++) {
        entries.addAll(removals.get(ii));
        entries.addAll(_favorites.arrange(items.get(ii)));
      }
      for (List<SavedEntry> tier : loadouts) {
        entries.addAll(tier);
      }
      if (entries.isEmpty()) {
        entries.add(new ItemList.Placeholder("No items match"));
      }
    }
    _list.setEntries(entries);
    // with items picked and nothing typed, enter applies them rather than picking a favorite
    if (isPickable(entries.get(0)) && !(query.isEmpty() && !_picked.isEmpty())) {
      _list.setSelectedIndex(0);
      _list.ensureIndexIsVisible(0);
    }
  }

  /**
   * Moves the selection through the results.
   */
  protected void move (int delta)
  {
    int size = _list.getModel().getSize();
    if (size == 0 || !isPickable(_list.getModel().getElementAt(0))) {
      return;
    }
    int index = Math.max(0, Math.min(size - 1, _list.getSelectedIndex() + delta));
    _list.setSelectedIndex(index);
    _list.ensureIndexIsVisible(index);
  }

  /**
   * Steps the selection one result down (or up), going back to the search field, with nothing
   * selected, after the last result (or before the first).
   */
  protected void cycle (int delta)
  {
    int size = _list.getModel().getSize();
    if (size == 0 || !isPickable(_list.getModel().getElementAt(0))) {
      return;
    }
    int index = _list.getSelectedIndex();
    index = index < 0 ? (delta > 0 ? 0 : size - 1) : index + delta;
    if (index < 0 || index >= size) {
      _list.clearSelection();
      _list.ensureIndexIsVisible(0);
    } else {
      _list.setSelectedIndex(index);
      _list.ensureIndexIsVisible(index);
    }
  }

  /**
   * Picks the selected result, and with nothing selected (or with CTRL held) puts the items
   * picked on the knight.
   */
  protected void enter (boolean apply)
  {
    Object selected = _list.getSelectedValue();
    if (isPickable(selected)) {
      pick(selected);
    } else {
      apply = true;
    }
    if (apply && !_picked.isEmpty()) {
      List<Object> picked = new ArrayList<>(_picked);
      close();
      for (Object value : picked) {
        _onPick.accept(slotOf(value), value instanceof KnightItem ? (KnightItem)value : null);
      }
    }
  }

  /**
   * Checks whether a result can be picked: an item, a slot to empty, or a saved loadout.
   */
  protected static boolean isPickable (Object value)
  {
    return value instanceof KnightItem || value instanceof Removal || value instanceof SavedEntry;
  }

  /**
   * Returns the slot a pick is for.
   */
  protected static Slot slotOf (Object value)
  {
    return value instanceof KnightItem ? ((KnightItem)value).slot : ((Removal)value).slot;
  }

  /**
   * Holds the given result in the search bar, if it can be picked, in place of any picked for the
   * same slot, and clears the text for the next search. A saved loadout is put on right away
   * instead, with anything picked so far then put on over it.
   */
  protected void pick (Object value)
  {
    if (value instanceof SavedEntry) {
      List<Object> picked = new ArrayList<>(_picked);
      _picked.clear();
      close();
      _onLoad.accept(((SavedEntry)value).entry.code);
      for (Object other : picked) {
        _onPick.accept(slotOf(other), other instanceof KnightItem ? (KnightItem)other : null);
      }
      return;
    }
    if (!isPickable(value)) {
      return;
    }
    Slot slot = slotOf(value);
    _picked.removeIf(other -> slotOf(other) == slot);
    _picked.add(value);
    updateChips();
    _field.setText("");
    refilter();
    _field.requestFocusInWindow();
  }

  protected void unpick (Object value)
  {
    _picked.remove(value);
    updateChips();
    refilter();
    _field.requestFocusInWindow();
  }

  /**
   * Shows the items picked in the search bar, and what enter does about them.
   */
  protected void updateChips ()
  {
    _chips.removeAll();
    boolean removing = false;
    int count = _picked.size();
    // the chips share a fixed amount of the bar, so the text keeps its room; when there are too
    // many for their names to be readable, they show just their icons
    int maxWidth = count == 0 ? 0 : Math.min(CHIP_MAX_WIDTH, CHIPS_WIDTH / count);
    boolean named = maxWidth >= CHIP_MIN_NAMED_WIDTH;
    for (Object value : _picked) {
      _chips.add(new Chip(value, named, named ? maxWidth : Integer.MAX_VALUE));
      removing |= value instanceof Removal;
    }
    _field.putClientProperty("JTextField.placeholderText", count == 0 ? null : removing ?
      "Press Enter to apply " + (count == 1 ? "1 change" : count + " changes") :
      "Press Enter to put on " + (count == 1 ? "1 item" : count + " items"));
    _chips.revalidate();
    _chips.repaint();
  }

  /**
   * A pick, shown by its icon and name in the search bar. Clicking it takes it back.
   */
  protected class Chip extends JLabel
  {
    /**
     * @param named whether to show the pick's name beside its icon.
     * @param maxWidth the most room the chip takes, its name cut short to fit.
     */
    public Chip (Object value, boolean named, int maxWidth)
    {
      _value = value;
      _maxWidth = maxWidth;
      // asked for again each time it's painted, as an item's icon may be a placeholder until now
      setIcon(new Icon() {
        @Override public void paintIcon (Component c, Graphics g, int x, int y) {
          currentIcon().paintIcon(c, g, x, y);
        }
        @Override public int getIconWidth () {
          return CHIP_ICON_SIZE;
        }
        @Override public int getIconHeight () {
          return CHIP_ICON_SIZE;
        }
      });
      if (named) {
        setText(value.toString());
        setFont(Theme.font(12.5f, Font.PLAIN));
        setForeground(Theme.TEXT);
        setIconTextGap(5);
      }
      setToolTipText(value + " (click to remove)");
      // (the gap after the chip is part of its right inset, outside the background)
      setBorder(BorderFactory.createEmptyBorder(1, 3, 1, (named ? 8 : 3) + CHIP_GAP));
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      Clickable.install(this, () -> unpick(value), hover -> {
        _hover = hover;
        repaint();
      });
    }

    @Override
    public Dimension getPreferredSize ()
    {
      // (a name that doesn't fit is cut short with "...")
      Dimension size = super.getPreferredSize();
      return new Dimension(Math.min(size.width, _maxWidth), size.height);
    }

    @Override
    protected void paintComponent (Graphics g)
    {
      Graphics2D g2 = Theme.smooth(g);
      g2.setColor(_hover ? Theme.SURFACE_HOVER : Theme.SURFACE);
      g2.fillRoundRect(0, 0, getWidth() - CHIP_GAP, getHeight(), CHIP_ARC, CHIP_ARC);
      g2.dispose();
      super.paintComponent(g);
    }

    /**
     * Returns the pick's icon, a blank one while an item's is still loading (repainting the chip
     * once it is in).
     */
    protected Icon currentIcon ()
    {
      return _value instanceof Removal ? ((Removal)_value).getIcon(CHIP_ICON_SIZE) :
        _icons.get((KnightItem)_value, CHIP_ICON_SIZE, this);
    }

    protected final Object _value;
    protected final int _maxWidth;
    protected boolean _hover;
  }

  /**
   * Ranks an item against a non-empty query from {@link Relevance#normalize}: its
   * {@link Relevance} tier, or -1 if it doesn't match. Besides the item's name and config name, the
   * query may name where the item goes, as shown beside it in the results.
   */
  protected static int rank (KnightItem item, String query)
  {
    return Relevance.rank(item.displayName, item.configName + " " + describe(item), query);
  }

  /**
   * Returns where an item goes, shown beside it in the results.
   */
  protected static String describe (KnightItem item)
  {
    Slot slot = item.slot;
    if (slot.accessory) {
      return slot.section.label + " - " + slot.label + " Accessory";
    } else if (slot == Slot.WEAPON) {
      return item.category == null ? slot.label : item.category;
    } else if (ItemCatalog.COSTUME.equals(item.category)) {
      return slot.label + " Costume";
    }
    return slot.label;
  }

  /**
   * An entry for taking off whatever is in a slot.
   */
  protected static class Removal
    implements ItemList.Command
  {
    public final Slot slot;
    public final String label;

    public Removal (Slot slot)
    {
      this.slot = slot;
      label = "No " + slot.label;
    }

    /**
     * Ranks the entry against a query from {@link Relevance#normalize}: its {@link Relevance}
     * tier, or -1 if it doesn't match. The query may also name the section of the slot.
     */
    public int rank (String query)
    {
      return Relevance.rank(label, getDetail(), query);
    }

    @Override
    public Icon getIcon (int size)
    {
      // the no-entry sign, smaller than the box, as item icons leave some room around them
      int glyphSize = Math.round(size * 0.7f);
      Icon glyph = Glyph.BLOCKED.icon(glyphSize, Theme.MUTED);
      return new Icon() {
        @Override public void paintIcon (Component c, Graphics g, int x, int y) {
          glyph.paintIcon(c, g, x + (size - glyphSize) / 2, y + (size - glyphSize) / 2);
        }
        @Override public int getIconWidth () {
          return size;
        }
        @Override public int getIconHeight () {
          return size;
        }
      };
    }

    @Override
    public String getDetail ()
    {
      return slot.accessory ? slot.section.label + " - " + slot.label + " Accessory" :
        slot.label;
    }

    @Override
    public String toString ()
    {
      return label;
    }
  }

  /**
   * An entry for putting on a saved loadout, shown with its helmet's icon.
   */
  protected class SavedEntry
    implements ItemList.Command
  {
    public final SavedLoadouts.Entry entry;

    public SavedEntry (SavedLoadouts.Entry entry)
    {
      this.entry = entry;
    }

    /**
     * Ranks the entry against a query from {@link Relevance#normalize}: its {@link Relevance}
     * tier, or -1 if it doesn't match.
     */
    public int rank (String query)
    {
      return Relevance.rank(entry.getName(), getDetail(), query);
    }

    @Override
    public Icon getIcon (int size)
    {
      KnightItem helmet = getHelmet(entry.code);
      return helmet != null && helmet.icon != null ? _icons.get(helmet, size, _list) :
        Glyph.SAVE.icon(size, Theme.MUTED);
    }

    @Override
    public String getDetail ()
    {
      return "Saved Loadout";
    }

    @Override
    public String toString ()
    {
      return entry.getName();
    }
  }

  /**
   * Returns the helmet of the loadout with the given share code, or null if it has none.
   */
  protected KnightItem getHelmet (String code)
  {
    return _helmets.computeIfAbsent(code, c -> {
      try {
        return ShareCode.decode(c, _catalog).items.get(Slot.HELMET);
      } catch (ShareCode.InvalidCodeException e) {
        return null;
      }
    });
  }

  /**
   * A panel filled and outlined like the controls over the view.
   */
  protected static class RoundedPanel extends JPanel
  {
    /**
     * @param radius the corner radius, or 0 for a pill.
     */
    public RoundedPanel (int radius, Color fill, LayoutManager layout)
    {
      super(layout);
      _radius = radius;
      _fill = fill;
      setOpaque(false);
    }

    @Override
    protected void paintComponent (Graphics g)
    {
      Graphics2D g2 = Theme.smooth(g);
      float w = getWidth() - 1, h = getHeight() - 1;
      float arc = _radius > 0 ? _radius * 2 : h;
      RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w, h, arc, arc);
      g2.setColor(_fill);
      g2.fill(shape);
      g2.setColor(Theme.BORDER);
      g2.draw(shape);
      g2.dispose();
    }

    protected final int _radius;
    protected final Color _fill;
  }

  /** The entries for taking off what's in each slot of gear and accessories (the style slots
   * always hold something). */
  protected static final List<Removal> REMOVALS = new ArrayList<>();
  static {
    for (Slot slot : Slot.values()) {
      if (slot.section != Slot.Section.STYLE) {
        REMOVALS.add(new Removal(slot));
      }
    }
  }

  protected static final int WIDTH = 560;

  /** The number of results shown at once, and the size of their icons. */
  protected static final int VISIBLE_ROWS = 9, ICON_SIZE = 28;

  protected static final int RESULTS_RADIUS = 12;

  /** How much less room the results leave on the right, so their scroll bar sits out by the
   * edge. */
  protected static final int SCROLL_SHIFT = 5;

  /** The size of the picked items' icons in the search bar, the space after each, and the
   * roundness of their backgrounds. */
  protected static final int CHIP_ICON_SIZE = 18, CHIP_GAP = 4, CHIP_ARC = 8;

  /** The room all the chips share in the search bar, the most any one takes, and the least that
   * leaves enough of a name to be worth showing. */
  protected static final int CHIPS_WIDTH = 280, CHIP_MAX_WIDTH = 150, CHIP_MIN_NAMED_WIDTH = 70;

  /** The color laid over the window while the search is up. */
  protected static final Color DIM = new Color(0, 0, 0, 120);

  protected final Window _owner;
  protected final Component _view;
  protected final Favorites _favorites;
  protected final SavedLoadouts _saved;
  protected final BiConsumer<Slot, KnightItem> _onPick;
  protected final Consumer<String> _onLoad;
  protected final IconCache _icons;
  protected final JWindow _dim;
  protected final boolean _dims;
  protected final JDialog _dialog;
  protected final JTextField _field;
  protected final ItemList _list;

  /** The picks so far (items, and slots to empty), in the order picked, and where they're
   * shown. */
  protected final List<Object> _picked = new ArrayList<>();
  protected final JPanel _chips;

  /** Every item, in name order, or null until the catalog is in. */
  protected List<KnightItem> _items;

  /** The catalog the items are from, and the saved loadouts' helmets read from it so far, by
   * share code. */
  protected ItemCatalog _catalog;
  protected final Map<String, KnightItem> _helmets = new HashMap<>();
}
