# POI API

This article describes how to use the POI API in mapsforge POI library (from `prototypes` branch).

If you have any questions or problems, don't hesitate to ask our public [forum](https://groups.google.com/group/mapsforge-dev) for help.

## Introduction

_Points of Interest_ (POIs) are points with a given position, category and data. A POI database is used to store a set of POIs and to search for POIs within a given area.

The mapsforge POI library uses SQLite for storing POIs. For efficiency reasons Android's SQLite implementation is not used. Instead [SQLite Android Bindings](https://sqlite.org/android/) is used to provide an SQLite implementation with [R-tree](https://sqlite.org/rtree.html) functionality.

All reading and writing operations are done via classes implementing the `PoiPersistenceManager` interface. This allows adding, removing and changing POIs at any time. POI categories can be defined on creation time only. Categories are implemented as trees and can be accessed via classes implementing the `PoiCategoryManager` interface.

Externally you can use your favorite SQLite manager for browsing the database, e.g. [spatialite-gui](https://www.gaia-gis.it/fossil/spatialite_gui/index).

## Quick Start

This section provides you with information how to create a POI database, how to use it for POI search and how to visualize the results.

### Creating a POI Database

The tool is implemented as a plugin to the [Osmosis](http://wiki.openstreetmap.org/wiki/Osmosis) software. To use the tool, you are required to have a working installation of Osmosis and the writer plugin copied to the plugins directory of Osmosis. You should also be familiar with the Osmosis tool.

Download the release or snapshot writer plugin (**jar-with-dependencies**) from [Maven Central](https://search.maven.org/search?q=g:org.mapsforge) or [Sonatype OSS Repository Hosting](https://oss.sonatype.org/content/repositories/snapshots/org/mapsforge/) and read the Osmosis [documentation](http://wiki.openstreetmap.org/wiki/Osmosis/Detailed_Usage#Plugin_Tasks) for how to install a plugin.

To convert OSM data to a POI database execute the following command:

```bash
$OSMOSIS_HOME/bin/osmosis --rb file=your_map_file.osm.pbf --poi-writer file=your_database.poi
```

The `--poi-writer`, or short `--pw` task indicates that the POI writer plugin should handle the data that was read from the given OSM stream.

#### Basic Options

|**Option**|**Description**|**Valid values**|**Default value**|
|----------|---------------|----------------|-----------------|
|`file`|Path to the output file, the file will be overwritten if existent. By convention we use the file ending `poi`.||mapsforge.poi|
|`bbox`|Bounding box definition as comma-separated list of coordinates in the form: minLat,minLon,maxLat,maxLon (be aware that osmosis does not allow **white space** in its command line parameters).|minLat, minLon, maxLat, maxLon in exactly this order as degrees|(blank)|
|`all-tags`|Export all tags. If false only the name will be exported.|true/false|true|
|`preferred-language`|If not specified, only the default language with no tag will be written to the file. If a language is specified, it will be written if its tag is found, otherwise the default language will be written. *Redundant if all-tags is true.*|language code as defined in ISO 639-1 or ISO 639-2 if an ISO 639-1 code doesn't exist|(blank)|
|`comment`|Writes a comment to the file.||(blank)|
|`progress-logs`|Enable progress logs|true/false|true|

#### Advanced Options (only use when you know what you are doing)

|**Option**|**Description**|**Valid values**|**Default value**|
|----------|---------------|----------------|-----------------|
|`tag-conf-file`|Path to an XML configuration file that contains mappings from OSM tags to category names and a hierarchy of those categories.|path to an XML file|(blank) internal default poi mapping is used|
|`names`|Add only named entities.|true/false|true|
|`ways`|Also parse ways.|true/false|true|
|`normalize`|Add normalized_name (for accent insensitive search). *Works if all-tags is true.*|true/false|false|
|`geo-tags`|Add geo tags.|true/false|false|
|`filter-categories`|Drop empty categories.|true/false|true|

### Example

With the POI database created you can now use it with mapsforge. For testing purposes, you may also use one of our ready-to-use POI databases from our [server](https://download.mapsforge.org/pois/) (not suitable for mass downloads). You will also need the SQLite Android Bindings native library files in your project. You can use the Samples project as a boilerplate, as it already has all necessary files and dependencies. The library files are located each within a separate sub-folder for each target architecture (_armeabi-v7a_, _arm64-v8a_, _x86_, _x86_64_). You can delete unneeded architectures to reduce file size.

The sources for those libraries can be found at [SQLite Android Bindings](https://sqlite.org/android/) site. To compile these manually you need the [Android NDK](https://developer.android.com/ndk/). The compilation process can be started with `ndk-build` from within the `jni` directory. The compiling process also moves the library files to their correct (sub)folders.

With everything set up you can check the ['POI search' example](https://github.com/mapsforge/mapsforge/blob/master/mapsforge-samples-android/src/main/java/org/mapsforge/samples/android/PoiSearchViewer.java) for:
- How a database is opened for read access. Any access to the database is encapsulated via classes implementing `PoiPersistenceManager`. The instantiation of these classes is done via a factory class. The categories and their hierarchy are maintained via classes implementing `PoiCategoryManager`. The category configuration is read-only.
- The `PoiPersistenceManager` object is used for querying the POIs in various ways. The query returns a collection of `PointOfInterest` objects. These are containers that contain a POI's position, ID, category and additional data. Additional data are stored as a string and can be arbitrary. There is no specification for the encoding of those data. The current implementation stores the POI's tags as an UTF-8 encoded string in the data field.
- It is always a good idea to close an open database when there will be no more operations on it. This can simply be done by the `close()` method.
- With this done you can simply retrieve the POIs and add them as overlays.
- In order to get POI write access call `getPoiPersistenceManager(filename, false)` via the factory class.

## Advanced Topics

### Custom Category Configurations

If the provided category configuration does not fulfill your needs, you can easily create your own.

The default internal poi mapping is defined in https://github.com/mapsforge/mapsforge/blob/master/mapsforge-poi-writer/src/main/config/poi-mapping.xml. So if you want to define your own custom mapping, use the internal mapping as a reference.

Please consult the XML-Schema documentation of https://github.com/mapsforge/mapsforge/blob/master/resources/poi-mapping.xsd to learn about how to implement your custom mapping. The XML-Schema is very easy to understand. We recommend to use a proper editor for XML to allow auto-completion and validation if you provide an XML-Schema.

### Filtered Search

The API supports POI search inside a specified rectangle, near a given position or by a tag-data pattern.
You can also use category filters for filtering the results based on the categories added to them.
And lastly you can search by OSM tags. With '*' you can search through all tags, 
e.g. `Tag tag1 = new Tag("*", Pergamonmuseum);` 
Then you add them to a list `tagList.add(new Tag("addr:street", "Bodestraße"))` and call
`persistenceManager.findInRect(bbox, categoryFilter, tagList, 1)`

### POI DB Schema

The DB schema consists of:
- `poi_categories` with the categories tree
- `poi_data` with the POI information
- `poi_category_map` with the POI categories mapping
- Virtual & shadow correlated tables holding the R-tree index
- `metadata` with the DB metadata

## Version history

|**Version**|**Date**|**Changes**|
|-----------|--------|-----------|
|1|2016-06-11|Initial release of the specification|
|2|2017-12-03|<ul><li>POI multiple categories</li></ul>|
