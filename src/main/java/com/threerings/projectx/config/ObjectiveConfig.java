package com.threerings.projectx.config;

import com.threerings.config.ConfigReference;
import com.threerings.editor.c;
import com.threerings.editor.e;
import com.threerings.export.Exportable;
import com.threerings.export.f;
import com.threerings.io.Streamable;
import com.threerings.opengl.gui.ay;
import com.threerings.opengl.gui.config.UserInterfaceConfig;
import com.threerings.opengl.gui.layout.AnchorLayout;
import com.threerings.projectx.client.et;
import com.threerings.projectx.uplink.data.SystemSender;
import com.threerings.projectx.util.A;
import com.threerings.tudey.config.ActionConfig;
import com.threerings.util.g;

@e({ObjectiveConfig.Simple.class, ObjectiveConfig.ReadSystemMail.class})
public abstract class ObjectiveConfig extends g implements f, Streamable, Exportable {
  public ObjectiveConfig() {
  }

  public String getText() {
    return null;
  }

  public ay b(A var1, et var2) {
    return null;
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

  public static class Simple extends ObjectiveConfig {
    @c
    public String text = "";
    @c(
      jc = 0.01,
      iS = "c"
    )
    public float childX = 0.5F;
    @c(
      jc = 0.01,
      iS = "c"
    )
    public float childY = 0.5F;
    @c(
      jc = 0.01,
      iS = "p"
    )
    public float parentX = 0.5F;
    @c(
      jc = 0.01,
      iS = "p"
    )
    public float parentY = 0.5F;
    @c(
      jh = true
    )
    public ConfigReference<UserInterfaceConfig> userInterface;

    public Simple() {
    }

    public final String getText() {
      return this.text;
    }

    public final ay b(A var1, et var2) {
      if (this.userInterface == null) {
        return null;
      } else {
        ay var3 = new ay(var1, this.userInterface);
        var2.yO().add(var3, new AnchorLayout.Anchor(this.childX, this.childY, this.parentX, this.parentY, 0, 0, false));
        return var3;
      }
    }
  }
}
