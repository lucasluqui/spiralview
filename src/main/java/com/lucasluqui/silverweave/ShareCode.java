package com.lucasluqui.silverweave;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.zip.CRC32;

import com.lucasluqui.silverweave.item.ItemCatalog;
import com.lucasluqui.silverweave.item.KnightItem;
import com.lucasluqui.silverweave.item.Slot;

/**
 * Short, shareable codes describing a knight's look.
 *
 * <p>A code is "SW-" followed by URL-safe base64 of:
 * <p>v1:
 * <pre>
 *   version (1 byte) | flags (1 byte) | slot mask (3 bytes) | config id (4 bytes) per set slot,
 *   in slot code order | checksum (2 bytes, low half of the CRC-32 of everything before it)
 * </pre>
 * <p>v2:
 * <pre>
 *   version (1 byte) | flags (1 byte) | slot mask (3 bytes) | config id (4 bytes) per set slot,
 *   in slot code order | recolor mask (3 bytes) | color id (2 bytes) per
 *   recolored slot, in slot code order | checksum (2 bytes, low half of the CRC-32 of everything
 *   before it)
 * </pre>
 * Items are identified by their config ids, which stay stable across updates, so codes stay valid
 * as long as the items exist.
 */
public final class ShareCode
{
  public static final String PREFIX = "SW-";

  /**
   * A decoded share code.
   */
  public static final class Knight
  {
    /** The items, by slot. Slots whose items don't exist (anymore) are left out. */
    public final Map<Slot, KnightItem> items = new EnumMap<>(Slot.class);

    public boolean shieldAway;

    /** How many items in the code couldn't be found in the catalog. */
    public int missing;
  }

  /**
   * Thrown for strings that aren't valid share codes.
   */
  public static final class InvalidCodeException extends Exception
  {
    public InvalidCodeException (String message)
    {
      super(message);
    }
  }

  public static String encode (Map<Slot, KnightItem> items, boolean shieldAway)
  {
    int mask = 0;
    for (Slot slot : items.keySet()) {
      mask |= 1 << slot.code;
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int recolors = 0;
    for (Map.Entry<Slot, KnightItem> entry : items.entrySet()) {
      if (entry.getValue().isCustom()) {
        recolors |= 1 << entry.getKey().code;
      }
    }
    out.write(recolors == 0 ? VERSION : RECOLOR_VERSION);
    out.write(shieldAway ? FLAG_SHIELD_AWAY : 0);
    writeMask(out, mask);
    for (Slot slot : byCode()) {
      if (slot != null && items.containsKey(slot)) {
        out.writeBytes(ByteBuffer.allocate(4).putInt(items.get(slot).configId).array());
      }
    }
    if (recolors != 0) {
      writeMask(out, recolors);
      for (Slot slot : byCode()) {
        if (slot != null && (recolors & (1 << slot.code)) != 0) {
          int color = items.get(slot).getRecolor();
          out.write(color >>> 8);
          out.write(color);
        }
      }
    }
    byte[] body = out.toByteArray();
    int crc = checksum(body, body.length);
    out.write(crc >>> 8);
    out.write(crc);
    return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
  }

  /**
   * Decodes a share code. Whitespace and a missing prefix are tolerated, since codes get pasted
   * from all sorts of places.
   */
  public static Knight decode (String code, ItemCatalog catalog)
    throws InvalidCodeException
  {
    String text = code == null ? "" : code.replaceAll("\\s+", "");
    if (text.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
      text = text.substring(PREFIX.length());
    }
    byte[] data;
    try {
      data = Base64.getUrlDecoder().decode(text);
    } catch (IllegalArgumentException e) {
      throw new InvalidCodeException("This isn't a Silverweave Share Code.");
    }
    if (data.length < 7) {
      throw new InvalidCodeException("This Share Code is incomplete.");
    }
    int crc = ((data[data.length - 2] & 0xFF) << 8) | (data[data.length - 1] & 0xFF);
    if (crc != checksum(data, data.length - 2)) {
      throw new InvalidCodeException("This Share Code is damaged; check that it was copied whole.");
    }
    if (data[0] != VERSION && data[0] != RECOLOR_VERSION) {
      throw new InvalidCodeException("This Share Code was made by a newer version of Silverweave.");
    }
    ByteBuffer buf = ByteBuffer.wrap(data, 0, data.length - 2);
    Knight knight = new Knight();
    knight.shieldAway = (buf.get(1) & FLAG_SHIELD_AWAY) != 0;
    buf.position(2);
    int mask = readMask(buf);
    Slot[] slots = byCode();
    for (int bit = 0; bit < 24; bit++) {
      if ((mask & (1 << bit)) == 0) {
        continue;
      }
      if (buf.remaining() < 4) {
        throw new InvalidCodeException("This Share Code is incomplete.");
      }
      int id = buf.getInt();
      Slot slot = bit < slots.length ? slots[bit] : null;
      KnightItem item = catalog.getItem(id);
      if (slot != null && item != null && item.slot == slot) {
        knight.items.put(slot, item);
      } else {
        knight.missing++;
      }
    }
    if (data[0] == RECOLOR_VERSION) {
      if (buf.remaining() < 3) {
        throw new InvalidCodeException("This Share Code is incomplete.");
      }
      int recolors = readMask(buf);
      for (int bit = 0; bit < 24; bit++) {
        if ((recolors & (1 << bit)) == 0) {
          continue;
        }
        if (buf.remaining() < 2) {
          throw new InvalidCodeException("This Share Code is incomplete.");
        }
        int color = buf.getShort() & 0xFFFF;
        Slot slot = bit < slots.length ? slots[bit] : null;
        // (skipping the colors of the items that went missing)
        KnightItem item = slot == null ? null : knight.items.get(slot);
        if (item != null) {
          knight.items.put(slot, item.withRecolor(color));
        }
      }
    }
    return knight;
  }

  protected static void writeMask (ByteArrayOutputStream out, int mask)
  {
    out.write(mask >>> 16);
    out.write(mask >>> 8);
    out.write(mask);
  }

  protected static int readMask (ByteBuffer buf)
  {
    return ((buf.get() & 0xFF) << 16) | ((buf.get() & 0xFF) << 8) | (buf.get() & 0xFF);
  }

  /**
   * Returns the slots indexed by their share code bit.
   */
  protected static Slot[] byCode ()
  {
    Slot[] slots = new Slot[24];
    for (Slot slot : Slot.values()) {
      slots[slot.code] = slot;
    }
    return slots;
  }

  protected static int checksum (byte[] data, int length)
  {
    CRC32 crc = new CRC32();
    crc.update(data, 0, length);
    return (int)(crc.getValue() & 0xFFFF);
  }

  protected static final byte VERSION = 1;

  /** The version of the codes for knights wearing custom items, which carry their colors. */
  protected static final byte RECOLOR_VERSION = 2;

  protected static final int FLAG_SHIELD_AWAY = 1;

  private ShareCode () { }
}
