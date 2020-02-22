package xyz.lucasallegri.spiralview.actions;

/*
 * 		Original author: onyxbits (spiral.onyxbits.de)
 * 		Origin: spiralspy-1.5.jar/de/onyxbits/spiralspy/SnapShotAction.java
 */

import com.threerings.opengl.GlApp;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

public final class SnapshotAction
  extends AbstractAction
{
  private GlApp app;
  
  public SnapshotAction(GlApp app)
  {
    putValue("Name", "Snapshot");
    putValue("ShortDescription", "Dump the contents of the framebuffer to a file");
    putValue("AcceleratorKey", KeyStroke.getKeyStroke(154, 0));
    this.app = app;
  }
  
  public void actionPerformed(ActionEvent e)
  {
    BufferedImage img = this.app.createSnapshot();
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File("snapshot.png"));
    if (chooser.showSaveDialog(null) != 0) {
      return;
    }
    File selected = chooser.getSelectedFile();
    try
    {
      ImageIO.write(img, "png", selected);
    }
    catch (Exception exp)
    {
      JOptionPane.showMessageDialog(null, exp.getMessage());
      exp.printStackTrace();
    }
  }
}
