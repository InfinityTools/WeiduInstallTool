# WeiDU Mod Launcher
*A graphical front end for WeiDU Mods.*

The WeiDU Mod Launcher is a graphical front end for the [WeiDU command line tool](https://github.com/WeiDUorg/weidu) which is used to install mods for Infinity Engine games, such as Baldur's Gate, Icewind Dale or Planescape Torment. The frontend is written in Java and uses the JavaFX framework to provide a modern UI.

The launcher provides all functionality of the WeiDU command line interpreter as well as many usability improvements to help with the mod installation task.

## Installation

...TODO...

## Building from Source

**Required tools:**
- [Oracle's JDK 21 or later](https://www.oracle.com/de/java/technologies/downloads/), or [OpenJDK 21 or later](https://adoptium.net/temurin/releases/?version=21)
- [Gradle](https://gradle.org/)

The following commands will build the application and create a package in the format native to the current platform.

**Windows:**
```
cd WeiduModLauncher
gradlew.bat clean jpackage
```

**Linux or macOS:**
```
cd WeiduModLauncher
./gradlew clean jpackage
```

The resulting package can be found in the `./build/jpackage` folder.

The `jpackage` task requires the following software to be installed on the system:
- Windows: WiX 3.0 or later is required.
- macOS: Xcode command line tools are required when the `--mac-sign` option is used to request that the package be signed.
- Linux:
  - For Red Hat Linux, the `rpm-build` package is required.
  - For Ubuntu Linux, the `fakeroot` package is required.

More details can be found in Oracle's [Packaging Tool User's Guide](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html).

## License

WeiDU Mod Launcher is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
