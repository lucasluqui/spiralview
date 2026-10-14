package com.lucasluqui.silverweave.ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;

/**
 * Covers the area the application window takes while it starts up, so that its parts don't show
 * up one by one.
 */
public class LoadingScreen
{
  /**
   * @param icon the application icon, or null.
   */
  public LoadingScreen (Image icon, String title, String status)
  {
    JPanel panel = new JPanel();
    panel.setBackground(Theme.BACKGROUND);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    panel.add(Box.createVerticalGlue());
    if (icon != null) {
      panel.add(centered(new JLabel(new ImageIcon(
        icon.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH)))));
      panel.add(Box.createVerticalStrut(18));
    }
    JLabel name = new JLabel(title);
    name.setFont(Theme.font(22f, Font.BOLD));
    name.setForeground(Theme.TEXT);
    panel.add(centered(name));
    panel.add(Box.createVerticalStrut(6));
    _status = Theme.muted(status, 12.5f);
    panel.add(centered(_status));
    panel.add(Box.createVerticalStrut(16));
    JProgressBar progress = SlimProgressBarUI.create(Theme.SIDE_ACCENT, 180, true);
    panel.add(centered(progress));
    panel.add(Box.createVerticalGlue());

    // a focusable frame rather than a plain window: Windows only lets an application bring its
    // windows to the front while it holds the foreground, which an unfocused window gives up
    _window = new JFrame(title);
    _window.setUndecorated(true);
    _window.setContentPane(panel);
    _window.setAlwaysOnTop(true);
    if (icon != null) {
      _window.setIconImage(icon);
    }
    // where the application window goes: the screen, minus the task bar
    _window.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
  }

  public void show ()
  {
    _window.setVisible(true);
    _window.toFront();
    // never leave the screen covered if startup fails before taking this down
    dismissAfter(FAILSAFE, null);
  }

  /**
   * Changes the message shown under the title.
   */
  public void setStatus (String status)
  {
    _status.setText(status);
  }

  /**
   * Takes the loading screen down after the given delay, which leaves the application time to
   * draw its first frames, and brings the application window to the front in its place.
   */
  public void dismiss (int delay, Window app)
  {
    if (_dismissing) {
      return;
    }
    _dismissing = true;
    dismissAfter(delay, app);
  }

  protected void dismissAfter (int delay, Window app)
  {
    Timer timer = new Timer(delay, e -> {
      if (!_window.isDisplayable()) {
        return;
      }
      if (app != null) {
        // hand the focus over while we still hold the foreground; briefly pinning the window on
        // top raises it even if Windows refuses to activate it
        app.setAlwaysOnTop(true);
        app.toFront();
        app.requestFocus();
        app.setAlwaysOnTop(false);
      }
      _window.dispose();
    });
    timer.setRepeats(false);
    timer.start();
  }

  protected static JComponent centered (JComponent comp)
  {
    comp.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    return comp;
  }

  protected static final int ICON_SIZE = 96;

  /** The longest the loading screen stays up, in milliseconds. */
  protected static final int FAILSAFE = 120000;

  protected final JFrame _window;
  protected final JLabel _status;
  protected boolean _dismissing;
}
