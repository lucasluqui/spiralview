package com.threerings.projectx.config;

// add Simple empty constructor
// replace return type of b method
// fix return type of b method within simple class

import com.threerings.config.ConfigReference;
import com.threerings.editor.c;
import com.threerings.editor.e;
import com.threerings.export.f;
import com.threerings.io.Streamable;
import com.threerings.opengl.gui.ax;
import com.threerings.opengl.gui.p;
import com.threerings.opengl.gui.config.UserInterfaceConfig;
import com.threerings.projectx.client.er;
import com.threerings.projectx.uplink.data.SystemSender;
import com.threerings.projectx.util.A;
import com.threerings.tudey.config.ActionConfig;
import com.threerings.util.g;

@e({ObjectiveConfig.Simple.class, ObjectiveConfig.ReadSystemMail.class})
public abstract class ObjectiveConfig extends g implements f, Streamable {
  public String getText() {
    return null;
  }

  public ax b(A var1, er var2) {
    return null;
  }

  public static class Simple extends ObjectiveConfig {
    @c
    public String text = "";
    @c(
        iM = 0.01,
        iC = "c"
    )
    public float childX = 0.5F;
    @c(
        iM = 0.01,
        iC = "c"
    )
    public float childY = 0.5F;
    @c(
        iM = 0.01,
        iC = "p"
    )
    public float parentX = 0.5F;
    @c(
        iM = 0.01,
        iC = "p"
    )
    public float parentY = 0.5F;
    @c(
        iR = true
    )
    public ConfigReference<UserInterfaceConfig> userInterface;

    public Simple() {
    }

    public final String getText() {
      return this.text;
    }

    public final ax b(A var1, er var2) {
      if (this.userInterface == null) {
        return null;
      } else {
        ax var3 = new ax(var1, this.userInterface);
        //var2.xK().add(var3, new b.a(this.childX, this.childY, this.parentX, this.parentY, 0, 0, false));
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
