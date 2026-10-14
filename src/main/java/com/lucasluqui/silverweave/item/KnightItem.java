package com.lucasluqui.silverweave.item;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

import com.threerings.config.ArgumentMap;

/**
 * An item from configs that can be put on a knight.
 */
public final class KnightItem
  implements Comparable<KnightItem>
{
  /**
   * Contributes an item's arguments to the knight model.
   */
  public interface ModelArgs
  {
    /**
     * Adds the item's arguments to the knight model arguments.
     *
     * @param colorization the knight's personal color.
     */
    void apply (int colorization, ArgumentMap args);
  }

  /**
   * The inventory icon of an item.
   */
  public static final class Icon
  {
    /** The resource path of the icon image. */
    public final String file;

    public Icon (String file, Supplier<Object> colorizations)
    {
      this.file = file;
      _colorizations = colorizations;
    }

    /**
     * Returns the icon's colorizations, as an array of spiralview's {@code ColorizationConfig}s,
     * or null if there are none.
     */
    public Object getColorizations ()
    {
      return _colorizations == null ? null : _colorizations.get();
    }

    protected final Supplier<Object> _colorizations;
  }

  public final String configName;

  public final int configId;

  /** Name shown to the user. */
  public final String displayName;

  public final Slot slot;

  /** Finer category within the slot (weapon type, for instance), or null. */
  public final String category;

  public final Icon icon;

  public KnightItem (String configName, int configId, String displayName, Slot slot,
    String category, Icon icon, ModelArgs args)
  {
    this.configName = configName;
    this.configId = configId;
    this.displayName = displayName;
    this.slot = slot;
    this.category = category;
    this.icon = icon;
    _args = args;
  }

  public void applyTo (int colorization, ArgumentMap args)
  {
    if (_args != null) {
      _args.apply(colorization, args);
    }
  }

  /**
   * Returns the accessory slots this item prevents from being used (for helmets and armor).
   */
  public Set<Slot> getBlockedSlots ()
  {
    return Collections.unmodifiableSet(_blocks);
  }

  /**
   * Returns the slots other than its own that this item occupies (for multi-slot accessories).
   */
  public Set<Slot> getExtraSlots ()
  {
    return Collections.unmodifiableSet(_extra);
  }

  /**
   * Returns the colorization id of a personal color item.
   */
  public int getColorization ()
  {
    return _colorization;
  }

  /**
   * Returns the height argument value of a height item.
   */
  public String getHeight ()
  {
    return _height;
  }

  /**
   * Checks whether this is the eye item for the eyes knights start out with.
   */
  public boolean isDefaultEyes ()
  {
    return _defaultEyes;
  }

  @Override
  public int compareTo (KnightItem other)
  {
    int cmp = String.CASE_INSENSITIVE_ORDER.compare(displayName, other.displayName);
    return cmp != 0 ? cmp : configName.compareTo(other.configName);
  }

  @Override
  public String toString ()
  {
    return displayName;
  }

  protected final ModelArgs _args;
  protected final Set<Slot> _blocks = EnumSet.noneOf(Slot.class);
  protected final Set<Slot> _extra = EnumSet.noneOf(Slot.class);
  protected int _colorization;
  protected String _height;
  protected boolean _defaultEyes;
}
