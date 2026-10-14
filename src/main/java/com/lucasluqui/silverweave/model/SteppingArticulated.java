package com.lucasluqui.silverweave.model;

import com.threerings.config.ConfigReference;
import com.threerings.expr.Function;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Transform3D;
import com.threerings.opengl.model.Articulated;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.scene.Scene;
import com.threerings.opengl.util.GlContext;

import static com.lucasluqui.silverweave.Log.log;

/**
 * The knight model implementation, which spawns footstep effects when the knight's animations
 * call for them.
 */
public class SteppingArticulated extends Articulated
{
  public SteppingArticulated (GlContext ctx, Scope parentScope, SteppingConfig config)
  {
    super(ctx, parentScope, config);
  }

  /**
   * Called by the knight's animations at each footfall.
   *
   * @param translation the encoded position of the foot, relative to the model.
   */
  @Scoped
  public void spawnStepEffect (String translation)
  {
    try {
      _step.getTranslation().decodeFromString(translation);
    } catch (Exception e) {
      log.warning("Unreadable step position.", "translation", translation, e);
      return;
    }
    _worldTransform.compose(_step, _world);
    long now = ScopeUtil.resolveTimestamp(this, "now").value;
    SteppingConfig config = (SteppingConfig)_config;

    // steps in quick succession that barely move use the lighter effect
    if (_lastStepTime + SHORT_STEP_INTERVAL >= now &&
        _world.getTranslation().distance(_lastStep.getTranslation()) <= SHORT_STEP_DISTANCE) {
      spawn(config.getShortStepEffect());
    } else {
      spawn(config.stepEffect);
      _lastStep.set(_world);
      _lastStepTime = now;
    }
  }

  /**
   * Spawns a transient effect into the knight's scene, for effects spawned within the knight.
   */
  @Scoped
  public Scene.Transient spawnTransient (ConfigReference<ModelConfig> ref, Transform3D transform)
  {
    Scene scene = getScene();
    return scene == null ? null : scene.spawnTransient(ref, transform);
  }

  /**
   * Spawns an effect at the current step position.
   *
   * <p>The effect takes the scene as its parent scope, which also spawns whatever the effect
   * spawns in turn.
   */
  protected void spawn (ConfigReference<ModelConfig> effect)
  {
    if (effect == null) {
      return;
    }
    Scene scene = getScene();
    boolean spawned;
    if (scene != null) {
      spawned = scene.spawnTransient(effect, _world) != null;
    } else {
      Function spawner = ScopeUtil.resolve(this, "spawnTransient", Function.NULL);
      spawned = spawner != Function.NULL && spawner.call(effect, _world) != null;
    }
    if (!_reported) {
      _reported = true;
      log.info("Knight step effect.", "effect", effect.getName(), "inScene", scene != null,
        "spawned", spawned);
    }
  }

  /**
   * Returns the scene the knight is in, or null.
   */
  protected Scene getScene ()
  {
    Scope parent = getParentScope();
    return (parent instanceof Model) ? ((Model)parent).getScene(this) : null;
  }

  /** The longest time and distance between steps for the second to count as a short step. */
  protected static final long SHORT_STEP_INTERVAL = 800L;
  protected static final float SHORT_STEP_DISTANCE = 0.4f;

  /** Whether the first step has been reported in the log, for troubleshooting. */
  protected static boolean _reported;

  protected final Transform3D _step = new Transform3D(Transform3D.RIGID);
  protected final Transform3D _world = new Transform3D();
  protected final Transform3D _lastStep = new Transform3D();
  protected long _lastStepTime;
}
