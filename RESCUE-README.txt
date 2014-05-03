Rescue is an evolution of the Rewrite branch that fixes a number of bugs and brings functionality to the level of the old master branch.


BUILDING
========

* Gradle is now the build system. 
  * You will need Gradle 1.8 to build (1.9 does not work yet, a limitation of the Android plugin)
  * To build from scratch type 'gradle clean build'. 
* You will need Android build tools 19 to build. 
* Android v9 (2.3) and up are supported.
  * I do not support 2.2 even though the changes are minimal, devices that old do not give a good experience
* The language level is now Java 1.7 (this does run on older devices without problems)
* The maven build is now supported. 
  * You will need maven 3.1.1 
  * As Google somehow does not upload the official android jars you will need a hack to get them:
    * Get the tool from https://github.com/mosabua/maven-android-sdk-deployer/ by running 'git clone https://github.com/mosabua/maven-android-sdk-deployer.git'
    * change into the directory maven-android-sdk-developer
    * install the jars for Android 19 by running 'mvn install -P 4.4'

* I am currently only providing debug builds, including for the Sample app. This will change of course.

* For building your own apps with Gradle, look at the Samples app.

SAMPLES APP
==========

* The Samples app in Applications/Android/Samples is meant as a template and test case for building apps based on this version.
* After a successful build, you will find the Samples apk in Applications/Android/Samples/build/apk 
* To run the Samples app, you will need to install a map called 'germany.map' onto the sdcard of a device or emulator.
* It is probably best if the map contains the area of central Berlin


TESTING
=======

* Unit tests are integrated and executed automatically by the Gradle build
* Device test are integrated into the Gradle build and excercise the Samples app.
  * To run the integrated device tests fire up emulators and connect devices.
  * Make sure you have a 'germany.map' on each of the test instruments
  * You can connect as many testing devices as you want, the tests will run on all in parallel
  * Run 'gradle connectedInstrumentTest' 
  * Output will be stored as html in Applications/Android/Samples/build/reports/instrumentTests/connected/index.html


CHANGES
=======

Bitmaps
-------
* The Android bitmaps are the biggest memory problem. I have ported the old master solution (with reference counting) and made some important other changes.
* Bitmap handling is now broken up into different bitmap types
  * TileBitmap is the class that handles the same-size bitmaps that are used for drawing and downloading. This makes it easy to reuse them on Android 3.x+
  * ResourceBitmap handles icons. The main improvement here is that they can be retrieved from an internal cache when the same file is loaded again. 
  * Bitmap: what is left over are the bitmaps for the drawing of the FrameBuffer (the biggest bitmaps) and internally created bitmaps for things like the scalebar. The bitmaps used for the FrameBuffer continue to be the biggest problems when rotating and changing MapViews as somehow they seem to be held in memory (I assume internally something holds a reference to them). I have been experimenting with different strategies, such as allocating a bigger square bitmap for the FrameBuffer, so that it does not have to be reallocated on device rotation. 


SVG Support
-----------

* SVG icon support is provided via the svg-android library. 
  * This was originally developed by larvalabs at https://code.google.com/p/svg-android/ under the Apache License. It incorporates code under a BSD license. If you do not agree with these licenses, do not use the code.
  * The SVG library provides limited support for SVG based icons for better scaling. The SVG spec is so complicated that there will probably never be a full support for Android.
  * A set of SVG icons that almost covers all of the icons used in the standard rendertheme is provided. For the missing icons I did not have the original code.
  
Device Scaling Support
----------------------

* One of the main issues for production has been support for higher DPI devices. Just adjusting the the font size has its limitation. 
* I have taken the approach of simply increasing the TILE_SIZE on higher DPI devices. This has the effect that everything is simply drawn bigger (including streets, buildings etc). 
* I have not changed the name TILE_SIZE at the moment as it affects so many files.
* The scaling factor for the device is determined at start-up and now the AndroidGraphicFactory needs to be instantiated explicitly at startup (see Samples for how to safely do this). 

Extended Rendertheme Support
----------------------------

* I put a mechanism in place so that renderthemes and icons can be safely loaded from Android asset files. This allows easier packaging.
* I have not yet changed the rendertheme version back to 2 (should happen though)


