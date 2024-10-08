package com.luuqui.spiralview;

import com.luuqui.util.ImageUtil;
import com.threerings.opengl.gui.tools.InterfaceTester;

public class InterfaceTesterHook extends InterfaceTester {

  public InterfaceTesterHook(String userInterface) {
    super(userInterface);
    this._frame.setTitle("Interface Editor");
    this._frame.setIconImage(ImageUtil.loadImageWithinJar("/img/icon-512.png"));
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
