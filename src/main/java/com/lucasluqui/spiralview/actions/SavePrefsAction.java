package com.lucasluqui.spiralview.actions;

/*
 * 		Original author: onyxbits (spiral.onyxbits.de)
 * 		Origin: spiralspy-1.5.jar/de/onyxbits/spiralspy/SavePrefsAction.java
 */

import com.lucasluqui.spiralview.Log;
import com.threerings.ClydeLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

@SuppressWarnings("serial")
public final class SavePrefsAction extends AbstractAction {

  private Preferences cfg = Preferences.userNodeForPackage(ClydeLog.class);
  
  public SavePrefsAction() {
    putValue("Name", "Save");
    putValue("ShortDescription", "Write the environment settings to a file");
  }
  
  public void actionPerformed(ActionEvent e) {
    JFileChooser chooser = new JFileChooser();
    if (chooser.showSaveDialog(null) != 0) {
      return;
    }
    File selected = chooser.getSelectedFile();
    try {
      this.cfg.exportSubtree(new FileOutputStream(selected));
    } catch (Exception ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(null, ex.getMessage());
      Log.log.error(ExceptionUtils.getStackTrace(ex));
    }
  }
}