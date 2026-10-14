package com.lucasluqui.silverweave.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.lucasluqui.silverweave.Island;

/**
 * Lets the user pick the island to test their loadout in. Islands can be marked as favorites with
 * the star on their cards, which lists them first.
 */
public class IslandPicker extends JPanel
{
  /**
   * @param onPick notified with the island the user picks.
   * @param onBack notified if the user goes back without picking.
   */
  public IslandPicker (Consumer<Island> onPick, Runnable onBack)
  {
    super(new BorderLayout());
    _onPick = onPick;
    setOpaque(false);

    JPanel header = new JPanel(new BorderLayout(12, 0));
    header.setOpaque(false);
    header.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
    JButton back = Theme.sidePill(new JButton("‹  Back"), Theme.SIDE_BUTTON, 10f);
    back.setPreferredSize(new Dimension(back.getPreferredSize().width, ItemPicker.BACK_HEIGHT));
    back.addActionListener(e -> onBack.run());
    header.add(back, BorderLayout.WEST);
    JLabel title = new JLabel("Test Loadout");
    title.setFont(Theme.sideFont(13f, Font.BOLD));
    title.setForeground(Theme.SIDE_TEXT);
    header.add(title, BorderLayout.CENTER);

    JPanel top = new JPanel();
    top.setOpaque(false);
    top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
    header.setAlignmentX(LEFT_ALIGNMENT);
    top.add(header);
    JLabel hint = Theme.sideNote("Pick an island to walk around in.", 11f);
    hint.setAlignmentX(LEFT_ALIGNMENT);
    hint.setBorder(BorderFactory.createEmptyBorder(14, 4, 0, 4));
    top.add(hint);
    top.add(Theme.sideHeader("Islands"));
    add(top, BorderLayout.NORTH);

    _cards = new JPanel();
    _cards.setOpaque(false);
    _cards.setLayout(new BoxLayout(_cards, BoxLayout.Y_AXIS));
    JPanel holder = new JPanel(new BorderLayout());
    holder.setOpaque(false);
    holder.add(_cards, BorderLayout.NORTH);
    add(holder, BorderLayout.CENTER);
  }

  public void setIslands (List<Island> islands)
  {
    _islands = islands;
    layoutCards();
  }

  /**
   * Lays out the island cards, favorites first, otherwise in the order given.
   */
  protected void layoutCards ()
  {
    List<Island> islands = new ArrayList<>();
    for (boolean favorite : new boolean[] { true, false }) {
      for (Island island : _islands) {
        if (isFavorite(island) == favorite) {
          islands.add(island);
        }
      }
    }
    _cards.removeAll();
    if (islands.isEmpty()) {
      JLabel none = Theme.sideNote("No islands are available.", 10.5f);
      none.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
      none.setAlignmentX(LEFT_ALIGNMENT);
      _cards.add(none);
    }
    for (Island island : islands) {
      _cards.add(new Card(island));
      _cards.add(Box.createVerticalStrut(8));
    }
    _cards.revalidate();
    _cards.repaint();
  }

  protected static boolean isFavorite (Island island)
  {
    return FAVORITES.get(island.id, null) != null;
  }

  /**
   * Adds an island to the favorites, or removes it if it is one already, and lists the cards
   * again in their new order.
   */
  protected void toggleFavorite (Island island)
  {
    if (isFavorite(island)) {
      FAVORITES.remove(island.id);
    } else {
      FAVORITES.put(island.id, "");
    }
    layoutCards();
  }

  /**
   * An island's card: its name and author over its picture, which fades into the card.
   */
  protected class Card extends JComponent
  {
    public Card (Island island)
    {
      _island = island;
      setAlignmentX(LEFT_ALIGNMENT);
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      // clicks on the star toggle the favorite rather than picking the island
      Clickable.install(this, () -> {
        if (_starPressed) {
          toggleFavorite(island);
        } else {
          _onPick.accept(island);
        }
      }, null);
      MouseAdapter mouse = new MouseAdapter() {
        @Override public void mousePressed (MouseEvent e) {
          _starPressed = getStarBounds().contains(e.getPoint());
        }
        @Override public void mouseEntered (MouseEvent e) {
          setHover(true);
          setStarHover(getStarBounds().contains(e.getPoint()));
        }
        @Override public void mouseExited (MouseEvent e) {
          setHover(false);
          setStarHover(false);
        }
        @Override public void mouseMoved (MouseEvent e) {
          setStarHover(getStarBounds().contains(e.getPoint()));
        }
      };
      addMouseListener(mouse);
      addMouseMotionListener(mouse);
      // (the tips come from getToolTipText(MouseEvent), once registered)
      setToolTipText("");
    }

    @Override
    public String getToolTipText (MouseEvent event)
    {
      if (!getStarBounds().contains(event.getPoint())) {
        return null;
      }
      return isFavorite(_island) ? "Remove from favorites" : "Add to favorites";
    }

    /**
     * Returns the clickable area around the favorite star, in the card's top right corner.
     */
    protected Rectangle getStarBounds ()
    {
      return new Rectangle(getWidth() - STAR_AREA, 0, STAR_AREA, STAR_AREA);
    }

    protected void setStarHover (boolean hover)
    {
      if (hover != _starHover) {
        _starHover = hover;
        repaint();
      }
    }

    @Override
    public Dimension getPreferredSize ()
    {
      return new Dimension(10, CARD_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize ()
    {
      return new Dimension(Integer.MAX_VALUE, CARD_HEIGHT);
    }

    protected void setHover (boolean hover)
    {
      if (hover == _hover) {
        return;
      }
      _hover = hover;
      if (!_fade.isRunning()) {
        _lastStep = System.nanoTime();
        _fade.start();
      }
      // an exit can go unreported (say, when another window pops up under the mouse), so keep
      // an eye on where the mouse really is while hovered
      if (hover) {
        _watch.start();
      } else {
        _watch.stop();
      }
    }

    /**
     * Moves the hover animation along by the time since its last step, stopping once it's there.
     */
    protected void step ()
    {
      long now = System.nanoTime();
      float delta = (now - _lastStep) / 1e6f / FADE_DURATION;
      _lastStep = now;
      _progress = _hover ? Math.min(1f, _progress + delta) : Math.max(0f, _progress - delta);
      if (_progress == (_hover ? 1f : 0f)) {
        _fade.stop();
      }
      repaint();
    }

    protected boolean isMouseOver ()
    {
      PointerInfo pointer = isShowing() ? MouseInfo.getPointerInfo() : null;
      if (pointer == null) {
        return false;
      }
      Point point = pointer.getLocation();
      SwingUtilities.convertPointFromScreen(point, this);
      return contains(point);
    }

    @Override
    protected void paintComponent (Graphics g)
    {
      Graphics2D g2 = Theme.smooth(g);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int width = getWidth(), height = getHeight();
      // ease in and out of the hover state
      float hover = _progress * _progress * (3f - 2f * _progress);
      Color base = blend(Theme.SIDE_CARD, Theme.SIDE_CARD_HOVER, hover);

      // the background and picture are put together off screen, where nothing needs a shaped
      // clip (which is slow to draw through), and then copied over pixel for pixel
      double density = g2.getTransform().getScaleX();
      BufferedImage frame = paintFrame(width, height, density, hover, base);
      Graphics2D blit = (Graphics2D)g.create();
      blit.scale(1 / density, 1 / density);
      blit.drawImage(frame, 0, 0, null);
      blit.dispose();

      if (hover > 0f) {
        g2.setColor(withAlpha(Theme.SIDE_ACCENT, Math.round(255 * hover)));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, RADIUS, RADIUS));
      }

      int x = 18;
      Font nameFont = Theme.sideFont(14f, Font.BOLD), authorFont = Theme.sideFont(10f, Font.ITALIC);
      FontMetrics nameMetrics = g2.getFontMetrics(nameFont);
      FontMetrics authorMetrics = g2.getFontMetrics(authorFont);
      int textHeight = nameMetrics.getHeight() +
        (_island.author == null ? 0 : 4 + authorMetrics.getHeight());
      int y = (height - textHeight) / 2;
      // the name casts a soft shadow, like the titles of the launcher's cards
      g2.setFont(nameFont);
      g2.setColor(new Color(0, 0, 0, 90));
      g2.drawString(_island.name, x + 1, y + nameMetrics.getAscent() + 2);
      g2.setColor(Theme.SIDE_TEXT);
      g2.drawString(_island.name, x, y + nameMetrics.getAscent());
      if (_island.author != null) {
        g2.setFont(authorFont);
        g2.setColor(Theme.SIDE_DESCRIPTION);
        g2.drawString("By " + _island.author, x,
          y + nameMetrics.getHeight() + 4 + authorMetrics.getAscent());
      }

      // the favorite star: filled for favorites, outlined while the card is hovered, on a dark
      // disc that keeps it clear of the picture
      boolean favorite = isFavorite(_island);
      if (favorite || hover > 0f) {
        float alpha = favorite ? 1f : hover;
        Rectangle star = getStarBounds();
        g2.translate(star.getCenterX(), star.getCenterY());
        g2.setColor(new Color(0, 0, 0, Math.round(90 * alpha)));
        g2.fillOval(-STAR_DISC / 2, -STAR_DISC / 2, STAR_DISC, STAR_DISC);
        if (favorite) {
          g2.setColor(Theme.FAVORITE);
          Glyph.STAR_FILLED.paint(g2);
        } else {
          g2.setColor(withAlpha(_starHover ? Theme.SIDE_TEXT : Theme.SIDE_DESCRIPTION,
            Math.round(255 * alpha)));
          g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
          Glyph.STAR.paint(g2);
        }
      }
      g2.dispose();
    }

    /**
     * Paints the card's background and picture into the reused off-screen frame, in device
     * pixels, and returns it.
     */
    protected BufferedImage paintFrame (int width, int height, double density, float hover,
      Color base)
    {
      int pixelWidth = (int)Math.ceil(width * density);
      int pixelHeight = (int)Math.ceil(height * density);
      if (_frame == null || _frame.getWidth() != pixelWidth ||
          _frame.getHeight() != pixelHeight) {
        _frame = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB_PRE);
        _mask = createMask(pixelWidth, pixelHeight, density);
      }
      Graphics2D g = _frame.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.setComposite(AlphaComposite.Src);
      g.setColor(base);
      g.fillRect(0, 0, pixelWidth, pixelHeight);
      g.setComposite(AlphaComposite.SrcOver);
      g.scale(density, density);

      // the picture fills the right of the card and fades into it towards the text; it's kept to
      // its own area, so the fade starts from the card's color right at that area's edge
      int left = width * 2 / 5;
      g.clipRect(left, 0, width - left, height);
      if (_island.icon != null) {
        BufferedImage picture = getPicture(width - left, height, density);
        // drawn at fractional positions so that it grows smoothly, about its center
        double zoom = (1 + HOVER_ZOOM * hover) / ((1 + HOVER_ZOOM) * density);
        AffineTransform transform = AffineTransform.getTranslateInstance(
          left + (width - left) / 2.0, height / 2.0);
        transform.scale(zoom, zoom);
        transform.translate(-picture.getWidth() / 2.0, -picture.getHeight() / 2.0);
        g.drawImage(picture, transform, null);
      } else {
        // no picture: a soft glow in the accent color stands in
        float radius = height * 1.2f;
        g.setPaint(new RadialGradientPaint(width - height * 0.6f, height / 2f, radius,
          new float[] { 0f, 1f }, new Color[] { withAlpha(Theme.SIDE_ACCENT, 70), withAlpha(base, 0) }));
        g.fillRect(left, 0, width - left, height);
      }
      g.setPaint(new GradientPaint(left, 0, base, width, 0, withAlpha(base, 40)));
      g.fillRect(left, 0, width - left, height);

      // round the corners of everything at once
      g.setClip(null);
      g.setTransform(new AffineTransform());
      g.setComposite(AlphaComposite.DstIn);
      g.drawImage(_mask, 0, 0, null);
      g.dispose();
      return _frame;
    }

    /**
     * Creates the card's shape as an alpha mask, in device pixels.
     */
    protected BufferedImage createMask (int pixelWidth, int pixelHeight, double density)
    {
      BufferedImage mask = new BufferedImage(pixelWidth, pixelHeight,
        BufferedImage.TYPE_INT_ARGB_PRE);
      Graphics2D g = mask.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(Color.WHITE);
      g.fill(new RoundRectangle2D.Double(0, 0, pixelWidth, pixelHeight, RADIUS * density,
        RADIUS * density));
      g.dispose();
      return mask;
    }

    /**
     * Returns the island's picture scaled to cover an area at its fully hovered size, in device
     * pixels. Scaling the full picture on every frame of the animation is too slow to be smooth,
     * so it's done once per size.
     */
    protected BufferedImage getPicture (int width, int height, double density)
    {
      if (_picture == null || _pictureWidth != width || _pictureHeight != height ||
          _pictureDensity != density) {
        BufferedImage icon = _island.icon;
        double scale = Math.max((double)width / icon.getWidth(), (double)height / icon.getHeight())
          * density * (1 + HOVER_ZOOM);
        _picture = IconCache.resizeSmoothly(icon, (int)Math.ceil(icon.getWidth() * scale),
          (int)Math.ceil(icon.getHeight() * scale));
        _pictureWidth = width;
        _pictureHeight = height;
        _pictureDensity = density;
      }
      return _picture;
    }

    protected final Island _island;
    protected final Timer _fade = new Timer(FRAME_INTERVAL, e -> step());
    protected final Timer _watch = new Timer(WATCH_INTERVAL, e -> {
      if (!isMouseOver()) {
        setHover(false);
      }
    });
    protected boolean _hover;

    /** Whether the mouse is over the favorite star, and whether the last press was on it. */
    protected boolean _starHover, _starPressed;

    /** How far the card is into its hover state, from 0 to 1. */
    protected float _progress;

    /** When the hover animation last stepped, in nanoseconds. */
    protected long _lastStep;

    /** The off-screen frame the card is put together in, and the card's shape as a mask. */
    protected BufferedImage _frame, _mask;

    /** The picture scaled for the card, and the size and density it was scaled for. */
    protected BufferedImage _picture;
    protected int _pictureWidth, _pictureHeight;
    protected double _pictureDensity;
  }

  protected static Color blend (Color from, Color to, float amount)
  {
    return new Color(
      Math.round(from.getRed() + (to.getRed() - from.getRed()) * amount),
      Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
      Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * amount));
  }

  protected static Color withAlpha (Color color, int alpha)
  {
    return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
  }

  /** The height and corner rounding of the island cards. */
  protected static final int CARD_HEIGHT = 96, RADIUS = Theme.SIDE_CARD_ARC;

  /** The size of the clickable area around a card's favorite star, and of the disc behind it. */
  protected static final int STAR_AREA = 36, STAR_DISC = 24;

  /** The favorite islands, by folder name (one key each). */
  protected static final Preferences FAVORITES =
    Preferences.userNodeForPackage(Island.class).node("favoriteIslands");

  /** How long the hover animation takes, in milliseconds. */
  protected static final float FADE_DURATION = 180f;

  /** How much the picture grows while hovered. */
  protected static final float HOVER_ZOOM = 0.05f;

  /** How often the hover animation steps, and how often the mouse is checked on while a card is
   * hovered, in milliseconds. */
  protected static final int FRAME_INTERVAL = 1000 / 60, WATCH_INTERVAL = 150;

  protected final Consumer<Island> _onPick;
  protected final JPanel _cards;

  /** The islands to choose from, in the order given. */
  protected List<Island> _islands = new ArrayList<>();
}
