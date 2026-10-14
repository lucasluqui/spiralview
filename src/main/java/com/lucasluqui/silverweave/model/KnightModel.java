package com.lucasluqui.silverweave.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ArticulatedConfig;
import com.threerings.opengl.model.config.ModelConfig;

import static com.lucasluqui.silverweave.Log.log;

/**
 * Prepares the knight model for use in spiralview.
 */
public final class KnightModel
{
  /**
   * Swaps the walking implementation of the knight model for a {@link SteppingConfig}, so that its
   * step effects work.
   *
   * @return the knight model config, which the caller should hold on to: the config manager only
   * keeps resource configs softly, and a reloaded copy would lose the swap.
   */
  @SuppressWarnings("unchecked")
  public static ModelConfig prepare (ConfigManager cfgmgr, String path)
  {
    ManagedConfig loaded = cfgmgr.getResourceConfig(path);
    if (!(loaded instanceof ModelConfig)) {
      return null;
    }
    ModelConfig config = (ModelConfig)loaded;
    ModelConfig.Implementation impl = config.implementation;
    if (impl instanceof ArticulatedConfig && addWeaponPoses((ArticulatedConfig)impl)) {
      config.wasUpdated();
    }
    if (!(impl instanceof ArticulatedConfig) || impl instanceof SteppingConfig ||
        !impl.getClass().getName().endsWith("$WalkingConfig")) {
      return config;
    }
    try {
      SteppingConfig stepping = new SteppingConfig();
      // copy everything the walking config inherits from ArticulatedConfig
      for (Class<?> clazz = ArticulatedConfig.class; clazz != Object.class;
          clazz = clazz.getSuperclass()) {
        for (Field field : clazz.getDeclaredFields()) {
          int mods = field.getModifiers();
          if (!Modifier.isStatic(mods) && !Modifier.isTransient(mods)) {
            field.setAccessible(true);
            field.set(stepping, field.get(impl));
          }
        }
      }
      stepping.stepEffect = (ConfigReference<ModelConfig>)impl.getClass()
        .getField("stepEffect").get(impl);
      config.implementation = stepping;
      config.wasUpdated();
    } catch (ReflectiveOperationException | RuntimeException e) {
      log.warning("Couldn't enable knight step effects.", e);
    }
    return config;
  }

  /**
   * Adds the weapon holding poses, which the knight model used here doesn't map.
   *
   * @return whether any were added.
   */
  protected static boolean addWeaponPoses (ArticulatedConfig impl)
  {
    List<ArticulatedConfig.AnimationMapping> mappings = new ArrayList<>(
      Arrays.asList(impl.animationMappings));
    boolean added = false;
    for (String pose : WEAPON_POSES) {
      boolean present = false;
      for (ArticulatedConfig.AnimationMapping mapping : mappings) {
        present |= pose.equals(mapping.name);
      }
      if (!present) {
        ArticulatedConfig.AnimationMapping mapping = new ArticulatedConfig.AnimationMapping();
        mapping.name = pose;
        mapping.animation = new ConfigReference<>("character/pc/" + pose + ".dat");
        mappings.add(mapping);
        added = true;
      }
    }
    if (added) {
      impl.animationMappings = mappings.toArray(new ArticulatedConfig.AnimationMapping[0]);
    }
    return added;
  }

  /**
   * Notes which config each model attached under the given one (at any depth) shows, for
   * {@link #restartChanged} once the model's config has changed.
   */
  public static Map<Model, ModelConfig> attachmentConfigs (Model model)
  {
    Map<Model, ModelConfig> configs = new IdentityHashMap<>();
    collectConfigs(model, configs);
    return configs;
  }

  /**
   * Restarts the models attached under the given one that are new or show something else than
   * they did when {@link #attachmentConfigs} was taken, as when an accessory replaces another.
   *
   * <p>Changing a model's config reuses the models attached to it, which keep their animations
   * and only start the automatic ones the first time they run; resetting them starts those over.
   */
  public static void restartChanged (Model model, Map<Model, ModelConfig> before)
  {
    for (Model child : model.getChildren()) {
      if (!before.containsKey(child) || before.get(child) != child.getConfig()) {
        // (which restarts whatever is attached to it as well)
        child.reset();
      } else {
        restartChanged(child, before);
      }
    }
  }

  protected static void collectConfigs (Model model, Map<Model, ModelConfig> configs)
  {
    for (Model child : model.getChildren()) {
      configs.put(child, child.getConfig());
      collectConfigs(child, configs);
    }
  }

  /**
   * Checks whether the user may play an animation of the knight model themselves. The ones
   * Silverweave plays on its own (the resting idle, weapon poses) or that settings drive (the
   * height) are left out.
   */
  public static boolean isPlayable (String name)
  {
    return !HIDDEN_ANIMATIONS.contains(name);
  }

  /**
   * Returns the name shown for a knight animation: a friendlier one for those the knight model
   * is known to have, and a tidied up version of its own name for the rest.
   */
  public static String label (String name)
  {
    String label = ANIMATION_LABELS.get(name);
    if (label != null) {
      return label;
    }
    // "Pose, Something" -> "Something Pose"
    int comma = name.indexOf(", ");
    if (comma > 0) {
      name = name.substring(comma + 2) + " " + name.substring(0, comma);
    }
    StringBuilder buf = new StringBuilder();
    for (String word : name.replace('_', ' ')
        .replaceAll("([a-z])([A-Z])", "$1 $2").trim().split("\\s+")) {
      if (word.isEmpty()) {
        continue;
      }
      if (buf.length() > 0) {
        buf.append(' ');
      }
      // numbered variants ("idle 003") read better without the padding
      if (word.matches("\\d+")) {
        buf.append(Integer.parseInt(word));
      } else {
        buf.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
      }
    }
    return buf.toString();
  }

  /** The weapon holding poses. */
  public static final String SWORD_POSE = "ready_sword", HANDGUN_POSE = "ready_pistol",
    BOMB_POSE = "ready_bomb";

  protected static final String[] WEAPON_POSES = { SWORD_POSE, HANDGUN_POSE, BOMB_POSE };

  /** Animations the user doesn't get to play with. */
  protected static final Set<String> HIDDEN_ANIMATIONS = Set.of(
    "idle", "idle 001", "height adjust", SWORD_POSE, HANDGUN_POSE, BOMB_POSE);

  /** The names shown for the knight's animations, where tidying up their own isn't enough. */
  protected static final Map<String, String> ANIMATION_LABELS = Map.ofEntries(
    Map.entry("use_inventory_start", "Inventory (Open)"),
    Map.entry("use_inventory_loop", "Inventory (Browsing)"),
    Map.entry("use_inventory_end", "Inventory (Close)"),
    Map.entry("use_interact", "Interact"),
    Map.entry("use_item", "Use Item"),
    Map.entry("walk_forward", "Walk"),
    Map.entry("Attack, Sword 3", "Sword Attack"),
    Map.entry("Hurt, Hard", "Hurt (Hard)"),
    Map.entry("Hurt, Soft", "Hurt (Soft)"),
    Map.entry("Hurt, Recover", "Hurt (Recover)"),
    Map.entry("Sitting Loop", "I'm Dead!"),
    Map.entry("rotate", "Hold and Turn"),
    Map.entry("Pose, OnHip", "Hands on Hips Pose"),
    Map.entry("Pose, OverShoulder", "Over the Shoulder Pose"));

  private KnightModel () { }
}
