package com.threerings.projectx.uplink.data;

import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.Map;

public enum SystemSender {
  SYSTEM,
  PVP,
  MAIL,
  MINING,
  SUPPORT,
  PROMO,
  ORIENTATION,
  AUCTION,
  SAXTON_HALE,
  MANN_CO,
  KORA,
  MISSION_REWARDS,
  LOTTERY,
  EXCHANGE;

  public final String name = " " + this.ordinal();
  public final int senderId = -1 * this.ordinal();
  //public static final Map<Integer, SystemSender> BY_ID = Maps.uniqueIndex(Arrays.asList(values()), new a());

  private SystemSender() {
  }

  public static boolean isSystemName(String var0) {
    return var0 != null && var0.startsWith(" ");
  }

  public static String displayName(String var0) {
    return "m.sender_" + var0.substring(1);
  }
}
