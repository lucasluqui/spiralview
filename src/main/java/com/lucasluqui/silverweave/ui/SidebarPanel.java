package com.lucasluqui.silverweave.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import com.formdev.flatlaf.FlatClientProperties;
import com.lucasluqui.silverweave.Favorites;
import com.lucasluqui.silverweave.Island;
import com.lucasluqui.silverweave.Loadout;
import com.lucasluqui.silverweave.SavedLoadouts;
import com.lucasluqui.silverweave.item.ItemCatalog;
import com.lucasluqui.silverweave.item.KnightItem;
import com.lucasluqui.silverweave.item.Slot;

/**
 * The panel beside the 3D view through which the knight is outfitted.
 */
public class SidebarPanel extends JPanel
{
  /** The width the sidebar prefers. */
  public static final int WIDTH = 380;

  /**
   * The things the sidebar asks its host to do.
   */
  public interface Actions
  {
    /** Launches a test session in which the user walks around an island as the current knight. */
    void testKnight (Island island);

    /** Asks the user for a name and saves the current knight under it. */
    void saveKnight ();

    /** Puts on the knight described by a share code. */
    void loadKnight (String code);

    /** Shows the given skybox, or none if null. */
    void setSkybox (String name);

    /** Sets one of the options of the skybox shown (such as its variant). */
    void setSkyboxOption (String option, String value);
  }

  public SidebarPanel (Loadout loadout, IconCache icons, Favorites favorites, SavedLoadouts saved,
    Actions actions)
  {
    super(new BorderLayout());
    _loadout = loadout;
    _saved = saved;
    _icons = icons;
    _favorites = favorites;
    _actions = actions;
    setBackground(Theme.SIDE_BACKGROUND);
    setPreferredSize(new Dimension(WIDTH, 1));
    setMinimumSize(new Dimension(WIDTH, 1));
    setMaximumSize(new Dimension(WIDTH, Integer.MAX_VALUE));

    add(createHeader(), BorderLayout.NORTH);

    _body = new JPanel(_cards);
    _body.setOpaque(false);
    _body.add(createLoadingCard(), LOADING);
    _body.add(createMainCard(), MAIN);
    _picker = new ItemPicker(_icons, _favorites, this::closePicker);
    _picker.setBorder(BorderFactory.createEmptyBorder(4, 16, 12, 16));
    _body.add(_picker, PICKER);
    _islandPicker = new IslandPicker(island -> {
      closeIslands();
      _actions.testKnight(island);
    }, this::closeIslands);
    _islandPicker.setBorder(BorderFactory.createEmptyBorder(4, 16, 12, 16));
    _body.add(_islandPicker, ISLANDS);
    add(_body, BorderLayout.CENTER);

    _footer = createFooter();
    add(_footer, BorderLayout.SOUTH);

    _loadout.addChangeListener(e -> refresh());
    _cards.show(_body, LOADING);
  }

  /**
   * Shows a loading message in place of the controls.
   */
  public void setLoading (String message)
  {
    _loadingLabel.setText(message);
    _progress.setVisible(true);
    _cards.show(_body, LOADING);
  }

  /**
   * Shows an error in place of the controls.
   */
  public void setError (String message)
  {
    _loadingLabel.setText("<html><div style='text-align:center;width:260px'>" + message + "</div></html>");
    _progress.setVisible(false);
    _cards.show(_body, LOADING);
  }

  /**
   * Fills the controls with the items from the catalog.
   */
  public void setCatalog (ItemCatalog catalog)
  {
    _catalog = catalog;
    _recolors = null;
    _picker.setColorChoices(this::getRecolors);
    _style.setCatalog(catalog);
    _savedPanel.setCatalog(catalog);
    for (Map.Entry<Slot, SlotRow> entry : _rows.entrySet()) {
      if (entry.getKey().accessory) {
        entry.getValue().setVisible(catalog.hasAccessories());
      }
    }
    for (JComponent note : _accessoryNotes) {
      note.setVisible(!catalog.hasAccessories());
    }
    _test.setEnabled(!_testRunning);
    refresh();
    _cards.show(_body, MAIN);
  }

  /**
   * Notes whether a test session is running, during which another can't be started.
   */
  public void setTestRunning (boolean running)
  {
    _testRunning = running;
    _test.setText(running ? "Test Running..." : "Test Loadout");
    _test.setEnabled(!running && _catalog != null);
  }

  /**
   * Sets the skyboxes the user can choose from.
   *
   * @param selected the config name of the current skybox, or null.
   */
  public void setSkyboxes (List<EnvironmentPanel.Choice> choices, String selected)
  {
    _environment.setChoices(choices, selected);
  }

  /**
   * Sets the options offered for the skybox shown, if it has any.
   */
  public void setSkyboxOptions (List<EnvironmentPanel.Option> options)
  {
    _environment.setOptions(options);
  }

  /**
   * Brings up the saved knights.
   */
  public void showSaved ()
  {
    _cards.show(_body, MAIN);
    _tabs.setSelectedComponent(_savedTab);
  }

  /**
   * Syncs every control with the loadout.
   */
  protected void refresh ()
  {
    for (Map.Entry<Slot, SlotRow> entry : _rows.entrySet()) {
      Slot slot = entry.getKey();
      String unavailable = null, bypassed = null;
      boolean blocked = false;
      if (slot.accessory) {
        KnightItem blocker = _loadout.getBlocker(slot);
        KnightItem occupant = _loadout.getOccupant(slot);
        KnightItem overridden = _loadout.getBypassed(slot);
        if (blocker != null) {
          unavailable = "Blocked by " + blocker.displayName;
          blocked = true;
        } else if (occupant != null) {
          unavailable = "Used by " + occupant.displayName;
        } else if (overridden != null) {
          bypassed = "Would otherwise be blocked by " + overridden.displayName;
        }
      }
      entry.getValue().update(_loadout.get(slot), unavailable, blocked, bypassed);
    }
    updateCustomWarning();
    _shieldAway.setSelected(_loadout.isShieldAway());
    _bypassBlocks.setSelected(_loadout.isBypassingBlocks());
    _style.refresh();
    if (_catalog != null) {
      _savedPanel.setCurrent(_loadout.toShareCode());
    }
  }

  protected JComponent createHeader ()
  {
    // the logo and name, as atop the launcher's side pane, but side by side to leave the height
    // to the gear
    JPanel header = new JPanel(new BorderLayout());
    header.setOpaque(false);
    header.setBorder(BorderFactory.createEmptyBorder(18, 20, 8, 20));
    JLabel title = new JLabel("Silverweave");
    title.setFont(Theme.sideFont(15f, Font.BOLD));
    title.setForeground(Theme.SIDE_TEXT);
    title.setIconTextGap(12);
    try (InputStream in = SidebarPanel.class.getResourceAsStream(LOGO)) {
      if (in != null) {
        title.setIcon(new ImageIcon(IconCache.resizeSmoothly(ImageIO.read(in), LOGO_SIZE,
          LOGO_SIZE)));
      }
    } catch (IOException e) {
      // no logo, then; the name stands alone
    }
    header.add(title, BorderLayout.CENTER);
    return header;
  }

  protected JComponent createLoadingCard ()
  {
    JPanel card = new JPanel();
    card.setOpaque(false);
    card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
    card.add(Box.createVerticalGlue());
    _loadingLabel = Theme.sideNote("Loading items...", 11f);
    _loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
    _loadingLabel.setAlignmentX(CENTER_ALIGNMENT);
    card.add(_loadingLabel);
    card.add(Box.createVerticalStrut(10));
    _progress = SlimProgressBarUI.create(Theme.SIDE_ACCENT, 160, true);
    _progress.setAlignmentX(CENTER_ALIGNMENT);
    card.add(_progress);
    card.add(Box.createVerticalGlue());
    return card;
  }

  protected JComponent createMainCard ()
  {
    JPanel gear = new JPanel();
    gear.setOpaque(false);
    gear.setLayout(new BoxLayout(gear, BoxLayout.Y_AXIS));
    gear.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 12));

    // a caution atop while items the game doesn't have are selected
    _customWarning = new JLabel("<html><div style='width:240px'>This loadout contains one or " +
      "more items that do not currently exist in the game.</div></html>",
      Glyph.WARNING.icon(14, Theme.SIDE_WARNING), SwingConstants.LEFT);
    _customWarning.setFont(Theme.sideFont(10f, Font.ITALIC));
    _customWarning.setForeground(Theme.SIDE_WARNING);
    _customWarning.setIconTextGap(8);
    _customWarning.setAlignmentX(LEFT_ALIGNMENT);
    _customWarning.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
    _customWarning.setVisible(false);
    gear.add(_customWarning);

    for (Slot.Section section : Slot.Section.values()) {
      if (section == Slot.Section.STYLE) {
        continue;
      }
      gear.add(Theme.sideHeader(section.label));
      boolean accessories = false;
      for (Slot slot : section.slots()) {
        if (slot.accessory && !accessories) {
          accessories = true;
          JLabel sub = Theme.sideNote("Accessories", 10.5f);
          sub.setAlignmentX(LEFT_ALIGNMENT);
          sub.setBorder(BorderFactory.createEmptyBorder(6, 4 + SlotRow.ACCESSORY_INDENT, 5, 4));
          gear.add(sub);
          JLabel note = Theme.sideNote("Unavailable: config/accessory.dat was not found.", 10f);
          note.setForeground(Theme.SIDE_FOOTNOTE);
          note.setAlignmentX(LEFT_ALIGNMENT);
          note.setBorder(BorderFactory.createEmptyBorder(0, 4 + SlotRow.ACCESSORY_INDENT, 4, 4));
          note.setVisible(false);
          _accessoryNotes.add(note);
          gear.add(note);
        }
        SlotRow row = new SlotRow(slot, slot.accessory, _icons, this::openPicker,
          s -> _loadout.set(s, null));
        row.setRecoloring(this::getRecolors, this::recolor);
        _rows.put(slot, row);
        gear.add(row);
        gear.add(Box.createVerticalStrut(slot.accessory ? 4 : 6));
      }
      if (section == Slot.Section.SHIELD) {
        gear.add(Box.createVerticalStrut(6));
        _shieldAway = new JCheckBox();
        _shieldAway.addActionListener(e -> _loadout.setShieldAway(_shieldAway.isSelected()));
        gear.add(option(_shieldAway, "Sling shield on back",
          "Carry the shield on the back instead of the arm."));
        gear.add(separator());
        _bypassBlocks = new JCheckBox();
        _bypassBlocks.addActionListener(
          e -> _loadout.setBypassingBlocks(_bypassBlocks.isSelected()));
        gear.add(option(_bypassBlocks, "Bypass accessory blocks",
          "Show accessories even where other gear would block them."));
      }
    }

    _style = new StylePanel(_loadout, _icons, _favorites);
    _style.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 12));

    // the tabs read as a row of pills, the chosen one filled in like the launcher's buttons
    _tabs = new JTabbedPane();
    _tabs.putClientProperty("JTabbedPane.tabAreaAlignment", "fill");
    _tabs.putClientProperty("JTabbedPane.tabHeight", TAB_HEIGHT);
    _tabs.putClientProperty(FlatClientProperties.STYLE, "tabType: underlined; " +
      "tabArc: 999; tabSelectionArc: 999; tabSelectionHeight: " + TAB_HEIGHT + "; " +
      "tabSelectionInsets: 4,2,4,2; tabInsets: 0,6,0,6; tabAreaInsets: 0,14,6,14; " +
      "showContentSeparator: false; " +
      "underlineColor: " + Theme.hex(Theme.SIDE_BUTTON) + "; " +
      "inactiveUnderlineColor: " + Theme.hex(Theme.SIDE_BUTTON) + "; " +
      "hoverColor: " + Theme.hex(Theme.SIDE_BACKGROUND) + "; " +
      "focusColor: " + Theme.hex(Theme.SIDE_BACKGROUND) + "; " +
      "selectedForeground: " + Theme.hex(Theme.SIDE_TEXT));
    _tabs.setForeground(Theme.SIDE_DESCRIPTION);
    _tabs.setFont(Theme.sideFont(10.5f, Font.BOLD));
    _tabs.setFocusable(false);
    _tabs.setBackground(Theme.SIDE_BACKGROUND);
    _tabs.addTab("Equipment", scroll(gear));
    _tabs.addTab("Style Kit", scroll(_style));
    _savedPanel = new SavedPanel(_saved, _icons, _actions::saveKnight, _actions::loadKnight);
    _savedPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 12));
    _savedTab = scroll(_savedPanel);
    _tabs.addTab("Loadouts", _savedTab);
    _environment = new EnvironmentPanel(_actions::setSkybox, _actions::setSkyboxOption);
    _tabs.addTab("Environment", _environment);
    return _tabs;
  }

  /**
   * Creates a row for an option of the knight's look, laid out like the launcher's settings: its
   * name over an italic explanation, with the checkbox at the right. The whole row toggles it.
   */
  protected static JComponent option (JCheckBox box, String title, String explanation)
  {
    JPanel row = new JPanel(new BorderLayout(10, 0)) {
      @Override public Dimension getMaximumSize () {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
      }
    };
    row.setOpaque(false);
    row.setAlignmentX(LEFT_ALIGNMENT);
    row.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 0));
    row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    JPanel text = new JPanel();
    text.setOpaque(false);
    text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
    JLabel name = new JLabel(title);
    name.setFont(Theme.sideFont(11.5f, Font.ITALIC));
    name.setForeground(Theme.SIDE_TEXT);
    text.add(name);
    text.add(Box.createVerticalStrut(2));
    text.add(Theme.sideNote(explanation, 9f));
    row.add(text, BorderLayout.CENTER);

    box.setOpaque(false);
    box.setFocusPainted(false);
    box.putClientProperty(FlatClientProperties.STYLE,
      "icon.background: " + Theme.hex(Theme.SIDE_CARD) + "; " +
      "icon.borderColor: " + Theme.hex(Theme.SIDE_FOOTNOTE) + "; " +
      "icon.hoverBorderColor: " + Theme.hex(Theme.SIDE_DESCRIPTION) + "; " +
      "icon.selectedBackground: " + Theme.hex(Theme.SIDE_ACCENT) + "; " +
      "icon.selectedBorderColor: " + Theme.hex(Theme.SIDE_ACCENT) + "; " +
      "icon.checkmarkColor: " + Theme.hex(Theme.SIDE_TEXT) + "; " +
      "icon.focusWidth: 0");
    JPanel east = new JPanel(new GridBagLayout());
    east.setOpaque(false);
    east.add(box);
    row.add(east, BorderLayout.EAST);
    Clickable.install(row, box::doClick, null);
    return row;
  }

  /**
   * Creates the thin line between two of the launcher's setting rows.
   */
  protected static JComponent separator ()
  {
    JPanel line = new JPanel() {
      @Override public Dimension getMaximumSize () {
        return new Dimension(Integer.MAX_VALUE, 9);
      }
    };
    line.setOpaque(false);
    line.setAlignmentX(LEFT_ALIGNMENT);
    line.setPreferredSize(new Dimension(1, 9));
    line.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createEmptyBorder(4, 4, 4, 0),
      BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SIDE_CARD_HOVER)));
    return line;
  }

  protected JComponent createFooter ()
  {
    JPanel footer = new JPanel();
    footer.setOpaque(false);
    footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
    footer.setBorder(BorderFactory.createEmptyBorder(12, 20, 18, 20));

    // the launcher's Play button
    _test = Theme.sidePill(new JButton("Test Loadout"), Theme.SIDE_ACCENT, 15f);
    _test.setToolTipText("Walk around one of the islands wearing this loadout");
    _test.setEnabled(false);
    _test.setAlignmentX(LEFT_ALIGNMENT);
    _test.setPreferredSize(new Dimension(1, TEST_HEIGHT));
    _test.setMaximumSize(new Dimension(Integer.MAX_VALUE, TEST_HEIGHT));
    _test.addActionListener(e -> showIslands());
    footer.add(_test);
    return footer;
  }

  /**
   * Shows the caution about custom items if the knight wears any, naming them when hovered.
   */
  protected void updateCustomWarning ()
  {
    List<String> customs = new ArrayList<>();
    for (Slot slot : Slot.values()) {
      KnightItem item = _loadout.get(slot);
      if (item != null && item.isCustom()) {
        customs.add(item.displayName);
      }
    }
    _customWarning.setToolTipText(customs.isEmpty() ? null :
      "<html>Not in the game:<br>" + String.join("<br>", customs) + "</html>");
    if (_customWarning.isVisible() == customs.isEmpty()) {
      _customWarning.setVisible(!customs.isEmpty());
      revalidate();
    }
  }

  public void setIslands (List<Island> islands)
  {
    _islandPicker.setIslands(islands);
  }

  /**
   * Replaces the outfitting controls with the island choice that starts a test session.
   */
  public void showIslands ()
  {
    if (_catalog == null || _testRunning) {
      return;
    }
    _cards.show(_body, ISLANDS);
    _footer.setVisible(false);
  }

  /**
   * Returns from the island choice to the outfitting controls.
   */
  protected void closeIslands ()
  {
    _cards.show(_body, MAIN);
    _footer.setVisible(true);
  }

  /**
   * Returns the colors custom items can be recolored in.
   */
  protected List<RecolorChooser.Choice> getRecolors ()
  {
    if (_recolors == null || _recolors.isEmpty()) {
      ItemCatalog catalog = _catalog;
      _recolors = RecolorChooser.createChoices(_icons.getColors(),
        catalog == null ? null : catalog::getColorName);
    }
    return _recolors;
  }

  /**
   * Puts the custom item in a slot on in another color.
   */
  protected void recolor (Slot slot, int color)
  {
    KnightItem item = _loadout.get(slot);
    if (item != null && item.isCustom()) {
      _loadout.set(slot, item.withRecolor(color));
    }
  }

  protected void openPicker (Slot slot)
  {
    if (_catalog == null) {
      return;
    }
    _openSlot = slot;
    _picker.open(slot, _catalog.getItems(slot), _loadout.get(slot), item -> _loadout.set(slot, item));
    _cards.show(_body, PICKER);
  }

  /**
   * Returns from the item picker to the slot list.
   */
  protected void closePicker ()
  {
    _cards.show(_body, MAIN);
    SlotRow row = _openSlot == null ? null : _rows.get(_openSlot);
    if (row != null) {
      row.requestFocusInWindow();
    }
  }

  protected static JScrollPane scroll (JComponent content)
  {
    WidthTrackingPanel holder = new WidthTrackingPanel();
    holder.add(content, BorderLayout.NORTH);
    JScrollPane scroll = new JScrollPane(holder,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getViewport().setBackground(Theme.SIDE_BACKGROUND);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    Theme.sideScroll(scroll, Theme.SIDE_BACKGROUND);
    return scroll;
  }

  /**
   * A panel that always fits the width of the scroll pane viewport holding it, so that its
   * contents wrap instead of running off the side.
   */
  protected static class WidthTrackingPanel extends JPanel
    implements Scrollable
  {
    public WidthTrackingPanel ()
    {
      super(new BorderLayout());
      setOpaque(false);
    }

    @Override public Dimension getPreferredScrollableViewportSize () {
      return getPreferredSize();
    }

    @Override public int getScrollableUnitIncrement (Rectangle visible, int orientation, int direction) {
      return 16;
    }

    @Override public int getScrollableBlockIncrement (Rectangle visible, int orientation, int direction) {
      return Math.max(16, visible.height - 32);
    }

    @Override public boolean getScrollableTracksViewportWidth () {
      return true;
    }

    @Override public boolean getScrollableTracksViewportHeight () {
      return false;
    }
  }

  /** The height of the Test Loadout button, as tall as the launcher's Play button. */
  protected static final int TEST_HEIGHT = 52;

  protected static final int TAB_HEIGHT = 36;

  /** The logo shown by the name, and its size. */
  protected static final String LOGO = "/img/icon-silverweave.png";
  protected static final int LOGO_SIZE = 36;

  protected static final String LOADING = "loading", MAIN = "main", PICKER = "picker",
    ISLANDS = "islands";

  protected final Loadout _loadout;
  protected final IconCache _icons;
  protected final Favorites _favorites;
  protected final SavedLoadouts _saved;
  protected final Actions _actions;
  protected final CardLayout _cards = new CardLayout();
  protected final JPanel _body;
  protected final ItemPicker _picker;
  protected final IslandPicker _islandPicker;
  protected final JComponent _footer;
  protected final Map<Slot, SlotRow> _rows = new EnumMap<>(Slot.class);
  protected final List<JComponent> _accessoryNotes = new ArrayList<>();

  protected ItemCatalog _catalog;
  protected Slot _openSlot;

  /** The colors custom items can be recolored in, once listed. */
  protected List<RecolorChooser.Choice> _recolors;
  protected JTabbedPane _tabs;
  protected StylePanel _style;
  protected EnvironmentPanel _environment;
  protected SavedPanel _savedPanel;

  /** The tab holding the saved loadouts. */
  protected JScrollPane _savedTab;
  protected JCheckBox _shieldAway, _bypassBlocks;
  protected JLabel _loadingLabel;

  /** The caution shown while the knight wears custom items. */
  protected JLabel _customWarning;
  protected JProgressBar _progress;
  protected JButton _test;
  protected boolean _testRunning;
}
