package com.lucasluqui.silverweave;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;

import static com.lucasluqui.silverweave.Log.log;

/**
 * A level loadouts can be tested in.
 *
 * <p>Islands live in {@code /silverweave/islands/}, one folder each, listed in {@code islands.txt}.
 * A folder holds the level ({@code scene.dat}), its card details ({@code island.properties}, with
 * {@code name} and {@code author}) and optionally a picture for the card ({@code icon.png}).
 */
public final class Island
{
  /** The name of the island's folder. */
  public final String id;

  /** The name shown to the user. */
  public final String name;

  /** Who made the island, or null if unknown. */
  public final String author;

  /** The classpath resource path of the island's level. */
  public final String scene;

  /** The island's picture, or null. */
  public final BufferedImage icon;

  /**
   * Reads the islands listed in the index, skipping any that are broken.
   */
  public static List<Island> loadAll ()
  {
    List<Island> islands = new ArrayList<>();
    try (InputStream index = Island.class.getResourceAsStream(ROOT + "islands.txt")) {
      if (index == null) {
        log.warning("No island index found.");
        return islands;
      }
      BufferedReader reader = new BufferedReader(
        new InputStreamReader(index, StandardCharsets.UTF_8));
      for (String line; (line = reader.readLine()) != null; ) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        Island island = load(line);
        if (island != null) {
          islands.add(island);
        }
      }
    } catch (IOException e) {
      log.warning("Failed to read the island index.", e);
    }
    return Collections.unmodifiableList(islands);
  }

  /**
   * Reads one island, or returns null if it has no level.
   */
  protected static Island load (String id)
  {
    String dir = ROOT + id + "/";
    if (Island.class.getResource(dir + "scene.dat") == null) {
      log.warning("Island has no scene, skipping.", "island", id);
      return null;
    }
    Properties props = new Properties();
    try (InputStream in = Island.class.getResourceAsStream(dir + "island.properties")) {
      if (in != null) {
        props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
      }
    } catch (IOException e) {
      log.warning("Failed to read island details.", "island", id, e);
    }
    BufferedImage icon = null;
    try (InputStream in = Island.class.getResourceAsStream(dir + "icon.png")) {
      if (in != null) {
        icon = ImageIO.read(in);
      }
    } catch (IOException e) {
      log.warning("Failed to read island icon.", "island", id, e);
    }
    String author = props.getProperty("author", "").trim();
    return new Island(id, props.getProperty("name", id).trim(),
      author.isEmpty() ? null : author, dir + "scene.dat", icon);
  }

  protected Island (String id, String name, String author, String scene, BufferedImage icon)
  {
    this.id = id;
    this.name = name;
    this.author = author;
    this.scene = scene;
    this.icon = icon;
  }

  /** Where the islands live on the classpath. */
  protected static final String ROOT = "/silverweave/islands/";
}
