package net.lucasallegri.spiralview;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import com.threerings.ClydeLog;
import com.threerings.opengl.model.tools.ModelViewer;

import net.lucasallegri.spiralview.actions.ClearPrefsAction;
import net.lucasallegri.spiralview.actions.RestorePrefsAction;
import net.lucasallegri.spiralview.actions.SavePrefsAction;
import net.lucasallegri.spiralview.actions.SnapshotAction;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.prefs.Preferences;

import static net.lucasallegri.spiralview.Log.log;

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
  }

  public static void main(String[] args) {
    Log.setupFileLogging();
    _prefs.remove("environment_models");
    new ModelViewerHook(args.length > 0 ? args[0] : null).startup();
  }

}
