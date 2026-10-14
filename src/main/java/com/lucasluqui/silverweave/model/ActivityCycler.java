package com.lucasluqui.silverweave.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.swing.Timer;

import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;

/**
 * Cycles through the activity animations of the equipped items.
 *
 * <p>These are animations named {@code ks_<activity>}, optionally followed by {@code :tags},
 * anywhere in the knight's model tree. Playing one stops the {@code ks_default} animation of the
 * same model until the activity is over. Each is played once and holds its last pose; looping
 * ones are stopped after their first pass. After the activities comes a turn with none.
 *
 * <p>A slung shield plays none of its activity animations, only its {@code ks_shield_away} pose
 * if it has one.
 */
public class ActivityCycler
{
  /**
   * @param onChange notified with the name of the activity being shown, or null if the equipped
   * items have no activity animations.
   */
  public ActivityCycler (Consumer<String> onChange)
  {
    _onChange = onChange;
    _cycle = new Timer(INTERVAL, e -> next());
    _watch = new Timer(WATCH_INTERVAL, e -> holdFinished());
  }

  /**
   * Finds the activity animations after the knight model is rebuilt and starts cycling through
   * them, if there are any.
   *
   * @param shieldAway whether the shield is slung on the knight's back.
   * @param shield the name of the shield's model config, or null if unknown.
   */
  public void setModel (Model knight, boolean shieldAway, String shield)
  {
    stopCurrent();
    _activities.clear();
    _slung.clear();
    collect(knight, shieldAway, shield);
    _index = -1;
    if (_activities.isEmpty()) {
      _cycle.stop();
      _onChange.accept(null);
    } else {
      next();
      _cycle.restart();
    }
    if (_activities.isEmpty() && _slung.isEmpty()) {
      _watch.stop();
    } else {
      _watch.start();
    }
  }

  /**
   * Gathers the activity animations of a model and its children (the equipped items).
   */
  protected void collect (Model model, boolean shieldAway, String shield)
  {
    Animation fallback = null, away = null;
    for (Animation animation : model.getAnimations()) {
      String activity = activityOf(animation.getName());
      if (DEFAULT.equals(activity) && fallback == null) {
        fallback = animation;
      } else if (SHIELD_AWAY.equals(activity) && away == null) {
        away = animation;
      }
    }
    ModelConfig config = model.getConfig();
    boolean isShield = away != null ||
      (shield != null && config != null && shield.equals(config.getName()));
    if (shieldAway && isShield) {
      if (away != null) {
        // the shield holds its away pose, and nothing else of it plays
        for (Animation animation : model.getAnimations()) {
          if (animation != away && animation.isPlaying()) {
            animation.stop(0f);
          }
        }
        Played played = new Played(away, null);
        played.start();
        _slung.add(played);
      }
      return; // slung, it isn't an item for the activities
    }
    if (away != null) {
      // a model kept through the rebuild may still be holding the pose from being slung
      if (away.isPlaying()) {
        away.stop(0f);
      }
      if (fallback != null && !fallback.isPlaying()) {
        fallback.start();
      }
    }
    for (Animation animation : model.getAnimations()) {
      String activity = activityOf(animation.getName());
      // (immutable sets can't be asked about null, which most animations' activity is)
      if (activity != null && ACTIVITIES.contains(activity)) {
        _activities.computeIfAbsent(activity, a -> new ArrayList<>())
          .add(new Played(animation, fallback));
      }
    }
    for (Model child : model.getChildren()) {
      collect(child, shieldAway, shield);
    }
  }

  protected void next ()
  {
    stopCurrent();
    List<String> names = new ArrayList<>(_activities.keySet());
    if (names.isEmpty()) {
      return;
    }
    // after the activities comes a turn with none, showing the items at rest
    _index = (_index + 1) % (names.size() + 1);
    if (_index == names.size()) {
      _onChange.accept(NONE);
      return;
    }
    String activity = names.get(_index);
    _current = _activities.get(activity);
    for (Played played : _current) {
      played.start();
    }
    _onChange.accept(label(activity));
  }

  protected void stopCurrent ()
  {
    if (_current != null) {
      for (Played played : _current) {
        played.stop();
      }
      _current = null;
    }
  }

  /**
   * Stops the looping activity (and slung shield) animations that have been through once,
   * leaving them on the pose they reached. The others stop and hold their last pose on their own.
   */
  protected void holdFinished ()
  {
    long now = System.nanoTime();
    if (_current != null) {
      holdFinished(_current, now);
    }
    holdFinished(_slung, now);
  }

  protected void holdFinished (List<Played> playing, long now)
  {
    for (Played played : playing) {
      Animation animation = played.animation;
      float duration = animation.getConfigDuration() / animation.getSpeed();
      if (!played.active || !animation.isPlaying() || !(duration > 0f)) {
        continue; // unknown or endless durations are left to play on
      }
      // stop on the last check before the end, as by the next one it may have wrapped around
      float elapsed = (now - played.started) / 1e9f + WATCH_INTERVAL / 1000f;
      if (elapsed >= duration) {
        // with no blending out the nodes keep the pose; nothing else is animating them
        animation.stop(0f);
      }
    }
  }

  /**
   * Returns the activity an animation is for ("attack" for "ks_attack:fire"), or null if it
   * isn't an activity animation.
   */
  protected static String activityOf (String name)
  {
    if (name == null || !name.startsWith(PREFIX)) {
      return null;
    }
    int tag = name.indexOf(':');
    return name.substring(PREFIX.length(), tag == -1 ? name.length() : tag);
  }

  /**
   * Returns the name shown for an activity ("Open Box" for "open_box").
   */
  protected static String label (String activity)
  {
    StringBuilder buf = new StringBuilder();
    for (String word : activity.split("_")) {
      if (!word.isEmpty()) {
        if (buf.length() > 0) {
          buf.append(' ');
        }
        buf.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
      }
    }
    return buf.toString();
  }

  /**
   * An activity animation, with the default animation of its model that it takes over from.
   */
  protected static class Played
  {
    public final Animation animation, fallback;
    public boolean active;

    /** When the animation was started, from {@link System#nanoTime}. */
    public long started;

    public Played (Animation animation, Animation fallback)
    {
      this.animation = animation;
      this.fallback = fallback;
    }

    public void start ()
    {
      active = true;
      started = System.nanoTime();
      animation.start();
      if (fallback != null) {
        fallback.stop();
      }
    }

    public void stop ()
    {
      active = false;
      animation.stop();
      if (fallback != null) {
        fallback.start();
      }
    }
  }

  /** The prefix of activity animation names, and the activity of the resting animation. */
  protected static final String PREFIX = "ks_", DEFAULT = "default";

  /** The activity of the pose a shield takes while slung on the back. */
  protected static final String SHIELD_AWAY = "shield_away";

  /** The name shown for the turn in which no activity is shown. */
  protected static final String NONE = "None";

  /** The activities of {@code KnightSprite.ActivityAnimation}, less the resting one and the
   * shield away pose. */
  protected static final Set<String> ACTIVITIES = Set.of("loading", "use_inventory", "talking",
    "interact", "sleeping", "open_box", "hatch_egg", "defense", "bash", "dodge", "attack",
    "charge", "charge_attack", "moving");

  /** How long each activity is shown, and how often looping animations are checked for having
   * been through once, in milliseconds. */
  protected static final int INTERVAL = 5000, WATCH_INTERVAL = 50;

  protected final Consumer<String> _onChange;
  protected final Timer _cycle, _watch;

  /** The activity animations, by activity, in name order. */
  protected final Map<String, List<Played>> _activities = new TreeMap<>();

  /** The away poses of the slung shield, held while it stays slung. */
  protected final List<Played> _slung = new ArrayList<>();

  protected List<Played> _current;
  protected int _index = -1;
}
