package com.lucasluqui.spiralview.actions;

import com.lucasluqui.spiralview.Log;
import com.threerings.opengl.GlApp;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class SnapshotAction extends AbstractAction
{
  private final GlApp app;
  private final String appName;

  public SnapshotAction (GlApp app, String appName)
  {
    putValue("Name", "Save Snapshot");
    putValue("ShortDescription", "Dump the contents of the framebuffer to a file");
    putValue("AcceleratorKey", KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
    this.app = app;
    this.appName = appName;
  }

  public void actionPerformed (ActionEvent e)
  {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    File file = new File(FileSystemView.getFileSystemView().getDefaultDirectory(), this.appName + "_" + fmt.format(new Date()) + ".png");

    try {
      ImageIO.write(this.app.createSnapshot(true), "png", file);
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage());
      ex.printStackTrace();
      Log.log.error(ExceptionUtils.getStackTrace(ex));
    }
  }
}
