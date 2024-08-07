package com.luuqui.spiralview;

import com.threerings.tudey.tools.SceneEditor;

public class SceneEditorHook extends SceneEditor {

  public SceneEditorHook(String scene) {
    super(scene);
  }

  protected void didInit() {
    super.didInit();

    this._grid.getColor().set(0.21F, 0.21F, 0.21F, 0.0F);
    this._compositor.getDefaultBackgroundColor().set(0.41F, 0.41F, 0.41F, 0.0F);
  }

  public static void main(String[] args) {
    ViewKL.setupLAF();
    new SceneEditorHook(null).startup();
  }

}
