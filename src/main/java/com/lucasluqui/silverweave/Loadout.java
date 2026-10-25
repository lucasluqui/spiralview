package com.lucasluqui.silverweave;

import java.awt.EventQueue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.prefs.Preferences;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.threerings.config.ArgumentMap;
import com.threerings.config.ConfigReference;
import com.threerings.opengl.model.config.ModelConfig;

import com.lucasluqui.silverweave.item.ItemCatalog;
import com.lucasluqui.silverweave.item.KnightItem;
import com.lucasluqui.silverweave.item.Slot;

import static com.lucasluqui.silverweave.Log.log;

/**
 * Holds the knight's current equipment.
 */
public class Loadout
{
  /**
   * Returns the item in a slot, or null if the slot is empty.
   */
  public KnightItem get (Slot slot)
  {
    return _items.get(slot);
  }

  /**
   * Puts an item in a slot (or empties it, if the item is null).
   */
  public void set (Slot slot, KnightItem item)
  {
    if (item == null) {
      _items.remove(slot);
    } else {
      _items.put(slot, item);
      // while bypassing, accessories are free to overlap
      if (!_bypassBlocks) {
        // a multi-slot accessory displaces whatever was in the other slots it covers
        for (Slot extra : item.getExtraSlots()) {
          _items.remove(extra);
        }
        // and anything covering this slot from elsewhere gets displaced too
        for (Slot other : Slot.values()) {
          KnightItem held = _items.get(other);
          if (other != slot && held != null && held.getExtraSlots().contains(slot)) {
            _items.remove(other);
          }
        }
      }
    }
    fireChanged();
  }

  public boolean isShieldAway ()
  {
    return _shieldAway;
  }

  public void setShieldAway (boolean away)
  {
    _shieldAway = away;
    fireChanged();
  }

  /**
   * Returns whether accessories are shown even in the slots the helmet, armor or other
   * accessories block.
   */
  public boolean isBypassingBlocks ()
  {
    return _bypassBlocks;
  }

  /**
   * Sets whether accessories are shown even in the slots the helmet, armor or other accessories
   * block. This is a viewing option rather than part of the knight, so it isn't cleared or
   * shared with it.
   */
  public void setBypassingBlocks (boolean bypass)
  {
    _bypassBlocks = bypass;
    fireChanged();
  }

  public void clear ()
  {
    _items.clear();
    _shieldAway = false;
    fireChanged();
  }

  /**
   * Puts random gear on the knight, for inspiration.
   */
  public void randomize (ItemCatalog catalog, Random random)
  {
    _items.clear();
    _shieldAway = false;
    for (Slot slot : new Slot[] {
        Slot.WEAPON, Slot.HELMET, Slot.ARMOR, Slot.SHIELD, Slot.COLOR, Slot.EYES }) {
      putRandom(slot, catalog, random);
    }
    // then accessories, in whichever slots the rolled helmet and armor leave open
    for (Slot slot : Slot.values()) {
      if (slot.accessory && random.nextFloat() < ACCESSORY_CHANCE &&
          getBlocker(slot) == null && getOccupant(slot) == null) {
        putRandom(slot, catalog, random);
      }
    }
    fireChanged();
  }

  /**
   * Returns the item occupying (but not equipped in) the given slot by way of a multi-slot
   * accessory, or null if there is none (as there never is while blocks are bypassed).
   */
  public KnightItem getOccupant (Slot slot)
  {
    return _bypassBlocks ? null : findOccupant(slot);
  }

  /**
   * Returns the item that blocks an accessory slot, or null if the slot is usable (as every
   * slot is while blocks are bypassed).
   */
  public KnightItem getBlocker (Slot slot)
  {
    return _bypassBlocks ? null : findBlocker(slot);
  }

  /**
   * Returns the item that would block or occupy an accessory slot if blocks weren't bypassed,
   * or null if the slot is usable anyway (or blocks aren't bypassed).
   */
  public KnightItem getBypassed (Slot slot)
  {
    if (!_bypassBlocks) {
      return null;
    }
    KnightItem blocker = findBlocker(slot);
    return blocker != null ? blocker : findOccupant(slot);
  }

  /**
   * Finds the multi-slot accessory covering the given slot from another, if any.
   */
  protected KnightItem findOccupant (Slot slot)
  {
    for (KnightItem item : _items.values()) {
      if (item.getExtraSlots().contains(slot)) {
        return item;
      }
    }
    return null;
  }

  /**
   * Finds the helmet or armor blocking an accessory slot, if any.
   */
  protected KnightItem findBlocker (Slot slot)
  {
    Slot host = slot.accessoryHost();
    KnightItem item = host == null ? null : _items.get(host);
    return (item != null && item.getBlockedSlots().contains(slot)) ? item : null;
  }

  public int getColorization ()
  {
    KnightItem color = _items.get(Slot.COLOR);
    return color == null ? ItemCatalog.DEFAULT_COLORIZATION : color.getColorization();
  }

  /**
   * Creates the reference to the knight model with everything applied.
   *
   * @param model the path of the base knight model.
   */
  public ConfigReference<ModelConfig> createModelReference (String model)
  {
    int colorization = getColorization();
    KnightItem height = _items.get(Slot.HEIGHT);
    ConfigReference<ModelConfig> ref = new ConfigReference<>(model,
      "Colorization", colorization, "Height", height == null ? "0" : height.getHeight());
    ArgumentMap args = ref.getArguments();

    // eyes go first, since a helmet may hide them
    apply(_items.get(Slot.EYES), colorization, args);
    apply(_items.get(Slot.HELMET), colorization, args);
    apply(_items.get(Slot.ARMOR), colorization, args);
    apply(_items.get(Slot.SHIELD), colorization, args);
    apply(_items.get(Slot.WEAPON), colorization, args);
    for (Slot slot : Slot.values()) {
      if (slot.accessory && getBlocker(slot) == null && getOccupant(slot) == null) {
        apply(_items.get(slot), colorization, args);
      }
    }
    if (_shieldAway) {
      args.put("Shield Away", "True");
    }
    return ref;
  }

  public String toShareCode ()
  {
    return ShareCode.encode(_items, _shieldAway);
  }

  /**
   * Replaces the loadout with the one described by a share code.
   *
   * @return the number of items in the code that no longer exist and were skipped.
   */
  public int applyShareCode (String code, ItemCatalog catalog)
    throws ShareCode.InvalidCodeException
  {
    ShareCode.Knight knight = ShareCode.decode(code, catalog);
    _items.clear();
    _items.putAll(knight.items);
    _shieldAway = knight.shieldAway;
    fireChanged();
    return knight.missing;
  }

  public void addChangeListener (ChangeListener listener)
  {
    _listeners.add(listener);
  }

  public void save (Preferences prefs)
  {
    prefs.put(PREF_KEY, toShareCode());
    prefs.putBoolean(BYPASS_PREF_KEY, _bypassBlocks);
  }

  /**
   * Restores the loadout from the given preferences node, skipping items that no longer exist.
   */
  public void restore (Preferences prefs, ItemCatalog catalog)
  {
    // the restored knight starts the history afresh
    _last = null;
    _undo.clear();
    _redo.clear();
    _bypassBlocks = prefs.getBoolean(BYPASS_PREF_KEY, false);
    String code = prefs.get(PREF_KEY, null);
    try {
      if (code != null) {
        applyShareCode(code, catalog);
        return;
      }
    } catch (ShareCode.InvalidCodeException e) {
      log.info("Discarding unreadable saved knight.", "error", e.getMessage());
    }
    // nothing else to show, but the option may have changed
    fireChanged();
  }


  protected void apply (KnightItem item, int colorization, ArgumentMap args)
  {
    if (item == null) {
      return;
    }
    try {
      item.applyTo(colorization, args);
    } catch (RuntimeException | LinkageError e) {
      log.warning("Failed to apply item to knight.", "item", item.configName, e);
    }
  }

  /**
   * Puts a random item from the catalog in a slot, without notifying the listeners.
   */
  protected void putRandom (Slot slot, ItemCatalog catalog, Random random)
  {
    // only the game's own items; custom ones are for the user to pick
    List<KnightItem> items = new ArrayList<>();
    for (KnightItem item : catalog.getItems(slot)) {
      if (!item.isCustom()) {
        items.add(item);
      }
    }
    if (!items.isEmpty()) {
      KnightItem item = items.get(random.nextInt(items.size()));
      _items.put(slot, item);
      for (Slot extra : item.getExtraSlots()) {
        _items.remove(extra);
      }
    }
  }

  public boolean canUndo ()
  {
    return !_undo.isEmpty();
  }

  public boolean canRedo ()
  {
    return !_redo.isEmpty();
  }

  public void undo ()
  {
    step(_undo, _redo);
  }

  public void redo ()
  {
    step(_redo, _undo);
  }

  protected void step (Deque<State> from, Deque<State> to)
  {
    if (from.isEmpty()) {
      return;
    }
    to.push(_last);
    State state = from.pop();
    _items.clear();
    _items.putAll(state.items());
    _shieldAway = state.shieldAway();
    _last = state;
    fireChanged();
  }

  /**
   * Records the change in the history and notifies the listeners. Changes made while handling the
   * same event count as one.
   */
  protected void fireChanged ()
  {
    State state = new State(new EnumMap<>(_items), _shieldAway);
    if (_last != null && !_grouping && !state.equals(_last)) {
      _undo.push(_last);
      if (_undo.size() > HISTORY_SIZE) {
        _undo.removeLast();
      }
      _redo.clear();
      _grouping = true;
      EventQueue.invokeLater(() -> _grouping = false);
    }
    _last = state;
    ChangeEvent event = new ChangeEvent(this);
    for (ChangeListener listener : new ArrayList<>(_listeners)) {
      listener.stateChanged(event);
    }
  }

  /** The chance of randomizing putting an accessory in each open accessory slot. */
  protected static final float ACCESSORY_CHANCE = 0.5f;

  /** The preference holding the saved knight's share code. */
  protected static final String PREF_KEY = "knight";

  /** The preference holding whether accessory blocks are bypassed. */
  protected static final String BYPASS_PREF_KEY = "bypassBlocks";

  protected final Map<Slot, KnightItem> _items = new EnumMap<>(Slot.class);
  protected final List<ChangeListener> _listeners = new ArrayList<>();
  protected boolean _shieldAway;

  protected boolean _bypassBlocks;

  /** The knight at some point in its history. */
  protected record State (Map<Slot, KnightItem> items, boolean shieldAway) { }

  /** The most changes that can be undone. */
  protected static final int HISTORY_SIZE = 100;

  /** The knights to go back and forward to, most recent first. */
  protected final Deque<State> _undo = new ArrayDeque<>(), _redo = new ArrayDeque<>();

  /** The knight as of the last change, or null before the history starts. */
  protected State _last;

  /** Whether a change was recorded while handling the current event. */
  protected boolean _grouping;
}
