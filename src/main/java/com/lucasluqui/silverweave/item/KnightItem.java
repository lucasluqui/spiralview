package com.lucasluqui.silverweave.item;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
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
   * Creates the model arguments of a custom item recolored in a color.
   */
  public interface Recolorer
  {
    /**
     * @param color the id of an {@code armor_primary} color, which also goes on the
     * {@code armor_secondary} and {@code armor_joint} parts.
     */
    ModelArgs recolor (int color);
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

  /**
   * Checks whether this is a custom item, which isn't in the game but stands for a family of
   * recolored items in any color.
   */
  public boolean isCustom ()
  {
    return _recolorer != null;
  }

  /**
   * Returns the color (an {@code armor_primary} color id) a custom item is recolored with,
   * or 0 for other items.
   */
  public int getRecolor ()
  {
    return _recolor;
  }

  /**
   * Returns the item the catalog lists for this one: for a custom item in another color, the
   * custom item in its default color. Other items are their own base.
   */
  public KnightItem getBase ()
  {
    return _base;
  }

  /**
   * Returns this custom item in the given color. The same instance is returned for the
   * same color every time. Items that aren't custom are returned as they are.
   */
  public KnightItem withRecolor (int color)
  {
    KnightItem base = _base;
    if (_recolorer == null || color == base._recolor || color <= 0) {
      return _recolorer == null || color <= 0 ? this : base;
    }
    return base._variants.computeIfAbsent(color, base::createVariant);
  }

  /**
   * Makes a custom item into one, recolored in its default color.
   */
  protected static KnightItem custom (String configName, int configId, String displayName,
    Slot slot, String category, Icon icon, int color, Recolorer recolorer)
  {
    KnightItem item = new KnightItem(configName, configId, displayName, slot, category, icon,
      recolorer.recolor(color));
    item._recolorer = recolorer;
    item._recolor = color;
    item._variants = new HashMap<>();
    return item;
  }

  /**
   * Creates this (base) custom item in another color.
   */
  protected KnightItem createVariant (int color)
  {
    KnightItem item = new KnightItem(configName, configId, displayName, slot, category, icon,
      _recolorer.recolor(color));
    item._blocks.addAll(_blocks);
    item._extra.addAll(_extra);
    item._recolorer = _recolorer;
    item._recolor = color;
    item._base = this;
    return item;
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

  /** For custom items: makes their model arguments in each color. */
  protected Recolorer _recolorer;
  protected int _recolor;
  protected KnightItem _base = this;

  /** For base custom items: the item in the other colors asked for so far. */
  protected Map<Integer, KnightItem> _variants;
}
