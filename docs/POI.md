# POI API

This article describes how to use the POI API from the mapsforge POI library.

**The library is still in alpha state and is very likely to change.** It came from the `prototypes` branch.

If you have any questions or problems, don't hesitate to ask our public [mapsforge-dev](https://groups.google.com/group/mapsforge-dev) mailing list for help. You can also report bugs and improvement requests via our [issue tracker](https://github.com/mapsforge/mapsforge/issues).

## Introduction

_Points of Interest_ (POIs) are points with a given position, category and data. A POI database is used to store a set of POIs and to search for POIs within a given area.

The mapsforge POI library uses SQLite for storing POIs. For efficiency reasons Android's SQLite implementation is not used. Instead a custom wrapper is used to provide a SQLite implementation with R-tree functionality. Another successfully tested option is SpatiaLite, an advanced spatial extension to SQLite.

All reading and writing operations are done via classes implementing the `PoiPersistenceManager` interface. This allows adding, removing and changing POIs at any time. POI categories can be defined on creation time only. Categories are implemented as trees and can be accessed via classes implementing the `PoiCategoryManager` interface.

Externally you can use your favorite SQLite manager for browsing the database, e.g. spatialite-gui.

## Quick Start

This section provides you with information how to create a POI database, how to use it for POI search and how to visualize the results. It is recommended that you first get familiar with:
- [Getting-Started-Developers](Getting-Started-Developers.md)
- [Getting-Started-Map-Writer](Getting-Started-Map-Writer.md)
- [Getting-Started-Android-App](Getting-Started-Android-App.md)
- Overlay API

### Creating a POI Database

The POI writer is implemented as an Osmosis plugin. For this tutorial it is necessary to checkout and install the latest libraries.

It is also necessary to install Osmosis. [poi-writer plugin](http://ci.mapsforge.org/job/dev/lastSuccessfulBuild/artifact/mapsforge-poi-writer/build/libs/mapsforge-poi-writer-dev-SNAPSHOT.jar) installation is similar to map-writer plugin, you can see its [guide](Getting-Started-Map-Writer.md#plugin-installation) for detailed instructions. You should now have a fully working environment for developing apps using the POI API.

To convert OSM data to a POI database execute the following command:

```bash
$OSMOSIS_HOME/bin/osmosis --rb file=your_map_file.osm.pbf --poi-writer file=your_database.poi preferred-language=en tag-conf-file=poi-mapping.xml
```

The `--poi-writer`, or short `--pw` task indicates that the POI writer plugin should handle the data that was read from the given OSM stream.

#### Basic Options

|**Option**|**Description**|**Valid values**|**Default value**|
|----------|---------------|----------------|-----------------|
|`file`|Path to the output file, the file will be overwritten if existent. By convention we use the file ending `poi`.||mapsforge.poi|
|`bbox`|Bounding box definition as comma-separated list of coordinates in the form: minLat,minLon,maxLat,maxLon (be aware that osmosis does not allow **white space** in its command line parameters)|minLat, minLon, maxLat, maxLon in exactly this order as degrees|(blank)|
|`preferred-language`|If not specified, only the default language with no tag will be written to the file. If a language is specified, it will be written if its tag is found, otherwise the default language will be written.|language code as defined in ISO 639-1 or ISO 639-2|(blank)|
|`comment`|writes a comment to the file||(blank)|

Note: Multilingual POIs is an incubating feature.

#### Advanced Options (only use when you know what you are doing)

|**Option**|**Description**|**Valid values**|**Default value**|
|----------|---------------|----------------|-----------------|
|`tag-conf-file`|Path to an XML configuration file that contains mappings from OSM tags to category names and a hierarchy of those categories.|path to an XML file|(blank) internal default poi mapping is used|

### Example

With the POI database created you can now use it with mapsforge. You will also need the SQLite wrapper native library files in your project. You can use the Samples project as a boilerplate, as it already has all necessary files and dependencies. The library files are located each within a separate sub-folder for each target architecture (_x86_, _armeabi_, _armeabi-v7_). You can delete unneeded architectures to reduce file size.

The sources for those libraries are located in the `sqlite3-android` folder. To compile these manually you need the [Android NDK](http://developer.android.com/tools/sdk/ndk/index.html). You can edit SQLite's compilation options within `Android.mk`. The compilation process can be started with `ndk-build` from within the `jni` directory. The compiling process also moves the library files to their correct (sub)folders.

With everything set up you can check the 'POI search' example in Samples for:
- How a database is opened for read-write access. Any access to the database is encapsulated via classes implementing `PoiPersistenceManager`. The instantiation of these classes is done via a factory class. The categories and their hierarchy are maintained via classes implementing `PoiCategoryManager`. The category configuration is read-only.
- The `PoiPersistenceManager` object is used for querying the POIs in various ways. The query returns a collection of `PointOfInterest` objects. These are containers that contain a POI's position, ID, category and additional data. Additional data are stored as a string and can be arbitrary. There is no specification for the encoding of those data. The current implementation stores the POI's name as an UTF-8 encoded string in the data field.
- It is always a good idea to close an open database when there will be no more operations on it. This can simply be done by the `close()` method.
- With this done you can simply retrieve the POIs and add them as overlays.

## Advanced Topics

### Custom Category Configurations

If the provided category configuration does not fulfill your needs, you can easily create your own.

The default internal poi mapping is defined in https://github.com/mapsforge/mapsforge/blob/dev/mapsforge-poi-writer/src/main/config/poi-mapping.xml. So if you want to define your own custom mapping, use the internal mapping as a reference.

Please consult the XML-Schema documentation of https://github.com/mapsforge/mapsforge/blob/dev/resources/poi-mapping.xsd to learn about how to implement your custom mapping. The XML-Schema is very easy to understand. We recommend to use a proper editor for XML to allow auto-completion and validation if you provide an XML-Schema.

### Filtered Search

The API supports POI search inside a specified rectangle, near a given position or by a name pattern.

You can also use category filters for filtering the results based on the categories added to them.

### POI DB Schema

The DB schema consists of:
- `poi_categories` with the categories tree
- `poi_data` with the POI information
- Virtual & shadow correlated tables holding the R-tree index
- `metadata` with the DB metadata
