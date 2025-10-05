package com.lucasluqui.spiralview.actions;

// Original author: onyxbits (spiral.onyxbits.de)
// File: spiralspy-1.5.jar/de/onyxbits/spiralspy/SnapShotPrefsAction.java

import com.lucasluqui.spiralview.Log;
import com.threerings.opengl.GlApp;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

public final class SnapshotToAction extends AbstractAction
{
  private final GlApp app;
  private final String appName;
  private String lastSavedPath = null;

  public SnapshotToAction (GlApp app, String appName)
  {
    putValue("Name", "Save Snapshot To...");
    putValue("ShortDescription", "Dump the contents of the framebuffer to a file in a specified path");
    putValue("AcceleratorKey", KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.CTRL_DOWN_MASK));
    this.app = app;
    this.appName = appName;
  }

  public void actionPerformed (ActionEvent e)
  {
    BufferedImage img = this.app.createSnapshot(true);
    JFileChooser chooser = new JFileChooser();
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    // Check if we've saved a snapshot before within this session
    // If we did, point the file chooser there.
    if (lastSavedPath != null) {
      chooser.setCurrentDirectory(new File(lastSavedPath));
    }
    chooser.setSelectedFile(new File(appName + "_" + fmt.format(new Date()) + ".png"));

    if (chooser.showSaveDialog(null) != 0) {
      return;
    }

    File selected = chooser.getSelectedFile();
    try {
      // Save the snapshot.
      ImageIO.write(img, "png", selected);

      // Store the path where this snapshot was saved if this is the first time saving
      // or the path differs from last one.
      String newPath = selected.getParentFile().getAbsolutePath();
      if (lastSavedPath == null || !lastSavedPath.equalsIgnoreCase(newPath)) {
        lastSavedPath = newPath;
      }
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage());
      ex.printStackTrace();
      Log.log.error(ExceptionUtils.getStackTrace(ex));
    }
  }
}
