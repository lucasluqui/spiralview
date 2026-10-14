package com.lucasluqui.silverweave;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.lucasluqui.silverweave.item.KnightItem;

import static com.lucasluqui.silverweave.Log.log;

/**
 * The items the user has marked as favorites, which are listed before the others.
 *
 * <p>Favorites are kept by config id (one preference key each), since those stay stable across
 * updates.
 */
public class Favorites
{
  public Favorites (Preferences prefs)
  {
    _prefs = prefs;
    try {
      for (String key : prefs.keys()) {
        try {
          _ids.add(Integer.parseInt(key));
        } catch (NumberFormatException e) {
          prefs.remove(key);
        }
      }
    } catch (BackingStoreException e) {
      log.warning("Failed to read favorites.", e);
    }
  }

  public boolean contains (KnightItem item)
  {
    return item != null && _ids.contains(item.configId);
  }

  /**
   * Adds an item to the favorites, or removes it if it is one already.
   */
  public void toggle (KnightItem item)
  {
    String key = String.valueOf(item.configId);
    if (_ids.remove(item.configId)) {
      _prefs.remove(key);
    } else {
      _ids.add(item.configId);
      _prefs.put(key, "");
    }
    for (Runnable listener : new ArrayList<>(_listeners)) {
      listener.run();
    }
  }

  /**
   * Returns the items with the favorites moved to the front, otherwise keeping their order.
   */
  public List<KnightItem> arrange (Collection<KnightItem> items)
  {
    List<KnightItem> arranged = new ArrayList<>(items.size());
    for (KnightItem item : items) {
      if (contains(item)) {
        arranged.add(item);
      }
    }
    for (KnightItem item : items) {
      if (!contains(item)) {
        arranged.add(item);
      }
    }
    return arranged;
  }

  public void addChangeListener (Runnable listener)
  {
    _listeners.add(listener);
  }

  protected final Preferences _prefs;
  protected final Set<Integer> _ids = new HashSet<>();
  protected final List<Runnable> _listeners = new ArrayList<>();
}
