# Changelog

<wiki:toc />

## Version 0.5.1
 **2015-13-18**
 - Same as 0.5.1-rc4
 
## Version 0.5.1-rc4
 - Fix for build of SwingMapViewer with new SVG libraries
 
## Version 0.5.1-rc3
 - SVG rendering for Java with SVG Salamander
 - Internal rendertheme (osmarender) using SVG symbols by default
 - Resource memory/file cache: common clear methods
 
## Version 0.5.1-rc2
 - Fix for NPE when using persistent tile cache.
 - Doc updates
 
## Version 0.5.1-rc1
 - Support for more general map data sources through [MapDataStore interface and MultiMapDataStore class](MapDataStore.md)
 - Persistent Caching.
 - Documentation updates.
 - Various bug fixes.
 - Improved grid drawing.
 - External Map Rotation example in Samples.

## Version 0.5.0
**2014-12-08**

Release 0.5.0 is, apart from minor documentation changes, identical to 0.5.0-rc4.

## Version 0.5.0-rc4
**2014-12-05**

 - Changes to support the new standard for encoding land/sea area. 
 - Fix for #332 (invalid geometries in map writer).
 - Fix for #549 (ordering of label drawing).
 - Documentation updates. 

## Version 0.5.0-rc3
**2014-11-27**

- Rendertheme V4: added display directive to force symbols to appear.
- Rendertheme V4: added optional map-background-outside to blank out non-map areas.
- Introduced MapViewerTemplate for easier development of Android apps and simplified the Android Samples app.
- Android Lollipop support, various upgrades to latest software, Java 8 (for maven only so far).
- Map Reader fix for long ways.
- Map Writer fix to clip multi-polygons and area detection.
- Cache calculation improvements.
- Improved documentation integrated into git.
- Move to github.

## Version 0.5.0-rc2

- Fixes to Rendertheme V4 with most notably improved tiling of area shaders.
- Smaller internal bug fixes and software updates.

## Version 0.5.0-rc1

- Rendertheme V4.
- Scalebar now with the option of using two units at the same time. New unit: nautical.
- Line breaks in labels: this is the most fragile change at the moment as it does not play well with the label placement algorithm.
- Improved tile caching: bugs fixed and optional threading for better performance.
- TileStoreLayer using the standard TMS directory layout of zoomlevel/y/x.
- New SVG library having better compliance with SVG spec.
- New render theme xml parser for better performance.
- Caching of rendered SVG symbols & delayed rendering.
- Simplify map writer plugin installation in Osmosis.

## Version 0.4.3

No functionality change, but mapsforge-map-android now build as a jar (not apklib) for Maven Central downloads.

## Version 0.4.2

No functionality change, but mapsforge build against mapsforge svg-android.

## Version 0.4.1

No functionality change, but version 0.4.1 is now available from Maven Central.

## Version 0.4.0

Version 0.4.0 is what during a period was known as the 'rescue' branch. It is based on an extensive rewrite of the mapsforge library that is incompatible with the previous releases. The mapfile format however has not changed and map files from 0.3 can still be used. 

- Better modular design of the entire project.
- Support for Java-only applications, see the SwingMapViewer subproject.
- Map views are not directly coupled to activities any more and can be used with modern Android design concepts.
- More flexible overlay model.
- Tightened-up memory handling.
- Support for device screen scaling.
- Support for SVG files as map icons.
- Gradle as build system in addition to Maven.
- Updated icons of default render theme.

For a demonstration of the capabilities refer to the Samples app.

## Version 0.3.x

**Version 0.3.0 (2012-03-18)**

- New version of the binary map file format, see [[Specification Binary Map File]].
- New render theme API for custom map styles. ([issue 31](https://github.com/mapsforge/mapsforge/issues#issue/31))
- Allow for custom tile download classes. ([issue 204](https://github.com/mapsforge/mapsforge/issues#issue/204))
- Position of the zoom controls is now configurable. ([issue 155](https://github.com/mapsforge/mapsforge/issues#issue/155))
- The map scale bar supports metric and imperial units. ([issue 167](https://github.com/mapsforge/mapsforge/issues#issue/167))
- A preferred language can now be set for the map-writer. ([issue 90](https://github.com/mapsforge/mapsforge/issues#issue/90))
- Introduced maven as build system for the mapsforge project.
- Major refactoring of the package structure in the map library.
- Moved the map-reader code to a new, Android-independent subproject.
- Many other minor improvements and bugfixes.


## Version 0.2.x

**Version 0.2.4 (2011-06-28)**

- AdvancedMapViewer now supports all location providers.
- Snap to position can be toggled if current position is shown.
- Support for multi-polygons in Overlay API.
- Support for long press events in Overlay API.
- Added metadata API for map files.
- Many other minor improvements and bugfixes.


**Version 0.2.3 (2011-05-12)**

- License change from GPL3 to LGPL3.
- Added animations to the MapView when zooming in and out.
- The text size of the map renderer can be adjusted at runtime.
- Rendering of symbols along ways, used for example on one-way roads.
- The maximum zoom level of the MapView may now be set at runtime.
- Many other minor improvements and bugfixes.


**Version 0.2.2 (2011-03-22)**

- Improved the overlay API and implemented some new classes.
- Better rendering of water areas and coastline situations.
- The tag list of the mapfile-writer can now be configured via an XML file.
- Added the possibility to render areas with a background pattern.
- Integrated support for piste maps features.
- Added a method to take a screenshot of the currently displayed map.
- The minimum zoom level of the MapView may now be set at runtime.
- Started to make the map tile cache on the memory card persistent.
- Many other minor improvements and bugfixes.


**Version 0.2.1 (2011-02-10)**

- Optimized binary map format, reduces the file size by 15-20%.
- Rewritten overlay implementation with many new features and bug fixes.
- Zoom level can now be changed by multi-tap and double-tap gestures.
- The mapfile-writer supports hard disk as temporary storage device.
- The mapfile-writer depends no longer on the JTS library and works faster.
- Added support for a couple of OpenStreetMap tags.
- Better rendering of way names.
- Better rendering of coastline and water areas.
- Many other minor improvements and bugfixes.


**Version 0.2.0 (2010-11-22)**

- New binary map format for more detailed and faster map rendering.
- Overlay API to display points, ways and polygons on top of the map.
- Better label and symbol placement with collision avoidance.
- The MapView now supports multi-touch and new tile download modes.
- First release of the Osmosis plugin to generate binary map files.
- Many other minor improvements and bugfixes.


## Version 0.1.x

**Version 0.1.0 (2010-06-27)**

- First public release, contains the android.map package.
