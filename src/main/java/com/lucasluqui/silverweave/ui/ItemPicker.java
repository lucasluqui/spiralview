package com.lucasluqui.silverweave.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.formdev.flatlaf.FlatClientProperties;
import com.lucasluqui.silverweave.Favorites;
import com.lucasluqui.silverweave.item.KnightItem;
import com.lucasluqui.silverweave.item.Slot;

/**
 * A searchable list of the items for one slot. Selecting an item puts it on the knight right away,
 * so the list can be browsed with the arrow keys while watching the model.
 */
public class ItemPicker extends JPanel
{
  public ItemPicker (IconCache icons, Favorites favorites, Runnable onBack)
  {
    super(new BorderLayout());
    _favorites = favorites;
    setOpaque(false);

    // header: back button and title
    JPanel header = new JPanel(new BorderLayout(12, 0));
    header.setOpaque(false);
    header.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
    JButton back = Theme.sidePill(new JButton("‹  Back"), Theme.SIDE_BUTTON, 10f);
    back.setPreferredSize(new Dimension(back.getPreferredSize().width, BACK_HEIGHT));
    back.addActionListener(e -> onBack.run());
    header.add(back, BorderLayout.WEST);
    _title = new JLabel();
    _title.setFont(Theme.sideFont(13f, Font.BOLD));
    _title.setForeground(Theme.SIDE_TEXT);
    header.add(_title, BorderLayout.CENTER);

    // search field and optional category chips
    _search = new JTextField();
    _search.putClientProperty("JTextField.showClearButton", true);
    _search.setFont(Theme.sideFont(11f, Font.PLAIN));
    Theme.sideField(_search);
    _search.getDocument().addDocumentListener(new DocumentListener() {
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
    // let the arrow keys drive the list while typing
    _search.addKeyListener(new KeyAdapter() {
      @Override public void keyPressed (KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_UP) {
          int idx = _list.getSelectedIndex() + (code == KeyEvent.VK_DOWN ? 1 : -1);
          if (idx >= 0 && idx < _list.getModel().getSize()) {
            _list.setSelectedIndex(idx);
            _list.ensureIndexIsVisible(idx);
          }
          e.consume();
        } else if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_ESCAPE) {
          onBack.run();
        }
      }
    });
    _chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    _chips.setOpaque(false);

    JPanel top = new JPanel();
    top.setOpaque(false);
    top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
    header.setAlignmentX(LEFT_ALIGNMENT);
    _search.setAlignmentX(LEFT_ALIGNMENT);
    _chips.setAlignmentX(LEFT_ALIGNMENT);
    top.add(header);
    top.add(Box.createVerticalStrut(14));
    top.add(_search);
    top.add(Box.createVerticalStrut(8));
    top.add(_chips);
    top.add(Box.createVerticalStrut(6));
    _search.setMaximumSize(new Dimension(Integer.MAX_VALUE, _search.getPreferredSize().height + 8));
    add(top, BorderLayout.NORTH);

    // see-through, so the rows are picked out with rounded highlights on the card behind
    _list = new ItemList(icons, favorites, ICON_SIZE, null);
    _list.setStyle(Theme.SIDE_SELECTION, Theme.SIDE_CARD_HOVER, Theme.SIDE_TEXT,
      Theme.SIDE_DESCRIPTION, Theme.sideFont(10.5f, Font.BOLD));
    _list.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting() && !_updating && _onPick != null) {
        Object value = _list.getSelectedValue();
        // (a custom item stays in the color it was given when picked again)
        if (value != null && (_current == null || _current.getBase() != value)) {
          _current = (value == NONE) ? null : (KnightItem)value;
          _onPick.accept(_current);
        }
        updateRecolor();
      }
    });
    _list.addMouseListener(new MouseAdapter() {
      @Override public void mouseClicked (MouseEvent e) {
        if (e.getClickCount() == 2) {
          onBack.run();
        }
      }
    });
    _list.addKeyListener(new KeyAdapter() {
      @Override public void keyPressed (KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          onBack.run();
        }
      }
    });
    JScrollPane scroll = new JScrollPane(_list);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    Theme.sideScroll(scroll, Theme.SIDE_CARD);
    add(new RoundedPanel(scroll, Theme.SIDE_CARD, Theme.SIDE_ARC, 6), BorderLayout.CENTER);

    // the colors to recolor custom items in
    _recolor = new JPanel(new BorderLayout(0, 6));
    _recolor.setOpaque(false);
    _colorLabel = new JLabel();
    _colorLabel.setFont(Theme.sideFont(10.5f, Font.BOLD));
    _colorLabel.setForeground(Theme.SIDE_DESCRIPTION);
    _colorLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
    _colorChooser = new RecolorChooser(this::recolor);
    _colorChooser.addKeyListener(new KeyAdapter() {
      @Override public void keyPressed (KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          onBack.run();
        }
      }
    });
    _recolor.add(_colorLabel, BorderLayout.NORTH);
    _recolor.add(_colorChooser, BorderLayout.CENTER);
    JPanel south = new JPanel(new BorderLayout());
    south.setOpaque(false);
    south.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
    south.add(new RoundedPanel(_recolor, Theme.SIDE_CARD, Theme.SIDE_ARC, 10), BorderLayout.CENTER);
    _recolorCard = south;
    _recolorCard.setVisible(false);
    add(_recolorCard, BorderLayout.SOUTH);

    // newly marked favorites move up right away, without scrolling the list about
    favorites.addChangeListener(() -> {
      if (_onPick != null) {
        refilter(false);
      }
    });
  }

  /**
   * Shows the items for a slot.
   *
   * @param current the item currently in the slot, which starts out selected.
   * @param onPick notified when the user selects an item (null for "None").
   */
  public void open (Slot slot, List<KnightItem> items, KnightItem current, Consumer<KnightItem> onPick)
  {
    _onPick = null;
    _items = items;
    _current = current;
    _category = null;
    _title.setText(slot.accessory ? slot.section.label + " - " + slot.label + " Accessory" :
      slot.label);
    _search.putClientProperty("JTextField.placeholderText",
      "Search " + String.format(Locale.ROOT, "%,d", items.size()) + " items...");

    // weapons and other slots with categories get filter chips
    _chips.removeAll();
    Set<String> categories = new LinkedHashSet<>();
    for (KnightItem item : items) {
      if (item.category != null) {
        categories.add(item.category);
      }
    }
    _chips.setVisible(categories.size() > 1);
    if (categories.size() > 1) {
      ButtonGroup group = new ButtonGroup();
      List<String> chips = new ArrayList<>();
      chips.add(null);
      chips.addAll(categories);
      for (String category : chips) {
        // pills, the chosen one filled in grey like the launcher's buttons
        JToggleButton chip = new JToggleButton(category == null ? "All" : category);
        chip.setFont(Theme.sideFont(9.5f, Font.BOLD));
        chip.setFocusable(false);
        chip.setForeground(Theme.SIDE_DESCRIPTION);
        chip.setBackground(Theme.SIDE_CARD);
        chip.setMargin(new Insets(3, 10, 3, 10));
        chip.putClientProperty(FlatClientProperties.STYLE, "arc: 999; borderWidth: 0; " +
          "focusWidth: 0; hoverBackground: " + Theme.hex(Theme.SIDE_CARD_HOVER) + "; selectedBackground: " +
          Theme.hex(Theme.SIDE_BUTTON) + "; selectedForeground: " + Theme.hex(Theme.SIDE_TEXT));
        chip.setSelected(category == null);
        chip.addActionListener(e -> {
          _category = category;
          refilter();
        });
        group.add(chip);
        _chips.add(chip);
      }
    }
    _chips.revalidate();

    _search.setText("");
    refilter();
    _onPick = onPick;
    updateRecolor();
    SwingUtilities.invokeLater(() -> _search.requestFocusInWindow());
  }

  /**
   * Rebuilds the visible list from the search text and category.
   */
  protected void refilter ()
  {
    refilter(true);
  }

  /**
   * Rebuilds the visible list from the search text and category: the closest matches first, and
   * favorites first among equally close ones.
   *
   * @param reveal whether to scroll the current item into view.
   */
  protected void refilter (boolean reveal)
  {
    String query = Relevance.normalize(_search.getText());
    _updating = true;
    try {
      List<List<KnightItem>> tiers = Relevance.tiers();
      for (KnightItem item : _items) {
        if (_category == null || _category.equals(item.category)) {
          int tier = rank(item, query);
          if (tier >= 0) {
            tiers.get(tier).add(item);
          }
        }
      }
      List<Object> visible = new ArrayList<>();
      visible.add(NONE);
      // custom items come after all of the game's own, whatever their rank
      List<KnightItem> customs = new ArrayList<>();
      for (List<KnightItem> tier : tiers) {
        for (KnightItem item : _favorites.arrange(tier)) {
          if (item.isCustom()) {
            customs.add(item);
          } else {
            visible.add(item);
          }
        }
      }
      visible.addAll(customs);
      _list.setEntries(visible);
      int idx = _current == null ? 0 : visible.indexOf(_current.getBase());
      if (idx >= 0) {
        _list.setSelectedIndex(idx);
        if (reveal) {
          _list.ensureIndexIsVisible(idx);
        }
      } else {
        _list.clearSelection();
      }
    } finally {
      _updating = false;
    }
  }

  /**
   * Sets where the colors offered for custom items come from.
   */
  public void setColorChoices (Supplier<List<RecolorChooser.Choice>> choices)
  {
    _colorChoices = choices;
    _colorChooser.setChoices(new ArrayList<>());
  }

  /**
   * Shows the colors if the current item is a custom one, with its color chosen.
   */
  protected void updateRecolor ()
  {
    boolean custom = _current != null && _current.isCustom();
    if (custom && _colorChooser.getChoices().isEmpty() && _colorChoices != null) {
      _colorChooser.setChoices(_colorChoices.get());
    }
    if (custom) {
      RecolorChooser.Choice choice = _colorChooser.getChoice(_current.getRecolor());
      _colorLabel.setText("Color - " + (choice == null ? "Custom" : choice.describe()));
      _colorChooser.setSelected(_current.getRecolor());
    }
    if (custom != _recolorCard.isVisible()) {
      _recolorCard.setVisible(custom);
      revalidate();
      // the list's height changes, which may hide the selected item
      SwingUtilities.invokeLater(() -> {
        int idx = _list.getSelectedIndex();
        if (idx >= 0) {
          _list.ensureIndexIsVisible(idx);
        }
      });
    }
  }

  /**
   * Puts the current custom item on in another color.
   */
  protected void recolor (int color)
  {
    if (_current != null && _current.isCustom() && _onPick != null) {
      _current = _current.withRecolor(color);
      _onPick.accept(_current);
      updateRecolor();
    }
  }

  /**
   * Ranks an item against a query from {@link Relevance#normalize}: its {@link Relevance} tier,
   * or -1 if it doesn't match. Everything matches an empty query, equally.
   */
  protected static int rank (KnightItem item, String query)
  {
    return query.isEmpty() ? 0 : Relevance.rank(item.displayName, item.configName, query);
  }
  protected static final int BACK_HEIGHT = 28;

  protected static final int ICON_SIZE = 28;

  /** The list entry standing for an empty slot. */
  protected static final Object NONE = new ItemList.Placeholder("None");

  protected final Favorites _favorites;
  protected final JLabel _title, _colorLabel;
  protected final JPanel _recolor, _recolorCard;
  protected final RecolorChooser _colorChooser;
  protected Supplier<List<RecolorChooser.Choice>> _colorChoices;
  protected final JTextField _search;
  protected final JPanel _chips;
  protected final ItemList _list;

  protected List<KnightItem> _items = new ArrayList<>();
  protected KnightItem _current;
  protected String _category;
  protected Consumer<KnightItem> _onPick;
  protected boolean _updating;
}
