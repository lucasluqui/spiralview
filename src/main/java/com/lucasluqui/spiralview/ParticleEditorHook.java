package com.lucasluqui.spiralview;

import java.awt.EventQueue;

import com.lucasluqui.util.ImageUtil;
import com.threerings.opengl.effect.tools.ParticleEditor;

public class ParticleEditorHook extends ParticleEditor
{
  public ParticleEditorHook (String particles)
  {
    super(particles);
    this._frame.setIconImage(ImageUtil.loadImageWithinJar("/img/icon.png"));
    ViewKL.maximize(this._frame);
  }

  protected void didInit ()
  {
    super.didInit();

    this._grid.getColor().set(0.21F, 0.21F, 0.21F, 0.0F);
    this._compositor.getDefaultBackgroundColor().set(0.41F, 0.41F, 0.41F, 0.0F);
    EditorLoading.dismiss(this._frame);
  }

  public static void main (String[] args)
  {
    ViewKL.setupLAF();
    EditorLoading.show("Particle Editor");
    ParticleEditorHook editor = new ParticleEditorHook(null);
    // shown from the event thread: showing the window starts the render loop there, which
    // deadlocks with a window being shown from any other thread
    EventQueue.invokeLater(editor::startup);
  }
}
