package com.lucasluqui.silverweave.model;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.expr.Scope;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ArticulatedConfig;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.GlContext;

/**
 * Stands in for {@code ProjectXModelConfig.WalkingConfig}, which is compiled against pcode's
 * obfuscated clyde, so neither its model implementation (which plays footstep effects) nor its
 * "step_effect" parameter is visible to the clyde spiralview runs on.
 */
public class SteppingConfig extends ArticulatedConfig
{
  /** The effect spawned at each footstep, if any. */
  @Editable(nullable=true)
  public ConfigReference<ModelConfig> stepEffect;

  /**
   * Returns the lighter variant of the step effect used for quick successive steps.
   */
  public ConfigReference<ModelConfig> getShortStepEffect ()
  {
    return stepEffect == null ? null : stepEffect.clone("Short Step", "true");
  }

  @Override
  public Model.Implementation getModelImplementation (
    GlContext ctx, Scope scope, Model.Implementation impl)
  {
    if (root == null) {
      return null;
    }
    if (impl instanceof SteppingArticulated) {
      ((SteppingArticulated)impl).setConfig(ctx, this);
      return impl;
    }
    return new SteppingArticulated(ctx, scope, this);
  }
}
