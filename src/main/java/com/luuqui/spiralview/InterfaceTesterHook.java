package com.luuqui.spiralview;

import com.threerings.opengl.gui.tools.InterfaceTester;

public class InterfaceTesterHook extends InterfaceTester {

  public InterfaceTesterHook(String userInterface) {
    super(userInterface);
  }

  protected void didInit() {
    super.didInit();

    this._grid.getColor().set(0.21F, 0.21F, 0.21F, 0.0F);
    this._compositor.getDefaultBackgroundColor().set(0.41F, 0.41F, 0.41F, 0.0F);
  }

  public static void main(String[] args) {
    ViewKL.setupLAF();
    new InterfaceTesterHook(args.length > 0 ? args[0] : null).startup();
  }

}
