package com.lucasluqui.silverweave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.lucasluqui.silverweave.Log.log;

/**
 * The knights the user has saved, kept as share codes under names of their choosing.
 *
 * <p>Each save is a child node of the preferences node, so there's no limit on how many there are.
 */
public class SavedLoadouts
{
  public static final class Entry
  {
    /** The preferences node name, which never changes. */
    public final String id;

    public final String code;

    /** When the knight was saved, in milliseconds since the epoch. */
    public final long created;

    public String getName ()
    {
      return _name;
    }

    public boolean isFavorite ()
    {
      return _favorite;
    }

    protected Entry (String id, String name, String code, long created, boolean favorite)
    {
      this.id = id;
      this.code = code;
      this.created = created;
      _name = name;
      _favorite = favorite;
    }

    protected String _name;
    protected boolean _favorite;
  }

  public SavedLoadouts (Preferences prefs)
  {
    _prefs = prefs;
    try {
      for (String id : prefs.childrenNames()) {
        Preferences node = prefs.node(id);
        String code = node.get(CODE, null);
        if (code == null) {
          node.removeNode();
          continue;
        }
        _entries.add(new Entry(id, node.get(NAME, "Unnamed"), code, node.getLong(CREATED, 0L),
          node.getBoolean(FAVORITE, false)));
      }
    } catch (BackingStoreException | IllegalStateException e) {
      log.warning("Failed to read saved knights.", e);
    }
  }

  /**
   * Returns the saved knights: favorites first, then newest first.
   */
  public List<Entry> getEntries ()
  {
    List<Entry> entries = new ArrayList<>(_entries);
    entries.sort(Comparator.comparing((Entry e) -> !e._favorite)
      .thenComparing(Comparator.comparingLong((Entry e) -> e.created).reversed()));
    return Collections.unmodifiableList(entries);
  }

  public Entry add (String name, String code)
  {
    long now = System.currentTimeMillis();
    String id = "k" + now;
    for (int ii = 1; contains(id); ii++) {
      id = "k" + now + "-" + ii;
    }
    Entry entry = new Entry(id, name, code, now, false);
    Preferences node = _prefs.node(id);
    node.put(NAME, name);
    node.put(CODE, code);
    node.putLong(CREATED, now);
    _entries.add(entry);
    fireChanged();
    return entry;
  }

  public void rename (Entry entry, String name)
  {
    entry._name = name;
    _prefs.node(entry.id).put(NAME, name);
    fireChanged();
  }

  public void toggleFavorite (Entry entry)
  {
    entry._favorite = !entry._favorite;
    _prefs.node(entry.id).putBoolean(FAVORITE, entry._favorite);
    fireChanged();
  }

  public void remove (Entry entry)
  {
    _entries.remove(entry);
    try {
      _prefs.node(entry.id).removeNode();
    } catch (BackingStoreException e) {
      log.warning("Failed to delete saved knight.", "id", entry.id, e);
    }
    fireChanged();
  }

  public void addChangeListener (Runnable listener)
  {
    _listeners.add(listener);
  }

  protected boolean contains (String id)
  {
    for (Entry entry : _entries) {
      if (entry.id.equals(id)) {
        return true;
      }
    }
    return false;
  }

  protected void fireChanged ()
  {
    for (Runnable listener : new ArrayList<>(_listeners)) {
      listener.run();
    }
  }

  protected static final String NAME = "name", CODE = "code", CREATED = "created",
    FAVORITE = "favorite";

  protected final Preferences _prefs;
  protected final List<Entry> _entries = new ArrayList<>();
  protected final List<Runnable> _listeners = new ArrayList<>();
}
