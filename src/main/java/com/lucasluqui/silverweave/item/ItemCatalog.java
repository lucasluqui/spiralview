package com.lucasluqui.silverweave.item;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.lucasluqui.silverweave.code.Code;
import com.lucasluqui.silverweave.code.ConfigPaths;
import com.lucasluqui.silverweave.code.Signatures;

import static com.lucasluqui.silverweave.Log.log;

/**
 * The knight-relevant items, sorted into {@link Slot}s.
 *
 * <p>Items are read and resolved by code itself, and their contribution to the knight model comes
 * from code's own methods. Those methods are found by signature since their names are obfuscated.
 */
public final class ItemCatalog
{
  /**
   * Loads the catalog.
   *
   * @param items the contents of {@code config/item.dat}.
   * @param accessories the contents of {@code config/accessory.dat}.
   * @param translator translates item name keys.
   */
  public static ItemCatalog load (Code code, InputStream items, InputStream accessories,
                                  Function<String, String> translator)
    throws IOException
  {
    return new ItemCatalog(code, items, accessories, translator);
  }

  /**
   * Returns the items for a slot, sorted by name.
   */
  public List<KnightItem> getItems (Slot slot)
  {
    List<KnightItem> items = _items.get(slot);
    return items == null ? Collections.emptyList() : items;
  }

  public KnightItem getItem (int configId)
  {
    return _byId.get(configId);
  }

  public KnightItem getItem (String configName)
  {
    return _byName.get(configName);
  }

  /**
   * Returns the name the game's items give a color custom items can be recolored in,
   * or null if none of the recolored families come in it.
   */
  public String getColorName (int color)
  {
    return _colorNames.get(color);
  }

  /**
   * Checks whether accessory configs were available.
   */
  public boolean hasAccessories ()
  {
    return _hasAccessories;
  }

  protected ItemCatalog (Code code, InputStream items, InputStream accessories,
                         Function<String, String> translator)
    throws IOException
  {
    _code = code;
    _translator = translator;

    String pkg = "com.threerings.projectx.item.config.";
    Class<?> itemClass = code.type(pkg + "ItemConfig");
    Class<?> original = code.type(pkg + "ItemConfig$Original");
    Class<?> argMap = code.argumentMapClass();

    // ItemConfig -> Original (follows derived items), and Original's player model arguments.
    Method resolve = Signatures.method(itemClass, null, original, code.configManagerClass());
    Method itemArgs = Signatures.method(original, null, void.class, int.class, argMap);

    Field creatable = field(itemClass, "creatable");
    // the config id is the item config's only int field (it isn't public, so match by type)
    Field configId = Signatures.field(itemClass, int.class);
    Field nameKey = field(original, "name");
    Field implementation = field(itemClass, "implementation");
    Class<?> derived = code.type(pkg + "ItemConfig$Derived");
    Field derivedItem = field(derived, "item");

    Code.ConfigSpace space = code.newConfigSpace();
    Object[] itemConfigs = code.readConfigs(items);
    space.addGroup(itemClass, itemConfigs);

    Class<?> accessoryClass = code.type(pkg + "AccessoryConfig");
    Object[] accessoryConfigs = new Object[0];
    if (accessories != null) {
      accessoryConfigs = code.readConfigs(accessories);
      space.addGroup(accessoryClass, accessoryConfigs);
      _hasAccessories = true;
    }

    // the implementation classes (whose names are fixed by the config format) and their slots.
    // subclasses come before their parents (costumes extend the regular gear, and so on)
    Map<Class<?>, Slot> slots = new LinkedHashMap<>();
    Map<Class<?>, String> categories = new HashMap<>();
    // costumes share their gear's slot, told apart only by category
    for (String gear : new String[] {"Helm", "Armor", "Shield"}) {
      Slot slot = gear.equals("Helm") ? Slot.HELMET : Slot.valueOf(gear.toUpperCase());
      Class<?> costume = putType(slots, pkg + "ItemConfig$" + gear + "Costume", slot);
      Class<?> base = putType(slots, pkg + "ItemConfig$" + gear, slot);
      if (costume != null) {
        categories.put(costume, COSTUME);
      }
      if (base != null) {
        categories.put(base, GEAR);
      }
    }
    for (String weapon : new String[] {"Sword", "Handgun", "Bomb"}) {
      Class<?> clazz = putType(slots, pkg + "ItemConfig$" + weapon, Slot.WEAPON);
      if (clazz != null) {
        categories.put(clazz, weapon);
      }
    }
    putType(slots, pkg + "ItemConfig$AccessoryTicket", null);
    putType(slots, pkg + "ItemConfig$Color", Slot.COLOR);
    putType(slots, pkg + "ItemConfig$Eye", Slot.EYES);
    putType(slots, pkg + "ItemConfig$Height", Slot.HEIGHT);

    AccessoryReader accessoryReader = _hasAccessories ?
      new AccessoryReader(code, space, accessoryClass, accessoryConfigs) : null;
    Set<String> seenAccessories = new HashSet<>();

    int failed = 0;
    for (Object config : itemConfigs) {
      String name = code.nameOf(config);
      if (!Boolean.TRUE.equals(Code.get(creatable, config)) ||
        name.contains("/Parts/") || name.contains("(Dev)") || name.endsWith(", Base")) {
        continue;
      }
      Object impl;
      try {
        // finish applying the arguments pcode can't (see ConfigPaths) before resolving
        List<Object> chain = applyNestedArguments(config, implementation, derived, derivedItem,
          itemClass, space);
        impl = Code.invoke(resolve, config, space.manager());
        // keep the instances we fixed up cached until resolution has picked them up
        Reference.reachabilityFence(chain);
      } catch (RuntimeException | LinkageError e) {
        failed++;
        continue;
      }
      if (impl == null) {
        continue;
      }
      Class<?> type = null;
      for (Class<?> candidate : slots.keySet()) {
        if (candidate.isInstance(impl)) {
          type = candidate;
          break;
        }
      }
      if (type == null) {
        continue;
      }
      Slot slot = slots.get(type);
      int id = ((Number) Code.get(configId, config)).intValue();

      try {
        KnightItem item;
        if (slot == null) {
          // accessory ticket: everything interesting lives in the accessory config
          if (accessoryReader == null) {
            continue;
          }
          item = accessoryReader.read(name, id, (String) Code.get(nameKey, impl), impl,
            seenAccessories);

        } else if (slot == Slot.COLOR) {
          // the color itself is the last part. i.e.: "m.personal_color|m.amber"
          item = new KnightItem(name, id,
            displayName(name, lastPart((String) Code.get(nameKey, impl))), slot, null, icon(impl), null);
          item._colorization = ((Number) Code.get(field(type, "color"), impl)).intValue();

        } else if (slot == Slot.EYES) {
          item = eyeItem(name, id, displayName(name, (String) Code.get(nameKey, impl)), icon(impl),
            Code.get(field(type, "eye"), impl));

        } else if (slot == Slot.HEIGHT) {
          String height = heightValue(Code.get(field(type, "height"), impl));
          String label = HEIGHT_LABELS.get(height);
          item = new KnightItem(name, id, label != null ? label :
            displayName(name, lastPart((String) Code.get(nameKey, impl))), slot, null, icon(impl), null);
          item._height = height;

        } else {
          item = new KnightItem(name, id, displayName(name, (String) Code.get(nameKey, impl)), slot,
            categories.get(type), icon(impl), modelArgs(itemArgs, impl));
          readBlockers(item, impl);
        }
        if (item != null) {
          add(item);
        }
      } catch (RuntimeException | LinkageError e) {
        failed++;
        log.warning("Failed to read item.", "item", name, e);
      }
    }

    // then the custom items for the families of recolored ones
    CustomItems customs = new CustomItems(DEFAULT_COLORIZATION);
    List<KnightItem> created = customs.create(_items, new HashSet<>(_byId.keySet()),
      new HashSet<>(_byName.keySet()));
    for (KnightItem custom : created) {
      add(custom);
    }
    _colorNames = customs.getColorNames();

    for (List<KnightItem> list : _items.values()) {
      Collections.sort(list);
    }
    log.info("Loaded item catalog.", "items", _byName.size(), "custom", created.size(),
      "failed", failed, "accessories", _hasAccessories);
  }

  /**
   * Follows an item's chain of derived configs, applying at each step the arguments that pcode
   * couldn't (see ConfigPaths). Each step is resolved from the previous step's corrected
   * arguments, so the values carry all the way down.
   *
   * @return the config instances along the chain, which must stay reachable until the item has
   * been resolved so that pcode finds the same (corrected) instances in its cache.
   */
  protected List<Object> applyNestedArguments (Object config, Field implementation,
    Class<?> derived, Field derivedItem, Class<?> itemClass, Code.ConfigSpace space)
  {
    List<Object> chain = new ArrayList<>();
    Object current = config;
    for (int depth = 0; depth < 16; depth++) {
      Object impl = Code.get(implementation, current);
      Object ref = derived.isInstance(impl) ? Code.get(derivedItem, impl) : null;
      Object next = ref == null ? null : space.resolve(itemClass, ref);
      if (next == null) {
        break;
      }
      ConfigPaths.applyNested(_code, next, _code.referenceArguments(ref));
      chain.add(next);
      current = next;
    }
    return chain;
  }

  /**
   * Creates the item for an eye type upgrade.
   */
  protected KnightItem eyeItem (String name, int id, String displayName,
                                KnightItem.Icon icon, Object eyeType)
  {
    if (eyeType == null) {
      return null;
    }
    //EyeType.updatePlayerModelArgs(ArgumentMap)
    Method update = Signatures.method(eyeType.getClass(), "updatePlayerModelArgs", void.class,
      _code.argumentMapClass());
    KnightItem item = new KnightItem(name, id, displayName, Slot.EYES, null, icon,
      modelArgs(update, eyeType, false));
    item._defaultEyes = eyeType instanceof Enum && ((Enum<?>)eyeType).name().equals("DEFAULT");
    return item;
  }

  /**
   * Returns the translated name for an item name key, falling back to the last part of the
   * config name if the key can't be translated.
   */
  protected String displayName (String configName, String key)
  {
    String name = (key == null || key.isEmpty()) ? null : _translator.apply(key);
    return (name != null && !name.trim().isEmpty()) ? name.trim() :
      configName.substring(configName.lastIndexOf('/') + 1).trim();
  }

  /**
   * Returns last part of a compound message key.
   */
  protected static String lastPart (String key)
  {
    return key == null ? null : key.substring(key.lastIndexOf('|') + 1);
  }

  /**
   * Reads an item's inventory icon, whose colorizations are only converted when first needed.
   */
  protected KnightItem.Icon icon (Object impl)
  {
    Field iconField = fieldOrNull(impl.getClass(), "icon");
    Object icon = iconField == null ? null : Code.get(iconField, impl);
    Field fileField = icon == null ? null : fieldOrNull(icon.getClass(), "file");
    String file = fileField == null ? null : (String) Code.get(fileField, icon);
    if (file == null || file.isEmpty()) {
      return null;
    }
    Field colorsField = fieldOrNull(icon.getClass(), "colorizations");
    Object colors = colorsField == null ? null : Code.get(colorsField, icon);
    return new KnightItem.Icon(file, colors == null ? null : () -> _code.toTool(colors));
  }

  /**
   * Returns the model argument value of a {@code KnightHeight} constant.
   */
  protected static String heightValue (Object height)
  {
    Class<?> clazz = ((Enum<?>)height).getDeclaringClass();
    Field value = fieldOrNull(clazz, "value");
    if (value == null) {
      // the enum carries a single public string field besides the ones inherited from Enum
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == String.class && Modifier.isPublic(field.getModifiers()) &&
          !Modifier.isStatic(field.getModifiers())) {
          value = field;
          break;
        }
      }
    }
    return value == null ? ((Enum<?>)height).name() : (String) Code.get(value, height);
  }

  /**
   * Records the accessory slots blocked by a helmet or armor.
   */
  protected void readBlockers (KnightItem item, Object impl)
  {
    Field blockers = fieldOrNull(impl.getClass(), "accessoryBlockers");
    Object[] locations = blockers == null ? null : (Object[]) Code.get(blockers, impl);
    if (locations != null) {
      for (Object location : locations) {
        Slot slot = Slot.forAccessoryLocation(((Enum<?>)location).name());
        if (slot != null) {
          item._blocks.add(slot);
        }
      }
    }
  }

  protected void add (KnightItem item)
  {
    _items.computeIfAbsent(item.slot, s -> new ArrayList<>()).add(item);
    _byName.put(item.configName, item);
    _byId.put(item.configId, item);
  }

  /**
   * Creates model arguments that invoke a pcode {@code (int colorization, ArgumentMap)} method.
   */
  protected KnightItem.ModelArgs modelArgs (Method method, Object target)
  {
    return modelArgs(method, target, true);
  }

  /**
   * Creates model arguments that invoke a pcode method taking an optional colorization followed
   * by the argument map to populate.
   */
  protected KnightItem.ModelArgs modelArgs (Method method, Object target, boolean colorized)
  {
    return (colorization, args) -> {
      Map<String, Object> pargs = _code.newArguments();
      if (colorized) {
        Code.invoke(method, target, colorization, pargs);
      } else {
        Code.invoke(method, target, pargs);
      }
      for (Map.Entry<String, Object> entry : pargs.entrySet()) {
        try {
          args.put(entry.getKey(), _code.toTool(entry.getValue()));
        } catch (IllegalArgumentException e) {
          log.warning("Dropping model argument.", "name", entry.getKey(), "error", e.getMessage());
        }
      }
    };
  }

  /**
   * Registers an implementation class if present in this version of pcode.
   */
  protected Class<?> putType (Map<Class<?>, Slot> slots, String name, Slot slot)
  {
    try {
      Class<?> clazz = _code.type(name);
      slots.put(clazz, slot);
      return clazz;
    } catch (IllegalStateException e) {
      log.warning("Item type not found in pcode, skipping.", "type", name);
      return null;
    }
  }

  /**
   * Returns a public (exported, hence stably named) field.
   */
  protected static Field field (Class<?> clazz, String name)
  {
    Field field = fieldOrNull(clazz, name);
    if (field == null) {
      throw new IllegalStateException("Missing field " + clazz.getName() + "." + name);
    }
    return field;
  }

  protected static Field fieldOrNull (Class<?> clazz, String name)
  {
    try {
      return clazz.getField(name);
    } catch (NoSuchFieldException e) {
      return null;
    }
  }

  /**
   * Reads accessory tickets, whose accessory config holds the location and model.
   */
  protected class AccessoryReader
  {
    public AccessoryReader (Code code, Code.ConfigSpace space, Class<?> accessoryClass,
      Object[] configs)
    {
      _space = space;
      _accessoryClass = accessoryClass;
      _accessoryArgs = Signatures.method(accessoryClass, null, void.class, int.class,
        code.argumentMapClass());
      for (Object config : configs) {
        _configs.put(code.nameOf(config), config);
      }
      Class<?> derived;
      try {
        derived = code.type(DERIVED_CONFIG);
      } catch (IllegalStateException e) {
        derived = null;
      }
      _derivedConfig = derived;
      _derivedBase = derived == null ? null : field(derived, "base");
      Class<?> removal;
      try {
        removal = code.type(accessoryClass.getName() + "$Remove");
      } catch (IllegalStateException e) {
        removal = null;
      }
      _removalClass = removal;
    }

    /**
     * Creates the item for an accessory ticket, or returns null if it duplicates one already read.
     */
    public KnightItem read (String name, int id, String nameKey, Object ticket, Set<String> seen)
    {
      Object ref = Code.get(field(ticket.getClass(), "accessory"), ticket);
      if (ref == null) {
        return null;
      }
      // many tickets grant the same accessory; list it once
      String key = _code.referenceName(ref) + _code.referenceArguments(ref);
      if (!seen.add(key)) {
        return null;
      }
      Object accessory = _space.resolve(_accessoryClass, ref);
      // the tickets that recover (or destroy) an accessory put nothing on the knight
      if (accessory == null || (_removalClass != null && _removalClass.isInstance(accessory))) {
        return null;
      }
      applyNestedArguments(accessory, ref);
      Object location = Code.get(field(accessory.getClass(), "location"), accessory);
      Slot slot = location == null ? null : Slot.forAccessoryLocation(((Enum<?>) location).name());
      if (slot == null) {
        return null;
      }
      // tickets without a name of their own use the accessory's
      if (nameKey == null || nameKey.isEmpty()) {
        Field aName = fieldOrNull(accessory.getClass(), "aName");
        nameKey = aName == null ? null : (String) Code.get(aName, accessory);
      }
      KnightItem item = new KnightItem(name, id, displayName(name, nameKey), slot, null, icon(ticket),
        modelArgs(_accessoryArgs, accessory));

      // multi-slot accessories hold further locations in other fields of the same enum type
      for (Field other : accessory.getClass().getFields()) {
        if (other.getType() == location.getClass() && !other.getName().equals("location")) {
          Object extra = Code.get(other, accessory);
          Slot extraSlot = extra == null ? null : Slot.forAccessoryLocation(((Enum<?>) extra).name());
          if (extraSlot != null && extraSlot != slot) {
            item._extra.add(extraSlot);
          }
        }
      }
      return item;
    }

    /**
     * Applies the arguments pcode couldn't (see ConfigPaths) to a resolved accessory, at each step
     * of its derivation. As in pcode, the base's arguments go first and the variant's after, each
     * for the parameters of the config it was given to.
     */
    protected void applyNestedArguments (Object accessory, Object ref)
    {
      List<Object> configs = new ArrayList<>();
      List<Map<String, Object>> args = new ArrayList<>();
      for (int depth = 0; ref != null && depth < 16; depth++) {
        Object config = _configs.get(_code.referenceName(ref));
        if (config == null) {
          break;
        }
        configs.add(config);
        args.add(_code.referenceArguments(ref));
        ref = _derivedConfig != null && _derivedConfig.isInstance(config) ?
          Code.get(_derivedBase, config) : null;
      }
      for (int ii = configs.size() - 1; ii >= 0; ii--) {
        ConfigPaths.applyNested(_code, accessory, configs.get(ii), args.get(ii));
      }
    }

    protected final Code.ConfigSpace _space;
    protected final Class<?> _accessoryClass;
    protected final Method _accessoryArgs;

    /** The class of the accessory configs that remove the accessory in a slot, if present. */
    protected final Class<?> _removalClass;

    /** The accessory configs as read (derived ones unresolved), by name. */
    protected final Map<String, Object> _configs = new HashMap<>();

    /** pcode's derived config class and its base reference, if present. */
    protected final Class<?> _derivedConfig;
    protected final Field _derivedBase;
  }

  /** The class of the configs derived from others with arguments. */
  protected static final String DERIVED_CONFIG = "com.threerings.config.DerivedConfig";

  /** The categories of gear and costume items. */
  public static final String GEAR = "Gear";
  public static final String COSTUME = "Costume";

  /** The names of the knight heights, by height argument value. */
  protected static final Map<String, String> HEIGHT_LABELS = Map.of(
    "-2", "Extra Short", "-1", "Short", "0", "Normal", "+1", "Tall", "+2", "Extra Tall");

  /** The default personal color (magenta). */
  public static final int DEFAULT_COLORIZATION = 257;

  protected final Code _code;
  protected final Function<String, String> _translator;
  protected final Map<Slot, List<KnightItem>> _items = new EnumMap<>(Slot.class);
  protected final Map<String, KnightItem> _byName = new HashMap<>();
  protected final Map<Integer, KnightItem> _byId = new HashMap<>();
  protected boolean _hasAccessories;

  /** The names of the colors custom items can be recolored in, by color id. */
  protected Map<Integer, String> _colorNames;
}
