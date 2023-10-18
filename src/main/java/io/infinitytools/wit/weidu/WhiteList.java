/*
 * Copyright (c) 2023 Argent77
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.infinitytools.wit.weidu;

import io.infinitytools.wit.utils.SystemInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Provides a whitelist of known WeiDU binaries.
 */
public class WhiteList {
  /**
   * Provides additional information about a WeiDU binary.
   *
   * @param version WeiDU version.
   * @param os      OS compatibility (Windows, Linux, macOS).
   * @param arch    Architecture compatibility (x86, x86_64).
   * @param variant A serial number that is used to distinguish between different variants for a specific platform,
   *                starting at 0.
   */
  public record Binary(int version, SystemInfo.Platform os, SystemInfo.Architecture arch, int variant) {
  }

  private static final String HASH_ALGORITHM = "SHA-256";
  private static final String JSON_FILE = "whitelist.json";

  private static final String KEY_VERSION = "version";
  private static final String KEY_OS = "os";
  private static final String KEY_ARCH = "arch";
  private static final String KEY_VARIANT = "variant";
  private static final String KEY_SHA256 = "sha256";

  private static final HashMap<String, Binary> BINARY_HASHES = new HashMap<>();

  /**
   * Determines whether the specified file {@link Path} refers to a white-listed WeiDU binary.
   *
   * @param file File {@link Path} to check.
   * @return {@code true} if the file matches a white-listed WeiDU binary, {@code false} otherwise.
   */
  public static boolean matches(Path file) {
    return find(file) != null;
  }

  /**
   * Determines whether the specified {@link InputStream} data matches a white-listed WeiDU binary.
   *
   * @param is {@link InputStream} data to check.
   * @return {@code true} if the data matches a white-listed WeiDU binary, {@code false} otherwise.
   */
  public static boolean matches(InputStream is) {
    return find(is) != null;
  }

  /**
   * Determines whether the data of the specified byte array matches a white-listed WeiDU binary.
   *
   * @param data Byte array with data to check.
   * @return {@code true} if the data matches a white-listed WeiDU binary, {@code false} otherwise.
   */
  public static boolean matches(byte[] data) {
    return find(data) != null;
  }

  /**
   * Returns the {@link Binary} instance that matches the specified file.
   *
   * @param file File {@link Path} to check.
   * @return {@link Binary} instance that matches the given argument, {@code null} otherwise.
   */
  public static Binary find(Path file) {
    if (file != null) {
      try (final ByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
        final String hash = generateHash(channel);
        Logger.debug("Hashing file (file: {}, hash: {})", file, hash);
        if (hash != null) {
          return getHashMap().get(hash);
        }
      } catch (IOException e) {
        Logger.debug(e, "Reading from file: {}", file);
      }
    }
    return null;
  }

  /**
   * Returns the {@link Binary} instance that matches the data of the specified input stream.
   *
   * @param is {@link InputStream} to check.
   * @return {@link Binary} instance that matches the given argument, {@code null} otherwise.
   */
  public static Binary find(InputStream is) {
    if (is != null) {
      final String hash = generateHash(is);
      if (hash != null) {
        return getHashMap().get(hash);
      }
    }
    return null;
  }

  /**
   * Returns the {@link Binary} instance that matches the data of the specified byte array.
   *
   * @param data Byte array to check.
   * @return {@link Binary} instance that matches the given argument, {@code null} otherwise.
   */
  public static Binary find(byte[] data) {
    final String hash = generateHash(data);
    if (hash != null) {
      return getHashMap().get(hash);
    }
    return null;
  }

  /**
   * Calculates the SHA256 hash code from the specified file {@link Path}.
   *
   * @param file File {@link Path} for hash calculation.
   * @return Hash code as string. Returns {@code null} if the hash could not be calculated.
   */
  public static String generateHash(Path file) {
    if (file != null) {
      try (final ByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
        return generateHash(channel);
      } catch (IOException e) {
        Logger.debug(e, "Not a file: {}", file);
      }
    }
    return null;
  }

  /**
   * Calculates the SHA256 hash code from the specified byte channel.
   *
   * @param channel {@link ByteChannel} to read data for hash calculation.
   * @return Hash code as string. Returns {@code null} if the hash could not be calculated.
   */
  public static String generateHash(ByteChannel channel) {
    if (channel != null) {
      try {
        final MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        final ByteBuffer buffer = ByteBuffer.allocate(16384);
        while (channel.read(buffer) != -1) {
          digest.update(buffer.array(), 0, buffer.position());
          buffer.clear();
        }
        return bytesToString(digest.digest());
      } catch (IOException | NoSuchAlgorithmException e) {
        Logger.debug(e, "Generating hash from byte channel");
      }
    }
    return null;
  }

  /**
   * Calculates the SHA256 hash code from the specified input stream.
   *
   * @param inputStream {@link InputStream} to read data for hash calculation.
   * @return Hash code as string. Returns {@code null} if the hash could not be calculated.
   */
  public static String generateHash(InputStream inputStream) {
    if (inputStream != null) {
      try {
        final MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        final byte[] buffer = new byte[16384];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          digest.update(buffer, 0, bytesRead);
        }
        return bytesToString(digest.digest());
      } catch (IOException | NoSuchAlgorithmException e) {
        Logger.debug(e, "Generating hash from input stream");
      }
    }
    return null;
  }

  /**
   * Calculates the SHA256 hash code from the specified byte array data.
   *
   * @param data Data to calculate the hash for.
   * @return Hash code as string. Returns {@code null} if the hash could not be calculated.
   */
  public static String generateHash(byte[] data) {
    if (data != null) {
      try {
        final MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        return bytesToString(digest.digest(data));
      } catch (NoSuchAlgorithmException e) {
        Logger.debug(e, "Generating hash from byte buffer (length: {})", data.length);
      }
    }
    return null;
  }

  /**
   * Returns the hash for a specified file from a hash file.
   *
   * @param hashFile Text file containing {@code SHA256} hash entries.
   * @param binFile  Binary file to get the hash code for. Specify {@code null} to return the first available hash.
   * @return Hash code as string if available, {@code null} otherwise.
   */
  public static String loadHash(Path hashFile, Path binFile) {
    if (hashFile != null) {
      try {
        final List<String> lines = Files.readAllLines(hashFile, StandardCharsets.UTF_8);

        String line = null;
        if (!lines.isEmpty()) {
          if (binFile == null) {
            line = lines.getFirst();
          } else {
            String fileName = binFile.getFileName().toString().toLowerCase(Locale.ROOT);
            line = lines
                .stream()
                .filter(l -> l.strip().toLowerCase(Locale.ROOT).endsWith(fileName))
                .findAny()
                .orElse(null);
          }
        }

        if (line != null) {
          final String[] items = line.split("[\\w*]+");
          if (items.length >= 2) {
            final String retVal = items[0].strip();
            if (retVal.length() == 64) {
              return retVal.toLowerCase(Locale.ROOT);
            }
          }
        }
      } catch (IOException e) {
        Logger.debug(e, "Parsing hash file: {}", hashFile);
      }
    }
    return null;
  }

  /**
   * Returns a fully initialized map of binary hashes.
   */
  private static HashMap<String, Binary> getHashMap() {
    load();
    return BINARY_HASHES;
  }

  private static String bytesToString(byte[] data) {
    StringBuilder sb = new StringBuilder(64);

    if (data != null) {
      for (byte value : data) {
        final String hex = Integer.toHexString(value & 0xff);
        if (hex.length() == 1) {
          sb.append('0');
        }
        sb.append(hex);
      }
    }

    return sb.toString().toLowerCase(Locale.ROOT);
  }

  private static byte[] stringToBytes(String data) {
    byte[] retVal = null;

    if (data != null) {
      final byte[] buffer = new byte[(data.length() + 1) / 2];
      try {
        for (int ofs = 0; ofs < buffer.length; ofs++) {
          final int pos = ofs * 2;
          final int endPos = Math.min(pos + 2, data.length());
          final int value = Integer.parseInt(data.substring(pos, endPos), 16);
          buffer[ofs] = (byte) value;
        }
        retVal = buffer;
      } catch (NumberFormatException e) {
        Logger.debug(e);
      }
    }

    return retVal;
  }

  private static void load() {
    if (!BINARY_HASHES.isEmpty()) {
      return;
    }

    String jsonString = null;
    try (final InputStream is = WhiteList.class.getResourceAsStream(JSON_FILE)) {
      jsonString = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
    } catch (NullPointerException | IOException e) {
      jsonString = "[]";
      Logger.debug(e, "Could not load {}", JSON_FILE);
    }

    try {
      final JSONArray items = new JSONArray(jsonString);
      for (int i = 0, length = items.length(); i < length; i++) {
        final JSONObject item = items.getJSONObject(i);
        final int version = item.optInt(KEY_VERSION, 0);
        assert version > 0;

        final String os = item.optString(KEY_OS, "").toUpperCase(Locale.ROOT);
        final SystemInfo.Platform platform = SystemInfo.Platform.valueOf(os);

        final String arch = item.optString(KEY_ARCH, "").toUpperCase(Locale.ROOT);
        final SystemInfo.Architecture architecture = SystemInfo.Architecture.valueOf(arch);

        final int variant = item.optInt(KEY_VARIANT, -1);
        assert variant >= 0;

        final String hash = item.optString(KEY_SHA256, "").toLowerCase(Locale.ROOT);
        assert hash.length() == 64; // string length for sha256 code

        BINARY_HASHES.put(hash, new Binary(version, platform, architecture, variant));
      }
    } catch (JSONException e) {
      Logger.debug(e, "JSON parse error");
    }
  }
}
