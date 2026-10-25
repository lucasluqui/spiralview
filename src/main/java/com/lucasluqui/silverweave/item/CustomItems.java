package com.lucasluqui.silverweave.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.threerings.config.ArgumentMap;
import com.threerings.config.ConfigReference;
import com.threerings.opengl.renderer.config.ColorizationConfig;

import static com.lucasluqui.silverweave.Log.log;

/**
 * Finds the families of items that are the same model in different colors
 * and creates a custom item for each that can be recolored in any color.
 */
public final class CustomItems
{
  /** The colorization classes custom items recolor, named as in the color definitions. */
  public static final int ARMOR_PRIMARY = 11, ARMOR_SECONDARY = 12, ARMOR_JOINT = 13;

  /** The color the custom items' icons are shown in. */
  public static final int ICON_COLOR = 57;

  /**
   * @param colorization the personal color the items' arguments are compared in.
   */
  public CustomItems (int colorization)
  {
    _colorization = colorization;
  }

  /**
   * Creates the custom items for the items in the catalog.
   *
   * @param ids the config ids already taken, to which the custom items' are added.
   * @param names the config names already taken, to which the custom items' are added.
   */
  public List<KnightItem> create (Map<Slot, List<KnightItem>> items, Set<Integer> ids,
    Set<String> names)
  {
    List<KnightItem> customs = new ArrayList<>();
    for (Map.Entry<Slot, List<KnightItem>> entry : items.entrySet()) {
      if (entry.getKey().section != Slot.Section.STYLE) {
        customs.addAll(create(entry.getKey(), entry.getValue(), ids, names));
      }
    }
    return customs;
  }

  /**
   * Returns names for the colors derived from the families' items.
   *
   * @return the names by color id, for the colors that families come in.
   */
  public Map<Integer, String> getColorNames ()
  {
    Map<Integer, String> names = new HashMap<>();
    for (Map.Entry<Integer, Map<String, Integer>> entry : _votes.entrySet()) {
      entry.getValue().entrySet().stream()
        .max(Map.Entry.<String, Integer>comparingByValue()
          .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER.reversed())))
        .ifPresent(best -> names.put(entry.getKey(), best.getKey()));
    }
    return names;
  }

  /**
   * Recolors model arguments in place: every colorization in the recolorable classes takes the
   * given color in its own class.
   *
   * @return the value to use in place of the given one.
   */
  public static Object recolor (Object value, int color)
  {
    if (value instanceof ColorizationConfig.Normal) {
      ColorizationConfig.Normal normal = (ColorizationConfig.Normal) value;
      int clazz = normal.colorization >> 8;
      return isRecolorableClass(clazz) ?
        ColorizationConfig.createConfig((clazz << 8) | color) : value;
    } else if (value instanceof ColorizationConfig.TransNormal) {
      // borrowed colors become the chosen color in the class they're applied to
      int clazz = ((ColorizationConfig.TransNormal) value).clazz;
      return isRecolorableClass(clazz) ? ColorizationConfig.createConfig((clazz << 8) | color) : value;
    } else if (isRecolorableId(value)) {
      return (((Integer) value >> 8) << 8) | color;
    } else if (value instanceof ConfigReference) {
      recolor(((ConfigReference<?>) value).getArguments(), color);
    } else if (value instanceof ArgumentMap) {
      ArgumentMap args = (ArgumentMap) value;
      for (String key : new ArrayList<>(args.keySet())) {
        if (!key.equals(PERSONAL_COLOR)) {
          args.put(key, recolor(args.get(key), color));
        }
      }
    } else if (value instanceof Object[]) {
      Object[] array = (Object[]) value;
      for (int ii = 0; ii < array.length; ii++) {
        Object recolored = recolor(array[ii], color);
        if (array.getClass().getComponentType().isInstance(recolored)) {
          array[ii] = recolored;
        }
      }
    }
    return value;
  }

  protected static boolean isRecolorableClass (int clazz)
  {
    return clazz >= ARMOR_PRIMARY && clazz <= ARMOR_JOINT;
  }

  /**
   * Checks whether an argument is a plain colorization id in an armor class, as some models take
   * them.
   */
  protected static boolean isRecolorableId (Object value)
  {
    return value instanceof Integer && isRecolorableClass((Integer) value >> 8) &&
      ((Integer) value & 0xFF) != 0;
  }

  /**
   * Finds the families among the items for a slot and creates their custom items.
   */
  protected List<KnightItem> create (Slot slot, List<KnightItem> items, Set<Integer> ids,
    Set<String> names)
  {
    // the items by their arguments with the colors left out
    Map<String, Family> families = new LinkedHashMap<>();
    for (KnightItem item : items) {
      ArgumentMap args = new ArgumentMap();
      try {
        item.applyTo(_colorization, args);
      } catch (RuntimeException | LinkageError e) {
        continue;
      }
      Scan scan = new Scan();
      String template = scan.describe(args);
      if (scan.colored) {
        families.computeIfAbsent(template, t -> new Family()).add(item, scan);
      }
    }

    // a family is named after what its items' names end in, and the biggest family of each name
    // gets the custom item.
    Map<String, Family> named = new LinkedHashMap<>();
    for (Family family : families.values()) {
      if (family.items.size() < 2 || family.colors.size() < 2) {
        continue;
      }
      String name = family.name();
      Family other = named.get(name);
      if (name != null && (other == null || family.items.size() > other.items.size())) {
        named.put(name, family);
      }
    }

    List<KnightItem> customs = new ArrayList<>();
    for (Map.Entry<String, Family> entry : named.entrySet()) {
      entry.getValue().vote(entry.getKey(), _votes);
      try {
        customs.add(entry.getValue().createCustom(slot, entry.getKey(), ids, names));
      } catch (RuntimeException | LinkageError e) {
        log.warning("Failed to create custom item.", "name", entry.getKey(), e);
      }
    }
    return customs;
  }

  /**
   * Items that are the same but for their colors.
   */
  protected static final class Family
  {
    public final List<KnightItem> items = new ArrayList<>();

    /** The distinct colorizations of the items. */
    public final Set<String> colors = new LinkedHashSet<>();

    public void add (KnightItem item, Scan scan)
    {
      items.add(item);
      colors.add(scan.colors.toString());
      int color = scan.uniformColor();
      if (color > 0) {
        _uniform.put(item, color);
      }
    }

    /**
     * Counts the words before the family name in the names of the items in a single color
     * as votes for that color's name.
     */
    public void vote (String name, Map<Integer, Map<String, Integer>> votes)
    {
      for (Map.Entry<KnightItem, Integer> entry : _uniform.entrySet()) {
        String full = entry.getKey().displayName.trim();
        String prefix = full.substring(0, full.length() - name.length()).trim();
        if (!prefix.isEmpty()) {
          votes.computeIfAbsent(entry.getValue(), c -> new HashMap<>()).merge(prefix, 1,
            Integer::sum);
        }
      }
    }

    /**
     * Returns the words all the items' names end in, or null if they have none in common.
     */
    public String name ()
    {
      String[] common = items.get(0).displayName.trim().split("\\s+");
      int count = common.length;
      for (KnightItem item : items) {
        String[] words = item.displayName.trim().split("\\s+");
        int same = 0;
        while (same < count && same < words.length && common[common.length - 1 - same]
            .equalsIgnoreCase(words[words.length - 1 - same])) {
          same++;
        }
        count = same;
      }
      return count == 0 ? null :
        String.join(" ", List.of(common).subList(common.length - count, common.length));
    }

    public KnightItem createCustom (Slot slot, String name, Set<Integer> ids, Set<String> names)
    {
      // the first item by name stands for the rest; only its colors change
      KnightItem model = items.get(0);
      for (KnightItem item : items) {
        if (item.compareTo(model) < 0) {
          model = item;
        }
      }
      int id = ("silverweave/custom/" + slot.name() + "/" + name).hashCode();
      while (ids.contains(id)) {
        id++;
      }
      // named as a sibling of the items, so it sorts and searches like them
      int comma = model.configName.lastIndexOf(", ");
      String configName = (comma < 0 ? model.configName : model.configName.substring(0, comma)) +
        ", Custom";
      while (names.contains(configName)) {
        configName += " ";
      }
      ids.add(id);
      names.add(configName);

      // the icon in a color none of the items come in, to tell it apart from them
      KnightItem.Icon modelIcon = model.icon;
      KnightItem.Icon icon = modelIcon == null ? null : new KnightItem.Icon(modelIcon.file,
        () -> recolor(modelIcon.getColorizations(), ICON_COLOR));
      KnightItem source = model;
      KnightItem custom = KnightItem.custom(configName, id, "Custom " + name, slot,
        model.category, icon, _uniform.getOrDefault(model, _uniform.values().stream()
          .findFirst().orElse(1)), color -> (colorization, args) -> {
          ArgumentMap own = new ArgumentMap();
          source.applyTo(colorization, own);
          recolor(own, color);
          args.putAll(own);
        });
      custom._blocks.addAll(model._blocks);
      custom._extra.addAll(model._extra);
      return custom;
    }

    /** The colors of the items that are in a single one. */
    protected final Map<KnightItem, Integer> _uniform = new LinkedHashMap<>();
  }

  /**
   * Describes model arguments with their colors left out, noting the colors.
   */
  protected static final class Scan
  {
    /** Whether the arguments have any recolorable colors. */
    public boolean colored;

    /** The recolorable colorizations found, in the order found. */
    public final List<Integer> colors = new ArrayList<>();

    /**
     * Returns the color id all the recolorable colorizations share, or 0 if they don't.
     */
    public int uniformColor ()
    {
      int color = 0;
      for (int colorization : colors) {
        if (colorization < 0 || (color != 0 && (colorization & 0xFF) != color)) {
          return 0;
        }
        color = colorization & 0xFF;
      }
      return color;
    }

    public String describe (Object value)
    {
      if (value instanceof ColorizationConfig.Normal) {
        int colorization = ((ColorizationConfig.Normal) value).colorization;
        if (isRecolorableClass(colorization >> 8)) {
          colored = true;
          colors.add(colorization);
          return "<" + (colorization >> 8) + ">";
        }
        return "Normal:" + colorization;
      } else if (value instanceof ColorizationConfig.TransNormal) {
        ColorizationConfig.TransNormal trans = (ColorizationConfig.TransNormal) value;
        if (isRecolorableClass(trans.clazz)) {
          colored = true;
          // (borrowed colors make the item a family of its own, see uniformColor)
          colors.add(-trans.colorization);
          return "<trans " + trans.clazz + ">";
        }
        return "TransNormal:" + trans.clazz + ":" + trans.colorization;
      } else if (isRecolorableId(value)) {
        colored = true;
        colors.add((Integer) value);
        return "<id " + ((Integer) value >> 8) + ">";
      } else if (value instanceof ConfigReference) {
        ConfigReference<?> ref = (ConfigReference<?>) value;
        return ref.getName() + describe(ref.getArguments());
      } else if (value instanceof ArgumentMap) {
        StringBuilder buf = new StringBuilder("{");
        // (argument maps keep their keys sorted)
        for (Map.Entry<String, Object> entry : ((ArgumentMap) value).entrySet()) {
          if (!entry.getKey().equals(PERSONAL_COLOR)) {
            buf.append(entry.getKey()).append('=').append(describe(entry.getValue())).append(", ");
          }
        }
        return buf.append('}').toString();
      } else if (value instanceof Object[]) {
        StringBuilder buf = new StringBuilder("[");
        for (Object element : (Object[]) value) {
          buf.append(describe(element)).append(", ");
        }
        return buf.append(']').toString();
      }
      return String.valueOf(value);
    }
  }

  protected final int _colorization;

  /** The names given to each color by the families' items, and how often. */
  protected final Map<Integer, Map<String, Integer>> _votes = new HashMap<>();

  /** The knight model argument for the personal color, which custom items leave alone. */
  protected static final String PERSONAL_COLOR = "Colorization";
}
