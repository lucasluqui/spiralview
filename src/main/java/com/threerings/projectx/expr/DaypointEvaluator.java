package com.threerings.projectx.expr;

import com.threerings.expr.FloatExpression;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.projectx.expr.ProjectXFloatExpression.Daypoint;
import java.util.Calendar;

class DaypointEvaluator extends FloatExpression.Evaluator
{
  public DaypointEvaluator (Daypoint daypoint, Scope scope)
  {
    this.now = ScopeUtil.resolveTimestamp(scope, "now");
    this.calendar = Calendar.getInstance();
  }

  public float evaluate ()
  {
    if (this.now.value - Daypoint.stamp >= 1000L) {
      this.calendar.setTimeInMillis(Daypoint.stamp = this.now.value);
      Daypoint.value = ((float) this.calendar.get(11) + (float) this.calendar.get(12) / 60.0F) / 24.0F;
    }

    return Daypoint.value;
  }

  private final MutableLong now;
  private final Calendar calendar;
}
