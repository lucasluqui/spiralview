package com.lucasluqui.silverweave.code;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.export.BinaryImporter;

/**
 * An isolated view of {@code projectx-pcode.jar}.
 *
 * <p>pcode bundles its own obfuscated build of clyde, which is not binary compatible with the
 * unobfuscated clyde spiralview is built against (mixing the two fails bytecode verification). Its
 * classes are therefore loaded in a dedicated class loader that never sees spiralview's classes, and
 * every member is located by signature. Only class names and exported field names are relied upon,
 * since the config file format depends on those and they survive re-obfuscation.
 */
public final class Code
  implements Closeable
{
  /**
   * Opens the pcode jar found on the application class path.
   */
  public static Code open ()
    throws IOException
  {
    String path = ITEM_CONFIG.replace('.', '/') + ".class";
    URL url = ClassLoader.getSystemResource(path);
    if (url == null) {
      throw new IOException("projectx-pcode.jar was not found on the class path.");
    }
    URL root;
    if ("jar".equals(url.getProtocol())) {
      root = ((JarURLConnection) url.openConnection()).getJarFileURL();
    } else {
      String str = url.toString();
      root = new URL(str.substring(0, str.length() - path.length()));
    }
    return new Code(root);
  }

  /**
   * Creates a view of the pcode classes at the given location.
   */
  public Code (URL location)
  {
    // the platform loader parent gives us the JDK without any of spiralview's classes
    _loader = new URLClassLoader("pcode", new URL[] { location }, ClassLoader.getPlatformClassLoader());

    Class<?> importer = type("com.threerings.export.BinaryImporter");
    _importerCtor = Signatures.constructor(importer, InputStream.class);
    _readObject = Signatures.method(importer, "readObject", Object.class);

    Class<?> exporter = type("com.threerings.export.BinaryExporter");
    _exporterCtor = Signatures.constructor(exporter, OutputStream.class);
    _writeObject = Signatures.method(exporter, "writeObject", void.class, Object.class);

    _argMap = type("com.threerings.config.ArgumentMap");
    _argMapCtor = Signatures.constructor(_argMap);

    _ref = type("com.threerings.config.ConfigReference");
    _refName = Signatures.field(_ref, String.class);
    _refArgs = Signatures.method(_ref, "getArguments", _argMap);

    _managed = type("com.threerings.config.ManagedConfig");
    _managedName = Signatures.field(_managed, String.class);

    _cfgmgr = type("com.threerings.config.ConfigManager");
    _getConfig = Signatures.method(_cfgmgr, "getConfig", _managed, Class.class, _ref);

    _group = type("com.threerings.config.ConfigGroup");
    _groupCtor = Signatures.constructor(_group, Class.class);
    _groupCfgmgr = Signatures.field(_group, _cfgmgr);
    _addConfig = Signatures.method(_group, "addConfig", void.class, _managed, boolean.class);
  }

  /**
   * Loads a pcode class by name.
   */
  public Class<?> type (String name)
  {
    try {
      return Class.forName(name, false, _loader);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Missing pcode class " + name, e);
    }
  }

  public Class<?> configManagerClass ()
  {
    return _cfgmgr;
  }

  public Class<?> argumentMapClass ()
  {
    return _argMap;
  }

  /**
   * Reads a config group file (such as {@code config/item.dat}) into an array of pcode configs.
   */
  public Object[] readConfigs (InputStream in)
    throws IOException
  {
    Object result;
    try {
      Object importer = _importerCtor.newInstance(in);
      try {
        result = _readObject.invoke(importer);
      } finally {
        ((Closeable) importer).close();
      }
    } catch (ReflectiveOperationException e) {
      throw new IOException("Failed to read configs.", unwrap(e));
    }
    if (result instanceof Object[]) {
      return (Object[]) result;
    }
    // groups containing derived configs are wrapped in a ConfigGroup.Data holding the array
    if (result != null) {
      for (Field field : result.getClass().getFields()) {
        if (field.getType().isArray() && _managed.isAssignableFrom(field.getType().getComponentType())) {
          try {
            return (Object[]) field.get(result);
          } catch (IllegalAccessException e) {
            throw new IOException("Failed to read configs.", e);
          }
        }
      }
    }
    throw new IOException("Unexpected config data: " + result);
  }

  public String nameOf (Object config)
  {
    return (String) get(_managedName, config);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> newArguments ()
  {
    try {
      return (Map<String, Object>) _argMapCtor.newInstance();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(unwrap(e));
    }
  }

  public boolean isReference (Object value)
  {
    return _ref.isInstance(value);
  }

  public String referenceName (Object ref)
  {
    return (String) get(_refName, ref);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> referenceArguments (Object ref)
  {
    return (Map<String, Object>) invoke(_refArgs, ref);
  }

  /**
   * Creates a fresh config space into which groups of configs can be added and resolved.
   */
  public ConfigSpace newConfigSpace ()
  {
    return new ConfigSpace();
  }

  /**
   * Converts a pcode value into the equivalent value for spiralview's own clyde classes.
   *
   * @throws IllegalArgumentException if the value cannot be converted.
   */
  public Object toTool (Object value)
  {
    if (value == null || value instanceof String || value instanceof Number ||
      value instanceof Boolean || value instanceof Character) {
      return value;
    }
    if (isReference(value)) {
      ConfigReference<ManagedConfig> ref = new ConfigReference<>(referenceName(value));
      for (Map.Entry<String, Object> entry : referenceArguments(value).entrySet()) {
        ref.getArguments().put(entry.getKey(), toTool(entry.getValue()));
      }
      return ref;
    }
    // anything else crosses over through the export format, which both sides share
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Object exporter = _exporterCtor.newInstance(out);
      try {
        _writeObject.invoke(exporter, value);
      } finally {
        ((Closeable)exporter).close();
      }
      try (BinaryImporter in = new BinaryImporter(new ByteArrayInputStream(out.toByteArray()))) {
        return in.readObject();
      }
    } catch (Exception | LinkageError e) {
      throw new IllegalArgumentException("Cannot convert " + value.getClass().getName(), unwrap(e));
    }
  }

  /**
   * Invokes a pcode method, unwrapping reflection exceptions.
   */
  public static Object invoke (Method method, Object target, Object... args)
  {
    try {
      return method.invoke(target, args);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw new IllegalStateException(cause);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads a field, wrapping access exceptions.
   */
  public static Object get (Field field, Object target)
  {
    try {
      return field.get(target);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void close ()
    throws IOException
  {
    _loader.close();
  }

  protected static Throwable unwrap (Throwable t)
  {
    return (t instanceof InvocationTargetException && t.getCause() != null) ? t.getCause() : t;
  }

  /**
   * A minimal pcode {@code ConfigManager} holding only the groups we add to it, which is enough for
   * pcode to resolve derived configs and parameter paths that cross config references.
   */
  public final class ConfigSpace
  {
    public Object manager ()
    {
      return _manager;
    }

    /**
     * Adds a group of configs of the given (pcode) class.
     */
    @SuppressWarnings("unchecked")
    public void addGroup (Class<?> cclass, Object[] configs)
    {
      try {
        Object group = _groupCtor.newInstance(cclass);
        _groupCfgmgr.set(group, _manager);
        for (Object config : configs) {
          invoke(_addConfig, group, config, false);
        }
        _groups.put(cclass, group);
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(unwrap(e));
      }
    }

    /**
     * Resolves a pcode config reference to the (possibly parameterized) config instance it
     * names, or null if it does not exist.
     */
    public Object resolve (Class<?> cclass, Object ref)
    {
      return ref == null ? null : invoke(_getConfig, _manager, cclass, ref);
    }

    @SuppressWarnings("unchecked")
    protected ConfigSpace ()
    {
      try {
        _manager = Signatures.constructor(_cfgmgr).newInstance();
        // the no-arg manager leaves its class tables unset; give it empty ones
        Map<String, Class<?>[]> classes = new HashMap<>();
        classes.put("resource", new Class<?>[0]);
        Signatures.field(_cfgmgr, HashMap.class, Signatures.typeArg(1, Class[].class))
          .set(_manager, classes);
        _groups = (Map<Class<?>, Object>)Signatures.field(
          _cfgmgr, HashMap.class, Signatures.typeArg(1, _group)).get(_manager);
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(unwrap(e));
      }
    }

    protected final Object _manager;
    protected final Map<Class<?>, Object> _groups;
  }

  protected final URLClassLoader _loader;

  protected final Constructor<?> _importerCtor, _exporterCtor, _argMapCtor, _groupCtor;
  protected final Method _readObject, _writeObject, _refArgs, _getConfig, _addConfig;
  protected final Field _refName, _managedName, _groupCfgmgr;
  protected final Class<?> _argMap, _ref, _managed, _cfgmgr, _group;

  /** The class used to locate pcode on the class path. */
  public static final String ITEM_CONFIG = "com.threerings.projectx.item.config.ItemConfig";
}
