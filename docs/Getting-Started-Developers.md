# Getting Started

**A quick description for developers of how to get started with mapsforge.**

This article describes how to build the mapsforge project and libraries from scratch and how developers should start working.

If you have any questions or problems, don't hesitate to ask our public [forum](https://groups.google.com/group/mapsforge-dev) for help.

## Checkout the code

The mapsforge code is now **only** at https://github.com/mapsforge/mapsforge. The old repositories are not maintained anymore.

## Components

Mapsforge consists of the following core components:

- mapsforge-core: platform unspecific general components and interfaces.
- mapsforge-map: platform unspecific elements used for map display.
- mapsforge-themes: internal render themes collection.
- mapsforge-map-reader: platform unspecific code to read mapsforge map files.
- mapsforge-poi: platform unspecific poi elements.
- [kXML2](http://www.kxml.org/): lightweight XML parser for render themes.

Extra Android components:

- mapsforge-map-android: android specific map elements.
- mapsforge-poi-android: android specific poi elements.
- [androidsvg](http://bigbadaboom.github.io/androidsvg/): SVG library for displaying SVG files as icons.
- [sqlite-android](https://sqlite.org/android/): SQLite Android Bindings.

Extra Java components:

- mapsforge-map-awt: a Java-only library to display mapsforge maps.
- mapsforge-poi-awt: java specific poi elements.
- [svg-salamander](https://github.com/blackears/svgSalamander): SVG library for displaying SVG files as icons.
- [xerial/sqlite-jdbc](https://github.com/xerial/sqlite-jdbc): SQLite JDBC Driver.

### Branches

The mapsforge code has now been consolidated into a main branch as well as releases:
- **master**: use this if you want the latest development features and can live with some instability.
- **release**: use this if you want to build applications on top of well-tested and stable code.

Code before latest release is not supported anymore (we do not have the resources to do this) and if you are starting development with mapsforge, its use is strongly discouraged.

## Development Applications

### mapsforge-samples-android

 The Samples app, in mapsforge-samples-android, is a sample app for Android demonstrating mapsforge capabilities and a good starting point if you want to develop your own mapsforge-based app.
- The Samples app in mapsforge-samples-android is meant as a template and test case for building apps based on this version.
- There is now a MapViewerTemplate class for building Android apps, that gives simple hooks to implement an Android based application. For its use refer to the Samples app.
- After a successful build, you will find the Samples apk in mapsforge-samples-android/build/outputs/apk
- To run the Samples app, you will need to install any map called 'berlin.map' onto the sdcard of a device or emulator.

### mapsforge-samples-awt

The mapsforge-samples-awt is a simple Java only app useful for testing maps.

## Building Mapforge

### Building mapsforge with Gradle

Gradle is the new build system favoured by Google for Android builds. Android Studio, the new IDE provided by Google for building Android apps, integrates nicely with Gradle. We use the Gradle Wrapper script, which also installs the required version of Gradle.

After checking out the code, a build from the command line should be as easy as 

    ./gradlew clean build

If you want to skip the tests, run 

    ./gradlew clean assemble

After the build completes successfully you will find the Samples app in the directory mapsforge-samples-android/build/outputs/apk. Currently the build results in unsigned apks.

### Start developing with Android Studio

Android Studio integrates tightly with gradle. The easiest way to create a new application is to follow the example of the Samples app. 

## How to contribute

[Guidelines](CONTRIBUTING.md) for repository contributors.