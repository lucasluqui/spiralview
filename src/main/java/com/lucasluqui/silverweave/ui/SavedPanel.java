package com.lucasluqui.silverweave.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Timer;

import com.formdev.flatlaf.FlatClientProperties;
import com.lucasluqui.silverweave.SavedLoadouts;
import com.lucasluqui.silverweave.ShareCode;
import com.lucasluqui.silverweave.item.ItemCatalog;
import com.lucasluqui.silverweave.item.KnightItem;
import com.lucasluqui.silverweave.item.Slot;

/**
 * The knights the user has saved, which can be put back on with a click.
 */
public class SavedPanel extends JPanel
{
  /**
   * @param onSave asked to save the current knight.
   * @param onLoad asked to put on the knight with the given share code.
   */
  public SavedPanel (SavedLoadouts saved, IconCache icons, Runnable onSave, Consumer<String> onLoad)
  {
    _saved = saved;
    _icons = icons;
    _onLoad = onLoad;
    setOpaque(false);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    // one of the launcher's grey pill buttons
    _save = Theme.sidePill(new JButton("Save Current Loadout...", Glyph.SAVE.icon(16,
      Theme.SIDE_TEXT)), Theme.SIDE_BUTTON, 11f);
    _save.setIconTextGap(8);
    _save.setAlignmentX(LEFT_ALIGNMENT);
    _save.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    _save.setEnabled(false);
    _save.addActionListener(e -> onSave.run());
    add(Box.createVerticalStrut(12));
    add(_save);

    add(Theme.sideHeader("Saved Loadouts"));
    _rows = new JPanel();
    _rows.setOpaque(false);
    _rows.setLayout(new BoxLayout(_rows, BoxLayout.Y_AXIS));
    _rows.setAlignmentX(LEFT_ALIGNMENT);
    add(_rows);

    saved.addChangeListener(this::rebuild);
    rebuild();
  }

  /**
   * Notes that the items are in, so saved knights can be shown and put on.
   */
  public void setCatalog (ItemCatalog catalog)
  {
    _catalog = catalog;
    _helmets.clear();
    _save.setEnabled(true);
    rebuild();
  }

  /**
   * Highlights the saved knight that matches the one being worn, if any.
   */
  public void setCurrent (String code)
  {
    _current = code;
    for (int ii = 0, nn = _rows.getComponentCount(); ii < nn; ii++) {
      if (_rows.getComponent(ii) instanceof Row) {
        _rows.getComponent(ii).repaint();
      }
    }
  }

  protected void rebuild ()
  {
    _rows.removeAll();
    if (_saved.getEntries().isEmpty()) {
      JLabel empty = Theme.sideNote("<html>No saved loadouts yet. Save the one you're wearing " +
        "to<br>come back to it anytime.</html>", 10.5f);
      empty.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
      empty.setAlignmentX(LEFT_ALIGNMENT);
      _rows.add(empty);
    }
    for (SavedLoadouts.Entry entry : _saved.getEntries()) {
      _rows.add(new Row(entry));
      _rows.add(Box.createVerticalStrut(6));
    }
    _rows.revalidate();
    _rows.repaint();
  }

  /**
   * Returns the helmet a saved knight wears, or null.
   */
  protected KnightItem getHelmet (String code)
  {
    if (_catalog == null) {
      return null;
    }
    return _helmets.computeIfAbsent(code, c -> {
      try {
        return ShareCode.decode(c, _catalog).items.get(Slot.HELMET);
      } catch (ShareCode.InvalidCodeException e) {
        return null;
      }
    });
  }

  protected void rename (SavedLoadouts.Entry entry)
  {
    Object name = JOptionPane.showInputDialog(this, "Name:", "Rename Loadout",
      JOptionPane.PLAIN_MESSAGE, null, null, entry.getName());
    if (name != null && !name.toString().trim().isEmpty()) {
      _saved.rename(entry, name.toString().trim());
    }
  }

  protected void delete (SavedLoadouts.Entry entry)
  {
    int choice = JOptionPane.showConfirmDialog(this,
      "Delete \"" + entry.getName() + "\"? This can't be undone.",
      "Delete Loadout", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
    if (choice == JOptionPane.OK_OPTION) {
      _saved.remove(entry);
    }
  }

  /**
   * A saved knight: its helmet and name, a favorite star and a button to copy its share code.
   * Clicking it puts the knight on; right clicking offers to rename or delete it.
   */
  protected class Row extends JPanel
  {
    public Row (SavedLoadouts.Entry entry)
    {
      super(new BorderLayout(10, 0));
      _entry = entry;
      setOpaque(false);
      setAlignmentX(LEFT_ALIGNMENT);
      setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 6));
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      setToolTipText("Saved " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(new Date(entry.created)));

      _icon = new JLabel();
      add(_icon, BorderLayout.WEST);

      JLabel name = new JLabel(entry.getName());
      name.setFont(Theme.sideFont(11f, Font.BOLD));
      name.setForeground(Theme.SIDE_TEXT);
      add(name, BorderLayout.CENTER);

      JPanel east = new JPanel();
      east.setOpaque(false);
      east.setLayout(new BoxLayout(east, BoxLayout.X_AXIS));
      JButton star = iconButton(entry.isFavorite() ?
        Glyph.STAR_FILLED.icon(16, Theme.FAVORITE) : Glyph.STAR.icon(16, Theme.SIDE_DESCRIPTION),
        entry.isFavorite() ? "Remove from favorites" : "Add to favorites");
      star.addActionListener(e -> _saved.toggleFavorite(entry));
      east.add(star);
      JButton copy = iconButton(Glyph.SHARE.icon(16, Theme.SIDE_DESCRIPTION), "Copy Share Code");
      Timer restore = new Timer(1200, e -> copy.setIcon(Glyph.SHARE.icon(16,
        Theme.SIDE_DESCRIPTION)));
      restore.setRepeats(false);
      copy.addActionListener(e -> {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
          new StringSelection(entry.code), null);
        copy.setIcon(Glyph.CHECK.icon(16, Theme.SIDE_ACCENT));
        restore.restart();
        Toast.show(copy, "Share Code copied to clipboard", Glyph.CHECK);
      });
      east.add(copy);
      JButton rename = iconButton(Glyph.RENAME.icon(16, Theme.SIDE_DESCRIPTION), "Rename");
      rename.addActionListener(e -> rename(entry));
      east.add(rename);
      JButton delete = iconButton(Glyph.CLEAR.icon(16, Theme.SIDE_DESCRIPTION), "Delete");
      // deleting can't be undone, so the bin warns in red
      delete.setRolloverIcon(Glyph.CLEAR.icon(16, Theme.SIDE_DANGER));
      delete.addActionListener(e -> delete(entry));
      east.add(delete);
      add(east, BorderLayout.EAST);

      Clickable.install(this, () -> _onLoad.accept(entry.code), hover -> {
        _hover = hover;
        repaint();
      });
      addMouseListener(new MouseAdapter() {
        @Override public void mousePressed (MouseEvent e) {
          maybeShowMenu(e);
        }
        @Override public void mouseReleased (MouseEvent e) {
          maybeShowMenu(e);
        }
      });
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
      KnightItem helmet = getHelmet(_entry.code);
      Icon icon = helmet != null && helmet.icon != null ? _icons.get(helmet, ICON_SIZE, this) :
        NO_HELMET;
      if (_icon.getIcon() != icon) {
        _icon.setIcon(icon);
      }

      Graphics2D g2 = Theme.smooth(g);
      g2.setColor(_hover ? Theme.SIDE_CARD_HOVER : Theme.SIDE_CARD);
      g2.fillRoundRect(0, 1, getWidth(), getHeight() - 2, Theme.SIDE_ARC, Theme.SIDE_ARC);
      if (_entry.code.equals(_current)) {
        g2.setColor(Theme.SIDE_ACCENT);
        g2.drawRoundRect(0, 1, getWidth() - 1, getHeight() - 3, Theme.SIDE_ARC, Theme.SIDE_ARC);
      }
      g2.dispose();
    }

    protected void maybeShowMenu (MouseEvent e)
    {
      if (!e.isPopupTrigger()) {
        return;
      }
      // styled like the launcher's Play menu
      JPopupMenu menu = new JPopupMenu();
      menu.setBackground(Theme.SIDE_CARD_HOVER);
      menu.putClientProperty(FlatClientProperties.POPUP_BORDER_CORNER_RADIUS, 5);
      menu.putClientProperty(FlatClientProperties.STYLE, "borderColor: " +
        Theme.hex(Theme.SIDE_CARD_HOVER) + "; borderInsets: 5,5,5,5");
      JMenuItem rename = menuItem("Rename...");
      rename.addActionListener(a -> rename(_entry));
      menu.add(rename);
      JMenuItem delete = menuItem("Delete");
      delete.addActionListener(a -> delete(_entry));
      menu.add(delete);
      menu.show(this, e.getX(), e.getY());
    }

    protected JMenuItem menuItem (String text)
    {
      JMenuItem item = new JMenuItem(text);
      item.setFont(Theme.sideFont(11f, Font.BOLD));
      item.setMargin(new Insets(10, 10, 10, 10));
      item.putClientProperty(FlatClientProperties.STYLE, "selectionArc: 15; " +
        "selectionBackground: " + Theme.hex(Theme.SIDE_SELECTION));
      return item;
    }

    protected JButton iconButton (Icon icon, String tooltip)
    {
      JButton button = new JButton(icon);
      button.putClientProperty("JButton.buttonType", "borderless");
      button.setToolTipText(tooltip);
      button.setFocusable(false);
      return button;
    }

    protected final SavedLoadouts.Entry _entry;
    protected final JLabel _icon;
    protected boolean _hover;
  }

  /** The size of the helmet icons. */
  protected static final int ICON_SIZE = 26;

  /** Shown for saved knights without a helmet. */
  protected static final Icon NO_HELMET = Glyph.SAVE.icon(ICON_SIZE, Theme.SIDE_DESCRIPTION);

  protected final SavedLoadouts _saved;
  protected final IconCache _icons;
  protected final Consumer<String> _onLoad;
  protected final JButton _save;
  protected final JPanel _rows;
  protected final Map<String, KnightItem> _helmets = new HashMap<>();
  protected ItemCatalog _catalog;
  protected String _current;
}
