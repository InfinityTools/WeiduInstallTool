/*
 * Copyright 2023 Argent77
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
package io.infinitytools.wml.weidu;

import io.infinitytools.wml.Configuration;
import io.infinitytools.wml.Globals;
import io.infinitytools.wml.github.Asset;
import io.infinitytools.wml.github.Release;
import io.infinitytools.wml.mod.ModInfo;
import io.infinitytools.wml.process.BufferConvert;
import io.infinitytools.wml.process.ProcessUtils;
import io.infinitytools.wml.process.SysProc;
import io.infinitytools.wml.utils.SystemInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.tinylog.Logger;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * Provides functionality for working with the WeiDU binary.
 */
public class Weidu {
  public static final String WEIDU_NAME = "weidu";

  /**
   * Name of the {@code .properties} file.
   */
  public static final String PROPERTY_FILE = "weidu.properties";
  /**
   * Properties key: Github organization
   */
  public static final String PROP_GITHUB_ORGANIZATION = "github_organization";
  /**
   * Properties key: Github repository
   */
  public static final String PROP_GITHUB_REPOSITORY = "github_repository";
  /**
   * Properties key: Recommended WeiDU version
   */
  public static final String PROP_WEIDU_VERSION = "weidu_version";
  /**
   * Properties key: Static download link for native WeiDU zip archive.
   */
  public static String PROP_DOWNLOAD_NATIVE = String.format("download_%s_%s",
      SystemInfo.getPlatform(), SystemInfo.getArchitecture());

  private static Weidu instance;
  private static Properties properties;

  /**
   * Provides access to the WeiDU class.
   *
   * @return Current {@link Weidu} instance.
   * @throws UnsupportedOperationException if the {@link Weidu} instance could not be initialized.
   */
  public static Weidu getInstance() throws UnsupportedOperationException {
    if (instance == null) {
      instance = new Weidu();
    }
    return instance;
  }

  /**
   * Unloads the current WeiDU class instance. Next call of {@link #getInstance()} initializes a new
   * {@link Weidu} instance.
   */
  public static void reset() {
    instance = null;
  }

  private final Path weidu;

  /**
   * Initializes access to the WeiDU binary for this system.
   *
   * @throws UnsupportedOperationException If the WeiDU binary could not be determined.
   */
  private Weidu() throws UnsupportedOperationException {
    final Path weiduPath = findWeiduBinary();
    if (weiduPath == null) {
      throw new UnsupportedOperationException("Could not find the WeiDU binary.");
    }

    // ensure that the binary is executable (Linux, macOS)
    try {
      makeBinaryExecutable(weiduPath);
    } catch (IOException e) {
      Logger.error(e, "chmod +x failed on WeiDU binary");
      throw new UnsupportedOperationException("Could not make WeiDU binary executable.");
    }

    // ensure that binary is actually a WeiDU binary
    if (!validateWeidu(weiduPath)) {
      throw new UnsupportedOperationException("Not a WeiDU binary: " + weiduPath);
    }

    this.weidu = weiduPath;
  }

  /**
   * Returns the {@link Path} to the detected WeiDU binary.
   */
  public Path getWeidu() {
    return weidu;
  }

  /**
   * Retrieves the WeiDU help description.
   */
  public String getHelp() {
    String retVal = null;

    if (!SystemInfo.IS_LINUX) {
      // Originally a Windows-only procedure, but appears to be required for macOS, too.
      // WeiDU expects the user to press ENTER repeatedly to advance output by one more page until the process
      // terminates.
      final SysProc sp = new SysProc(weidu.toString(), "--help", "--no-exit-pause");
      try {
        final Future<Integer> result = sp.execute();
        long waitTime = 0;
        long waitTimeTotal = 0;
        while (sp.isRunning() && waitTimeTotal < 2000) {
          sp.setInput(SystemInfo.NEWLINE);
          Thread.sleep(waitTime);
          waitTime = Math.min(waitTime + 1, 10L);
          waitTimeTotal += waitTime;
        }

        result.get(1000, TimeUnit.MILLISECONDS);
      } catch (ExecutionException | IOException | InterruptedException | TimeoutException e) {
        Logger.error(e, "Error executing WeiDU process");
      }
      final byte[] data = sp.getOutput();
      final String output = BufferConvert.decodeBytes(data).decoded();
      if (output != null) {
        // Removing useless extra lines
        List<String> list = new ArrayList<>(Arrays.asList(output.split("\r?\n")));
        for (int i = 0; i < list.size(); i++) {
          final String line = list.get(i);
          if (line.contains("Press Enter For More Options") || line.contains("Enter arguments:")) {
            list.remove(i);
            list.remove(i - 1); // previous line is always empty
            i--;
          }
        }
        retVal = list.stream().collect(Collectors.joining("\n", "", "\n"));
      }
    } else {
      // Other systems are much less painful
      final byte[] data = ProcessUtils.getProcessOutput(weidu.toString(), "--help", "--no-exit-pause");
      retVal = BufferConvert.decodeBytes(data).decoded();
    }

    if (retVal == null) {
      retVal = "";
    }

    // First line is unnecessary; contains WeiDU path and version number
    retVal = retVal.replaceFirst(".*\r?\n", "");

    return retVal;
  }

  /**
   * Retrieves the WeiDU version as {@link Version} record.
   *
   * @return WeiDU version as {@link Version} record if available, {@code null} otherwise.
   */
  public Version getVersion() {
    String retVal = null;
    try {
      final byte[] data = ProcessUtils.getProcessOutput(weidu.toString(), "--version");
      String output = BufferConvert.decodeBytes(data).decoded();
      // stripping weidu path portion
      output = output.substring(output.lastIndexOf(']') + 1).strip();
      final String[] items = output.split("[ \t]+");
      for (final String item : items) {
        if (item.matches("[0-9]+")) {
          retVal = item;
          break;
        }
      }
    } catch (NullPointerException e) {
      Logger.debug(e, "Getting WeiDU version");
    }

    if (retVal != null) {
      return Version.of(retVal);
    } else {
      return null;
    }
  }

  /**
   * Returns a list of available mod languages.
   *
   * @param tp2File {@link Path} to the tp2 file of the mod.
   * @return Array of available mod languages, as string. Language names are returned verbatim. Array index corresponds
   * to language index. An empty array is returned if no explicit languages are defined.
   * @throws FileNotFoundException if {@code tp2File} is not available.
   */
  public String[] getModLanguages(Path tp2File) throws FileNotFoundException {
    if (tp2File == null) {
      throw new NullPointerException("tp2File is null");
    }

    if (!Files.exists(tp2File, LinkOption.NOFOLLOW_LINKS)) {
      throw new FileNotFoundException("File does not exist: " + tp2File);
    }

    final byte[] data = ProcessUtils.getProcessOutput(getWorkingDir(tp2File), weidu.toString(), "--nogame",
        "--list-languages", tp2File.toString());
    final String output = BufferConvert.decodeBytes(data).decoded();

    return parseLanguages(output);
  }

  /**
   * Returns a JSON structure containing component information for the specified mod.
   *
   * @param tp2File  {@link Path} to the tp2 file of the mod.
   * @param language Index of the language component information should be retrieved for. An out-of-bounds index
   *                 defaults to the first available language.
   * @param charsets List of suggested {@link Charset} instances to use for converting process output to text.
   *                 Uses a generic set of default charsets if not specified.
   *                 (Note: Text encoding may differ wildly across mods and languages.)
   * @return A {@link JSONArray} object containing parsed JSON data with mod component information.
   * @throws FileNotFoundException if {@code tp2File} is not available.
   */
  public JSONArray getModComponentInfo(Path tp2File, int language, Charset... charsets) throws FileNotFoundException {
    if (tp2File == null) {
      throw new NullPointerException("tp2File is null");
    }

    if (!Files.exists(tp2File, LinkOption.NOFOLLOW_LINKS)) {
      throw new FileNotFoundException("File does not exist: " + tp2File);
    }

    // Getting mod info data as raw byte data and determine best character encoding based on selected language
    final byte[] outputData = ProcessUtils.getProcessOutput(getWorkingDir(tp2File), weidu.toString(), "--nogame",
        "--list-components-json", tp2File.toString(), Integer.toString(language));
    Logger.debug("Parsing mod component JSON data (buffer={} bytes)", outputData.length);

    // preparing list of potential character sets
    final List<Charset> csList = new ArrayList<>(Arrays.asList(charsets));
    if (csList.isEmpty()) {
      csList.add(StandardCharsets.UTF_8);
    }

    // determining a working charset for the given mod information data and convert to text
    String output = null;
    for (int i = 0; i < csList.size(); i++) {
      final Charset cs = csList.get(i);
      final CharsetDecoder decoder = cs.newDecoder();
      final CodingErrorAction action = (i + 1 < csList.size()) ? CodingErrorAction.REPORT : CodingErrorAction.REPLACE;
      decoder.onMalformedInput(action);
      decoder.onUnmappableCharacter(action);
      try {
        output = decoder.decode(ByteBuffer.wrap(outputData)).toString();
        Logger.debug("Successfully decoding mod component info (charset={})", cs);
        break;
      } catch (CharacterCodingException e) {
        Logger.debug(e, "Error decoding mod component info (charset={})", cs);
      }
    }

    return parseJson(output);
  }

  /**
   * Determines the working directory for invoking a WeiDU process with the specified tp2 file.
   * <p>
   * Without a correct working path WeiDU fails to resolve translation strings for the specified mod.
   * </p>
   *
   * @param tp2File {@link Path} to the tp2 file of the mod.
   * @return Working directory as {@link Path} instance for the specified mod. Returns {@code null} if the working path
   * could not be determined.
   */
  public Path getWorkingDir(Path tp2File) {
    Path retVal = null;

    if (tp2File != null) {
      String tp2FolderName = null;
      final Path tp2Folder = tp2File.getParent();
      if (tp2Folder != null) {
        tp2FolderName = tp2Folder.getFileName().toString();
      }

      final String modName = ModInfo.stripModName(tp2File, false);

      retVal = tp2File.getParent();
      if (tp2FolderName != null && tp2FolderName.equalsIgnoreCase(modName)) {
        retVal = retVal.getParent();
      }
    }

    return retVal;
  }

  /**
   * Scans the specified WeiDU output data for language definitions and returns them as string array.
   * Returns an empty string array if no explicit language definition was found.
   */
  private String[] parseLanguages(String data) {
    final HashMap<Integer, String> map = new HashMap<>();
    if (data != null) {
      final String[] lines = data.split("\r?\n");
      for (final String line : lines) {
        if (line.matches("[0-9]+:.*")) {
          String[] items = line.split(":", 2);
          try {
            int key = Integer.parseInt(items[0]);
            map.put(key, items[1].strip());
          } catch (NumberFormatException e) {
            Logger.error(e, "Unexpected language number");
          }
        }
      }
    }

    int size = 1 + map.keySet().stream().max(Comparator.comparingInt(a -> a)).orElse(-1);
    final String[] retVal = new String[size];
    Arrays.fill(retVal, ModInfo.DEFAULT_LANGUAGE);
    map.forEach((idx, text) -> retVal[Math.max(0, idx)] = text);

    return retVal;
  }

  /**
   * Scans the specified WeiDU output data for a JSON array string and returns it as {@link JSONArray} object.
   * Returns {@code null} if no valid JSON data was found.
   */
  private JSONArray parseJson(String data) {
    JSONArray retVal = null;

    if (data != null) {
      final String[] lines = data.split("\r?\n");
      for (final String line : lines) {
        final String curLine = line.strip();
        try {
          if (curLine.matches("\\[\\{\"index\":.*}]")) {
            try {
              retVal = new JSONArray(curLine);
            } catch (JSONException e) {
              Logger.error(e, "Invalid component info JSON structure");
            }
            break;
          }
        } catch (PatternSyntaxException e) {
          Logger.error(e, "Invalid regular expression. NEEDS FIX!!!");
        }
      }
    }

    return retVal;
  }

  /**
   * Returns a property from the "weidu.properties" file.
   *
   * @param key Property name.
   * @return Property value if successful, {@code null} otherwise.
   */
  public static String getProperty(String key) {
    return getProperty(key, null);
  }

  /**
   * Returns a property from the "weidu.properties" file.
   *
   * @param key      Property name.
   * @param defValue Return value if the property is not available.
   * @return Property value if successful, {@code defValue} otherwise.
   */
  public static String getProperty(String key, String defValue) {
    String retVal = defValue;
    if (properties == null) {
      properties = new Properties();
      try (final InputStream is = Globals.class.getClassLoader().getResourceAsStream(PROPERTY_FILE)) {
        properties.load(is);
      } catch (Exception e) {
        Logger.error(e, "Could not load \"weidu.properties\"");
        properties = null;
        return retVal;
      }
    }

    if (key != null) {
      retVal = properties.getProperty(key, defValue);
    }

    return retVal;
  }

  /**
   * Returns a list of {@link Path} instances that are checked for the WeiDU binary.
   *
   * @param checkSystemPath Whether to check the system PATH as well.
   * @return List of search paths for the WeiDU binary.
   */
  public static List<Path> getSearchPaths(boolean checkSystemPath) {
    final List<Path> searchPaths = new ArrayList<>();

    // list of potential relative paths
    final List<Path> relPaths = getRelativePaths(WEIDU_NAME);

    // list of potential base paths to search
    final List<Path> basePaths = new ArrayList<>();

    // 1. checking custom binary path
    final String customPath = Configuration.getInstance().getOption(Configuration.Key.WEIDU_PATH);
    if (customPath != null) {
      Path path = Path.of(customPath).normalize();
      if (Files.isRegularFile(path)) {
        searchPaths.add(path);
      }
    }

    // 2. checking app data base path
    final Path appDataPath = Globals.APP_DATA_PATH;
    basePaths.add(appDataPath);

    // assembling list of potential WeiDU binary paths
    for (final Path basePath : basePaths) {
      for (final Path relPath : relPaths) {
        searchPaths.add(basePath.resolve(relPath.toString()));
      }
    }

    // 3. checking system PATHs
    if (checkSystemPath) {
      String sysPath = System.getenv("PATH");
      if (sysPath != null) {
        final String weiduFileName = WEIDU_NAME + SystemInfo.EXE_SUFFIX;
        final String[] sysPaths = sysPath.split(Pattern.quote(SystemInfo.PATH_SEPARATOR));
        for (final String path : sysPaths) {
          final Path weiduPath = Path.of(path, weiduFileName);
          if (Files.isRegularFile(weiduPath)) {
            searchPaths.add(weiduPath);
          }
        }
      }
    }

    return searchPaths;
  }

  /**
   * Returns a list of relative paths for the specified binary file.
   *
   * @param binName Base name of the binary, without file extension.
   * @return A {@link List} of relative {@link Path}s to the specified binary file.
   */
  private static List<Path> getRelativePaths(String binName) {
    final String binFile = binName + SystemInfo.EXE_SUFFIX;
    return Arrays.asList(
        Path.of(binName, SystemInfo.getPlatform().toString(), SystemInfo.getArchitecture().toString(), binFile),
        Path.of(binName, SystemInfo.getPlatform().toString(), binFile),
        Path.of(binName, binFile),
        Path.of(binFile)
    );
  }

  /**
   * Attempts to find the WeiDU binary for this system.
   *
   * @return Path to the WeiDU binary if found, {@code null} otherwise.
   */
  private static Path findWeiduBinary() {
    Path retVal = null;

    final List<Path> searchPaths = getSearchPaths(true);

    // finding binary path
    for (final Path path : searchPaths) {
      if (Files.isRegularFile(path)) {
        retVal = path;
        break;
      }
    }

    return retVal;
  }

  /**
   * Attempts to make the specified file executable on unix-like systems.
   *
   * @param filePath Path to file.
   * @throws IOException                   if an I/O error occurs.
   * @throws UnsupportedOperationException if the operation is not available on the current system.
   */
  private static void makeBinaryExecutable(Path filePath) throws IOException, UnsupportedOperationException {
    if (filePath == null) {
      throw new NullPointerException("File path is null");
    }

    if (!Files.exists(filePath)) {
      throw new IOException("Does not exist: " + filePath);
    }

    if (SystemInfo.IS_LINUX || SystemInfo.IS_MACOS) {
      Set<PosixFilePermission> perms = Files.getPosixFilePermissions(filePath);
      if (!perms.contains(PosixFilePermission.OWNER_EXECUTE)) {
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(filePath, perms);
      }
    }
  }

  /**
   * Attempts to determine whether the specified file is a WeiDU executable.
   *
   * @param filePath {@link Path} of the executable file.
   * @return {@code true} if the specified file is a WeiDU executable, {@code false} otherwise.
   */
  private static boolean validateWeidu(Path filePath) {
    boolean retVal = false;

    if (filePath != null && Files.isRegularFile(filePath)) {
      try {
        final byte[] data = ProcessUtils.getProcessOutput(filePath.toString(), "--version");
        final String output = BufferConvert.decodeBytes(data).decoded();
        if (output != null) {
          retVal = output.strip().matches(".*\\bWeiDU version [0-9]+");
        }
      } catch (PatternSyntaxException e) {
        Logger.error(e, "Invalid regular expression. NEEDS FIX!!!");
      } catch (Exception e) {
        Logger.error(e, "Process execution error");
      }
    }

    return retVal;
  }

  /**
   * A convenience method that downloads a zip archive, extracts the specified binary file and installs it
   * in the given directory.
   *
   * @param binName   the WeiDU binary filename.
   * @param targetDir Directory where the WeiDU binary should be installed.
   * @param overwrite whether to overwrite an existing file in the target directory.
   * @throws FileNotFoundException if the zip archive could not be found.
   * @throws ZipException          if there is a problem with the downloaded zip archive.
   * @throws IOException           if an I/O error occurs.
   */
  public static void installWeidu(String binName, Path targetDir, boolean overwrite,
                                  BiPredicate<String, Double> feedbackFunc)
      throws FileNotFoundException, IOException {
    if (!overwrite && Files.exists(targetDir.resolve(binName))) {
      // no further actions needed
      return;
    }

    double progress = 0.0;

    // 1. Get WeiDU zip file URL
    WeiduAsset zipAsset;
    if (!feedbackFunc.test("Fetching download URL", progress)) {
      return;
    }
    try {
      zipAsset = findWeiduAsset();
    } catch (IOException e) {
      Logger.debug(e, "Unrecoverable findWeiduAsset() error");
      throw new IOException("Could not determine WeiDU binary URL.", e);
    }

    // 2. Download zip file to memory buffer
    progress += 0.1;
    final double progressScale = 0.8;
    final byte[] buf = new byte[65536];
    long curSize = 0;
    final URL url = URI.create(zipAsset.downloadUrl()).toURL();
    final int bufferSize = (int) ((zipAsset.size() > 0) ? zipAsset.size() : 4_000_000);
    final ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
    try (BufferedInputStream is = new BufferedInputStream(url.openStream())) {
      int bytesRead;
      while ((bytesRead = is.read(buf, 0, buf.length)) >= 0) {
        baos.write(buf, 0, bytesRead);
        curSize += bytesRead;
        if (bytesRead > 0) {
          double curProgress = ((double) curSize / (double) zipAsset.size()) * progressScale;
          if (!feedbackFunc.test("Downloading archive",
              progress + Math.min(progressScale, curProgress))) {
            return;
          }
        }
      }
    } catch (IOException e) {
      Logger.debug(e, "Error downloading WeiDU archive");
      throw new IOException("Could not download WeiDU binary.", e);
    }

    // 3. Extract WeiDU binary to destination
    progress += .05;
    if (!feedbackFunc.test("Unpacking executable", progress)) {
      return;
    }
    Path outputFile = null;
    Files.createDirectories(targetDir);
    final byte[] zipBuf = baos.toByteArray();
    try (final ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBuf))) {
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        final Path p = Path.of(zipEntry.getName());
        if (!zipEntry.isDirectory() && p.getFileName().toString().equalsIgnoreCase(binName)) {
          // match found!
          outputFile = targetDir.resolve(p.getFileName().toString());
          try (final FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
            int bytesRead;
            while ((bytesRead = zis.read(buf)) >= 0) {
              fos.write(buf, 0, bytesRead);
            }
          }
        }
        zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
    }

    // 3.1. Make binary executable (Linux, macOS)
    if (!SystemInfo.IS_WINDOWS) {
      makeBinaryExecutable(outputFile);
    }

    progress = 1.0;
    feedbackFunc.test("Done", progress);
  }

  /**
   * Attempts to find a WeiDU asset for the current system.
   *
   * @return {@code Asset} record with information about the WeiDU asset. Size -1 indicates that exact size is not
   * available. Returns {@code null} if a matching asset could not be found.
   * @throws IOException on an unrecoverable error.
   */
  private static WeiduAsset findWeiduAsset() throws IOException {
    WeiduAsset retVal = null;

    // trying latest release first
    final Release release = new Release(getProperty(PROP_GITHUB_ORGANIZATION), getProperty(PROP_GITHUB_REPOSITORY),
        Release.ID_LATEST);

    final String osKey = switch (SystemInfo.getPlatform()) {
      case WINDOWS -> "windows";
      case MACOS -> "mac";
      case LINUX -> "linux";
      default -> throw new IOException("Unsupported operating system: " + SystemInfo.getPlatform());
    };

    final List<String> archKeys = new ArrayList<>();
    if (SystemInfo.getArchitecture() == SystemInfo.Architecture.X86_64) {
      archKeys.add("amd64");
      archKeys.add("x86_64");
    } else if (SystemInfo.getArchitecture() == SystemInfo.Architecture.X86) {
      archKeys.add("x86");
      archKeys.add("");
    } else {
      throw new IOException("Unsupported platform: " + SystemInfo.getArchitecture());
    }

    Asset weiduAsset = null;
    for (final Asset asset : release.getAssets()) {
      final String name = asset.getName().toLowerCase();
      if (!name.contains("legacy")) { // exclude Windows legacy builds
        if (name.contains(osKey)) {
          if (archKeys.stream().anyMatch(s -> s.isEmpty() || name.matches(".*\\b" + s + "\\b.*\\.zip"))) {
            weiduAsset = asset;
            break;
          }
        }
      }
    }

    if (weiduAsset != null) {
      retVal = new WeiduAsset(weiduAsset.getName(), weiduAsset.getBrowserDownloadUrl(), weiduAsset.getSize());
    }

    if (retVal == null) {
      // falling back to hardcoded URLs
      final String urlString = getProperty(PROP_DOWNLOAD_NATIVE);
      if (urlString == null) {
        throw new IOException("WeiDU binary is not available for the current system.");
      }

      final String[] urlItems = urlString.split("/");
      retVal = new WeiduAsset(urlItems[urlItems.length - 1], urlString, -1L);
    }

    return retVal;
  }

  /**
   * Used internally to return information about a specific remote asset.
   */
  private record WeiduAsset(String name, String downloadUrl, long size) {
  }

  /**
   * Storage for a WeiDU version number.
   */
  public record Version(int major, int minor) implements Comparable<Version> {
    /**
     * Creates a new {@link Version} record from the specified version string.
     *
     * @param version WeiDU version string.
     * @return {@link Version} record with the version split into major and minor parts. Unrecognized versions
     * are treated as 0.
     */
    public static Version of(String version) {
      int major = 0;
      int minor = 0;
      if (version != null && !version.isEmpty()) {
        final Function<String, Integer> convert = s -> {
          try {
            return Integer.parseInt(s);
          } catch (NumberFormatException e) {
            Logger.debug(e, "Unexpected WeiDU version string");
            return 0;
          }
        };
        major = convert.apply(version.substring(0, Math.max(0, version.length() - 2)));
        minor = convert.apply(version.substring(Math.max(0, version.length() - 2)));
      }
      return new Version(major, minor);
    }

    /**
     * Converts major and minor version parts to a decimal number.
     */
    public double toNumber() {
      try {
        return Double.parseDouble(major + "." + minor);
      } catch (NumberFormatException e) {
        Logger.warn(e, "Unexpected number parsing error");
        return major;
      }
    }

    @Override
    public int compareTo(Version o) {
      if (o != null) {
        return (major != o.major) ? major - o.major : minor - o.minor;
      } else {
        return 1;
      }
    }

    @Override
    public String toString() {
      return String.format("%d%02d", major, minor);
    }
  }
}
