
# Specification: Mapsforge Binary Map File Format

## Conceptual design

The *mapsforge binary map file format* is designed for map rendering on devices with limited resources like mobile phones.
It allows for efficient storage of geographical information (e.g. OpenStreetMap data), fast tile-based access,
and filtering of map objects by zoom level.

The map file consists of several sub-files, each storing the map objects for a different zoom interval. Zoom intervals are non-overlapping groups of consecutive zoom levels. Each zoom interval is represented by a single member of the group, the so-called *base zoom level*. 


## General remarks

- All latitude and longitude coordinates are stored in microdegrees (degrees × 10<sup>6</sup>).
- Numeric fields with a fixed byte size are stored with *Big Endian* byte order.
- Unsigned numeric fields with a variable byte encoding are marked with *`VBE-U` INT* and stored as follows:
  - the first bit of each byte is used for continuation info, the other seven bits for data.
  - the value of the first bit is 1 if the following byte belongs to the field, 0 otherwise.
  - each byte holds seven bits of the numeric value, starting with the least significant ones.
- Signed numeric fields with a variable byte encoding are marked with *`VBE-S` INT* and stored as follows:
  - the first bit of each byte is used for continuation info, the other six (last byte) or seven (all other bytes) bits for data.
  - the value of the first bit is 1 if the following byte belongs to the field, 0 otherwise.
  - each byte holds six (last byte) or seven (all other bytes) bits of the numeric value, starting with the least significant ones.
  - the second bit in the last byte indicates the sign of the number. A value of 0 means positive, 1 negative.
  - numeric value is stored as magnitude for negative values (as opposed to two's complement).
- All strings are stored in UTF-8 as follows:
  - the length *L* of the UTF-8 encoded string in bytes as *`VBE-U` INT*.
  - *L* bytes for the UTF-8 encoding of the string.


## File structure

For each zoom interval a so called **sub-file** is created. A sub-file consists of a **tile index segment** that stores a fixed-size pointer for each tile created in the **tile data segment**. The order of storing tiles to the tile data segment and their corresponding pointers to the tile index segment is row-wise and within a row column-wise. Rows and columns are inherently given by the grid layout of the tiles that is defined by the rectangular bounding box. For each tile in the grid, meta information is available in the **tile header** accompanied by its payload data (POIs and ways).

To read the data of a specific tile in the sub-file, the position of the fixed-size pointer in the index can be computed from the tile coordinates. The index entry points to the offset in the sub-file where the data is stored. Tile coordinates are implicitly given due to the structure of the tile index, thus no tile coordinates need to be stored along each tile.

    # meta data
    file header
    
    # sub-files
    for each sub-file
        # tile index segment
        tile index header
        tile index entries
    
        # tile data segment
        for each tile
            tile header
            for each POI
                POI data
            for each way
                way properties
                way data


## File header


|**bytes**|**optional**|**name**|**description**|
|---------|------------|--------|---------------|
|20||magic byte|`mapsforge binary OSM`|
|4||header size|size of the file header in bytes (without magic byte) as 4-byte *INT*|
|4||file version|version number of the currently used binary file format as 4-byte *INT*|
|8||file size|The total size of the map file in bytes|
|8||date of creation|date in milliseconds since 01.01.1970 as 8-byte *LONG*|
|16||bounding box|geo coordinates of the bounding box in microdegrees as 4\*4-byte *INT*, in the order minLat, minLon, maxLat, maxLon|
|2||tile size|the tile size in pixels (e.g. 256)|
|variable||projection|defines the projection used to create this file as a string|
|1||flags|<ul><li>1. bit (mask 0x80): flag for existence of debug information</li><li>2. bit (mask 0x40): flag for existence of the *map start position* field</li><li>3. bit (mask 0x20): flag for existence of the *start zoom level* field</li><li>4. bit (mask 0x10): flag for existence of the *language(s) preference* field</li><li>5. bit (mask 0x08): flag for existence of the *comment* field</li><li>6. bit (mask 0x04): flag for existence of the *created by* field</li><li>7.-8. bit (mask 0x02, 0x01): reserved for future use</li></ul>|
|8|yes|map start position|geo coordinate in microdegrees as 2\*4-byte *INT*, in the order lat, lon|
|1|yes|start zoom level|zoom level of the map at first load|
|variable|yes|language(s) preference|The preferred language(s) for names as defined in ISO 639-1 or ISO 639-2. This field is copied from the preferred-languages option of the map writer.|](|variable||zoom interval configuration|<ul><li>for each zoom interval:<ul><li>base zoom level as *BYTE*</li><li>minimal zoom level as *BYTE*</li><li>maximal zoom level as *BYTE*</li><li>absolute start position of the sub file as 8-byte *LONG*</li><li>size of the sub-file as 8-byte *LONG*</li></ul></li></ul>|) as string|
|variable|yes|comment|comment as a string|
|variable|yes|created by|The name of the application which created the file as a string|
|variable||POI tags|<ul><li>amount of tags as 2-byte *SHORT*</li><li>tag names as a strings</li><li>tag IDs are implicitly derived from the order of tag names, starting with 0</li></ul>|
|variable||way tags|<ul><li>amount of tags as 2-byte *SHORT*</li><li>tag names as a strings</li><li>tag IDs are implicitly derived from the order of tag names, starting with 0</li></ul>|
|1||amount of zoom intervals|defines the amount of zoom intervals used in this file|
|variable||zoom interval configuration|<ul><li>for each zoom interval:<ul><li>base zoom level as *BYTE*</li><li>minimal zoom level as *BYTE*</li><li>maximal zoom level as *BYTE*</li><li>absolute start position of the sub file as 8-byte *LONG*</li><li>size of the sub-file as 8-byte *LONG*</li></ul></li></ul>|


### Tile index header


|**bytes**|**optional**|**name**|**description**|
|---------|------------|--------|---------------|
|16|yes|index signature|If the debug bit in the file header is set:<br />`+++IndexStart+++`|


### Tile index entry


|**bytes**|**optional**|**name**|**description**|
|---------|------------|--------|---------------|
|5||index entry|<ul><li>1. bit (mask: 0x80 00 00 00 00): flag to indicate whether the tile is completely covered by water (e.g. a tile amidst the ocean)</li><li>2.-40. bit (mask: 0x7f ff ff ff ff): 39 bit offset of the tile in the sub file as 5-bytes *LONG* (optional debug information and index size is also counted; byte order is BigEndian i.e. most significant byte first)<br />If the tile is empty offset(tile,,i,,) = offset(tile,,i+1,,)</li></ul><br />Note: to calculate how many tile index entries there will be, use the formulae at [http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames] to find out how many tiles will be covered by the bounding box at the base zoom level of the sub file|


### Tile header


|**bytes**|**optional**|**name**|**description**|
|---------|------------|--------|---------------|
|32|yes|tile signature|If the debug bit in the file header is set:<br />`###TileStartX,Y###` where X and Y indicate the tile coordinates of the current tile; the text is always padded to 32 bytes by adding whitespaces|
|variable||zoom table|A table indicating the number of POIs and ways in this tile for the different zoom levels covered by the enclosing sub-file. Let Z be the number of zoom levels supported by the enclosing sub-file (e.g. 6 for a sub-file that covers levels 12-17). Then the table has Z rows and 2 columns (first column: POIs, second column: ways). Each cell in the table represents the number of POIs or ways on the specific zoom level. The table is written row-wise and values are encoded as *`VBE-U` INT*.|
|variable||first way offset|offset in bytes to the first way in this tile as *`VBE-U` INT*. The counting starts at the following byte (i.e. first way offset itself is not counted).|


### POI data


|**bytes**|**optional**|**name**|**description**|
|---------|------------|--------|---------------|
|32|yes|POI signature|If the debug bit in the file header is set:<br />`***POIStartX***` where X defines the OSM-ID of the POI; the text is always padded to 32 bytes by adding whitespaces|
|variable||position|geo coordinate difference to the top-left corner of the current tile as *`VBE-S` INT*, in the order lat-diff, lon-diff|
|1||special byte|<ul><li>1.-4. bit: layer (OSM-Tag: layer=...) + 5 (to avoid negative values)</li><li>5.-8. bit: amount of tags for the POI</li></ul>|
|variable||tag id|for each tag of the POI:<ul><li>tag id as *`VBE-U` INT*</li><li>variable values as different data types, whose content can be evaluated from tag's wildcard</li></ul>|
|1||flags|<ul><li>1. bit: flag for existence of a POI name</li><li>2. bit: flag for existence of a house number</li><li>3. bit: flag for existence of an elevation</li><li>4.-8. bit: reserved for future use</li></ul>|
|variable|yes|name|name of the POI as a string|
|variable|yes|house number|house number of the POI as a string|
|variable|yes|elevation|elevation of the POI in meters as *`VBE-S` INT*|



### Way properties


|**bytes**|**optional**|**name**|**description**|
|---------|------------|--------|---------------|
|32|yes|way signature|If the debug bit in the file header is set:<br />`---WayStartX---` where X defines the OSM-ID of the way; the text is always padded to 32 bytes by adding whitespaces|
|variable||way data size|number of bytes that are needed to encode the current way as *`VBE-U` INT*, starting from the sub tile bitmap (i.e. way signature and way size are not counted)|
|2||sub tile bitmap|A tile on zoom level z is made up of exactly 16 sub tiles on zoom level z+2<br />for each sub tile (row-wise, left to right):<ul><li>1 bit that represents a flag whether the way is relevant for the sub tile</li></ul>Special case: coastline ways must always have all 16 bits set.|
|1||special byte|<ul><li>1.-4. bit: layer (OSM-Tag: layer=...) + 5 (to avoid negative values)</li><li>5.-8. bit: amount of tags for the way</li></ul>|
|variable||tag id|for each tag of the way:<ul><li>tag id as *`VBE-U` INT*</li><li>variable values as different data types, whose content can be evaluated from tag's wildcard</li></ul>|
|1||flags|<ul><li>1. bit: flag for existence of a way name</li><li>2. bit: flag for existence of a house number</li><li>3. bit: flag for existence of a reference</li><li>4. bit: flag for existence of a label position</li><li>5. bit: flag for existence of *number of way data blocks* field<ul><li>case 0: field does not exist, number of blocks is one</li><li>case 1: field exists, more than one block</li></ul></li><li>6. bit: flag indicating encoding of way coordinate blocks<ul><li>case 0: single delta encoding</li><li>case 1: double delta encoding</li></ul></li><li>7.-8. bit: reserved for future use</li></ul>|
|variable|yes|name|name of the way as a string|
|variable|yes|house number|house number of the way as a string|
|variable|yes|reference|reference of the way as a string|
|variable|yes|label position|geo coordinate difference to the first way node in microdegrees as 2 × *`VBE-S` INT*, in the order lat-diff, lon-diff|
|variable|yes|number of way data blocks|The amount of following way data blocks as *`VBE-U` INT*.|


### Way data


|**bytes**|**optional**|**name**|**description**|
|---------|------------|--------|---------------|
|variable||number of way coordinate blocks|The amount of following way coordinate blocks as *`VBE-U` INT*. An amount larger than 1 indicates a multipolygon with the first block representing the outer way coordinates and the following blocks the inner way coordinates.|
|variable||way coordinate block|for each way coordinate block:<ul><li>amount of way nodes of this way as *`VBE-U` INT*</li><li>geo coordinate difference to the top-left corner of the current tile as *`VBE-S` INT*, in the order lat-diff, lon-diff</li><li>geo coordinates of the remaining way nodes stored as differences to the previous way node in microdegrees as 2 × *`VBE-S` INT* in the order lat-diff, lon-diff using either single or double delta encoding (see below).</li></ul>|

Coordinates in a way data block are encoded in either 'single-delta' or 'double-delta' format according to the flag in the way properties. The encoder chooses the most efficient format on a way-by-way basis so most maps will contain examples of both types.

For single-delta encoding the lat-diff and lon-diff values describe the offset of the node compared to its predecessor.

    let x1 be the lat of the previous way node and x2 be the lat of the current way node.
    Then the difference is defined as x2 - x1.

For double-delta encoding the lat-diff and lon-diff values describe the *change* of the offset compared to the offset of the previous node, after the first node. The following pseudocode shows how to decode coordinates encoded in this format.

    set 'previousLat' to the latitude (in degrees) of the top-left corner of the current tile
    set 'previousOffset' to zero
    set 'count' to zero
    
    while there is data to be read:
        set 'encodedValue' to the next item of data (VBE-S, in microdegrees) 
        set 'lat' to 'previousLat' + 'previousOffset' + 'encodedValue' / 1,000,000
        if 'count' is greater than zero, then
            set 'previousOffset' to 'lat' - 'previousLat'
        set 'previousLat' to 'lat'
        
        'lat' contains the decoded data
        add one to 'count'


    Example of decoding double-delta encoded data:
        tile origin: 52.123456, encoded values: -8286, -57, 129, -15, -129
        decoded values: 52.11517, 52.115113, 52.115185, 52.115242, 52.11517


## Version history


|**Version**|**Date**|**Changes**|
|-----------|--------|-----------|
|1|2010-11-21|Initial release of the specification|
|2|2011-01-26|<ul><li>Introduced variable byte encoding for some numeric fields to reduce the file size</li><li>Modified some field names and descriptions for clarification</li><li>Offset encoding is now used on all coordinates</li></ul>|
|3|2012-03-18|<ul><li>Ways are stored as multiple segments</li><li>Ways can also have a house number</li><li>Removed obsolete data</li><li>Added *language preference* field to the header</li><li>Added *file size* field to the header</li><li>Added *start zoom level* field to the header</li><li>Added *created by* field to the header</li><li>Added a flag for single and double delta encoding</li><li>Reordered some fields</li><li>Removed some data type related limitations</li></ul>|
|4|2015-11-25|<ul><li>Multilingual names storage</li></ul>|
|5|2017-12-03|<ul><li>Variable tag values storage</li></ul>|
