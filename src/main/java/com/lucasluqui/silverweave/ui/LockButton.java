package com.lucasluqui.silverweave.ui;

import javax.swing.JButton;

/**
 * A padlock that locks or unlocks a slot.
 */
public class LockButton extends JButton
{
  public LockButton (Runnable onClick)
  {
    putClientProperty("JButton.buttonType", "borderless");
    setFocusable(false);
    addActionListener(e -> onClick.run());
    setLocked(false);
  }

  public void setLocked (boolean locked)
  {
    Glyph glyph = locked ? Glyph.LOCKED : Glyph.UNLOCKED;
    setIcon(glyph.icon(ICON_SIZE, locked ? Theme.SIDE_ACCENT : Theme.SIDE_FOOTNOTE));
    setRolloverIcon(glyph.icon(ICON_SIZE, locked ? Theme.SIDE_ACCENT : Theme.SIDE_DESCRIPTION));
    setToolTipText(locked ? "Locked: Click to Unlock (re-enables for randomize)" :
      "Unlocked: Click to Lock (prevents from changing on randomize)");
  }

  protected static final int ICON_SIZE = 14;
}
