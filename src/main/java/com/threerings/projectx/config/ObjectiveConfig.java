package com.threerings.projectx.config;

import com.threerings.config.ConfigReference;
import com.threerings.opengl.gui.ay;
import com.threerings.opengl.gui.config.UserInterfaceConfig;
import com.threerings.projectx.client.ew;
import com.threerings.projectx.uplink.data.SystemSender;
import com.threerings.projectx.util.C;
import com.threerings.tudey.config.ActionConfig;

public class ObjectiveConfig
{
  public String getText() {
    return null;
  }
  public Object b (C var1, ew var2) {
    return null;
  }

  // does this work?
  public Object b (C var1, Object var2) {
    return null;
  }

  public static class Simple
  {
    public Simple () {}

    public String text = "";
    public ConfigReference<UserInterfaceConfig> userInterface;

    public final String getText() {
      return this.text;
    }

    public final Object b (C var1, ew var2)
    {
      if (this.userInterface == null) {
        return null;
      } else {
        Object var3 = new ay(var1, this.userInterface);
        return var3;
      }
    }
  }

  public static class ReadSystemMail extends ObjectiveConfig.Simple
  {
    public SystemSender sender;
    public ActionConfig action;

    public ReadSystemMail ()
    {
      this.sender = SystemSender.ORIENTATION;
      this.action = new ActionConfig.SpawnActor();
    }
  }
}
