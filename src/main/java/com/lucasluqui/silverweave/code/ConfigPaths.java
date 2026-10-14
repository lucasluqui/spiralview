package com.lucasluqui.silverweave.code;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.lucasluqui.silverweave.Log.log;

/**
 * Applies config parameter arguments along their paths (such as
 * {@code implementation.model["Red"].colorization}) on pcode configs.
 *
 * <p>pcode applies arguments itself, but paths that reach into the arguments of a config reference
 * need the referenced config to be loaded, which our minimal config space doesn't do, so such
 * arguments would silently be dropped. This walks those paths directly instead.
 */
public final class ConfigPaths
{
  /**
   * Applies the given arguments to a parameterized pcode config wherever a parameter's path goes
   * through a config reference's arguments.
   */
  public static void applyNested (Code code, Object config, Map<String, Object> args)
  {
    applyNested(code, config, config, args);
  }

  /**
   * Applies the given arguments for another config's parameters to a config instance, as for the
   * configs an instance was derived through (whose parameters address the same fields).
   *
   * @param parameterized the config whose parameters the arguments are for.
   */
  public static void applyNested (Code code, Object config, Object parameterized,
    Map<String, Object> args)
  {
    if (config == null || parameterized == null || args == null || args.isEmpty()) {
      return;
    }
    Object[] params = (Object[]) fieldValue(parameterized, "parameters");
    if (params == null) {
      return;
    }
    for (Object param : params) {
      String name = (String) fieldValue(param, "name");
      if (name == null || !args.containsKey(name)) {
        continue;
      }
      Object paths = fieldValue(param, "paths");
      if (paths instanceof String[]) {
        applyPaths(code, config, name, (String[]) paths, args.get(name));
      } else if (fieldValue(param, "options") instanceof Object[]) {
        applyChoice(code, config, param, name, args.get(name));
      }
    }
  }

  /**
   * Applies the option of a choice parameter that the argument names: each of the option's values
   * goes along the paths of the direct parameter it's for, as pcode does with the paths it can.
   */
  protected static void applyChoice (Code code, Object config, Object param, String name,
    Object choice)
  {
    Map<String, Object> values = null;
    for (Object option : (Object[]) fieldValue(param, "options")) {
      if (option != null && choice != null && choice.equals(fieldValue(option, "name"))) {
        values = optionArguments(option);
        break;
      }
    }
    Object directs = fieldValue(param, "directs");
    if (values == null || !(directs instanceof Object[])) {
      return;
    }
    for (Object direct : (Object[]) directs) {
      String dname = (String) fieldValue(direct, "name");
      Object paths = fieldValue(direct, "paths");
      if (dname != null && paths instanceof String[] && values.containsKey(dname)) {
        applyPaths(code, config, name + "/" + dname, (String[]) paths, values.get(dname));
      }
    }
  }

  /**
   * Returns the values a choice parameter's option sets, held in the option's only map field, or
   * null if they can't be read.
   */
  @SuppressWarnings("unchecked")
  protected static Map<String, Object> optionArguments (Object option)
  {
    for (Field field : option.getClass().getDeclaredFields()) {
      if (Map.class.isAssignableFrom(field.getType())) {
        try {
          field.setAccessible(true);
          return (Map<String, Object>) field.get(option);
        } catch (ReflectiveOperationException | RuntimeException e) {
          log.debug("Couldn't read choice option.", "error", e);
          return null;
        }
      }
    }
    return null;
  }

  /**
   * Sets a value along those of the given paths that go through a config reference's arguments,
   * the ones pcode leaves out.
   */
  protected static void applyPaths (Code code, Object config, String name, String[] paths,
    Object value)
  {
    for (String path : paths) {
      if (path.contains("[\"")) {
        try {
          set(code, config, parse(path), value);
        } catch (RuntimeException e) {
          log.debug("Couldn't apply nested argument.", "param", name, "path", path, "error", e);
        }
      }
    }
  }

  /**
   * Splits a path into field names, {@link Key}s and {@link Integer} indices.
   */
  protected static List<Object> parse (String path)
  {
    List<Object> steps = new ArrayList<>();
    int ii = 0, nn = path.length();
    while (ii < nn) {
      char c = path.charAt(ii);
      if (c == '.' || Character.isWhitespace(c)) {
        ii++;
      } else if (c == '[') {
        int end = path.indexOf(']', ii);
        if (path.charAt(ii + 1) == '"') {
          int close = path.indexOf('"', ii + 2);
          steps.add(new Key(path.substring(ii + 2, close)));
          end = path.indexOf(']', close);
        } else {
          steps.add(Integer.valueOf(path.substring(ii + 1, end).trim()));
        }
        ii = end + 1;
      } else {
        int end = ii;
        while (end < nn && path.charAt(end) != '.' && path.charAt(end) != '[') {
          end++;
        }
        steps.add(path.substring(ii, end).trim());
        ii = end;
      }
    }
    return steps;
  }

  protected static void set (Code code, Object root, List<Object> steps, Object value)
  {
    Object target = root;
    for (int ii = 0; ii < steps.size() - 1; ii++) {
      target = step(code, target, steps.get(ii));
      if (target == null) {
        return;
      }
    }
    Object last = steps.get(steps.size() - 1);
    if (last instanceof Key) {
      arguments(code, target).put(((Key) last).name, value);
    } else if (last instanceof Integer) {
      Array.set(target, (Integer)last, value);
    } else {
      Field field = field(target.getClass(), (String) last);
      try {
        field.set(target, coerce(value, field.getType()));
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  protected static Object step (Code code, Object target, Object step)
  {
    if (step instanceof Key) {
      return arguments(code, target).get(((Key) step).name);
    } else if (step instanceof Integer) {
      int index = (Integer) step;
      if (target instanceof List) {
        return ((List<?>)target).get(index);
      }
      return Array.get(target, index);
    }
    try {
      return field(target.getClass(), (String) step).get(target);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  protected static Map<String, Object> arguments (Code code, Object target)
  {
    if (code.isReference(target)) {
      return code.referenceArguments(target);
    } else if (target instanceof Map) {
      return (Map<String, Object>)target;
    }
    throw new IllegalArgumentException("Not a reference or map: " + target);
  }

  /**
   * Finds the public field for an editor property name ("step_effect" is field "stepEffect").
   */
  protected static Field field (Class<?> clazz, String property)
  {
    StringBuilder name = new StringBuilder();
    boolean upper = false;
    for (char c : property.toCharArray()) {
      if (c == '_') {
        upper = true;
      } else {
        name.append(upper ? Character.toUpperCase(c) : c);
        upper = false;
      }
    }
    try {
      return clazz.getField(name.toString());
    } catch (NoSuchFieldException e) {
      throw new IllegalArgumentException("No field " + name + " in " + clazz.getName());
    }
  }

  protected static Object coerce (Object value, Class<?> type)
  {
    if (value instanceof Number) {
      Number number = (Number)value;
      if (type == int.class || type == Integer.class) {
        return number.intValue();
      } else if (type == float.class || type == Float.class) {
        return number.floatValue();
      } else if (type == double.class || type == Double.class) {
        return number.doubleValue();
      } else if (type == long.class || type == Long.class) {
        return number.longValue();
      } else if (type == short.class || type == Short.class) {
        return number.shortValue();
      } else if (type == byte.class || type == Byte.class) {
        return number.byteValue();
      }
    }
    return value;
  }

  protected static Object fieldValue (Object target, String name)
  {
    try {
      return target.getClass().getField(name).get(target);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  /** A map key step, such as {@code ["Red"]}. */
  protected static final class Key
  {
    public final String name;

    public Key (String name)
    {
      this.name = name;
    }
  }

  private ConfigPaths () { }
}
