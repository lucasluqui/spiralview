package com.lucasluqui.silverweave.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatClientProperties;

/**
 * Colors, fonts and small helpers shared by the Silverweave interface.
 */
public final class Theme
{
  public static final Color BACKGROUND = new Color(29, 31, 37);
  public static final Color SURFACE = new Color(38, 41, 49);
  public static final Color SURFACE_HOVER = new Color(48, 52, 62);
  public static final Color BORDER = new Color(56, 60, 71);
  public static final Color TEXT = new Color(226, 229, 236);
  public static final Color MUTED = new Color(136, 143, 158);
  public static final Color ACCENT = new Color(125, 170, 255);
  public static final Color SELECTION = new Color(46, 57, 80);
  public static final Color FAVORITE = new Color(240, 196, 84);
  public static final Color DANGER = new Color(235, 96, 96);

  /** The fill of the panels floating over the 3D view, when they can be translucent. */
  public static final Color OVERLAY = new Color(29, 31, 37, 215);

  /** The background color used for the 3D view, as RGB fractions. */
  public static final float[] VIEW_BACKGROUND = { 0.16f, 0.17f, 0.20f };

  // The side panel follows Knight Launcher's look: its slate side pane, darker rounded cards,
  // grey and blue pill buttons and italic secondary text. The overlays keep the colors above.

  /** The side panel's background, Knight Launcher's side pane. */
  public static final Color SIDE_BACKGROUND = new Color(45, 48, 57);

  /** The fill of the cards, rows and fields on the side panel, and of hovered ones. */
  public static final Color SIDE_CARD = new Color(34, 36, 43);
  public static final Color SIDE_CARD_HOVER = new Color(56, 60, 71);

  /** The highlight of selected entries in the side panel's lists. */
  public static final Color SIDE_SELECTION = new Color(67, 72, 86);

  /** The color of the scroll bar thumbs over the view. */
  public static final Color OVERLAY_THUMB = new Color(90, 96, 114);

  /** The fill of the side panel's secondary pill buttons. */
  public static final Color SIDE_BUTTON = new Color(107, 114, 128);

  /** The side panel's main action (the launcher's Play button) and focus color. */
  public static final Color SIDE_ACCENT = new Color(0, 133, 255);

  /** The side panel's color for warnings and things that can't be undone. */
  public static final Color SIDE_DANGER = new Color(251, 91, 91);

  /** The side panel's text: titles, descriptions and the faintest footnotes. */
  public static final Color SIDE_TEXT = Color.WHITE;
  public static final Color SIDE_DESCRIPTION = new Color(169, 173, 188);
  public static final Color SIDE_FOOTNOTE = new Color(90, 96, 114);

  /** The roundness of the side panel's rows and of its larger cards. */
  public static final int SIDE_ARC = 16, SIDE_CARD_ARC = 28;

  /**
   * Returns the base interface font at the given size and style.
   */
  public static Font font (float size, int style)
  {
    Font base = UIManager.getFont("Label.font");
    if (base == null) {
      base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }
    return base.deriveFont(style, size);
  }

  /**
   * Returns a font able to show symbols (crosses, media controls) the interface font lacks;
   * the logical Dialog font falls back to the platform's symbol fonts.
   */
  public static Font symbolFont (float size)
  {
    return new Font(Font.DIALOG, Font.PLAIN, 1).deriveFont(size);
  }

  /**
   * Creates the small, spaced-out header that introduces a group of controls.
   */
  public static JLabel sectionHeader (String text)
  {
    JLabel label = new JLabel(text.toUpperCase());
    label.setFont(font(10.5f, Font.BOLD));
    label.setForeground(MUTED);
    label.setBorder(BorderFactory.createEmptyBorder(14, 4, 6, 4));
    label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    return label;
  }

  /**
   * Creates a label in the secondary text color.
   */
  public static JLabel muted (String text, float size)
  {
    JLabel label = new JLabel(text);
    label.setFont(font(size, Font.PLAIN));
    label.setForeground(MUTED);
    return label;
  }

  /**
   * Returns the side panel's font: Figtree, with its semi-bold cut for bold text, at Knight
   * Launcher's sizes (which run a fifth larger than the size given).
   */
  public static Font sideFont (float size, int style)
  {
    Font base = (style & Font.BOLD) != 0 ? SEMI_BOLD : null;
    if (base == null) {
      return font(size * SIDE_FONT_SCALE, style);
    }
    // the cut is bold already; only italics are left to derive
    return base.deriveFont(style & Font.ITALIC, size * SIDE_FONT_SCALE);
  }

  /**
   * Creates the heading that introduces a group of controls on the side panel.
   */
  public static JLabel sideHeader (String text)
  {
    JLabel label = new JLabel(text);
    label.setFont(sideFont(12f, Font.BOLD));
    label.setForeground(SIDE_TEXT);
    label.setBorder(BorderFactory.createEmptyBorder(16, 4, 8, 4));
    label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    return label;
  }

  /**
   * Creates a line of the side panel's italic secondary text.
   */
  public static JLabel sideNote (String text, float size)
  {
    JLabel label = new JLabel(text);
    label.setFont(sideFont(size, Font.ITALIC));
    label.setForeground(SIDE_DESCRIPTION);
    return label;
  }

  /**
   * Turns a button into one of the side panel's pills.
   *
   * @param fill the pill's color, usually {@link #SIDE_BUTTON} or {@link #SIDE_ACCENT}.
   */
  public static <T extends AbstractButton> T sidePill (T button, Color fill, float size)
  {
    button.setFont(sideFont(size, Font.BOLD));
    button.setBackground(fill);
    button.setForeground(SIDE_TEXT);
    button.setFocusPainted(false);
    button.setFocusable(false);
    button.setBorderPainted(false);
    button.putClientProperty(FlatClientProperties.STYLE, "arc: 999; borderWidth: 0; " +
      "hoverBackground: " + hex(brighter(fill, 18)) + "; pressedBackground: " +
      hex(brighter(fill, -12)) + "; disabledBackground: " + hex(SIDE_CARD_HOVER) +
      "; disabledText: " + hex(SIDE_DESCRIPTION));
    return button;
  }

  /**
   * Gives a text field the side panel's rounded, darker look.
   */
  public static void sideField (JTextField field)
  {
    field.setBackground(SIDE_CARD);
    field.setForeground(SIDE_TEXT);
    field.setCaretColor(SIDE_TEXT);
    field.setMargin(new Insets(4, 12, 4, 8));
    field.putClientProperty(FlatClientProperties.STYLE, "arc: 999; focusWidth: 0; " +
      "innerFocusWidth: 0; borderColor: " + hex(SIDE_CARD) + "; focusedBorderColor: " +
      hex(SIDE_DESCRIPTION) + "; placeholderForeground: " + hex(SIDE_FOOTNOTE));
  }

  /**
   * Gives a scroll pane over the view a rounded thumb alone over the see-through panel: the track
   * is left clear, and the thumb gets colors of its own, since the look-and-feel derives them from
   * the track.
   */
  public static void overlayScroll (JScrollPane scroll)
  {
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.getVerticalScrollBar().setOpaque(false);
    scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
      "track: #00000000; hoverTrackColor: #00000000; pressedTrackColor: #00000000; " +
      "thumb: " + hex(OVERLAY_THUMB) + "; hoverThumbColor: " +
      hex(brighter(OVERLAY_THUMB, 20)) + "; pressedThumbColor: " +
      hex(brighter(OVERLAY_THUMB, 35)) + "; thumbArc: 999; thumbInsets: 1,3,1,1; " +
      "hoverThumbWithTrack: false; pressedThumbWithTrack: false");
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setBorder(BorderFactory.createEmptyBorder());
  }

  /**
   * Gives a scroll pane on the side panel the launcher's slim, rounded scroll bar.
   */
  public static void sideScroll (JScrollPane scroll, Color track)
  {
    scroll.getVerticalScrollBar().setBackground(track);
    scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
      "width: 11; trackArc: 999; thumbArc: 999; trackInsets: 0,5,0,0; thumbInsets: 0,5,0,0; " +
      "track: " + hex(track));
  }

  /**
   * Returns a color in the "#rrggbb" form styles and HTML take.
   */
  public static String hex (Color color)
  {
    return String.format("#%06x", color.getRGB() & 0xFFFFFF);
  }

  /**
   * Returns a color with the given amount added to (or taken from) each component.
   */
  public static Color brighter (Color color, int amount)
  {
    return new Color(clamp(color.getRed() + amount), clamp(color.getGreen() + amount),
      clamp(color.getBlue() + amount), color.getAlpha());
  }

  /**
   * Prepares a graphics context for smooth shape rendering.
   */
  public static Graphics2D smooth (Graphics g)
  {
    Graphics2D g2 = (Graphics2D)g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    return g2;
  }

  protected static int clamp (int value)
  {
    return Math.max(0, Math.min(255, value));
  }

  protected static Font loadFont (String path)
  {
    try (InputStream in = Theme.class.getResourceAsStream(path)) {
      return in == null ? null : Font.createFont(Font.TRUETYPE_FONT, in);
    } catch (FontFormatException | IOException e) {
      return null;
    }
  }

  /** How much larger the side panel's fonts run than the size asked for, as in the launcher. */
  protected static final float SIDE_FONT_SCALE = 1.2f;

  /** Figtree's semi-bold cut, which the side panel uses for bold text, if it could be loaded. */
  protected static final Font SEMI_BOLD = loadFont("/font/Figtree-SemiBold.ttf");

  private Theme () { }
}
