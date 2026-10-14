package com.lucasluqui.silverweave.ui;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

/**
 * Makes a custom component respond to clicks reliably. Swing's {@code mouseClicked} is dropped
 * whenever the mouse moves even slightly between press and release, which makes custom controls
 * feel like they ignore clicks; this treats any left press and release inside the component as a
 * click instead.
 */
public final class Clickable
{
  /**
   * Installs click and hover handling on a component.
   *
   * @param hover notified when the mouse enters or leaves the component; may be null.
   */
  public static void install (Component component, Runnable onClick, Consumer<Boolean> hover)
  {
    MouseAdapter adapter = new MouseAdapter() {
      @Override public void mousePressed (MouseEvent e) {
        _armed = SwingUtilities.isLeftMouseButton(e);
        if (_armed && component.isFocusable()) {
          component.requestFocusInWindow();
        }
      }
      @Override public void mouseReleased (MouseEvent e) {
        boolean click = _armed && SwingUtilities.isLeftMouseButton(e) && component.contains(e.getPoint());
        _armed = false;
        if (click) {
          onClick.run();
        }
      }
      @Override public void mouseEntered (MouseEvent e) {
        if (hover != null) {
          hover.accept(true);
        }
      }
      @Override public void mouseExited (MouseEvent e) {
        if (hover != null) {
          hover.accept(component.contains(e.getPoint()));
        }
      }
      protected boolean _armed;
    };
    component.addMouseListener(adapter);
  }

  private Clickable () { }
}
