package com.lucasluqui.util;

import com.samskivert.util.Config;

public class BuildConfig
{
  public BuildConfig ()
  {
    // empty.
  }

  public static String getName ()
  {
    return _build.getValue("name", "view");
  }

  public static String getVersion ()
  {
    return _build.getValue("version", "0");
  }

  /** The build config file. */
  protected static Config _build = new Config("build");
}
