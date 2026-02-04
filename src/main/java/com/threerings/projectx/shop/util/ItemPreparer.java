package com.threerings.projectx.shop.util;

public enum ItemPreparer {
  BASIC,
  GREMLIN {
  },
  LOCKED {
  },
  LOCK_NON_STACKABLE {
  },
  DEPOT {
  },
  STATUE {
    public final int getMaxBuyItems() {
      return 1;
    }

    public final int getMaxBuyQuantity() {
      return 1;
    }
  };

  private ItemPreparer ()
  {

  }
}
