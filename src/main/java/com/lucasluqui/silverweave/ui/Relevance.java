package com.lucasluqui.silverweave.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Ranks names against a search query, so that the closest matches can be listed first. Every
 * term of the query has to turn up somewhere for a name to match at all; how well it matches then
 * puts it in one of a few tiers, best first:
 *
 * <ol>
 * <li>the name is the query;
 * <li>the name starts with the query;
 * <li>the query appears in the name as a phrase, from the start of a word;
 * <li>each term starts a word of its own in the name, in any order;
 * <li>each term appears somewhere in the name;
 * <li>the terms are only all found counting the extra text (the item's config name, say).
 * </ol>
 */
public final class Relevance
{
  /** The number of tiers a match can fall in. */
  public static final int TIERS = 6;

  /**
   * Prepares a query typed by the user for ranking: lowercased, with runs of whitespace (and any
   * at the ends) collapsed.
   */
  public static String normalize (String query)
  {
    return query.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }

  /**
   * Ranks a name against a query.
   *
   * @param extra further text the terms may be found in, at the lowest tier; may be null.
   * @param query a non-empty query, as given by {@link #normalize}.
   * @return the tier the name falls in, 0 being the best, or -1 if it doesn't match.
   */
  public static int rank (String name, String extra, String query)
  {
    String text = normalize(name);
    if (text.equals(query)) {
      return 0;
    }
    if (text.startsWith(query)) {
      return 1;
    }
    if (startsWord(text, query)) {
      return 2;
    }
    String[] terms = query.split(" ");
    if (startWords(text, terms)) {
      return 3;
    }
    if (containsAll(text, terms)) {
      return 4;
    }
    if (extra != null && containsAll(text + " " + extra.toLowerCase(Locale.ROOT), terms)) {
      return 5;
    }
    return -1;
  }

  /**
   * Returns an empty list for each tier, to sort matches into.
   */
  public static <T> List<List<T>> tiers ()
  {
    List<List<T>> tiers = new ArrayList<>(TIERS);
    for (int ii = 0; ii < TIERS; ii++) {
      tiers.add(new ArrayList<>());
    }
    return tiers;
  }

  /**
   * Checks whether the query appears in the text at the start of a word.
   */
  protected static boolean startsWord (String text, String query)
  {
    for (int idx = text.indexOf(query); idx >= 0; idx = text.indexOf(query, idx + 1)) {
      if (idx == 0 || !Character.isLetterOrDigit(text.charAt(idx - 1))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks whether each term starts a different word of the text, so a term given twice needs two
   * words. The longest terms claim their words first: two terms can only want the same word if one
   * starts the other, and the shorter one is then the easier to place elsewhere.
   */
  protected static boolean startWords (String text, String[] terms)
  {
    String[] words = text.split("[^\\p{L}\\p{N}]+");
    boolean[] used = new boolean[words.length];
    String[] sorted = terms.clone();
    Arrays.sort(sorted, Comparator.comparingInt(String::length).reversed());
    outer: for (String term : sorted) {
      for (int ii = 0; ii < words.length; ii++) {
        if (!used[ii] && words[ii].startsWith(term)) {
          used[ii] = true;
          continue outer;
        }
      }
      return false;
    }
    return true;
  }

  /**
   * Checks whether each of the terms appears somewhere in the text.
   */
  protected static boolean containsAll (String text, String[] terms)
  {
    for (String term : terms) {
      if (!text.contains(term)) {
        return false;
      }
    }
    return true;
  }

  private Relevance () { }
}
