package xyz.lucasallegri.spiralview.actions;

/*
 * 		Original author: onyxbits (spiral.onyxbits.de)
 * 		Origin: spiralspy-1.5.jar/de/onyxbits/spiralspy/SnapShotAction.java
 */

import com.threerings.ClydeLog;
import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

public final class ClearPrefsAction
  extends AbstractAction
{
  private Preferences cfg = Preferences.userNodeForPackage(ClydeLog.class);
  
  public ClearPrefsAction()
  {
    putValue("Name", "Clear");
    putValue("ShortDescription", "Clear preferences settings");
  }
  
  public void actionPerformed(ActionEvent e)
  {
    try
    {
      this.cfg.removeNode();
      JOptionPane.showMessageDialog(null, "Preferences cleared, you need to restart SpiralSpy.");
    }
    catch (Exception exp)
    {
      exp.printStackTrace();
      JOptionPane.showMessageDialog(null, exp.getMessage());
    }
  }
}
