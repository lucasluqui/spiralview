package com.lucasluqui.silverweave.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The places on a knight that can be customized. Accessory slots are named after the
 * {@code ItemCodes.AccessoryLocation} constants.
 */
public enum Slot
{
  WEAPON(Section.WEAPON, "Weapon", false, 0),

  HELMET(Section.HELMET, "Helmet", false, 1),
  HELM_TOP(Section.HELMET, "Top", true, 7),
  HELM_BROW(Section.HELMET, "Brow", true, 8),
  HELM_FRONT(Section.HELMET, "Front", true, 9),
  HELM_BACK(Section.HELMET, "Back", true, 10),
  HELM_SIDE(Section.HELMET, "Side", true, 11),

  ARMOR(Section.ARMOR, "Armor", false, 2),
  ARMOR_FRONT(Section.ARMOR, "Front", true, 12),
  ARMOR_BACK(Section.ARMOR, "Back", true, 13),
  ARMOR_REAR(Section.ARMOR, "Rear", true, 14),
  ARMOR_ARMS(Section.ARMOR, "Arms", true, 15),
  ARMOR_ANKLE(Section.ARMOR, "Ankle", true, 16),
  ARMOR_AURA(Section.ARMOR, "Aura", true, 17),

  SHIELD(Section.SHIELD, "Shield", false, 3),

  COLOR(Section.STYLE, "Personal Color", false, 4),
  EYES(Section.STYLE, "Eyes", false, 5),
  HEIGHT(Section.STYLE, "Height", false, 6);

  /**
   * The groups slots are presented in.
   */
  public enum Section
  {
    HELMET("Helmet"),
    ARMOR("Armor"),
    WEAPON("Weapon"),
    SHIELD("Shield"),
    STYLE("Style");

    public final String label;

    Section (String label)
    {
      this.label = label;
    }

    /**
     * Returns the slots in this section, in display order.
     */
    public List<Slot> slots ()
    {
      List<Slot> slots = new ArrayList<>();
      for (Slot slot : Slot.values()) {
        if (slot.section == this) {
          slots.add(slot);
        }
      }
      return Collections.unmodifiableList(slots);
    }
  }

  public final Section section;
  public final String label;
  public final boolean accessory;

  /** The slot's bit in share codes.
   *  NEVER! change these, or existing codes break. */
  public final int code;

  Slot (Section section, String label, boolean accessory, int code)
  {
    this.section = section;
    this.label = label;
    this.accessory = accessory;
    this.code = code;
  }

  public static Slot forAccessoryLocation (String location)
  {
    try {
      Slot slot = valueOf(location);
      return slot.accessory ? slot : null;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Returns the gear slot whose item carries accessories in this slot.
   */
  public Slot accessoryHost ()
  {
    return !accessory ? null : (section == Section.HELMET ? HELMET : ARMOR);
  }
}
