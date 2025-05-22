package com.threerings.stats.data;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.ar;
import com.threerings.presents.dobj.DSet;
import com.threerings.stats.a;
import java.io.Serializable;
import java.util.zip.CRC32;

public abstract class Stat implements DSet.Entry, Cloneable {
  protected Type _type;
  protected boolean _modified;
  protected byte _modCount;
  private static CRC32 aXG = new CRC32();
  private static HashIntMap<Type> aXH = new HashIntMap();

  public static Type dR(int var0) {
    return (Type)aXH.get(var0);
  }

  public static int a(Type var0, Stat var1) {
    int var2 = du(var0.name());
    if (aXH.containsKey(var2)) {
      a.log.f("Stat type collision! " + var0.name() + " and " + ((Type)aXH.get(var2)).name() + " both map to '" + var2 + "'.", new Object[0]);
      return -1;
    } else {
      var1._type = var0;
      aXH.put(var2, var0);
      return var2;
    }
  }

  public static int du(String var0) {
    aXG.reset();
    aXG.update(var0.getBytes());
    return (int)aXG.getValue();
  }

  public String toString() {
    StringBuilder var1;
    (var1 = new StringBuilder(ar.af(this._type.name()))).append("=").append(this.Ml());
    var1.append("(").append(this._modCount).append(")");
    return var1.toString();
  }

  public abstract String Ml();

  public final Stat Mm() {
    try {
      return (Stat)super.clone();
    } catch (CloneNotSupportedException var2) {
      throw new AssertionError(var2);
    }
  }

  public interface Type extends Serializable {
    String name();

    int code();
  }
}
