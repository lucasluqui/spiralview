package com.luuqui.spiralview;

import com.threerings.opengl.effect.tools.ParticleEditor;

public class ParticleEditorHook extends ParticleEditor {

  public ParticleEditorHook(String particles) {
    super(particles);
  }

  protected void didInit() {
    super.didInit();

    this._grid.getColor().set(0.21F, 0.21F, 0.21F, 0.0F);
    this._compositor.getDefaultBackgroundColor().set(0.41F, 0.41F, 0.41F, 0.0F);
  }

  public static void main(String[] args) {
    ViewKL.setupLAF();
    new ParticleEditorHook(null).startup();
  }

}
