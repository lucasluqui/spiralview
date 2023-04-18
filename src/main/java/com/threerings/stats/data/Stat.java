package com.threerings.stats.data;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.U;
import com.samskivert.util.aq;
import com.threerings.presents.dobj.DSet;
import com.threerings.stats.a;
import java.io.Serializable;
import java.util.zip.CRC32;




























































public abstract class Stat
  implements DSet.Entry, Cloneable
{
  protected Type _type;
  protected boolean _modified;
  protected byte _modCount;

  public static Type dT(int paramInt)
  {
    return (Type)aXj.get(paramInt);
  }







  public static int a(Type paramType, Stat paramStat)
  {
    int i = dy(paramType.name());


    if (aXj.containsKey(i)) {
      a.log.f("Stat type collision! " + paramType.name() + " and " + ((Type)aXj.get(i)).name() + " both map to '" + i + "'.", new Object[0]);

      return -1;
    }


    paramStat._type = paramType;


    aXj.put(i, paramType);
    return i;
  }




  public static int dy(String paramString)
  {
    aXi.reset();
    aXi.update(paramString.getBytes());
    return (int)aXi.getValue();
  }



































  public String toString()
  {
    StringBuilder localStringBuilder;

































    (localStringBuilder = new StringBuilder(aq.ac(this._type.name()))).append("=").append(NQ());
    localStringBuilder.append("(").append(this._modCount).append(")");
    return localStringBuilder.toString();
  }






  public abstract String NQ();






  public final Stat NR()
  {
    try
    {
      return (Stat)super.clone();
    } catch (CloneNotSupportedException localCloneNotSupportedException) {
      throw new AssertionError(localCloneNotSupportedException);
    }
  }












  private static CRC32 aXi = new CRC32();


  private static HashIntMap<Type> aXj = new HashIntMap();

  public static abstract interface Type
    extends Serializable
  {
    public abstract String name();

    public abstract int code();
  }
}
