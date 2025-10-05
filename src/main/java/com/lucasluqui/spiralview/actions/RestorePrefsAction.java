package com.lucasluqui.spiralview.actions;

// Original author: onyxbits (spiral.onyxbits.de)
// File: spiralspy-1.5.jar/de/onyxbits/spiralspy/RestorePrefsAction.java

import com.lucasluqui.spiralview.Log;
import com.threerings.ClydeLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

@SuppressWarnings("serial")
public final class RestorePrefsAction extends AbstractAction
{
  @SuppressWarnings("unused")
  private final Preferences cfg = Preferences.userNodeForPackage(ClydeLog.class);

  public RestorePrefsAction ()
  {
    putValue("Name", "Restore");
    putValue("ShortDescription", "Read the environment from a file");
  }

  public void actionPerformed (ActionEvent e)
  {
    JFileChooser chooser = new JFileChooser();
    if (chooser.showOpenDialog(null) != 0) {
      return;
    }
    File selected = chooser.getSelectedFile();
    try {
      Preferences.importPreferences(new FileInputStream(selected));
      JOptionPane.showMessageDialog(null, "Preferences loaded, you need to restart spiralview.");
    } catch (Exception ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(null, ex.getMessage());
      Log.log.error(ExceptionUtils.getStackTrace(ex));
    }
  }
}