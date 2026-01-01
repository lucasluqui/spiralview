package com.lucasluqui.spiralview;

import com.samskivert.util.Config;

public class BuildConfig
{
  protected static Config _build = new Config("build");

  public BuildConfig ()
  {
    // empty.
  }

  public static String getVersion ()
  {
    return _build.getValue("version", "0");
  }
}
