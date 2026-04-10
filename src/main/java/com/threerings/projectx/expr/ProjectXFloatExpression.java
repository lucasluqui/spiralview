package com.threerings.projectx.expr;

import com.threerings.editor.EditorTypes;
import com.threerings.expr.FloatExpression;
import com.threerings.expr.Scope;

@EditorTypes({
  ProjectXFloatExpression.Daypoint.class
})
public abstract class ProjectXFloatExpression extends FloatExpression
{
  public static class Daypoint extends ProjectXFloatExpression {
    @Override
    public Evaluator createEvaluator (Scope scope)
    {
      return new DaypointEvaluator(this, scope);
    }

    static float value;
    static long stamp;
  }
}
