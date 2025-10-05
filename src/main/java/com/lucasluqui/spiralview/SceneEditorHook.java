package com.lucasluqui.spiralview;

import com.lucasluqui.util.ImageUtil;
import com.threerings.tudey.tools.SceneEditor;

public class SceneEditorHook extends SceneEditor
{
  public SceneEditorHook (String scene)
  {
    super(scene);
    this._frame.setIconImage(ImageUtil.loadImageWithinJar("/img/icon.png"));
  }

  protected void didInit ()
  {
    super.didInit();

    // Modify the camera handler to allow unrestricted camera rotation.
    //((OrbitCameraHandler) this._camhand).setCoordLimits(
    //  (-(float) Math.PI / 2F),
    //  ((float) Math.PI / 2F),
    //  2.0F,
    //  50.0F
    //);
    this._grid.getColor().set(0.21F, 0.21F, 0.21F, 0.0F);
    this._compositor.getDefaultBackgroundColor().set(0.41F, 0.41F, 0.41F, 0.0F);
  }

  public static void main (String[] args)
  {
    ViewKL.setupLAF();
    new SceneEditorHook(null).startup();
  }
}
