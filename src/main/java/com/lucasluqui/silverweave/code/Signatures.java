package com.lucasluqui.silverweave.code;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Reflection helpers that locate members by their signature rather than by name, so lookups keep
 * working when pcode is re-obfuscated and its member names shuffle around.
 */
public final class Signatures
{
  /**
   * Finds the public instance method of {@code owner} (or its supertypes) with exactly the given
   * return and parameter types.
   *
   * @param nameHint the name to prefer if more than one method matches; may be null.
   * @throws IllegalStateException if no method matches or the match is ambiguous.
   */
  public static Method method (Class<?> owner, String nameHint, Class<?> ret, Class<?>... params)
  {
    // collect by name so that overrides of the same method collapse into one candidate
    Map<String, Method> candidates = new LinkedHashMap<>();
    for (Method m : owner.getMethods()) {
      if (Modifier.isStatic(m.getModifiers()) || m.isBridge() || m.isSynthetic() ||
        m.getDeclaringClass() == Object.class || OBJECT_METHODS.contains(m.getName())) {
        continue;
      }
      if (m.getReturnType() == ret && Arrays.equals(m.getParameterTypes(), params)) {
        candidates.putIfAbsent(m.getName(), m);
      }
    }
    if (nameHint != null && candidates.containsKey(nameHint)) {
      return candidates.get(nameHint);
    }
    if (candidates.size() == 1) {
      return candidates.values().iterator().next();
    }
    throw new IllegalStateException((candidates.isEmpty() ? "No" : "Ambiguous") +
      " method " + describe(owner, ret, params) + (candidates.isEmpty() ? "" : ": " + candidates.keySet()));
  }

  /**
   * Finds a public constructor with exactly the given parameter types.
   */
  public static <T> Constructor<T> constructor (Class<T> owner, Class<?>... params)
  {
    try {
      return owner.getConstructor(params);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("No constructor " + describe(owner, null, params), e);
    }
  }

  /**
   * Finds the single non-static field declared on {@code owner} or its superclasses whose raw type
   * is {@code type} and whose generic type satisfies {@code generic}.
   */
  public static Field field (Class<?> owner, Class<?> type, Predicate<Type> generic)
  {
    List<Field> matches = new ArrayList<>();
    for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field f : c.getDeclaredFields()) {
        if (!Modifier.isStatic(f.getModifiers()) && f.getType() == type &&
          generic.test(f.getGenericType())) {
          matches.add(f);
        }
      }
    }
    if (matches.size() != 1) {
      throw new IllegalStateException((matches.isEmpty() ? "No" : "Ambiguous") + " field of type " +
        type.getSimpleName() + " in " + owner.getName() + (matches.isEmpty() ? "" : ": " + matches));
    }
    Field field = matches.get(0);
    field.setAccessible(true);
    return field;
  }

  /**
   * Finds a field by type alone.
   */
  public static Field field (Class<?> owner, Class<?> type)
  {
    return field(owner, type, t -> true);
  }

  /**
   * Returns a predicate matching a parameterized type whose type argument at {@code index} has the
   * given raw class (or, for generic arrays, the given component raw class wrapped in an array).
   */
  public static Predicate<Type> typeArg (int index, Class<?> raw)
  {
    return type -> {
      if (!(type instanceof ParameterizedType)) {
        return false;
      }
      Type[] args = ((ParameterizedType) type).getActualTypeArguments();
      return index < args.length && rawClass(args[index]) == raw;
    };
  }

  /**
   * Returns the raw class of a (possibly generic) type, or null if it cannot be determined.
   */
  public static Class<?> rawClass (Type type)
  {
    if (type instanceof Class) {
      return (Class<?>) type;
    } else if (type instanceof ParameterizedType) {
      return rawClass(((ParameterizedType) type).getRawType());
    } else if (type instanceof GenericArrayType) {
      Class<?> component = rawClass(((GenericArrayType) type).getGenericComponentType());
      return component == null ? null : java.lang.reflect.Array.newInstance(component, 0).getClass();
    }
    return null;
  }

  protected static String describe (Class<?> owner, Class<?> ret, Class<?>[] params)
  {
    StringBuilder buf = new StringBuilder();
    if (ret != null) {
      buf.append(ret.getSimpleName()).append(' ');
    }
    buf.append(owner.getName()).append('(');
    for (int ii = 0; ii < params.length; ii++) {
      buf.append(ii == 0 ? "" : ", ").append(params[ii].getSimpleName());
    }
    return buf.append(')').toString();
  }

  /** Method names declared by Object, which are never the obfuscated method we're after. */
  protected static final List<String> OBJECT_METHODS = Arrays.asList(
    "toString", "hashCode", "equals", "clone", "getClass", "notify", "notifyAll", "wait", "finalize");

  private Signatures () { }
}
