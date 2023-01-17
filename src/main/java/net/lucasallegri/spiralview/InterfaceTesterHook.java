package net.lucasallegri.spiralview;

import com.threerings.opengl.gui.tools.InterfaceTester;

public class InterfaceTesterHook {

  public static void main(String[] args) {
    Log.setupFileLogging();
    new InterfaceTester(args.length > 0 ? args[0] : null).startup();
  }

}
