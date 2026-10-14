package com.lucasluqui.silverweave.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.Timer;

import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.Model;

import com.lucasluqui.silverweave.item.KnightItem;

/**
 * Keeps the knight looking alive between the animations the user plays: unarmed knights fidget
 * with one of their idle animations every so often, and armed knights hold their weapon at the
 * ready.
 */
public class KnightAnimator
{
  public KnightAnimator ()
  {
    _timer = new Timer(TICK, e -> tick());
    _timer.start();
  }

  /**
   * Picks up the knight's animations after its model is rebuilt.
   *
   * @param weapon the weapon the knight holds, or null.
   */
  public void setModel (Model model, KnightItem weapon)
  {
    _animations = model.getAnimations();
    _pose = weapon == null ? null : poseFor(animationCategory(weapon));
    // rebuilding keeps the model's animations, so a pose for a weapon no longer held plays on
    for (String pose : KnightModel.WEAPON_POSES) {
      if (!pose.equals(_pose)) {
        stop(find(pose));
      }
    }
    if (_pose != null) {
      stop(find(BASE_IDLE));
    }
    _fidget = null;
    restartCountdown();
    tick();
  }

  /**
   * Notes the animation the user is playing (or null once it ends or is stopped). The user's
   * animations take over until they're done, and the knight holds still for a moment after, so the
   * end of the animation can be seen before the idles take over again.
   */
  public void setUserAnimation (Animation animation)
  {
    _user = animation;
    if (animation != null) {
      stop(_fidget);
      stop(find(_pose));
      stop(find(BASE_IDLE));
      _fidget = null;
    } else {
      holdAfterUser();
    }
  }

  protected void tick ()
  {
    if (_animations == null) {
      return;
    }
    if (_user != null) {
      if (_user.isPlaying()) {
        return;
      }
      // over on its own, possibly before the animation panel noticed
      _user = null;
      holdAfterUser();
    }
    if (System.currentTimeMillis() < _resumeAt) {
      return;
    }
    if (_pose != null) {
      // the pose may not loop by itself, so keep it going
      Animation pose = find(_pose);
      if (pose != null && !pose.isPlaying()) {
        pose.start();
      }
      return;
    }
    if (_fidget != null && _fidget.isPlaying()) {
      return;
    }
    if (_fidget != null) {
      // just finished: wait a full interval before the next one
      _fidget = null;
      restartCountdown();
    }
    Animation base = find(BASE_IDLE);
    List<Animation> fidgets = new ArrayList<>();
    for (Animation animation : _animations) {
      if (animation != base && animation.getName().matches("idle \\d+")) {
        fidgets.add(animation);
      }
    }
    if (System.currentTimeMillis() >= _nextFidget && !fidgets.isEmpty()) {
      // the fidget takes over from the base idle until it's done
      stop(base);
      _fidget = fidgets.get(_random.nextInt(fidgets.size()));
      _fidget.start();
      return;
    }
    // the base idle may not loop by itself, so keep it going between fidgets
    if (base != null && !base.isPlaying()) {
      base.start();
    }
  }

  /**
   * Keeps the idles from taking over again until the knight has held still for a moment after the
   * user's animation, and starts the countdown to the next fidget over.
   */
  protected void holdAfterUser ()
  {
    _resumeAt = System.currentTimeMillis() + USER_HOLD;
    restartCountdown();
  }

  protected void restartCountdown ()
  {
    _nextFidget = System.currentTimeMillis() + FIDGET_INTERVAL - FIDGET_JITTER +
      _random.nextInt(FIDGET_JITTER * 2 + 1);
  }

  protected Animation find (String name)
  {
    if (name == null || _animations == null) {
      return null;
    }
    for (Animation animation : _animations) {
      if (animation.getName().equals(name)) {
        return animation;
      }
    }
    return null;
  }

  protected static void stop (Animation animation)
  {
    if (animation != null && animation.isPlaying()) {
      animation.stop();
    }
  }

  /**
   * Returns the category of weapon whose animations a weapon uses: its own, except for the handguns
   * that are animated like swords.
   */
  protected static String animationCategory (KnightItem weapon)
  {
    for (String sh : SWORD_HANDGUNS) {
      if (weapon.configName.startsWith(sh)) {
        return "Sword";
      }
    }
    return weapon.category;
  }

  /**
   * Returns the pose for a weapon category, or null if there is none.
   */
  protected static String poseFor (String category)
  {
    if ("Sword".equals(category)) {
      return KnightModel.SWORD_POSE;
    } else if ("Handgun".equals(category)) {
      return KnightModel.HANDGUN_POSE;
    } else if ("Bomb".equals(category)) {
      return KnightModel.BOMB_POSE;
    }
    return null;
  }

  /** The config name prefixes of the handguns animated as swords. */
  protected static final String[] SWORD_HANDGUNS = {
    "Weapon/Handgun/Owlite Wand/", "Weapon/Handgun/WizardWand/" };

  /** The idle the knight loops while unarmed, between fidgets with the other numbered idles. */
  protected static final String BASE_IDLE = "idle 001";

  /** How often the knight fidgets, give or take the jitter, in milliseconds. */
  protected static final int FIDGET_INTERVAL = 15000, FIDGET_JITTER = 2000;

  /** How long the knight holds still after the user's animation is over, in milliseconds. */
  protected static final int USER_HOLD = 3000;

  /** How often the animations are checked on, in milliseconds. */
  protected static final int TICK = 200;

  protected final Timer _timer;
  protected final Random _random = new Random();
  protected Animation[] _animations;
  protected String _pose;
  protected Animation _fidget, _user;
  protected long _nextFidget;

  /** When the idles may take over again after the user's animation, in milliseconds. */
  protected long _resumeAt;
}
