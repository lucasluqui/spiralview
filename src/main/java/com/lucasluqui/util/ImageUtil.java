package com.lucasluqui.util;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static com.lucasluqui.util.Log.log;

public class ImageUtil
{
  public static BufferedImage loadImageWithinJar (String fileName)
  {
    BufferedImage buff = null;
    try {
      buff = javax.imageio.ImageIO.read(ImageUtil.class.getResourceAsStream(fileName));
    } catch (IOException e) {
      log.error(e);
      return null;
    }
    return buff;
  }
}
