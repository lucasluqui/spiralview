package com.lucasluqui.silverweave.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.RenderingHints;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.threerings.media.image.ColorPository;
import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;
import com.threerings.opengl.renderer.config.ColorizationConfig;

import com.lucasluqui.silverweave.item.KnightItem;

import static com.lucasluqui.silverweave.Log.log;

/**
 * Loads colorized item inventory icons in the background, and hands them out at the sizes the
 * interface asks for.
 */
public class IconCache
{
  /**
   * Loads an image from the resources, or returns null.
   */
  public interface ImageLoader
  {
    BufferedImage load (String path)
      throws Exception;
  }

  public IconCache (ImageLoader loader, Supplier<ColorPository> colors)
  {
    _loader = loader;
    _colors = colors;
  }

  /**
   * Returns the icon for an item at the given size. If it isn't loaded yet, a blank placeholder
   * is returned and {@code repaint} is repainted once it is available.
   */
  public Icon get (KnightItem item, int size, Component repaint)
  {
    if (item == null || item.icon == null) {
      return blank(size);
    }
    return fetch(item.configName + '@' + size, () -> load(item.icon, size), size, repaint);
  }

  /**
   * Loads the icons of the given items at the given sizes in the background, behind anything
   * asked for with {@link #get}, so that they're ready by the time lists show them.
   */
  public void preload (Collection<KnightItem> items, int... sizes)
  {
    for (KnightItem item : items) {
      if (item.icon != null) {
        _executor.execute(new Preload(item, sizes));
      }
    }
  }

  /**
   * Returns an image that is a colorization mask, painted with the given colors, at the given
   * size. If it isn't loaded yet, a blank placeholder is returned and {@code repaint} is repainted
   * once it is available.
   *
   * @param colors the colors for the mask's magenta, red and blue areas, in that order.
   */
  public Icon getMask (String path, int size, Color[] colors, Component repaint)
  {
    return fetch(path + '@' + size, () -> {
      try {
        BufferedImage image = _loader.load(path);
        return image == null ? blank(size) : createIcon(paintMask(image, colors), size);
      } catch (Exception | LinkageError e) {
        log.debug("Failed to load icon.", "file", path, "error", e);
        return blank(size);
      }
    }, size, repaint);
  }

  /**
   * Paints a colorization mask: each pixel's magenta, red and blue shares become the same shares
   * of the given colors, so the soft edges between the areas stay soft.
   */
  public static BufferedImage paintMask (BufferedImage mask, Color[] colors)
  {
    BufferedImage result = new BufferedImage(mask.getWidth(), mask.getHeight(),
      BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < mask.getHeight(); y++) {
      for (int x = 0; x < mask.getWidth(); x++) {
        int argb = mask.getRGB(x, y), alpha = argb >>> 24;
        int r = (argb >> 16) & 0xFF, b = argb & 0xFF;
        float magenta = Math.min(r, b), red = r - magenta, blue = b - magenta;
        float total = magenta + red + blue;
        if (alpha == 0 || total == 0) {
          continue;
        }
        float[] shares = { magenta / total, red / total, blue / total };
        float or = 0, og = 0, ob = 0, oa = 0;
        for (int ii = 0; ii < 3; ii++) {
          or += shares[ii] * colors[ii].getRed();
          og += shares[ii] * colors[ii].getGreen();
          ob += shares[ii] * colors[ii].getBlue();
          oa += shares[ii] * colors[ii].getAlpha();
        }
        int a = Math.round(oa * alpha / 255f);
        result.setRGB(x, y, (a << 24) | (Math.round(or) << 16) | (Math.round(og) << 8) |
          Math.round(ob));
      }
    }
    return result;
  }

  /**
   * Returns a cached icon, or a blank one while the loader makes it in the background, after
   * which the components waiting on it are repainted.
   */
  protected Icon fetch (String key, Supplier<Icon> loader, int size, Component repaint)
  {
    Icon icon = _icons.get(key);
    if (icon != null) {
      return icon;
    }
    List<Component> waiting = _waiting.computeIfAbsent(key, k -> new ArrayList<>());
    if (repaint != null && !waiting.contains(repaint)) {
      waiting.add(repaint);
    }
    if (_pending.add(key)) {
      _executor.execute(() -> {
        Icon loaded = loader.get();
        _made.add(key);
        EventQueue.invokeLater(() -> deliver(key, loaded));
      });
    }
    return blank(size);
  }

  /**
   * Stores a loaded icon and repaints the components waiting on it.
   */
  protected void deliver (String key, Icon icon)
  {
    _pending.remove(key);
    _icons.put(key, icon);
    List<Component> targets = _waiting.remove(key);
    if (targets != null) {
      for (Component target : targets) {
        target.repaint();
      }
    }
  }

  /**
   * Loads an item's icon at a few sizes ahead of time. Kept apart from the other loading tasks
   * so that it can be queued behind them.
   */
  protected class Preload
    implements Runnable
  {
    public Preload (KnightItem item, int[] sizes)
    {
      _item = item;
      _sizes = sizes;
    }

    @Override
    public void run ()
    {
      for (int size : _sizes) {
        String key = _item.configName + '@' + size;
        if (_made.add(key)) {
          Icon loaded = load(_item.icon, size);
          EventQueue.invokeLater(() -> deliver(key, loaded));
        }
      }
    }

    protected final KnightItem _item;
    protected final int[] _sizes;
  }

  /**
   * Loads, colorizes and scales an icon. Runs on the loader thread.
   */
  protected Icon load (KnightItem.Icon icon, int size)
  {
    try {
      BufferedImage image = loadColorized(icon);
      return image == null ? blank(size) : createIcon(image, size);
    } catch (Exception | LinkageError e) {
      log.debug("Failed to load icon.", "file", icon.file, "error", e);
      return blank(size);
    }
  }

  /**
   * Returns an icon's colorized image, reusing recent ones for the same item at other sizes. Runs
   * on the loader thread.
   */
  protected BufferedImage loadColorized (KnightItem.Icon icon)
    throws Exception
  {
    BufferedImage image = _sources.get(icon);
    if (image == null) {
      image = colorize(icon, _loader.load(icon.file));
      // (until the colors are in, the image is only a stand-in)
      if (image != null && _colors.get() != null) {
        _sources.put(icon, image);
      }
    }
    return image;
  }

  /**
   * Applies an icon's colorizations to its image.
   */
  protected BufferedImage colorize (KnightItem.Icon icon, BufferedImage image)
  {
    Object configs = icon.getColorizations();
    ColorPository colors = _colors.get();
    if (image == null || !(configs instanceof ColorizationConfig[]) || colors == null) {
      return image;
    }
    List<Colorization> zations = new ArrayList<>();
    for (ColorizationConfig config : (ColorizationConfig[])configs) {
      Colorization zation = config == null ? null : config.getColorization(colors);
      if (zation != null) {
        zations.add(zation);
      }
    }
    return zations.isEmpty() ? image :
      ImageUtil.recolorImage(image, zations.toArray(new Colorization[0]));
  }

  /**
   * Creates a smoothly downscaled icon, with a full-resolution variant for high density screens
   * so that Swing doesn't upscale a small bitmap there.
   */
  protected static Icon createIcon (BufferedImage image, int size)
  {
    int pixels = (int)Math.ceil(size * screenScale());
    BufferedImage dense = scale(image, pixels);
    if (pixels <= size) {
      return new ImageIcon(dense);
    }
    return new ImageIcon(new BaseMultiResolutionImage(scale(dense, size), dense));
  }

  /**
   * Scales an image to fit a square of the given size. Large reductions are done in halving
   * steps, since a single bilinear or bicubic pass skips most source pixels and looks jagged.
   */
  protected static BufferedImage scale (BufferedImage source, int size)
  {
    BufferedImage image = convert(source);
    float ratio = Math.min((float)size / image.getWidth(), (float)size / image.getHeight());
    int width = Math.max(1, Math.round(image.getWidth() * ratio));
    int height = Math.max(1, Math.round(image.getHeight() * ratio));
    while (image.getWidth() / 2 >= width && image.getHeight() / 2 >= height) {
      image = resize(image, image.getWidth() / 2, image.getHeight() / 2,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }
    BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB_PRE);
    Graphics2D g = result.createGraphics();
    applyQualityHints(g, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g.drawImage(image, (size - width) / 2, (size - height) / 2, width, height, null);
    g.dispose();
    return result;
  }

  /**
   * Resizes an image with the same care as the icons: halving steps for large reductions, then a
   * bicubic pass to the exact size.
   */
  public static BufferedImage resizeSmoothly (BufferedImage source, int width, int height)
  {
    BufferedImage image = convert(source);
    while (image.getWidth() / 2 >= width && image.getHeight() / 2 >= height) {
      image = resize(image, image.getWidth() / 2, image.getHeight() / 2,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }
    return resize(image, width, height, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
  }

  protected static BufferedImage resize (BufferedImage image, int width, int height, Object interp)
  {
    BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
    Graphics2D g = result.createGraphics();
    applyQualityHints(g, interp);
    g.drawImage(image, 0, 0, width, height, null);
    g.dispose();
    return result;
  }

  /**
   * Copies an image into premultiplied ARGB, which filters cleanly at transparent edges (indexed
   * and non-premultiplied sources fringe when interpolated).
   */
  protected static BufferedImage convert (BufferedImage image)
  {
    if (image.getType() == BufferedImage.TYPE_INT_ARGB_PRE) {
      return image;
    }
    BufferedImage result = new BufferedImage(
      image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
    Graphics2D g = result.createGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return result;
  }

  protected static void applyQualityHints (Graphics2D g, Object interp)
  {
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interp);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
      RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
  }

  /**
   * Returns the pixel density of the main screen (1 on standard screens, 1.5 at 150% scaling...).
   */
  protected static double screenScale ()
  {
    try {
      return Math.max(1, GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
        .getDefaultConfiguration().getDefaultTransform().getScaleX());
    } catch (HeadlessException e) {
      return 1;
    }
  }

  protected Icon blank (int size)
  {
    return _blanks.computeIfAbsent(size,
      s -> new ImageIcon(new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB)));
  }

  protected final ImageLoader _loader;
  protected final Supplier<ColorPository> _colors;
  protected final Map<String, Icon> _icons = new HashMap<>();
  protected final Map<String, List<Component>> _waiting = new HashMap<>();
  protected final Set<String> _pending = new HashSet<>();
  protected final Map<Integer, Icon> _blanks = new HashMap<>();
  /** The keys of the icons made so far, kept by the loader thread alone. */
  protected final Set<String> _made = new HashSet<>();
  /** The recently colorized icon images, kept by the loader thread alone. */
  protected final Map<KnightItem.Icon, BufferedImage> _sources =
    new LinkedHashMap<>(SOURCE_CACHE_SIZE, 0.75f, true) {
      @Override protected boolean removeEldestEntry (Map.Entry<KnightItem.Icon, BufferedImage> e) {
        return size() > SOURCE_CACHE_SIZE;
      }
    };

  /** Loads the icons one at a time, the most recently asked for first: lists scrolled through
   * quickly ask for many, and the one the user just picked shouldn't wait behind them. Preloads
   * go last, behind everything asked for. */
  protected final ExecutorService _executor = new ThreadPoolExecutor(1, 1, 0L,
    TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>() {
      @Override public boolean offer (Runnable task) {
        return task instanceof Preload ? offerLast(task) : offerFirst(task);
      }
    }, r -> {
      Thread thread = new Thread(r, "Silverweave icons");
      thread.setDaemon(true);
      return thread;
    });

  /** How many colorized icon images are kept for reuse at other sizes. */
  protected static final int SOURCE_CACHE_SIZE = 64;
}
