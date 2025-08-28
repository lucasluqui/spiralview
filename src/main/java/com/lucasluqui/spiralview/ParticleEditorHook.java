package com.lucasluqui.spiralview;

import com.lucasluqui.util.ImageUtil;
import com.threerings.opengl.effect.tools.ParticleEditor;

public class ParticleEditorHook extends ParticleEditor {

  public ParticleEditorHook(String particles) {
    super(particles);
    this._frame.setTitle("Three Rings Particle Editor");
    this._frame.setIconImage(ImageUtil.loadImageWithinJar("/img/icon-512.png"));
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
