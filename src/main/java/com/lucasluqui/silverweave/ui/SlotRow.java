package com.lucasluqui.silverweave.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.lucasluqui.silverweave.item.KnightItem;
import com.lucasluqui.silverweave.item.Slot;

/**
 * A clickable row showing what is equipped in a slot; clicking it opens the item picker.
 */
public class SlotRow extends JPanel
{
  /** How far accessory rows are set in from the gear rows. */
  public static final int ACCESSORY_INDENT = 10;

  public SlotRow (Slot slot, boolean compact, IconCache icons,
    Consumer<Slot> onOpen, Consumer<Slot> onClear)
  {
    super(new BorderLayout(10, 0));
    _slot = slot;
    _onOpen = onOpen;
    _icons = icons;
    _iconSize = compact ? 20 : 26;
    // accessories are set in under the gear they go on
    _indent = slot.accessory ? ACCESSORY_INDENT : 0;
    setOpaque(false);
    setFocusable(true);
    setAlignmentX(LEFT_ALIGNMENT);
    setBorder(BorderFactory.createEmptyBorder(compact ? 5 : 8, 14 + _indent, compact ? 5 : 8, 8));

    // gear rows sit under a header naming their slot already; only accessories need a label
    if (slot.accessory) {
      _label = new JLabel(slot.label);
      _label.setFont(Theme.sideFont(compact ? 10f : 10.5f, Font.ITALIC));
      _label.setForeground(Theme.SIDE_DESCRIPTION);
      _label.setIconTextGap(SLOT_ICON_GAP);
      _label.setPreferredSize(new Dimension((compact ? 56 : 68) + SLOT_ICON_SIZE + SLOT_ICON_GAP,
        _iconSize));
      add(_label, BorderLayout.WEST);
    } else {
      _label = null;
    }

    _name = new JLabel();
    _nameFont = Theme.sideFont(compact ? 10.5f : 11f, Font.BOLD);
    _noteFont = Theme.sideFont(compact ? 10.5f : 11f, Font.ITALIC);
    _name.setFont(_nameFont);
    _name.setIconTextGap(8);
    add(_name, BorderLayout.CENTER);

    JPanel east = new JPanel();
    east.setOpaque(false);
    east.setLayout(new BoxLayout(east, BoxLayout.X_AXIS));
    // marks a slot that's only usable because blocks are bypassed; says why when hovered
    _bypassed = new JLabel(BLOCKED);
    _bypassed.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
    _bypassed.setVisible(false);
    east.add(_bypassed);
    // the color of a custom item, which opens the colors to choose from
    _color = new JButton();
    _color.putClientProperty("JButton.buttonType", "borderless");
    _color.setFocusable(false);
    _color.setVisible(false);
    _color.addActionListener(e -> showColors());
    east.add(_color);

    // keeps the slot as it is when the knight is randomized
    _lock = new LockButton(() -> {
      if (_onLock != null) {
        _onLock.accept(_slot);
      }
    });
    east.add(_lock);
    _clear = new JButton("✕");
    _clear.putClientProperty("JButton.buttonType", "borderless");
    _clear.setFont(Theme.symbolFont(10f));
    _clear.setForeground(Theme.SIDE_DESCRIPTION);
    _clear.setFocusable(false);
    _clear.addActionListener(e -> onClear.accept(_slot));
    // (keeps its room while there's nothing to remove, so that the lock stays put)
    _clear.setPreferredSize(_clear.getPreferredSize());
    _clear.setMaximumSize(_clear.getPreferredSize());
    east.add(_clear);
    _chevron = new JLabel("›");
    _chevron.setFont(Theme.sideFont(14f, Font.PLAIN));
    _chevron.setForeground(Theme.SIDE_DESCRIPTION);
    _chevron.setBorder(BorderFactory.createEmptyBorder(0, 4, 2, 4));
    east.add(_chevron);
    add(east, BorderLayout.EAST);

    Clickable.install(this, this::open, hover -> {
      _hover = hover;
      repaint();
    });
    addFocusListener(new FocusAdapter() {
      @Override public void focusGained (FocusEvent e) {
        repaint();
      }
      @Override public void focusLost (FocusEvent e) {
        repaint();
      }
    });
    getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open");
    getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "open");
    getActionMap().put("open", new AbstractAction() {
      @Override public void actionPerformed (ActionEvent e) {
        open();
      }
    });
    update(null, null);
  }

  public Slot getSlot ()
  {
    return _slot;
  }

  /**
   * Lets the slot be locked from the row, keeping it as it is when the knight is randomized.
   *
   * @param locked tells whether a slot is locked.
   * @param onLock notified with the slot when the lock is clicked.
   */
  public void setLocking (Predicate<Slot> locked, Consumer<Slot> onLock)
  {
    _locked = locked;
    _onLock = onLock;
    updateLock();
  }

  /**
   * Lets the color of the custom items in this slot be changed from the row.
   *
   * @param choices the colors to choose from.
   * @param onRecolor notified with the slot and the id of the color chosen.
   */
  public void setRecoloring (Supplier<List<RecolorChooser.Choice>> choices,
    BiConsumer<Slot, Integer> onRecolor)
  {
    _colorChoices = choices;
    _onRecolor = onRecolor;
  }

  /**
   * Updates the row to show the given item.
   *
   * @param unavailable if non-null, the slot cannot be used and this explains why.
   */
  public void update (KnightItem item, String unavailable)
  {
    update(item, unavailable, false, null);
  }

  /**
   * Updates the row to show the given item.
   *
   * @param unavailable if non-null, the slot cannot be used and this explains why.
   * @param blocked whether the slot is unavailable because other gear blocks it, which is
   * marked in red like a bypassed block.
   * @param bypassed if non-null, the slot is only usable because blocks are bypassed, and this
   * explains what would otherwise stop it.
   */
  public void update (KnightItem item, String unavailable, boolean blocked, String bypassed)
  {
    _bypassed.setVisible(bypassed != null && unavailable == null);
    _bypassed.setToolTipText(bypassed == null ? null :
      "<html><font color='" + Theme.hex(Theme.SIDE_DANGER) + "'>" + bypassed + "</font></html>");
    _item = item;
    _enabled = unavailable == null;
    _blocked = blocked && !_enabled;
    // empty and unusable slots read as the launcher's italic notes
    if (!_enabled) {
      _name.setText(unavailable);
      _name.setForeground(_blocked ? Theme.SIDE_DANGER : Theme.SIDE_FOOTNOTE);
      _name.setFont(_noteFont);
    } else if (item == null) {
      _name.setText("None");
      _name.setForeground(Theme.SIDE_DESCRIPTION);
      _name.setFont(_noteFont);
    } else {
      _name.setText(item.displayName);
      _name.setForeground(Theme.SIDE_TEXT);
      _name.setFont(_nameFont);
    }
    setToolTipText(item == null ? null : item.displayName);
    updateColor();
    updateLock();
    boolean removable = item != null && _enabled;
    _clear.setEnabled(removable);
    _clear.setText(removable ? "✕" : "");
    _clear.setToolTipText(removable ? "Remove" : null);
    // unusable slots don't open, but keep the arrow's room so that they're as tall as the rest
    _chevron.setForeground(_enabled ? Theme.SIDE_DESCRIPTION : TRANSPARENT);
    setCursor(Cursor.getPredefinedCursor(_enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    repaint();
  }

  @Override
  public boolean contains (int x, int y)
  {
    // the indent is empty space, not part of the row
    return x >= _indent && super.contains(x, y);
  }

  @Override
  public Dimension getMaximumSize ()
  {
    return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
  }

  @Override
  protected void paintComponent (Graphics g)
  {
    // fetch the icon at paint time so that it shows up as soon as it has loaded
    _name.setIcon(_blocked ? BLOCKED :
      _enabled && _item != null ? _icons.get(_item, _iconSize, this) : null);
    if (_label != null) {
      _label.setIcon(_icons.getMask(slotIcon(_slot), SLOT_ICON_SIZE, SLOT_ICON_COLORS, this));
    }

    Graphics2D g2 = Theme.smooth(g);
    g2.setColor(_hover && _enabled ? Theme.SIDE_CARD_HOVER : Theme.SIDE_CARD);
    g2.fillRoundRect(_indent, 1, getWidth() - _indent, getHeight() - 2, Theme.SIDE_ARC,
      Theme.SIDE_ARC);
    if (isFocusOwner()) {
      g2.setColor(Theme.SIDE_ACCENT);
      g2.drawRoundRect(_indent, 1, getWidth() - _indent - 1, getHeight() - 3, Theme.SIDE_ARC,
        Theme.SIDE_ARC);
    }
    g2.dispose();
  }

  /**
   * Returns the path of the inventory icon for an accessory slot.
   */
  protected static String slotIcon (Slot slot)
  {
    return "ui/icon/inventory/categories/icon_accessory-" +
      (slot.section == Slot.Section.HELMET ? "h" : "a") + slot.label.toLowerCase(Locale.ROOT) +
      ".png";
  }

  /**
   * Shows whether the slot is locked.
   */
  protected void updateLock ()
  {
    _lock.setVisible(_locked != null);
    _lock.setLocked(_locked != null && _locked.test(_slot));
  }

  /**
   * Shows the color of the item, if it's a custom one.
   */
  protected void updateColor ()
  {
    boolean custom = _enabled && _item != null && _item.isCustom() && _colorChoices != null;
    RecolorChooser.Choice choice = null;
    if (custom) {
      for (RecolorChooser.Choice each : _colorChoices.get()) {
        if (each.id == _item.getRecolor()) {
          choice = each;
          break;
        }
      }
    }
    _color.setVisible(custom);
    _color.setIcon(RecolorChooser.swatchIcon(choice, COLOR_SWATCH));
    _color.setToolTipText(choice == null ? "Change color" :
      "Color: " + choice.describe() + " - click to change");
    if (_chooser != null) {
      _chooser.setSelected(custom ? _item.getRecolor() : 0);
      _chooserLabel.setText("Color - " + (choice == null ? "Custom" : choice.describe()));
      if (!custom) {
        _colors.setVisible(false);
      }
    }
  }

  /**
   * Pops up the colors under the row's color.
   */
  protected void showColors ()
  {
    if (_item == null || !_item.isCustom() || _colorChoices == null) {
      return;
    }
    if (_colors == null) {
      _chooser = new RecolorChooser(color -> {
        if (_onRecolor != null) {
          _onRecolor.accept(_slot, color);
        }
      });
      _chooserLabel = new JLabel();
      _chooserLabel.setFont(Theme.sideFont(10.5f, Font.BOLD));
      _chooserLabel.setForeground(Theme.SIDE_DESCRIPTION);
      _chooserLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 6, 0));
      JPanel content = new JPanel(new BorderLayout());
      content.setBackground(Theme.SIDE_CARD);
      content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      content.add(_chooserLabel, BorderLayout.NORTH);
      content.add(_chooser, BorderLayout.CENTER);
      _colors = new JPopupMenu();
      _colors.setBackground(Theme.SIDE_CARD);
      _colors.add(content);
      // the arrow keys move through the colors while they're up (popups don't take the focus,
      // and would otherwise use the keys for menu items)
      KeyEventDispatcher keys = e -> {
        if (e.getID() != KeyEvent.KEY_PRESSED || !_colors.isVisible()) {
          return false;
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          _colors.setVisible(false);
          return true;
        }
        return _chooser.handleKey(e);
      };
      _colors.addPopupMenuListener(new PopupMenuListener() {
        @Override public void popupMenuWillBecomeVisible (PopupMenuEvent e) {
          KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keys);
        }
        @Override public void popupMenuWillBecomeInvisible (PopupMenuEvent e) {
          KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keys);
        }
        @Override public void popupMenuCanceled (PopupMenuEvent e) {
        }
      });
    }
    _chooser.setChoices(_colorChoices.get());
    updateColor();
    _colors.pack();
    _colors.show(_color, _color.getWidth() - _colors.getPreferredSize().width, _color.getHeight());
  }

  protected void open ()
  {
    if (_enabled) {
      _onOpen.accept(_slot);
    }
  }

  protected final Slot _slot;
  protected final Consumer<Slot> _onOpen;
  protected final IconCache _icons;
  protected final int _iconSize, _indent;
  protected final JLabel _label, _name, _chevron, _bypassed;
  protected final Font _nameFont, _noteFont;
  protected final JButton _clear, _color;
  protected final LockButton _lock;

  /** Tells whether the slot is locked, and who locks it. */
  protected Predicate<Slot> _locked;
  protected Consumer<Slot> _onLock;

  protected KnightItem _item;

  /** The colors for custom items, and who changes them. */
  protected Supplier<List<RecolorChooser.Choice>> _colorChoices;
  protected BiConsumer<Slot, Integer> _onRecolor;

  /** The popup with the colors, once shown. */
  protected JPopupMenu _colors;
  protected RecolorChooser _chooser;
  protected JLabel _chooserLabel;
  protected boolean _hover, _enabled = true, _blocked;

  /** The color of the arrow on rows that don't open. */
  protected static final Color TRANSPARENT = new Color(0, 0, 0, 0);

  /** The size of the swatch showing a custom item's color. */
  protected static final int COLOR_SWATCH = 14;

  /** The size of the accessory slot icons, and their space from the slot names. */
  protected static final int SLOT_ICON_SIZE = 18, SLOT_ICON_GAP = 6;

  /** The colors the slot icons' masks are painted in: the disc behind, the part of the knight
   * the slot is on, and the rest of the knight. */
  protected static final Color[] SLOT_ICON_COLORS = {
    Theme.SIDE_SELECTION, Theme.SIDE_ACCENT, Theme.SIDE_DESCRIPTION };

  /** Marks the slots other gear blocks, as the bypassed ones are marked. */
  protected static final Icon BLOCKED = Glyph.BLOCKED.icon(14, Theme.SIDE_DANGER);
}
