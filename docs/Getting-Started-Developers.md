
# Getting Started

**A quick description for developers of how to get started with mapsforge.**

This article describes how to build the mapsforge project and libraries from scratch and how developers should start working.

If you have any questions or problems, don't hesitate to ask our public [mapsforge-dev](https://groups.google.com/group/mapsforge-dev) mailing list for help. You can also report bugs and improvement requests via our [issue tracker](https://code.google.com/p/mapsforge/issues/list).

## Requirements
|Tool|Version|
|----|---|
|Android Build Version Tools|21|
|Gradle|2.1 if building with gradle|
|Maven|1.3.1 if building with maven|
|Java|1.7|

## Checkout the code

There are problems checking out the code from this repository with git > 1.7.2, the reason is unknown but it might be a corruption of the Google repository. 
You can clone the repository with git version 1.7.2 or via one of the clones, eg. at https://github.com/applantation/mapsforge

## Components

Mapsforge consists of the following components:

- mapsforge-core: platform unspecific general components and interfaces.
- mapsforge-map: platform unspecific elements used for map display.
- mapsforge-map-reader: platform unspecific code to read mapsforge map files.
- mapsforge-map-android: android specific elements.
- kXML2: lightweight XML parser for render themes.
- androidsvg: SVG library for displaying SVG files as icons.
 
The jars build from the above components are required elements for a mapsforge application on Android.

- mapsforge-map-awt: a Java-only library to display mapsforge maps.
 

### Branches

The mapsforge code has now been consolidated into two main branches as well as releases
- master: the latest stable development branch. Use this if you want to use newer mapsforge functionality that has not yet been released, but you still want a certain stability.
- dev: unstable development, features in progress. Use this if you want the latest development features and you can live with some instability.
- 0.4.0 release: use this if you want to build applications built on top of well-tested and stable code.

Code before the 0.4.0 release is not supported anymore and if you are starting development with mapsforge, its use is strongly discouraged. 

For a while, what has become the 0.4.0 release was known as the 'rescue' branch, while what is known as dev now was the 'rescue-exp' branch. Rescue has been merged into master, and rescue-exp has become dev. The use of the cloned repositories holding these branches is discouraged, they will only exist for a transition period before being removed.

## Development Applications

### Samples Android App

 The Samples app, in Applications/Android/Samples, is a sample app for Android demonstrating mapsforge capabilities and a good starting point if you want to develop your own mapsforge-based app.
- The Samples app in Applications/Android/Samples is meant as a template and test case for building apps based on this version.
- After a successful build, you will find the Samples apk in Applications/Android/Samples/build/apk
- To run the Samples app, you will need to install a map called 'germany.map' onto the sdcard of a device or emulator.
- It is probably best if the map contains the area of central Berlin

### Swing Map Viewer

The SwingMapViewer is a simple Java only app useful for testing maps.

## Building Mapforge

### Building mapsforge with Gradle

Gradle is the new build system favoured by Google for Android builds. Android Studio, the new IDE provided by Google for building Android apps, integrates nicely with Gradle. 

The current version for building mapsforge is Gradle 2,1.

After checking out the code, a build from the command line should be as easy as 

    gradle clean build

If you want to skip the tests, run 

    gradle clean assemble

After the build completes successfully you will find the Samples app in the directory Applications/Android/Samples/build/apk. Currently the build results in unsigned apks.  

### Start developing with Android Studio

Android Studio integrates tightly with gradle. The easiest way to create a new application is to follow the example of the Samples app. 


### Building mapsforge with Maven

A second way to build mapsforge is using maven. This was the original way of building mapsforge.

The mapsforge project uses the free [Apache maven](http://maven.apache.org/) tool to automatize the build process. Both version 2 and version 3 can be used. If you want to learn more about maven, please refer to the [official documentation](http://maven.apache.org/guides/index.html).

To start a complete build of all modules, open a command prompt, change to the directory which contains the copy of the mapsforge repository and execute the following command:
    mvn clean install

This will tell maven to delete any pre-existing generated files and directories during the build. Although the cleanup step is not always needed, we recommend to do so every time in order to avoid problems and have repeatable results.

In the beginning, maven automatically downloads all missing plug-ins and files. Depending on the speed of your Internet connection, this may take some time. All downloaded files are stored locally in a special maven directory to avoid downloading them again at each build.

During the build process, maven compiles, tests and packages all modules in the correct order. A new directory `target` is created for each module which contains – among test reports and other generated files – the new artifacts. Eventually these artifacts are installed in your local repository so that you can use them in other maven projects.


### Start developing with Eclipse

If you want to contribute to the mapsforge project, we recommend to use the latest stable version of the [Eclipse IDE](http://eclipse.org/).

As Eclipse needs to know the path to your local maven repository, you have to add a new classpath variable named `M2_REPO`. This can either be done manually via `Window > Preferences > Java > Build Path > Classpath Variables > New` or automatically via the [Maven Eclipse Plugin](http://maven.apache.org/plugins/maven-eclipse-plugin/configure-workspace-mojo.html). Depending on the currently running operating system, execute one of the following commands:

**Linux**
    mvn eclipse:configure-workspace "-Declipse.workspace=path/to/your/eclipse/workspace/"

**Windows**
    mvn eclipse:configure-workspace "-Declipse.workspace=x:\path\to\your\eclipse\workspace\"

After you have configured your Eclipse workspace, checked out the code and built the complete project (see the instructions above), execute the following command:
    mvn eclipse:eclipse

This will tell maven to generate all missing Eclipse project files which are not checked in into our repository. It also ensures that all mapsforge projects use the same code formatter profile, compiler settings, file encoding, new line delimiters and so on.

You should install the [Checkstyle](http://eclipse-cs.sourceforge.net/), [FindBugs](http://findbugs.sourceforge.net/downloads.html) and [PMD](http://pmd.sourceforge.net/integrations.html#eclipse) Eclipse plug-ins to regularly analyze the quality of the source code. The same set of rules is shared across all mapsforge modules. Running the above maven command will also copy the necessary configuration files into each project directory.

Each of the mapsforge modules is now configured as an Eclipse project and can be added to your current workspace via `File > Import > General > Existing Projects into Workspace`.

To build the Android sample application, you need to make a few adjustments:
* Select `Project > Properties` from the menu.
* In the dialog that opens, go to `Java Build Path Order and Export`. Make sure the entry for `M2_REPO/android/android/5.0_r1/android-5.0_r1.jar` is unchecked and all others are checked.
* Go to `Java Compiler` and set the compiler compliance level to 1.7.
* Then clean the project (`Project > Clean`).

Without these steps, you may have issues with the app crashing with a `java.lang.NoClassDefFoundError` exception. If that happens, carry out the above steps and build again.

## How to contribute

As an open source project, we welcome new contributors and appreciate your help.

Before you start working on an unresolved issue or try to implement a new feature, please contact us via our public [mapsforge-dev](https://groups.google.com/group/mapsforge-dev) mailing list. You may also create a new issue or comment on an existing one to describe your ideas. We will then discuss the best way to realize your proposal and figure out how we can help you to get started quickly.

If you are only requesting a small change in the code, you may attach a patch file to the corresponding issue. Make sure that your patch is derived from the latest version in our repository, otherwise we might be unable to apply it. Please follow our code and style conventions. Detailed information about them can be found in the [[Project Conventions|Project conventions]] article.

Besides writing code, improving our written documentation and keeping our issue tracker up-to-date is equally important. Participating in this way, you are required to have a [Google account](https://accounts.google.com/). We will grant potentially required permissions on individual basis and request.

Please note that the mapsforge project is licenced under the [GNU LGPL3 licence](http://www.gnu.org/licenses/lgpl.html). Thus, all your contributions are going to be published under this license.