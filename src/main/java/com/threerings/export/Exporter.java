package com.threerings.export;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.threerings.config.ArgumentMap;
import com.threerings.math.Matrix3f;
import com.threerings.math.Matrix4f;
import com.threerings.math.Quaternion;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;
import com.threerings.opengl.renderer.Color4f;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class Exporter implements Closeable {
  protected Object _object;
  protected ObjectMarshaller _marshaller;
  protected Replacer _replacer;
  protected Object[] _a1 = new Object[1];
  protected Object[] _a2 = new Object[1];

  public Exporter() {
  }

  public Exporter setReplacer(Replacer replacer) {
    this._replacer = replacer;
    return this;
  }

  public abstract void writeObject(Object var1) throws IOException;

  public void defaultWriteFields() throws IOException {
    if (this._marshaller == null) {
      throw new IllegalStateException("Not invoking a custom writeFields method.");
    } else {
      this._marshaller.writeFields(this._object, this, false);
    }
  }

  public final void kz() throws IOException {
    defaultWriteFields();
  }

  public void write(String name, boolean value, boolean defvalue) throws IOException {
    if (value != defvalue) {
      this.write(name, value);
    }

  }

  public void a(String name, boolean value, boolean defValue) throws IOException {
    write(name, value, defValue);
  }

  public abstract void write(String var1, boolean var2) throws IOException;
  public abstract void a(String var1, boolean var2) throws IOException;

  public void write(String name, byte value, byte defvalue) throws IOException {
    if (value != defvalue) {
      this.write(name, value);
    }

  }

  public void a(String name, byte value, byte defValue) throws IOException {
    write(name, value, defValue);
  }

  public abstract void write(String var1, byte var2) throws IOException;
  public abstract void a(String var1, byte var2) throws IOException;

  public void write(String name, char value, char defvalue) throws IOException {
    if (value != defvalue) {
      this.write(name, value);
    }

  }

  public void a(String name, char value, char defValue) throws IOException {
    write(name, value, defValue);
  }

  public abstract void write(String var1, char var2) throws IOException;
  public abstract void a(String var1, char var2) throws IOException;

  public void write(String name, double value, double defvalue) throws IOException {
    if (value != defvalue) {
      this.write(name, value);
    }

  }

  public void a(String name, double value, double defValue) throws IOException {
    write(name, value, defValue);
  }

  public abstract void write(String var1, double var2) throws IOException;
  public abstract void a(String var1, double var2) throws IOException;

  public void write(String name, float value, float defvalue) throws IOException {
    if (value != defvalue) {
      this.write(name, value);
    }

  }

  public void a(String name, float value, float defValue) throws IOException {
    write(name, value, defValue);
  }

  public abstract void write(String var1, float var2) throws IOException;
  public abstract void a(String var1, float var2) throws IOException;

  public void write(String name, int value, int defvalue) throws IOException {
    if (value != defvalue) {
      this.write(name, value);
    }

  }

  public void a(String name, int value, int defValue) throws IOException {
    write(name, value, defValue);
  }

  public abstract void write(String var1, int var2) throws IOException;
  public abstract void a(String var1, int var2) throws IOException;

  public void write(String name, long value, long defvalue) throws IOException {
    if (value != defvalue) {
      this.write(name, value);
    }

  }

  public void a(String name, long value, long defValue) throws IOException {
    write(name, value, defValue);
  }

  public abstract void write(String var1, long var2) throws IOException;
  public abstract void a(String var1, long var2) throws IOException;

  public void write(String name, short value, short defvalue) throws IOException {
    if (value != defvalue) {
      this.write(name, value);
    }

  }

  public void a(String name, short value, short defValue) throws IOException {
    write(name, value, defValue);
  }

  public abstract void write(String var1, short var2) throws IOException;
  public abstract void a(String var1, short var2) throws IOException;

  public void write(String name, boolean[] value, boolean[] defvalue) throws IOException {
    this.write(name, value, defvalue, boolean[].class);
  }

  public void a(String name, boolean[] value, boolean[] defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, boolean[] value) throws IOException {
    this.write(name, (Object)value, (Class)boolean[].class);
  }

  public void a(String name, boolean[] value) throws IOException {
    write(name, value);
  }

  public void write(String name, byte[] value, byte[] defvalue) throws IOException {
    this.write(name, value, defvalue, byte[].class);
  }

  public void a(String name, byte[] value, byte[] defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, byte[] value) throws IOException {
    this.write(name, (Object)value, (Class)byte[].class);
  }

  public void a(String name, byte[] value) throws IOException {
    write(name, value);
  }

  public void write(String name, char[] value, char[] defvalue) throws IOException {
    this.write(name, value, defvalue, char[].class);
  }

  public void a(String name, char[] value, char[] defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, char[] value) throws IOException {
    this.write(name, (Object)value, (Class)char[].class);
  }

  public void a(String name, char[] value) throws IOException {
    write(name, value);
  }

  public void write(String name, double[] value, double[] defvalue) throws IOException {
    this.write(name, value, defvalue, double[].class);
  }

  public void a(String name, double[] value, double[] defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, double[] value) throws IOException {
    this.write(name, (Object)value, (Class)double[].class);
  }

  public void a(String name, double[] value) throws IOException {
    write(name, value);
  }

  public void write(String name, float[] value, float[] defvalue) throws IOException {
    this.write(name, value, defvalue, float[].class);
  }

  public void a(String name, float[] value, float[] defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, float[] value) throws IOException {
    this.write(name, (Object)value, (Class)float[].class);
  }

  public void a(String name, float[] value) throws IOException {
    write(name, value);
  }

  public void write(String name, int[] value, int[] defvalue) throws IOException {
    this.write(name, value, defvalue, int[].class);
  }

  public void a(String name, int[] value, int[] defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, int[] value) throws IOException {
    this.write(name, (Object)value, (Class)int[].class);
  }

  public void a(String name, int[] value) throws IOException {
    write(name, value);
  }

  public void write(String name, long[] value, long[] defvalue) throws IOException {
    this.write(name, value, defvalue, long[].class);
  }

  public void a(String name, long[] value, long[] defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, long[] value) throws IOException {
    this.write(name, (Object)value, (Class)long[].class);
  }

  public void a(String name, long[] value) throws IOException {
    write(name, value);
  }

  public void write(String name, short[] value, short[] defvalue) throws IOException {
    this.write(name, value, defvalue, short[].class);
  }

  public void a(String name, short[] value, short[] defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, short[] value) throws IOException {
    this.write(name, (Object)value, (Class)short[].class);
  }

  public void a(String name, short[] value) throws IOException {
    write(name, value);
  }

  public void write(String name, String value, String defvalue) throws IOException {
    this.write(name, value, defvalue, String.class);
  }

  public void a(String name, String value, String defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, String value) throws IOException {
    this.write(name, (Object)value, (Class)String.class);
  }

  public void a(String name, String value) throws IOException {
    write(name, value);
  }

  public void write(String name, ByteBuffer value, ByteBuffer defvalue) throws IOException {
    this.write(name, value, defvalue, ByteBuffer.class);
  }

  public void a(String name, ByteBuffer value, ByteBuffer defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, ByteBuffer value) throws IOException {
    this.write(name, (Object)value, (Class)ByteBuffer.class);
  }

  public void a(String name, ByteBuffer value) throws IOException {
    write(name, value);
  }

  public void write(String name, CharBuffer value, CharBuffer defvalue) throws IOException {
    this.write(name, value, defvalue, CharBuffer.class);
  }

  public void a(String name, CharBuffer value, CharBuffer defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, CharBuffer value) throws IOException {
    this.write(name, (Object)value, (Class)CharBuffer.class);
  }

  public void a(String name, CharBuffer value) throws IOException {
    write(name, value);
  }

  public void write(String name, DoubleBuffer value, DoubleBuffer defvalue) throws IOException {
    this.write(name, value, defvalue, DoubleBuffer.class);
  }

  public void a(String name, DoubleBuffer value, DoubleBuffer defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, DoubleBuffer value) throws IOException {
    this.write(name, (Object)value, (Class)DoubleBuffer.class);
  }

  public void a(String name, DoubleBuffer value) throws IOException {
    write(name, value);
  }

  public void write(String name, FloatBuffer value, FloatBuffer defvalue) throws IOException {
    this.write(name, value, defvalue, FloatBuffer.class);
  }

  public void a(String name, FloatBuffer value, FloatBuffer defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, FloatBuffer value) throws IOException {
    this.write(name, (Object)value, (Class)FloatBuffer.class);
  }

  public void a(String name, FloatBuffer value) throws IOException {
    write(name, value);
  }

  public void write(String name, IntBuffer value, IntBuffer defvalue) throws IOException {
    this.write(name, value, defvalue, IntBuffer.class);
  }

  public void a(String name, IntBuffer value, IntBuffer defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, IntBuffer value) throws IOException {
    this.write(name, (Object)value, (Class)IntBuffer.class);
  }

  public void a(String name, IntBuffer value) throws IOException {
    write(name, value);
  }

  public void write(String name, LongBuffer value, LongBuffer defvalue) throws IOException {
    this.write(name, value, defvalue, LongBuffer.class);
  }

  public void a(String name, LongBuffer value, LongBuffer defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, LongBuffer value) throws IOException {
    this.write(name, (Object)value, (Class)LongBuffer.class);
  }

  public void a(String name, LongBuffer value) throws IOException {
    write(name, value);
  }

  public void write(String name, ShortBuffer value, ShortBuffer defvalue) throws IOException {
    this.write(name, value, defvalue, ShortBuffer.class);
  }

  public void a(String name, ShortBuffer value, ShortBuffer defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, ShortBuffer value) throws IOException {
    this.write(name, (Object)value, (Class)ShortBuffer.class);
  }

  public void a(String name, ShortBuffer value) throws IOException {
    write(name, value);
  }

  public void write(String name, Color4f value, Color4f defvalue) throws IOException {
    this.write(name, value, defvalue, Color4f.class);
  }

  public void a(String name, Color4f value, Color4f defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, Color4f value) throws IOException {
    this.write(name, (Object)value, (Class)Color4f.class);
  }

  public void a(String name, Color4f value) throws IOException {
    write(name, value);
  }

  public void write(String name, Matrix3f value, Matrix3f defvalue) throws IOException {
    this.write(name, value, defvalue, Matrix3f.class);
  }

  public void a(String name, Matrix3f value, Matrix3f defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, Matrix3f value) throws IOException {
    this.write(name, (Object)value, (Class)Matrix3f.class);
  }

  public void a(String name, Matrix3f value) throws IOException {
    write(name, value);
  }

  public void write(String name, Matrix4f value, Matrix4f defvalue) throws IOException {
    this.write(name, value, defvalue, Matrix4f.class);
  }

  public void a(String name, Matrix4f value, Matrix4f defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, Matrix4f value) throws IOException {
    this.write(name, (Object)value, (Class)Matrix4f.class);
  }

  public void a(String name, Matrix4f value) throws IOException {
    write(name, value);
  }

  public void write(String name, Quaternion value, Quaternion defvalue) throws IOException {
    this.write(name, value, defvalue, Quaternion.class);
  }

  public void a(String name, Quaternion value, Quaternion defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, Quaternion value) throws IOException {
    this.write(name, (Object)value, (Class)Quaternion.class);
  }

  public void a(String name, Quaternion value) throws IOException {
    write(name, value);
  }

  public void write(String name, Vector2f value, Vector2f defvalue) throws IOException {
    this.write(name, value, defvalue, Vector2f.class);
  }

  public void a(String name, Vector2f value, Vector2f defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, Vector2f value) throws IOException {
    this.write(name, (Object)value, (Class)Vector2f.class);
  }

  public void a(String name, Vector2f value) throws IOException {
    write(name, value);
  }

  public void write(String name, Vector3f value, Vector3f defvalue) throws IOException {
    this.write(name, value, defvalue, Vector3f.class);
  }

  public void a(String name, Vector3f value, Vector3f defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, Vector3f value) throws IOException {
    this.write(name, (Object)value, (Class)Vector3f.class);
  }

  public void a(String name, Vector3f value) throws IOException {
    write(name, value);
  }

  public void write(String name, Exportable value, Exportable defvalue) throws IOException {
    this.write(name, value, defvalue, Exportable.class);
  }

  public void a(String name, Exportable value, Exportable defValue) throws IOException {
    write(name, value, defValue);
  }

  public void write(String name, Exportable value) throws IOException {
    this.write(name, (Object)value, (Class)Exportable.class);
  }

  public void a(String name, Exportable value) throws IOException {
    write(name, value);
  }

  public <T> void write(String name, T value, T defvalue, Class<T> clazz) throws IOException {
    this._a1[0] = value;
    this._a2[0] = defvalue;
    if (!Arrays.deepEquals(this._a1, this._a2)) {
      this.write(name, value, clazz);
    }

  }

  public <T> void a(String name, T value, T defvalue, Class<T> clazz) throws IOException {
    write(name, value, defvalue, clazz);
  }

  public abstract <T> void write(String var1, T var2, Class<T> var3) throws IOException;
  public abstract <T> void a(String var1, T var2, Class<T> var3) throws IOException;

  public abstract void close() throws IOException;

  public abstract void finish() throws IOException;

  protected void writeFields(Exportable object) throws IOException {
    Object oobject = this._object;
    ObjectMarshaller omarshaller = this._marshaller;

    try {
      this._object = object;
      this._marshaller = ObjectMarshaller.getObjectMarshaller(object.getClass());
      this._marshaller.writeFields(this._object, this, true);
    } finally {
      this._object = oobject;
      this._marshaller = omarshaller;
    }

  }

  protected static Class<?> getClass(Object value) {
    if (value instanceof Enum) {
      return ((Enum)value).getDeclaringClass();
    } else if (value instanceof Exportable) {
      return value.getClass();
    } else if (value instanceof Collection) {
      if (value instanceof List) {
        return value instanceof ImmutableList ? ImmutableList.class : ArrayList.class;
      } else if (value instanceof Set) {
        return value instanceof ImmutableSet ? ImmutableSet.class : (value instanceof EnumSet ? EnumSet.class : HashSet.class);
      } else if (value instanceof Multiset) {
        return value instanceof ImmutableMultiset ? ImmutableMultiset.class : HashMultiset.class;
      } else {
        return ArrayList.class;
      }
    } else if (value instanceof Map) {
      return value instanceof ImmutableMap ? ImmutableMap.class : (value instanceof ArgumentMap ? ArgumentMap.class : (value instanceof TreeMap ? TreeMap.class : HashMap.class));
    } else {
      if (value instanceof Buffer) {
        if (value instanceof ByteBuffer) {
          return ByteBuffer.class;
        }

        if (value instanceof CharBuffer) {
          return CharBuffer.class;
        }

        if (value instanceof DoubleBuffer) {
          return DoubleBuffer.class;
        }

        if (value instanceof FloatBuffer) {
          return FloatBuffer.class;
        }

        if (value instanceof IntBuffer) {
          return IntBuffer.class;
        }

        if (value instanceof LongBuffer) {
          return LongBuffer.class;
        }

        if (value instanceof ShortBuffer) {
          return ShortBuffer.class;
        }
      } else if (value instanceof File) {
        return File.class;
      }

      return value.getClass();
    }
  }

  protected static enum DummyEnum {
    DummyEnum() {
    }
  }

  public static class Replacement {
    public final Object value;
    public final Class<?> clazz;

    public Replacement(Object value, Class<?> clazz) {
      this.value = value;
      this.clazz = clazz;
    }
  }

  public interface Replacer {
    Replacement getReplacement(Object var1, Class<?> var2);
  }
}
