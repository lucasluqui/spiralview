package com.luuqui.spiralview;

import javax.swing.*;

import com.luuqui.util.ImageUtil;
import com.threerings.opengl.model.tools.ModelViewer;

import com.luuqui.spiralview.actions.ClearPrefsAction;
import com.luuqui.spiralview.actions.RestorePrefsAction;
import com.luuqui.spiralview.actions.SavePrefsAction;
import com.luuqui.spiralview.actions.SnapshotAction;

public class ModelViewerHook extends ModelViewer {

  public ModelViewerHook(String model) {
    super(model);
    JMenuBar mbar = this._frame.getJMenuBar();
    JMenu file = mbar.getMenu(0);
    JMenu environment = new JMenu("Environment");
    environment.add(new JMenuItem(new SavePrefsAction()));
    environment.add(new JMenuItem(new RestorePrefsAction()));
    environment.add(new JMenuItem(new ClearPrefsAction()));
    file.add(new JMenuItem(new SnapshotAction(this)), 0);
    file.add(environment, 0);
    this._frame.setIconImage(ImageUtil.loadImageWithinJar("/img/icon-128.png"));
  }

  protected void didInit() {
    super.didInit();

    this._grid.getColor().set(0.21F, 0.21F, 0.21F, 0.0F);
    this._compositor.getDefaultBackgroundColor().set(0.41F, 0.41F, 0.41F, 0.0F);
  }

  public static void main(String[] args) {
    _prefs.remove("environment_models");
    ViewKL.setupLAF();
    new ModelViewerHook(args.length > 0 ? args[0] : null).startup();
  }

}
