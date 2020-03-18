# RenderTheme

**This article describes how to use XML-based renderthemes to style maps, including the extensions of Rendertheme V4 or newer (new with release 0.5).**

If you have any questions or problems, don't hesitate to ask our public [forum](https://groups.google.com/group/mapsforge-dev) for help.

## Introduction

A render-theme is an XML file which contains rules and rendering instructions. Such files can be used to customize the visual style of the rendered map. The mapsforge map library comes with built-in render-themes similar to the [Osmarender](http://wiki.openstreetmap.org/wiki/Osmarender) style. More internal render-themes will be added in the future. External render-theme files are also supported and can be activated via the `tileRendererLayer.setXmlRenderTheme(new ExternalRenderTheme(File))` method at runtime.

Here is an example of a simple render-theme with a few different rules and rendering instructions:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rendertheme xmlns="http://mapsforge.org/renderTheme" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://mapsforge.org/renderTheme renderTheme.xsd" version="1">

    <!-- matches all ways with a "highway=trunk" or a "highway=motorway" tag -->
    <rule e="way" k="highway" v="trunk|motorway">
        <line stroke="#FF9900" stroke-width="2.5" />
    </rule>

    <!-- matches all closed ways (first node equals last node) with an "amenity=…" tag -->
    <rule e="way" k="amenity" v="*" closed="yes">
        <area fill="#DDEECC" stroke="#006699" stroke-width="0.3" />
    </rule>

    <!-- matches all nodes with a "tourism=hotel" tag on zoom level 16 and above -->
    <rule e="node" k="tourism" v="hotel" zoom-min="16">
        <symbol src="file:/path/to/symbol/icon/hotel.png" />
        <caption k="name" font-style="bold" font-size="10" fill="#4040ff" />
    </rule>
</rendertheme>
```

Syntax and semantics of render-theme files are similar but not identical to [Osmarender rules](http://wiki.openstreetmap.org/wiki/Osmarender/Rules). A formal render-theme description exists as an *XML schema document*, it can be found in the [repository](https://github.com/mapsforge/mapsforge/blob/master/resources/renderTheme.xsd). If an invalid render-theme is submitted to the map library, an `org.xml.sax.SAXException` will be thrown during XML parsing.

## Rules

A rule element has several attributes to specify which map elements the rule matches.

|**Attribute**|**Valid values**|**Description**|**Required**|
|-------------|----------------|---------------|------------|
|e|<ul><li>node</li><li>way</li><li>any</li></ul>|Defines which map element type will be matched.|yes|
|k|[string](http://www.w3.org/TR/xmlschema-2/#string)|The key of the tile source tag. <ul><li>A vertical bar "\|" can be used to specify multiple keys.</li><li>An asterisk "`*`" serves as wildcard character.</li>|yes|
|v|[string](http://www.w3.org/TR/xmlschema-2/#string)|The value of the tile source tag. <ul><li>A vertical bar "\|" can be used to specify multiple values.</li><li>An asterisk "`*`" serves as wildcard character.</li><li>A tilde "~" matches if the map element does not have a tag with the specified key.</li>|yes|
|closed|<ul><li>yes</li><li>no</li><li>any</li></ul>|Defines which ways will be matched. A way is considered as closed if its first node and its last node are equal.|no (default is *any*)|
|zoom-min|[unsignedByte](http://www.w3.org/TR/xmlschema-2/#unsignedByte)|The minimum zoom level on which the rule will be matched.|no (default is 0)|
|zoom-max|[unsignedByte](http://www.w3.org/TR/xmlschema-2/#unsignedByte)|The maximum zoom level on which the rule will be matched.|no (default is 127)|

Rules can be nested to any level of depth. This can be used to define rendering instructions which depend on multiple rules. It may also be used to structure complex declarations and to avoid redundancy:

```xml
<rule e="way" k="*" v="*" closed="no">
    <rule e="way" k="highway" v="motorway">
        <rule e="way" k="tunnel" v="true|yes">
            …
        </rule>
        <rule e="way" k="tunnel" v="~|no|false">
            …
        </rule>
    </rule>
</rule>
```

## Rendering Instructions

A rendering instruction specifies how a map element is drawn. Each rule element might include any number of rendering instructions. Except for labels and symbols, all rendering instructions are drawn on the map in the order of their definition.

At the moment, the following rendering instructions are available:

- area
- caption
- circle
- line
- lineSymbol
- pathText
- symbol

## Map background

The `rendertheme` root element has an optional `map-background` attribute which can be used to define the background color of the map. The default value is "#FFFFFF" (white).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rendertheme xmlns="http://mapsforge.org/renderTheme" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://mapsforge.org/renderTheme renderTheme.xsd" version="1" map-background="#FFEE99">
    …
</rendertheme>
```

## Rendertheme Version 4 or newer

With mapsforge release 0.5, we introduce the enhanced **Rendertheme V4 XML** with many new features to style a map.

Rendertheme V4 or newer remains **fully backward compatible** with version 3, meaning that any previously developed rendertheme definition will still render with V4 without any changes required (you do not even need to change the version number in the header).

You can find the full xsd in the mapsforge repository at https://github.com/mapsforge/mapsforge/blob/master/resources/renderTheme.xsd.

But if you want to develop your renderthemes further, Rendertheme V4 offers a number of enhancements. If you want to make use of the new features, you will first need to set your rendertheme version in the header to 4 or newer:

```xml
<rendertheme xmlns="http://mapsforge.org/renderTheme" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://mapsforge.org/renderTheme renderTheme.xsd" version="5" map-background="#f8f8f8" map-background-outside="#dddddd">
```

###Header Elements
 
The following header elements can be used:
 - version: should be "4" or newer now.
 - map-background: a color value to set the color of a blank tile. This should not be used to set the color of the sea or land. 
 - map-background-outside: a color value to set the color of the background of a map outside the map area. Effectively everything outside the map area will be overwritten by this color. For transparent layers, the color value will be ignored, but the outside area will be erased to transparent.  

## Stylemenus

One of the limitations of V3 was that even minor changes to a style required an entirely new rendertheme definition. Rendertheme V4 introduces the **stylemenu** that allows you to selectively turn rules on and off, in effect defining multiple layers in a map that can be controlled individually. 

To make use of style menus, you will first need to group the elements of your map into categories by attaching a **cat** element to them.

```xml
<rule cat="areas" e="way" k="landuse" v="landfill|quarry" zoom-min="14">
```

The same cat tag can be given to as many elements as you like, but each element can only have one cat tag.
However, this does not limit your ability to group elements, as in the style definitions, multiple categories can be combined into one layer. 

You can think about the cat tag as a switch that switches all elements in the rendertheme below it either on or off. If a category is turned off, all elements below it become invisible. If a category is turned on, all elements below become visible (unless they are themselves controlled via a category which is turned off).

Layers are combinations of categories that can be toggled on and off together and other layers (via **overlay**) that can be toggled on and off individually. In general, categories are not visible to a map user, but layers are. To make layers more user friendly, they can be named with the name tag in various languages.

```xml
<layer id="public_transport" visible="true">
    <name lang="de" value="Öffentlicher Verkehr"/>
    <name lang="en" value="Public Transport"/>
    <name lang="es" value="Transporte público"/>
    <name lang="fr" value="Transport public"/>
    <cat id="public_transport"/>
    <cat id="rail"></cat>
</layer>
```

A set of layer definitions makes up a style:

```xml
<stylemenu id="r4menu" defaultvalue="terrain" defaultlang="en">
    <layer id="shopping" visible="true">
        <name lang="de" value="Shopping"/>
        <name lang="en" value="Shopping"/>
        <name lang="es" value="Tiendas"/>
        <name lang="fr" value="Shopping"/>
        <cat id="shopping"/>
    </layer>

    <layer id="terrain" visible="true">
        <name lang="de" value="Topographischer Hintergrund"/>
        <name lang="en" value="Topographic Colours"/>
        <name lang="es" value="Colores topográficos"/>
        <name lang="fr" value="Couleurs topographiques"/>
        <cat id="topo"/>
    </layer>
</stylemenu>
```

The **visible** attribute is meant to indicate which of the layers are visible in the user interface. Layers where visible is set to false should not be seen by a user, and are thus useful to build base-layers of common categories from which then user-visible layers can be inherited, like this:

```xml
<layer id="base">
    <cat id="roads"/>
    <cat id="waterbodies"/>
    <cat id="landuse"/>
    <cat id="places"/>
    <overlay id="emergency"/>
    <overlay id="food"/>
</layer>

<layer id="simple" parent="base" visible="true">
    <name lang="de" value="Auto"/>
    <name lang="en" value="Driving"/>
    <name lang="es" value="Conducción"/>
    <name lang="fr" value="Conduite"/>
    <cat id="transport"/>
    <cat id="barrier"/>
    <cat id="driving"/>
    <overlay id="parking"/>
    <overlay id="shopping"/>
</layer>

<layer id="standard" parent="base" visible="true">
    <name lang="de" value="Stadt"/>
    <name lang="en" value="City"/>
    <name lang="es" value="City"/>
    <name lang="fr" value="Ville"/>
    <cat id="areas"/>
    <overlay id="tourism"/>
    <overlay id="sports"/>
    <overlay id="amenities"/>
    <overlay id="buildings"/>
    <overlay id="public_transport"/>
    <overlay id="accommodation"/>
    <overlay id="shopping"/>
</layer>
```

To turn layers on by default, add the **enabled=true** attribute. In this case, buildings should be by default visible, while parking related elements not:

```xml
<layer id="parking">
    <name lang="de" value="Parkplätze"/>
    <name lang="en" value="Parking"/>
    <name lang="es" value="Aparcamiento"/>
    <name lang="fr" value="Parking"/>
    <cat id="parking"/>
</layer>

<layer enabled="true" id="buildings">
    <name lang="de" value="Gebäude"/>
    <name lang="en" value="Buildings"/>
    <name lang="es" value="Edificios"/>
    <name lang="fr" value="Bâtiments"/>
    <cat id="buildings"/>
</layer>
```

The Samples app has a completely worked style menu.

## Map Integration

When a V4 rendertheme is parsed, the style menu definitons are parsed and a callback is made into the application giving it access to the stylemenu definitions. 

On Android, the stylemenus can thus be converted into standard Android settings, providing a seamless integration of the rendertheme definitions that do not require changes every time the rendertheme changes. There is an example on how to do this in the Samples app, see the Settings.java file. 

## Symbol Positioning

In previous versions it was difficult to position captions relative to map icons. Now it is possible to specify the position directly, without having to rely on position calculations. The positioning works regardless of map scaling as the relative distances are computed automatically. 

To associate a caption with a symbol, the symbol needs an id and the caption needs to refer to this symbol:

```xml
<rule cat="public_transport" e="node" k="aeroway" v="helipad" zoom-min="14">
    <symbol id="helipad" src="assets:symbols/transport/helicopter_pad.svg"/>
    <rule e="any" k="*" v="*" zoom-min="17">
        <caption priority="-20" symbol-id="helipad" k="name" position="above" font-style="bold" font-size="12" fill="#0092DA" stroke="#FFFFFF" stroke-width="2.0"/>
    </rule>
</rule>
```

The options for positioning are

- below: below icon, text is centered
- below_right
- below_left
- above: above icon, text is centered
- above_right
- above_left
- left: left of icon, text is right aligned
- right: right of icon, text is left aligned
- center
- auto: the default if nothing is set, allows the mapsforge library to select the best position

The **auto** (or nothing) setting is recommended. In version 0.5 of mapsforge this is always executed as below, but in future versions this will allow mapsforge to set the position. We are thinking of an improvement that will avoid labels that span multiple tiles and can thus improve rendering speed. 

## Priorities

In previous versions, labels were drawn in the order they were encountered often meaning that less important icons/captions were drawn first and did not leave any space for more important labels (e.g. town names drawn in favour of more important city names). This issue has been addressed by introducing priority levels for labels and icons.

Labels and icons are now drawn in order of priority, higher priorities first. The default priority is 0, so anything with a priority less than 0 will only be drawn if the space is not yet taken up, priorities higher than 0 will be drawn before default priority labels.

```xml
<rule e="node" k="place" v="town" zoom-min="8">
    <caption priority="30" k="name" font-style="bold" font-size="14" fill="#333380" stroke="#FFFFFF" stroke-width="2.0"/>
</rule>
<rule e="node" k="place" v="city" zoom-min="6" zoom-max="6">
    <caption priority="40" k="name" font-style="bold" font-size="11" fill="#333380" stroke="#FFFFFF" stroke-width="2.0"/>
</rule>
```

## Display
The display directive has been added whereever priorities can be used. The following values can be used:
 - always: an element will always be displayed regardless of space. 
 - ifspace: an element will only be displayed if the layout algorithm determines there is space and no higher priority element will take it. 
 - never: an element is not displayed. This is useful to blank out elements.

## SVG Symbols

Symbols can be either defined in the raster PNG format or as vector graphics in SVG format. Mapsforge uses libraries for [Android](https://github.com/BigBadaboom/androidsvg) and [Java](https://github.com/blackears/svgSalamander) that support a large subset of the [Tiny SVG](http://www.w3.org/TR/SVGTiny12/index.html) specification.

To speed up map rendering, SVG symbols are now only rendered at the point where they are needed and on Android are then written to disk in the PNG format. Subsequent uses will retrieve the file just like another raster image source. The file cache where these files are stored can be cleared through a static call.  

```java
AndroidGraphicFactory.clearResourceFileCache();
```
### SVG Scaling

SVG resources can now be automatically scaled to accommodate different device resolutions.

The options available are:

- If no size is given, a svg is rendered to a 20x20px multiplied by the device scale factor and any user adjustments.
- symbol-percent: the svg is rendered as a percentage size of its default size. This is the best way to make certain svg smaller or bigger than others.
- symbol-width and/or symbol-height: additional parameters give the absolute pixel size for the symbol, again adjusted by the scale factors. If only one dimension is set, the other is calculated from aspect ratio.

The tiling directive has now been removed as we have introduced code that tiles area shaders in a continuous pattern regardless of shader bitmap size. 

### SVG Bitmap Design

For area symbols it is best to design a symbol to fit a tile unless you know particular tile sizes will be used. To keep SVG sizes down, it is advisable to make use of the reuse facilities within SVG files and to remove any unneccessary comments and metadata. 

A simple SVG symbol for a cemetery can look like this:

```xml
<?xml version='1.0' encoding='UTF-8' standalone='no'?>
<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.0//EN' 'http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd'>
<svg width='256' height='256' viewport-fill="#33D40B" viewport-fill-opacity="0.3" xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'>
    <defs>
        <g id="cross">
            <rect width="24" height="96" x="20" y="2" id="rect3092" style="fill:#000000;stroke:#000000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1" />
            <rect width="60" height="24" x="2" y="26" id="rect3094" style="fill:#000000;fill-rule:evenodd;stroke:#000000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1" />
        </g>
    </defs>

    <g transform="translate(0,0)"><use xlink:href="#cross"/></g>
    <g transform="translate(140,70)"><use xlink:href="#cross"/></g>
    <g transform="translate(30,140)"><use xlink:href="#cross"/></g>
</svg>
```
