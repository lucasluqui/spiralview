// set method b to return 0 instead of all the gibberish.
// comment super() call in Achievement constructor.
// set all qualifies to false.

package com.threerings.projectx.data;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.z;
import com.threerings.presents.dobj.SimpleEntry;
import com.threerings.projectx.a;
import com.threerings.projectx.item.data.ItemCodes;
import com.threerings.stats.data.Stat;
import com.threerings.util.N;

public class Achievement {
  protected long _unlocked;
  private static z<Type> awf;

  public static int b(Type var0) {
    return 0;
  }

  public Achievement(Type var1, long var2) {
    //super(var1);
    this._unlocked = var2;
  }

  public static enum Type {
    REACHED_RESCUE_CAMP {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    REACHED_HAVEN,
    REACHED_TERMINAL_1,
    REACHED_SUBTOWN_1,
    EARNED_TIER_2 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    REACHED_SUBTOWN_2,
    EARNED_TIER_3 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
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
    ITEMS_CRAFTED_1 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    ITEMS_CRAFTED_2 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    ITEMS_CRAFTED_3 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    ITEMS_CRAFTED_4 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    SWORDS_OWNED_1 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    SWORDS_OWNED_2 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    SWORDS_OWNED_3 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    HANDGUNS_OWNED_1 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    HANDGUNS_OWNED_2 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    HANDGUNS_OWNED_3 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    BOMBS_OWNED_1 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    BOMBS_OWNED_2 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    BOMBS_OWNED_3 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    HELMS_OWNED_1 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    HELMS_OWNED_2 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    HELMS_OWNED_3 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    ARMORS_OWNED_1 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    ARMORS_OWNED_2 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    ARMORS_OWNED_3 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    SHIELDS_OWNED_1 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    SHIELDS_OWNED_2 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    SHIELDS_OWNED_3 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    RARITY_SET_1 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    LEVELLED_ITEM_1,
    @Deprecated
    MINERALS_DEPOSITED_1,
    @Deprecated
    MINERALS_DEPOSITED_2,
    @Deprecated
    MINERALS_DEPOSITED_3,
    @Deprecated
    MINERALS_DEPOSITED_4,
    @Deprecated
    MINERALS_DEPOSITED_5,
    SLOTS_FILLED_1 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    REVIVE_SELF,
    REVIVE_FRIEND,
    @Deprecated
    REVIVE_FRIEND_MINIMUM,
    HEALTH_CAPSULES_USED_1 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    REMEDY_CAPSULES_USED_1 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }
    },
    MECHA_KNIGHT_ACTIVATED,
    REVIVELESS_COMPLETE_TIER_1,
    REVIVELESS_COMPLETE_TIER_2,
    REVIVELESS_COMPLETE_TIER_3,
    FULL_RUN,
    REVIVELESS_FULL_RUN,
    FIRECRACKER_BOMB_OWNED,
    LOCKDOWN_SNOWBALL,
    CASINO_SPIN_1 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }

      public final boolean isHidden() {
        return true;
      }
    },
    CASINO_WHAMMIE {
      public final boolean isHidden() {
        return true;
      }
    },
    CASINO_SPIN_2 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }

      public final boolean isHidden() {
        return true;
      }
    },
    CASINO_SPIN_3 {
      public final boolean qualifies(PlayerObject var1) {
        return false;
      }

      public final boolean isHidden() {
        return true;
      }
    },
    CASINO_VIP {
      public final boolean isHidden() {
        return true;
      }
    },
    UNUSED;

    protected final int _code;

    public boolean qualifies(PlayerObject var1) {
      return false;
    }

    public boolean isHidden() {
      return this == UNUSED || this.isObsolete();
    }

    public boolean isOnSteam() {
      return !this.isHidden();
    }

    public boolean isObsolete() {
      switch (this) {
        case MINERALS_DEPOSITED_1:
        case MINERALS_DEPOSITED_2:
        case MINERALS_DEPOSITED_3:
        case MINERALS_DEPOSITED_4:
        case MINERALS_DEPOSITED_5:
        case REVIVE_FRIEND_MINIMUM:
          return true;
        default:
          return false;
      }
    }

    public int code() {
      return this._code;
    }

    public String key() {
      return "m.achievement_" + Integer.toHexString(this._code);
    }

    public String description() {
      String var1 = "d.achievement_" + Integer.toHexString(this._code);
      return this.isObsolete() ? N.c("m.newline", new String[]{var1, "m.achievement_obsolete"}) : var1;
    }

    public String icon() {
      return "ui/icon/cheevos/" + Integer.toHexString(this._code) + ".png";
    }

    private Type() {
      this._code = Achievement.b(this);
    }
  }
}
