// add Simple empty constructor
// replace return type of b method
// fix return type of b method within simple class

package com.threerings.projectx.config;

import com.threerings.config.ConfigReference;
import com.threerings.editor.c;
import com.threerings.editor.e;
import com.threerings.export.d;
import com.threerings.io.Streamable;
import com.threerings.opengl.gui.ay;
import com.threerings.opengl.gui.config.UserInterfaceConfig;
import com.threerings.projectx.client.ew;
import com.threerings.projectx.uplink.data.SystemSender;
import com.threerings.projectx.util.C;
import com.threerings.tudey.config.ActionConfig;
import com.threerings.util.g;

@e({ObjectiveConfig.Simple.class, ObjectiveConfig.ReadSystemMail.class})
public abstract class ObjectiveConfig extends g implements d, Streamable {
  public String getText() {
    return null;
  }

  public Object b(C var1, ew var2) {
    return null;
  }

  public static class Simple extends ObjectiveConfig {
    public Simple () {}

    @c
    public String text = "";
    @c(
      iG = 0.01,
      iv = "c"
    )
    public float childX = 0.5F;
    @c(
      iG = 0.01,
      iv = "c"
    )
    public float childY = 0.5F;
    @c(
      iG = 0.01,
      iv = "p"
    )
    public float parentX = 0.5F;
    @c(
      iG = 0.01,
      iv = "p"
    )
    public float parentY = 0.5F;
    @c(
      iL = true
    )
    public ConfigReference<UserInterfaceConfig> userInterface;

    public final String getText() {
      return this.text;
    }

    public final Object b(C var1, ew var2) {
      if (this.userInterface == null) {
        return null;
      } else {
        Object var3 = new ay(var1, this.userInterface);
        //var2.xS().add(var3, new b.a(this.childX, this.childY, this.parentX, this.parentY, 0, 0, false));
        return var3;
      }
    }
  }

  public static class ReadSystemMail extends Simple {
    @c
    public SystemSender sender;
    @c
    public ActionConfig action;

    public ReadSystemMail() {
      this.sender = SystemSender.ORIENTATION;
      this.action = new ActionConfig.SpawnActor();
    }
  }
}