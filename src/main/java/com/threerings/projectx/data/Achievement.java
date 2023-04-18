package com.threerings.projectx.data;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.z;
import com.threerings.presents.dobj.SimpleEntry;
import com.threerings.projectx.a;
import com.threerings.projectx.item.data.ItemCodes;
import com.threerings.stats.data.Stat;
import com.threerings.util.N;
















public class Achievement
{
  protected long _unlocked;
  private static z<Type> auJ;

  public enum Type
  {
    REACHED_RESCUE_CAMP,
    REACHED_HAVEN,
    REACHED_TERMINAL_1,
    REACHED_SUBTOWN_1,
    EARNED_TIER_2,
    REACHED_SUBTOWN_2,
    EARNED_TIER_3,
    REACHED_CORE_TERMINAL,
    DEFEATED_ROYAL_JELLY,
    DEFEATED_VANADUKE,
    DEFEATED_SNARBOLAX,
    DEFEATED_ROARMULUS,
    CRAFTED_RARITY_1,
    CRAFTED_RARITY_2,
    CRAFTED_RARITY_3,
    CRAFTED_RARITY_4,
    CRAFTED_RARITY_5,
    ITEMS_CRAFTED_1,
    ITEMS_CRAFTED_2,
    ITEMS_CRAFTED_3,
    ITEMS_CRAFTED_4,
    SWORDS_OWNED_1,
    SWORDS_OWNED_2,
    SWORDS_OWNED_3,
    HANDGUNS_OWNED_1,
    HANDGUNS_OWNED_2,
    HANDGUNS_OWNED_3,
    BOMBS_OWNED_1,
    BOMBS_OWNED_2,
    BOMBS_OWNED_3,
    HELMS_OWNED_1,
    HELMS_OWNED_2,
    HELMS_OWNED_3,
    ARMORS_OWNED_1,
    ARMORS_OWNED_2,
    ARMORS_OWNED_3,
    SHIELDS_OWNED_1,
    SHIELDS_OWNED_2,
    SHIELDS_OWNED_3,
    RARITY_SET_1,
    LEVELLED_ITEM_1,
    MINERALS_DEPOSITED_1,
    MINERALS_DEPOSITED_2,
    MINERALS_DEPOSITED_3,
    MINERALS_DEPOSITED_4,
    MINERALS_DEPOSITED_5,
    SLOTS_FILLED_1,
    REVIVE_SELF,
    REVIVE_FRIEND,
    REVIVE_FRIEND_MINIMUM,
    HEALTH_CAPSULES_USED_1,
    REMEDY_CAPSULES_USED_1,
    MECHA_KNIGHT_ACTIVATED,
    REVIVELESS_COMPLETE_TIER_1,
    REVIVELESS_COMPLETE_TIER_2,
    REVIVELESS_COMPLETE_TIER_3,
    FULL_RUN,
    REVIVELESS_FULL_RUN,
    FIRECRACKER_BOMB_OWNED,
    LOCKDOWN_SNOWBALL,
    CASINO_SPIN_1,
    CASINO_WHAMMIE,
    CASINO_SPIN_2,
    CASINO_SPIN_3,
    CASINO_VIP,
    UNUSED;

    protected final int _code;

    public boolean qualifies(PlayerObject paramPlayerObject)
    {
      return false;
    }





    public boolean isHidden()
    {
      return (this == UNUSED) || (isObsolete());
    }











    public boolean isOnSteam()
    {
      return !isHidden();
    }




    public boolean isObsolete()
    {
      return true;
    }





    public int code()
    {
      return this._code;
    }




    public String key()
    {
      return "m.achievement_" + Integer.toHexString(this._code);
    }




    public String description()
    {
      String str = "d.achievement_" + Integer.toHexString(this._code);
      if (isObsolete()) return N.a("m.newline", new String[] { str, "m.achievement_obsolete" }); return str;
    }






    public String icon()
    {
      return "ui/icon/cheevos/" + Integer.toHexString(this._code) + ".png";
    }

    private Type()
    {
      this._code = Achievement.b(this);
    }
  }


















  public static int b(Type paramType)
  {
    return 1;
  }
































  public Achievement(Type paramType, long paramLong)
  {
    //super(paramType);
    this._unlocked = paramLong;
  }
}
