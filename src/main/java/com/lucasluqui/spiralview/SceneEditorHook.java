package com.lucasluqui.spiralview;

import com.lucasluqui.spiralview.actions.SnapshotAction;
import com.lucasluqui.spiralview.actions.SnapshotToAction;
import com.lucasluqui.util.ImageUtil;
import com.threerings.tudey.tools.SceneEditor;

import javax.swing.*;

public class SceneEditorHook extends SceneEditor
{
  public SceneEditorHook (String scene)
  {
    super(scene);
    JMenuBar mbar = this._frame.getJMenuBar();
    JMenu tools = mbar.getMenu(mbar.getMenuCount() - 1);
    tools.add(new JMenuItem(new SnapshotAction(this, "scene")), tools.getItemCount());
    tools.add(new JMenuItem(new SnapshotToAction(this, "scene")), tools.getItemCount());
    this._frame.setIconImage(ImageUtil.loadImageWithinJar("/img/icon.png"));
  }

  protected void didInit ()
  {
    super.didInit();

    this._grid.getColor().set(0.21F, 0.21F, 0.21F, 0.0F);
    this._compositor.getDefaultBackgroundColor().set(0.41F, 0.41F, 0.41F, 0.0F);
  }

  public static void main (String[] args)
  {
    ViewKL.setupLAF();
    new SceneEditorHook(null).startup();
  }
}
