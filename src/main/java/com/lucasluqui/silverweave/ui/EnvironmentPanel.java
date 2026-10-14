package com.lucasluqui.silverweave.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.formdev.flatlaf.FlatClientProperties;

/**
 * Lets the user pick the skybox shown around the knight.
 */
public class EnvironmentPanel extends JPanel
{
  /**
   * A selectable skybox.
   */
  public static final class Choice
  {
    /** The scene global config name. */
    public final String name;

    /** The name shown to the user. */
    public final String label;

    public Choice (String name, String label)
    {
      this.name = name;
      this.label = label;
    }

    @Override
    public String toString ()
    {
      return label;
    }
  }

  /**
   * A setting of the skybox shown (one of its model's choice parameters, like its variant).
   */
  public static final class Option
  {
    /** The parameter name, which is also shown. */
    public final String name;

    /** The values to choose from, and the one chosen. */
    public final List<String> values;
    public final String value;

    public Option (String name, List<String> values, String value)
    {
      this.name = name;
      this.values = values;
      this.value = value;
    }
  }

  /**
   * @param onSelect notified with the name of the chosen skybox, or null for none.
   * @param onOption notified with the name and new value of a skybox option the user changes.
   */
  public EnvironmentPanel (Consumer<String> onSelect, BiConsumer<String, String> onOption)
  {
    _onOption = onOption;
    super(new BorderLayout());
    setOpaque(false);
    setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));

    JPanel top = new JPanel();
    top.setOpaque(false);
    top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
    top.add(Theme.sideHeader("Skybox"));
    _search = new JTextField();
    _search.putClientProperty("JTextField.showClearButton", true);
    _search.putClientProperty("JTextField.placeholderText", "Search skyboxes...");
    _search.setFont(Theme.sideFont(11f, Font.PLAIN));
    Theme.sideField(_search);
    _search.setAlignmentX(LEFT_ALIGNMENT);
    _search.setMaximumSize(new Dimension(Integer.MAX_VALUE, _search.getPreferredSize().height + 8));
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
    top.add(_search);
    top.add(Box.createVerticalStrut(8));
    add(top, BorderLayout.NORTH);

    _list = new JList<>(_model);
    _list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    _list.setFixedCellHeight(30);
    _list.setOpaque(false);
    // see-through, with the chosen skybox (and the one under the mouse) picked out by a rounded
    // highlight on the card behind
    _list.setCellRenderer(new DefaultListCellRenderer() {
      @Override public Component getListCellRendererComponent (
        JList<?> list, Object value, int index, boolean selected, boolean focused) {
        super.getListCellRendererComponent(list, value, index, selected, false);
        _highlight = selected ? Theme.SIDE_SELECTION :
          (index == _hoverIndex ? Theme.SIDE_CARD_HOVER : null);
        setText(value == NONE ? "None" : value.toString());
        setFont(value == NONE ? NONE_FONT : CHOICE_FONT);
        setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        setOpaque(false);
        setForeground(value == NONE && !selected ? Theme.SIDE_DESCRIPTION : Theme.SIDE_TEXT);
        return this;
      }
      @Override protected void paintComponent (Graphics g) {
        if (_highlight != null) {
          Graphics2D g2 = Theme.smooth(g);
          g2.setColor(_highlight);
          g2.fillRoundRect(0, 1, getWidth(), getHeight() - 2, 12, 12);
          g2.dispose();
        }
        super.paintComponent(g);
      }
      protected Color _highlight;
    });
    MouseAdapter hover = new MouseAdapter() {
      @Override public void mouseMoved (MouseEvent e) {
        setHoverIndex(indexAt(e.getPoint()));
      }
      @Override public void mouseDragged (MouseEvent e) {
        setHoverIndex(indexAt(e.getPoint()));
      }
      @Override public void mouseExited (MouseEvent e) {
        setHoverIndex(-1);
      }
    };
    _list.addMouseListener(hover);
    _list.addMouseMotionListener(hover);
    _list.addListSelectionListener(e -> {
      Object value = _list.getSelectedValue();
      if (!e.getValueIsAdjusting() && !_updating && value != null) {
        _selected = value == NONE ? null : ((Choice)value).name;
        onSelect.accept(_selected);
      }
    });
    JScrollPane scroll = new JScrollPane(_list);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    Theme.sideScroll(scroll, Theme.SIDE_CARD);
    add(new RoundedPanel(scroll, Theme.SIDE_CARD, Theme.SIDE_ARC, 6), BorderLayout.CENTER);

    // the options of the skybox shown, under the list
    _options = new JPanel();
    _options.setOpaque(false);
    _options.setLayout(new BoxLayout(_options, BoxLayout.Y_AXIS));
    _options.setVisible(false);
    add(_options, BorderLayout.SOUTH);
  }

  /**
   * Shows the options of the skybox shown, or hides them if it has none.
   */
  public void setOptions (List<Option> options)
  {
    _options.removeAll();
    if (!options.isEmpty()) {
      _options.add(Theme.sideHeader("Skybox Options"));
    }
    for (Option option : options) {
      JPanel row = new JPanel(new BorderLayout(10, 0));
      row.setOpaque(false);
      row.setAlignmentX(LEFT_ALIGNMENT);
      row.setBorder(BorderFactory.createEmptyBorder(0, 4, 6, 0));
      JLabel label = new JLabel(option.name);
      label.setFont(Theme.sideFont(11f, Font.ITALIC));
      label.setForeground(Theme.SIDE_TEXT);
      label.setPreferredSize(new Dimension(OPTION_LABEL_WIDTH, label.getPreferredSize().height));
      row.add(label, BorderLayout.WEST);
      JComboBox<String> values = new JComboBox<>(option.values.toArray(new String[0]));
      values.setFont(Theme.sideFont(10.5f, Font.BOLD));
      values.setFocusable(false);
      values.setBackground(Theme.SIDE_CARD);
      values.setForeground(Theme.SIDE_TEXT);
      values.putClientProperty(FlatClientProperties.STYLE, "arc: 999; borderWidth: 0; " +
        "focusWidth: 0; buttonBackground: " + Theme.hex(Theme.SIDE_CARD) + "; popupBackground: " +
        Theme.hex(Theme.SIDE_CARD) + "; selectionArc: 10; borderColor: " +
        Theme.hex(Theme.SIDE_CARD));
      values.setSelectedItem(option.value);
      values.addActionListener(e -> _onOption.accept(option.name, (String)values.getSelectedItem()));
      row.add(values, BorderLayout.CENTER);
      row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
      _options.add(row);
    }
    _options.setVisible(!options.isEmpty());
    revalidate();
    repaint();
  }

  /**
   * Sets the available skyboxes.
   *
   * @param selected the config name of the current skybox, or null.
   */
  public void setChoices (List<Choice> choices, String selected)
  {
    _choices = new ArrayList<>(choices);
    _selected = selected;
    refilter();
  }

  /**
   * Returns the index of the entry under a point in the list, or -1 if there's none.
   */
  protected int indexAt (Point point)
  {
    int index = _list.locationToIndex(point);
    Rectangle bounds = index < 0 ? null : _list.getCellBounds(index, index);
    return bounds != null && bounds.contains(point) ? index : -1;
  }

  protected void setHoverIndex (int index)
  {
    if (index != _hoverIndex) {
      _hoverIndex = index;
      _list.repaint();
    }
  }

  protected void refilter ()
  {
    String[] terms = _search.getText().trim().toLowerCase(Locale.ROOT).split("\\s+");
    _updating = true;
    try {
      _model.clear();
      _model.addElement(NONE);
      Object current = NONE;
      for (Choice choice : _choices) {
        String text = (choice.label + " " + choice.name).toLowerCase(Locale.ROOT);
        boolean match = true;
        for (String term : terms) {
          match &= term.isEmpty() || text.contains(term);
        }
        if (match) {
          _model.addElement(choice);
          if (choice.name.equals(_selected)) {
            current = choice;
          }
        }
      }
      if (current != NONE || _selected == null) {
        _list.setSelectedValue(current, true);
      } else {
        _list.clearSelection();
      }
    } finally {
      _updating = false;
    }
  }

  /** The list entry standing for no skybox. */
  protected static final Object NONE = new Object();

  /** The fonts of the skyboxes in the list, and of the entry standing for none. */
  protected static final Font CHOICE_FONT = Theme.sideFont(10.5f, Font.BOLD),
    NONE_FONT = Theme.sideFont(10.5f, Font.ITALIC);

  /** The width of the option names. */
  protected static final int OPTION_LABEL_WIDTH = 90;

  protected final JTextField _search;
  protected final JPanel _options;
  protected final BiConsumer<String, String> _onOption;
  protected final DefaultListModel<Object> _model = new DefaultListModel<>();
  protected final JList<Object> _list;
  protected List<Choice> _choices = new ArrayList<>();
  protected String _selected;
  protected boolean _updating;

  /** The index of the entry under the mouse, or -1. */
  protected int _hoverIndex = -1;
}
