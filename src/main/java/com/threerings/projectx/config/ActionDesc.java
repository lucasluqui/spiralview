package com.threerings.projectx.config;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.io.Streamable;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.util.DeepObject;

@EditorTypes({
  ActionDesc.Lift.class,
  ActionDesc.Interact.class,
  ActionDesc.Talk.class,
  ActionDesc.Zone.class,
  ActionDesc.PointOfInterest.class,
  ActionDesc.Unlock.class,
  ActionDesc.UnlockGold.class,
  ActionDesc.Shop.class,
  ActionDesc.Resurrect.class,
  ActionDesc.Energy.class
})
public abstract class ActionDesc extends DeepObject implements Exportable, Streamable {
  public static final Lift LIFT = new Lift();
  public static final Interact INTERACT = new Interact();
  public static final Talk TALK = new Talk();
  public static final Zone ZONE = new Zone();
  public static final PointOfInterest POI = new PointOfInterest();
  public static final Unlock UNLOCK = new Unlock();
  public static final UnlockGold UNLOCK_GOLD = new UnlockGold();
  public static final Shop SHOP = new Shop();
  public static final Resurrect RESURRECT = new Resurrect();

  public ActionDesc() {
  }

  public int getActivity(ConfigManager cfgmgr) {
    return 2;
  }

  public int getActivityLength() {
    return 670;
  }

  public abstract String getHelpTip();

  public abstract ConfigReference<ModelConfig> getEffect(boolean var1);

  protected static ConfigReference<ModelConfig> createEffect(boolean occupied, String icon) {
    return new ConfigReference(occupied ? "ui/icon/interact_occupied.dat" : "ui/icon/interact.dat", "Texture", "ui/icon/context/" + icon + ".png", new Object[0]);
  }

  protected static int getEmoteActivity(ConfigManager cfgmgr, String emote) {
    EmoteConfig config = (EmoteConfig) cfgmgr.getConfig(EmoteConfig.class, emote);


    return config == null ? 2 : 9 + config.getAnimatedIndex(cfgmgr);
  }

  public static class ConfirmEnergy extends ActionDesc {
    public int amount;

    public ConfirmEnergy(int amount) {
      this.amount = amount;
    }

    public ConfirmEnergy() {
    }

    public String getHelpTip() {
      return "m.confirm_energy";
    }

    public ConfigReference<ModelConfig> getEffect(boolean occupied) {
      return new ConfigReference("ui/icon/interact_energy.dat", "text", String.valueOf(this.amount), new Object[]{"variant", "confirm", "occupied", String.valueOf(occupied)});
    }
  }

  public static class Energy extends ActionDesc {
    @Editable(
      min = 0.0
    )
    public int amount;

    public Energy() {
    }

    public String getHelpTip() {
      return "m.energy";
    }

    public ConfigReference<ModelConfig> getEffect(boolean occupied) {
      return new ConfigReference("ui/icon/interact_energy.dat", "text", String.valueOf(this.amount), new Object[]{"variant", "default", "occupied", String.valueOf(occupied)});
    }
  }

  public static class Resurrect extends ActionDesc {
    protected static final ConfigReference<ModelConfig> EFFECT = createEffect(false, "raise");
    protected static final ConfigReference<ModelConfig> OCCUPIED_EFFECT = createEffect(true, "raise");

    public Resurrect() {
    }

    public String getHelpTip() {
      return "m.resurrect";
    }

    public ConfigReference<ModelConfig> getEffect(boolean occupied) {
      return occupied ? OCCUPIED_EFFECT : EFFECT;
    }
  }

  public static class Shop extends ActionDesc {
    protected static final ConfigReference<ModelConfig> EFFECT = createEffect(false, "shop");
    protected static final ConfigReference<ModelConfig> OCCUPIED_EFFECT = createEffect(true, "shop");

    public Shop() {
    }

    public String getHelpTip() {
      return "m.shop";
    }

    public ConfigReference<ModelConfig> getEffect(boolean occupied) {
      return occupied ? OCCUPIED_EFFECT : EFFECT;
    }

    public int getActivity(ConfigManager cfgmgr) {
      return getEmoteActivity(cfgmgr, "Wave");
    }

    public int getActivityLength() {
      return 1667;
    }
  }

  public static class UnlockGold extends ActionDesc {
    protected static final ConfigReference<ModelConfig> EFFECT = createEffect(false, "unlock_gold");
    protected static final ConfigReference<ModelConfig> OCCUPIED_EFFECT = createEffect(true, "unlock_gold");

    public UnlockGold() {
    }

    public String getHelpTip() {
      return "m.unlock_gold";
    }

    public ConfigReference<ModelConfig> getEffect(boolean occupied) {
      return occupied ? OCCUPIED_EFFECT : EFFECT;
    }
  }

  public static class Unlock extends ActionDesc {
    protected static final ConfigReference<ModelConfig> EFFECT = createEffect(false, "unlock");
    protected static final ConfigReference<ModelConfig> OCCUPIED_EFFECT = createEffect(true, "unlock");

    public Unlock() {
    }

    public String getHelpTip() {
      return "m.unlock";
    }

    public ConfigReference<ModelConfig> getEffect(boolean occupied) {
      return occupied ? OCCUPIED_EFFECT : EFFECT;
    }
  }

  public static class PointOfInterest extends ActionDesc {
    protected static final ConfigReference<ModelConfig> EFFECT = createEffect(false, "pointofinterest");
    protected static final ConfigReference<ModelConfig> OCCUPIED_EFFECT = createEffect(true, "pointofinterest");

    public PointOfInterest() {
    }

    public String getHelpTip() {
      return "m.poi";
    }

    public ConfigReference<ModelConfig> getEffect(boolean occupied) {
      return occupied ? OCCUPIED_EFFECT : EFFECT;
    }
  }

  public static class Zone extends ActionDesc {
    protected static final ConfigReference<ModelConfig> EFFECT = createEffect(false, "zone");
    protected static final ConfigReference<ModelConfig> OCCUPIED_EFFECT = createEffect(true, "zone");

    public Zone() {
    }

    public String getHelpTip() {
      return "m.zone";
    }

    public ConfigReference<ModelConfig> getEffect(boolean occupied) {
      return occupied ? OCCUPIED_EFFECT : EFFECT;
    }
  }

  public static class Talk extends ActionDesc {
    protected static final ConfigReference<ModelConfig> EFFECT = createEffect(false, "talk");
    protected static final ConfigReference<ModelConfig> OCCUPIED_EFFECT = createEffect(true, "talk");

    public Talk() {
    }

    public String getHelpTip() {
      return "m.talk";
    }

    public ConfigReference<ModelConfig> getEffect(boolean occupied) {
      return occupied ? OCCUPIED_EFFECT : EFFECT;
    }

    public int getActivity(ConfigManager cfgmgr) {
      return getEmoteActivity(cfgmgr, "Wave");
    }

    public int getActivityLength() {
      return 1667;
    }
  }

  public static class Interact extends ActionDesc {
    protected static final ConfigReference<ModelConfig> EFFECT = createEffect(false, "interact");
    protected static final ConfigReference<ModelConfig> OCCUPIED_EFFECT = createEffect(true, "interact");

    public Interact() {
    }

    public String getHelpTip() {
      return "m.interact";
    }

    public ConfigReference<ModelConfig> getEffect(boolean occupied) {
      return occupied ? OCCUPIED_EFFECT : EFFECT;
    }
  }

  public static class Lift extends ActionDesc {
    protected static final ConfigReference<ModelConfig> EFFECT = createEffect(false, "lift");
    protected static final ConfigReference<ModelConfig> OCCUPIED_EFFECT = createEffect(true, "lift");

    public Lift() {
    }

    public String getHelpTip() {
      return "m.lift";
    }

    public ConfigReference<ModelConfig> getEffect(boolean occupied) {
      return occupied ? OCCUPIED_EFFECT : EFFECT;
    }

    public int getActivity(ConfigManager cfgmgr) {
      return 273;
    }

    public int getActivityLength() {
      return 277;
    }
  }
}
