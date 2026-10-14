package com.lucasluqui.silverweave.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.lucasluqui.silverweave.Favorites;
import com.lucasluqui.silverweave.Loadout;
import com.lucasluqui.silverweave.item.ItemCatalog;
import com.lucasluqui.silverweave.item.KnightItem;
import com.lucasluqui.silverweave.item.Slot;

/**
 * The knight's style kit: personal color, height and eyes.
 */
public class StylePanel extends JPanel
{
  public StylePanel (Loadout loadout, IconCache icons, Favorites favorites)
  {
    _loadout = loadout;
    _favorites = favorites;
    setOpaque(false);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    add(Theme.sideHeader(Slot.COLOR.label));
    _colors = createList(icons, favorites, Slot.COLOR, 228);

    add(Theme.sideHeader(Slot.HEIGHT.label));
    // only a handful of heights, all shown at once
    _heights = createList(icons, null, Slot.HEIGHT, 0);

    add(Theme.sideHeader(Slot.EYES.label));
    _eyes = createList(icons, favorites, Slot.EYES, 260);
    add(Box.createVerticalGlue());

    favorites.addChangeListener(() -> {
      if (_catalog != null) {
        fill(false);
      }
    });
  }

  /**
   * Populates the panel with the style items from the catalog.
   */
  public void setCatalog (ItemCatalog catalog)
  {
    _catalog = catalog;
    fill(true);
    revalidate();
    repaint();
  }

  /**
   * Updates the controls to match the loadout.
   */
  public void refresh ()
  {
    refresh(true);
  }

  /**
   * Fills the lists from the catalog, favorites first.
   *
   * @param reveal whether to scroll the selected entries into view.
   */
  protected void fill (boolean reveal)
  {
    // colors in palette (colorization id) order, which runs around the hue wheel
    List<KnightItem> colors = new ArrayList<>(_catalog.getItems(Slot.COLOR));
    colors.sort(Comparator.comparingInt(KnightItem::getColorization));

    List<KnightItem> heights = new ArrayList<>(_catalog.getItems(Slot.HEIGHT));
    heights.sort(Comparator.comparingInt(h -> parseHeight(h.getHeight())));

    // the eyes knights start out with come first, standing in for no eyes item at all
    List<KnightItem> eyes = new ArrayList<>(_catalog.getItems(Slot.EYES));
    _defaultEyes = null;
    for (KnightItem eye : eyes) {
      if (eye.isDefaultEyes()) {
        _defaultEyes = eye;
        break;
      }
    }
    eyes.remove(_defaultEyes);
    List<Object> eyeEntries = new ArrayList<>();
    eyeEntries.add(_defaultEyes != null ? _defaultEyes : DEFAULT_EYES);
    eyeEntries.addAll(_favorites.arrange(eyes));

    _updating = true;
    try {
      _colors.setEntries(_favorites.arrange(colors));
      _heights.setEntries(heights);
      _eyes.setEntries(eyeEntries);
    } finally {
      _updating = false;
    }
    refresh(reveal);
  }

  protected void refresh (boolean reveal)
  {
    _updating = true;
    try {
      int colorization = _loadout.getColorization();
      select(_colors, item -> item.getColorization() == colorization, reveal);

      KnightItem height = _loadout.get(Slot.HEIGHT);
      String value = height == null ? "0" : height.getHeight();
      select(_heights, item -> value.equals(item.getHeight()), reveal);

      KnightItem eyes = _loadout.get(Slot.EYES);
      if (eyes != null && eyes != _defaultEyes) {
        _eyes.setSelectedValue(eyes, reveal);
      } else if (_eyes.getModel().getSize() > 0) {
        // the default eyes always come first
        _eyes.setSelectedIndex(0);
        if (reveal) {
          _eyes.ensureIndexIsVisible(0);
        }
      }
    } finally {
      _updating = false;
    }
  }

  /**
   * Selects the first item in a list that matches, or nothing.
   */
  protected static void select (ItemList list, Predicate<KnightItem> match,
    boolean reveal)
  {
    for (int ii = 0, nn = list.getModel().getSize(); ii < nn; ii++) {
      Object value = list.getModel().getElementAt(ii);
      if (value instanceof KnightItem && match.test((KnightItem)value)) {
        list.setSelectedIndex(ii);
        if (reveal) {
          list.ensureIndexIsVisible(ii);
        }
        return;
      }
    }
    list.clearSelection();
  }

  /**
   * Creates and adds the list for a style slot.
   *
   * @param height the height of the list, or 0 to show every entry.
   */
  protected ItemList createList (IconCache icons, Favorites favorites, Slot slot, int height)
  {
    // see-through, so the rows are picked out with rounded highlights on the card behind
    ItemList list = new ItemList(icons, favorites, 24, null);
    list.setStyle(Theme.SIDE_SELECTION, Theme.SIDE_CARD_HOVER, Theme.SIDE_TEXT,
      Theme.SIDE_DESCRIPTION, Theme.sideFont(10.5f, Font.BOLD));
    list.addListSelectionListener(e -> {
      Object value = list.getSelectedValue();
      if (!e.getValueIsAdjusting() && !_updating && value != null) {
        pick(slot, value instanceof KnightItem ? (KnightItem)value : null);
      }
    });
    JScrollPane scroll = new JScrollPane(list) {
      @Override public Dimension getPreferredSize () {
        return new Dimension(10, height > 0 ? height - 2 * CARD_INSET :
          list.getPreferredSize().height);
      }
    };
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    Theme.sideScroll(scroll, Theme.SIDE_CARD);
    if (height == 0) {
      scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    }
    RoundedPanel card = new RoundedPanel(scroll, Theme.SIDE_CARD, Theme.SIDE_ARC, CARD_INSET);
    card.setAlignmentX(LEFT_ALIGNMENT);
    add(card);
    return list;
  }

  /**
   * Puts a style item on the knight; the defaults are stored as an empty slot.
   */
  protected void pick (Slot slot, KnightItem item)
  {
    boolean standard = item == null ||
      (slot == Slot.COLOR && item.getColorization() == ItemCatalog.DEFAULT_COLORIZATION) ||
      (slot == Slot.HEIGHT && "0".equals(item.getHeight())) ||
      (slot == Slot.EYES && item.isDefaultEyes());
    _loadout.set(slot, standard ? null : item);
  }

  protected static int parseHeight (String value)
  {
    try {
      return Integer.parseInt(value.replace("+", ""));
    } catch (NumberFormatException | NullPointerException e) {
      return 0;
    }
  }

  /** The space between the lists and the edges of the cards holding them. */
  protected static final int CARD_INSET = 6;

  /** The eye list entry standing for the default eyes, if the catalog has no item for them. */
  protected static final Object DEFAULT_EYES = new ItemList.Placeholder("Normal");

  protected final Loadout _loadout;
  protected final Favorites _favorites;
  protected final ItemList _colors, _heights, _eyes;
  protected ItemCatalog _catalog;
  protected KnightItem _defaultEyes;
  protected boolean _updating;
}
