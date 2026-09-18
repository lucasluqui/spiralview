package com.lucasluqui.silverweave;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.editor.swing.editors.ConfigReferenceEditor;
import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.ModelObserver;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.model.tools.ModelTool;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.io.File;

public class SilverweaveTool
  extends ModelTool
  implements ChangeListener, ConfigUpdateListener<ModelConfig>, ModelObserver
{
  public SilverweaveTool ()
  {
    super("silverweave");
    this._frame.setTitle("Silverweave");

    this._cpanel.add(this._epanel = new EditorPanel(this));

    // forced to load the base character model, that's all we'll use.
    ModelConfig.Derived impl = new ModelConfig.Derived();
    impl.model = new ConfigReference(this._rsrcmgr.getResourcePath(new File("rsrc\\character\\pc\\model.dat")));

    this._epanel.setObject(impl);
    ((ConfigReferenceEditor) this._epanel.getPropertyEditor("model")).setNoTransfer(true);
    this._epanel.addChangeListener(this);
  }

  @Override
  public void configUpdated (ConfigEvent<ModelConfig> configEvent)
  {

  }

  @Override
  protected CanvasToolPrefs createEditablePrefs ()
  {
    return null;
  }

  @Override
  public boolean modelCompleted (Model model)
  {
    return false;
  }

  @Override
  public boolean animationStarted (Animation animation)
  {
    return false;
  }

  @Override
  public boolean animationStopped (Animation animation, boolean b)
  {
    return false;
  }

  @Override
  public void stateChanged (ChangeEvent e)
  {

  }

  @Override
  public float getWindowScaleFactor ()
  {
    return super.getWindowScaleFactor();
  }

  @Override
  public void postRunnable (Runnable r)
  {
    super.postRunnable(r);
  }

  @Override
  public void runOnRunQueue (Runnable r)
  {
    super.runOnRunQueue(r);
  }

  protected JPanel _cpanel;
  protected EditorPanel _epanel;
}
