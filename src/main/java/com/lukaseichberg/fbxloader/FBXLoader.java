package com.lukaseichberg.fbxloader;

import com.samskivert.io.StreamUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
//import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

import com.google.common.base.Charsets;

public class FBXLoader {

  private static final byte[] FBX_FILE_HEADER = { 0x4B, 0x61, 0x79, 0x64, 0x61, 0x72, 0x61, 0x20, 0x46, 0x42, 0x58, 0x20,
      0x42, 0x69, 0x6E, 0x61, 0x72, 0x79, 0x20, 0x20, 0x00, 0x1A, 0x00 };

  private final ByteBuffer buffer;

  private long version;

  public static FBXFile loadFBXFile(String filePath) throws IOException {
    File file = new File(filePath);
    return loadFBXFile(
        file.getAbsolutePath(), file.getName(), new FileInputStream(file));
  }

  public static FBXFile loadFBXFile (String name, InputStream in) throws IOException {
    return loadFBXFile(null, name, in);
  }

  public static FBXFile loadFBXFile (String path, String name, InputStream in)
      throws IOException
  {
    return new FBXLoader(ByteBuffer.wrap(StreamUtil.toByteArray(in))).load(path, name);
  }

  private FBXLoader (ByteBuffer buffer) {
    this.buffer = buffer;
  }

  private FBXFile load(String path, String name) throws IOException {
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    byte[] header = getBytes(FBX_FILE_HEADER.length);
    if (!Arrays.equals(header, FBX_FILE_HEADER)) {
      throw new IOException("Invalid header");
    }

    version = getUInt();

    FBXNode rootNode = new FBXNode(name, null);
    FBXNode childNode;
    while ((childNode = readNodeRecord(rootNode)) != null) {
      rootNode.add(childNode);
    }
    return new FBXFile(path, (int) version, rootNode);
  }

  private FBXNode readNodeRecord(FBXNode parent) throws IOException {
    long endOffset = getNodeLength();
    if (endOffset == 0) return null;
    long numProperties = getNodeLength();
    /* long propertyListLen = */ getNodeLength();
    String name = getString(getByte());

    FBXNode node = new FBXNode(name, parent);

    for (int i = 0; i < numProperties; i++) {
      FBXDataType dataType = getDataType();
      Object data = getData(dataType);
      FBXProperty property = new FBXProperty(dataType, data, node);
      node.add(property);
    }

    if (buffer.position() < endOffset) {
      int endSize = getRecordSize();
      while (buffer.position() < (endOffset - endSize)) {
        FBXNode child = readNodeRecord(node);
        if (child != null) {
          node.add(child);
        }
      }

      byte[] lastBytes = getBytes(endSize);
      for (byte b : lastBytes) {
        if (b != 0) {
          throw new IOException("Null-Record error");
        }
      }
    }

    if (buffer.position() != endOffset) {
      throw new IOException("EndOffset error");
    }
    return node;
  }

  private Object getData(FBXDataType dataType) throws IOException {
    switch (dataType.category()) {
      case BASIC:
        return getDataBasic(dataType);
      case ARRAY:
        return getDataArray(dataType);
      case SPECIAL:
        return getDataSpecial(dataType);
    }
    throw new IOException("Unknown data type '" + dataType + "'");
  }

  private Object getDataBasic(FBXDataType dataType) throws IOException {
    switch (dataType) {
      case SHORT:
        return buffer.getShort();
      case BOOLEAN:
        return getByte() == 1;
      case INT:
        return buffer.getInt();
      case FLOAT:
        return buffer.getFloat();
      case DOUBLE:
        return buffer.getDouble();
      case LONG:
        return buffer.getLong();
      default:
        throw new IOException("Unknown basic data type '" + dataType + "'");
    }
  }

  private Object getDataSpecial(FBXDataType dataType) throws IOException {
    switch (dataType) {
      case STRING:
        return getString((int) getUInt());
      case RAW:
        return getBytes((int) getUInt());
      default:
        throw new IOException("Unknown special data type '" + dataType + "'");
    }
  }

  private Object getDataArray(FBXDataType dataType) throws IOException {
    int arrayLength = (int) getUInt();
    int encoding = (int) getUInt();
    int compressedLength = (int) getUInt();

    byte[] data = getBytes(compressedLength);

    if (encoding == 1) {
      data = inflate(data);
    }

    if (data.length != arrayLength * dataType.size()) {
      throw new IOException("Array data length error");
    }

    ByteBuffer dataBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

    switch (dataType) {
      case FLOAT_ARRAY:
        float[] floats = new float[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
          floats[i] = dataBuffer.getFloat();
        }
        return floats;

      case DOUBLE_ARRAY:
        double[] doubles = new double[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
          doubles[i] = dataBuffer.getDouble();
        }
        return doubles;

      case LONG_ARRAY:
        long[] longs = new long[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
          longs[i] = dataBuffer.getLong();
        }
        return longs;

      case INT_ARRAY:
        int[] ints = new int[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
          ints[i] = dataBuffer.getInt();
        }
        return ints;

      case BOOLEAN_ARRAY:
        boolean[] bools = new boolean[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
          bools[i] = dataBuffer.get() == 1;
        }
        return bools;

      default:
        throw new IOException("Unknown array data type '" + dataType + "'");
    }
  }

  private byte[] inflate(byte[] data) throws IOException {
    InflaterInputStream gzis = new InflaterInputStream(new ByteArrayInputStream(data));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] newBuffer = new byte[1024];
    while (gzis.available() > 0) {
      int l = gzis.read(newBuffer);
      if (l > 0) {
        out.write(newBuffer, 0, l);
      }
    }
    return out.toByteArray();
  }

  private byte[] getBytes(int length) {
    byte[] data = new byte[length];
    buffer.get(data);
    return data;
  }

  private String getString(int length) {
    return new String(getBytes(length), Charsets.UTF_8);
  }

  private FBXDataType getDataType() throws IOException {
    char dataType = getChar();
    switch (dataType) {
      case 'Y':
        return FBXDataType.SHORT;
      case 'C':
        return FBXDataType.BOOLEAN;
      case 'I':
        return FBXDataType.INT;
      case 'F':
        return FBXDataType.FLOAT;
      case 'D':
        return FBXDataType.DOUBLE;
      case 'L':
        return FBXDataType.LONG;
      case 'f':
        return FBXDataType.FLOAT_ARRAY;
      case 'd':
        return FBXDataType.DOUBLE_ARRAY;
      case 'l':
        return FBXDataType.LONG_ARRAY;
      case 'i':
        return FBXDataType.INT_ARRAY;
      case 'b':
        return FBXDataType.BOOLEAN_ARRAY;
      case 'S':
        return FBXDataType.STRING;
      case 'R':
        return FBXDataType.RAW;
      default:
        throw new IOException("Unknown data type '" + dataType + "'");
    }
  }

  private char getChar() {
    return (char) buffer.get();
  }

  private int getByte() {
    return buffer.get() & 0xFF;
  }

  private long getUInt() {
    return buffer.getInt() & 0x00000000FFFFFFFFl;
  }

  private long getNodeLength () {
    return version >= 7500 ? buffer.getLong() : getUInt();
  }

  private int getRecordSize () {
    return version >= 7500 ? 25 : 13;
  }

}
