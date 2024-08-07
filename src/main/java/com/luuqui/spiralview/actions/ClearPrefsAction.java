package com.luuqui.spiralview.actions;

/*
 * 		Original author: onyxbits (spiral.onyxbits.de)
 * 		Origin: spiralspy-1.5.jar/de/onyxbits/spiralspy/ClearPrefsAction.java
 */

import com.luuqui.spiralview.Log;
import com.threerings.ClydeLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

@SuppressWarnings("serial")
public final class ClearPrefsAction extends AbstractAction {

  private Preferences cfg = Preferences.userNodeForPackage(ClydeLog.class);
  
  public ClearPrefsAction() {
    putValue("Name", "Clear");
    putValue("ShortDescription", "Clear preferences settings");
  }
  
  public void actionPerformed(ActionEvent e) {
    try {
      this.cfg.removeNode();
      JOptionPane.showMessageDialog(null, "Preferences cleared, you need to restart spiralview.");
      Log.log.info("Preferences cleared, you need to restart spiralview.");
    } catch (Exception ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(null, ex.getMessage());
      Log.log.error(ExceptionUtils.getStackTrace(ex));
    }
  }
}
