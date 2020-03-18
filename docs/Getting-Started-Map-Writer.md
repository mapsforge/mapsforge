# Mapsforge Map-Writer

The Mapsforge Map-Writer is a plug-in for the OpenStreetMap Osmosis Tool to convert OSM data files into maps that can be displayed with mapsforge.

This documentation is intended for those who want to create map files for use with mapsforge.

If you have any questions or problems, don't hesitate to ask our public [forum](https://groups.google.com/group/mapsforge-dev) for help.

## Introduction

This document describes the mapsforge map-writer plugin. It allows to convert OpenStreetMap data into the .map format which is needed to display maps with mapsforge-based applications. The tool is implemented as a plugin to the [Osmosis](http://wiki.openstreetmap.org/wiki/Osmosis) software. To use the tool, you are required to have a working installation of Osmosis and the writer plugin copied to the plugins directory of Osmosis. You should also be familiar with the Osmosis tool.

The mapsforge writer has not changed significantly from version 0.3 and files generated with either version can be loaded into mapsforge applications 0.4.+

## Plugin Usage

- Activate the plugin with the Osmosis parameter ‘--mapfile-writer’, or short ‘--mw’.
- The plugin requires an input stream of valid OSM data. Use for example the MySQL, PostGIS, XML or PBF-Binary tasks of Osmosis to read OSM data.
- Use the following optional parameters to configure the process of map creation:

### Basic Options

|**Option**|**Description**|**Valid Values**|**Default Value**|
|----------|---------------|----------------|-----------------|
|`file`|path to the output file, the file will be overwritten if existent||mapsforge.map|
|`type`|switch for main memory or hd mode|ram, hd|ram|
|`bbox`|bounding box definition as comma-separated list of coordinates in the form: minLat,minLon,maxLat,maxLon (be aware that osmosis does not allow **white space** in its command line parameters)|minLat, minLon, maxLat, maxLon in exactly this order as degrees|(blank)|
|`map-start-position`|write a start position to the file which is used, when the file is first opened in the MapViewer|latitude, longitude in degrees|(blank)|
|`map-start-zoom`|write a start zoom level to the file which is used, when the file is first opened in the MapViewer|zoom level as integer in [0, 21]|(blank)|
|`preferred-languages`|<ul><li>If not specified, only the default language with no tag will be written to the file.</li><li>If only one language is specified, it will be written if its tag is found, otherwise the default language will be written.</li><li>If multiple comma separated languages are specified, the default language will be written, followed by the specified languages (if present and if different than the default). (**v4**)</li></ul>|language code as defined in ISO 639-1 or ISO 639-2 if an ISO 639-1 code doesn't exist|(blank)|
|`tag-values`|enable usage of variable tag values, color strings and hex codes. (**v5**)|true/false|false|
|`comment`|writes a comment to the file||(blank)|
|`progress-logs`|enable progress logs|true/false|true|

### Advanced Options (only use when you know what you are doing)

|**Option**|**Description**|**Valid Values**|**Default Value**|
|----------|---------------|----------------|-----------------|
|`tag-conf-file`|path to an XML file that customizes the definition which OSM-tags are recognized|path to an XML file, please read section 'Defining a Custom Tag Mapping via XML' carefully before using thsi parameter|(blank) internal default tag mapping is used|
|`polygon-clipping`|use polygon clipping to reduce map file size (minimal performance overhead)|true/false|true|
|`way-clipping`|use way clipping to reduce map file size (minimal performance overhead)|true/false|true|
|`label-position`|compute label/symbol position for polygons that cover multiple tiles|true/false|false|
|`polylabel`|use in `label-position` calculation:<ul><li>Mapbox [polylabel](https://github.com/mapbox/polylabel) algorithm</li><li>JTS [interior point](https://locationtech.github.io/jts/javadoc/org/locationtech/jts/algorithm/InteriorPointArea.html) algorithm (minimal performance overhead)</li></ul>|<ul><li>true</li><li>false</li></ul>|false|
|`simplification-factor`|simplifies ways and polygons with a topology preserving algorithm similar to the Douglas Peucker algorithm, using as the maximum distance difference value the given simplification factor (evaluated in pixels on max zoom level of a base zoom level); on base zoom levels higher than 12, no simplification is computed|positive real number|2.5|
|`simplification-max-zoom`|The maximum base zoom level for which we apply a simplification algorithm to filter way points|positive integer|12|
|`bbox-enlargement`|amount of meters used for enlarging bounding boxes in computations|positive integer|20|
|`zoom-interval-conf`|configure the zoom intervals used in this file, configuration is given in the form: baseZoomA, minZoomA, maxZoomA, baseZoomB, minZoomB, maxZoomB,..., baseZoomN, minZoomN, maxZoomN, in most cases you do **not** need to alter the standard configuration|intervals must not overlap and must not contain gaps|5,0,7,10,8,11,14,12,21|
|`threads`|use multiple threads to increase performance (possible OOM)|positive integer|1|
|`debug-file`|switch for writing debug information to the file, _do **not activate** this option unless you know what you are doing_|true/false|false|

### Examples

- Write map file for Berlin using Binary-PBF format and writing into file /tmp/berlin.map:<br/>`$ bin/osmosis --rb file=../data/berlin.osm.pbf --mapfile-writer file=/tmp/berlin.map`<br/>
- Write map file for Germany using Binary-PBF format and writing into file /tmp/germany.map, setting the processing mode to hard disk:<br/>`$ bin/osmosis --rb file=../data/germany.osm.pbf --mapfile-writer file=/tmp/germany.map type=hd`<br/>
- Write map file for Bremen using XML format and writing into file /tmp/bremen.map, setting map start position to Bremen HBF:<br/>`$ bin/osmosis --rx file=../data/bremen.osm --mapfile-writer file=/tmp/bremen.map map-start-position=53.083418,8.81376`<br/>
- Write map file for Berlin-Dahlem. Data has been exported as XML from OSM website, so that we must use a bounding box definition:<br/>`$ bin/osmosis --rx file=../data/dahlem.osm --mw file=/tmp/dahlem-high.map bbox=52.4477300,13.2756600,52.4588200,13.2986600 `

### Known Pitfalls

- The plugin requires a bounding box definition, which is either included in the data or is given via the command line parameter `bbox`. Take note that the XML export functionality of the OSM website currently produces invalid bounding box definitions, so that the `bbox` parameter must be used in this case.
- If you installed the plugin into the user home, please make sure that you run osmosis with exactly this user and not with another user (e.g. the root user).
- There is a potential issue with tile grid artifacts inside areas at zoom levels 12-13, specially if they have stroke or semi-transparent fill. To solve this you can try a different zoom interval:
```bash
zoom-interval-conf=5,0,7,10,8,11,12,12,13,14,14,21
```

## Plugin Installation

- Download the release or snapshot writer plugin (**jar-with-dependencies**) from [Maven Central](https://search.maven.org/search?q=g:org.mapsforge) or [Sonatype OSS Repository Hosting](https://oss.sonatype.org/content/repositories/snapshots/org/mapsforge/) and read the Osmosis [documentation](http://wiki.openstreetmap.org/wiki/Osmosis/Detailed_Usage#Plugin_Tasks) for how to install a plugin.
- You may want to increase the Java heap space that may be allocated for osmosis. You can do so by editing the script $OSMOSIS_HOME/bin/osmosis(.bat). Insert a line with 'JAVACMD_OPTIONS=-Xmx800m'. This sets the maximum available Java heap space to 800M. Of course you can set this parameter to a value which ever fits best for your purpose.
- See http://wiki.openstreetmap.org/wiki/Osmosis/Installation for further information about Osmosis usage.

### Installation Example

- Let the home directory of the user ($USER_HOME) be "/home/prometheus/".
- Let the directory where osmosis has been extracted to ($OSMOSIS_HOME) be "/home/prometheus/development/osm/osmosis/osmosis-0.40.1/".
- Create the directory "/home/prometheus/.openstreetmap/osmosis/plugins/" and copy the mapsforge-map-writer-x.x.x.jar to this directory.

### Hardware Requirements

The currently realeased version of the map-writer gives you the choice of either processing the input data completely in main memory or using the hard disk for storing temporary data structures. You can switch between these processing modes by using the 'type' parameter, see Plugin Usage below.

We recommend using the main memory mode only for small input files (< 200 MB in PBF format) as it requires quite a huge amount of memory (about ten times the size of the input file).

## Land/Sea Feature Encoding

For a minimum compatibility of maps generated, the following standard for encoding land and sea areas is suggested:
 - either have a single sea polygon at the bottom or more complex sea polygons (this is really up to map makers to decide, the single sea polygon is smaller in the map file but has the effect of some double drawing), tag natural=sea
 - define land areas with tag natural=nosea
 - everything else

It is encouraged to include land/sea areas for the entire bbox of the map (so even if the real map data covers just one irregularly shaped polygon, the land/sea should be for the whole bbox), the overhead is often not great and the result looks much better on maps.

The old practice of implicitly assuming that the background is land-coloured and not defining land areas is deprecated. 

## Defining a Custom Tag Mapping via XML

This section describes how to configure the ***known tag set*** via an XML file. The *known tag set* comprises all OSM tags (for ways and POIs) that are known to the renderer. You can use the XML configuration to define which subset of the known tag set should be included in the map file and to configure the zoom levels on which map objects first appear.

The default internal tag mapping defined in https://github.com/mapsforge/mapsforge/blob/master/mapsforge-map-writer/src/main/config/tag-mapping.xml is kept in sync with the known tag set of the renderer. So, if you want to define your own custom mapping use the internal mapping as a reference.
Please consult the XML-Schema documentation of https://github.com/mapsforge/mapsforge/blob/master/resources/tag-mapping.xsd to learn about how to implement your custom mapping. The XML-Schema is very easy to understand. We recommend to use Eclipse as an editor for XML as it allows for auto-completion if you provide an XML-Schema.

You need to be aware that this configuration only defines what data is to be included in the map file. How the data is eventually rendered is specified by a rule-set that is attached to the renderer. So if you add any tag to the writer’s tag configuration that is not recognized by the renderer, it will not be displayed in the map. In this case, you have to make sure that you also define in which way the new tag is to be rendered. How to configure the rendering is described in the article.

## Changelog

### 0.10.0

- Polygon label/symbol centroid

### 0.9.0

- Variable tag values (**v5**)
- Osmosis 0.46 with protobuf 3

### 0.8.0

- Multiple threads option (default 1)

### 0.7.0

- Polygon label position enhancements
- House numbers (nodes) include at zoom 17
- Simplification max zoom option

### 0.6.1

- Add all tags from OSM wiki to area heuristics
- Minor changes to tag-mapping.xml (place=locality)

### 0.6.0

- Multilingual maps (**v4**)
- Language improved parsing
- Fix invalid number of way nodes

### 0.5.2

 - Minor changes to tag-mapping.xml (administrative boundaries, national parks)

### 0.5.1

- No significant changes

### 0.5.0

The mapfile format has not changed in 0.5.0, but the creation process has changed somewhat and it is recommended to use the latest version of the map file writer.
 - The encoding of multi-polygons is now much more efficient as multi-polygons are clipped to tile boundaries. Previous versions resulted in very large files that then triggered OOM situations in the map file reader.
 - Some smaller changes to the tag-mapping.xml (remove trees, duplicates).
 - Heuristics to only encode certain ways as areas.

### 0.4.0

There are no significant changes in 0.4.0 and the mapfile format has not changed.
- Small adjustments in the tag-mapping.xml configuration
- Updates to run with Osmosis 0.4.3.

### 0.3.0

- implements new binary format version 3
- improved handling of multi-polygons and relations
- improved clipping of polygons and lines (ways)
- more efficient encoding of coordinates
- supports definition of preferred language
- many other improvements and bug fixes

### 0.2.4

- minor improvements and bugfixes

### 0.2.3

- minor improvements and bugfixes

### 0.2.2

- improved coast line handling: detects tiles that are completely covered by water and marks them as such in the binary file
- the tag list of the map-writer can now be configured via an XML file, please read section 'Defining a Custom Tag Mapping via XML' carefully and see the new option 'tag-conf-file' below
- computes the distribution of tags in the input file and based on this assigns internal ids in an optimized fashion, reduces file size
- polygon clipping is more restrictive due to a conceptual problem with clipping outlined (instead of filled) polygons, increases file size (admittedly overcompensates file size reduction mentioned above)
- bug fixes

### 0.2.1

- supports hard disk as temporary storage device, reduces required amount of main memory significantly, see Hardware Requirements below
- support for MS Windows platform
- removed dependency on JTS library
- writes new file format v2, reduces file sizes about 20%, see SpecificationBinaryMapFile
- bug fixes
