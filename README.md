The mapsforge project provides free and open software for the rendering of maps based on OpenStreetMap data. Currently, we offer a library for ad-hoc map rendering on Android devices and in Java stand-alone applications.

**The current stable release is 0.5.1. See the [changelog](docs/Changelog.md) for details, [download official 0.5.1 binaries](docs/Downloads.md).**

- The mapsforge project uses a [compact file format](docs/Specification-Binary-Map-File.md) for fast ad-hoc rendering of OpenStreetMap data.
- We provide tools to [compile your own maps](docs/Getting-Started-Map-Writer.md) and also [precompiled maps](http://download.mapsforge.org).
- It provides [simple boilerplate code](docs/Getting-Started-Android-App.md) to build applications for Android that display OpenStreetMap-based maps.
- It provides a library to build standalone applications in Java.
- Mapsforge maps can be flexibly styled with XML style files ([render themes](docs/Rendertheme.md)).
- Mapsforge supports Android 2.3 and above. Mapsforge has been tested on Android 5 Lollipop.
- Mapsforge is used by many [applications](docs/Mapsforge-Applications.md).
- Mapsforge is in active development: [changelog](docs/Changelog.md), [contributors](docs/Contributors.md). 
- [API documentation](http://mapsforge.org/docs).
- [Official binary releases](docs/Downloads.md). 
- [Latest builds from CI server](http://ci.mapsforge.org/) and [Sonar server](http://sonar.mapsforge.org/).
- Mapsforge is free and open source, licensed under the [LGPL3 license](https://www.gnu.org/copyleft/lesser.html).
- [Mapsforge Talks](docs/Mapsforge-Talks.md).
- [Mailing List](https://groups.google.com/forum/#!forum/mapsforge-dev).

** Fork to implement a markerclusterer, ideally like http://google-maps-utility-library-v3.googlecode.com/svn/trunk/markerclusterer/examples/advanced_example.html ;-) **
based on http://ge.tt/7Zq63CH/v/1

** This fork does not a change to the libary itself, it just adds one additional sample, derived from some generic classes:
https://github.com/vennekamp/mapsforge/tree/master/Applications/Android/Samples/src/main/java/org/mapsforge/applications/android/samples/markerclusterer 
and a sample implementation (https://github.com/vennekamp/mapsforge/blob/master/Applications/Android/Samples/src/main/java/org/mapsforge/applications/android/samples/ClusterMapActivity.java), added to Sample.java.

### Screenshots:

![Screenshot Samples App Berlin 1](docs/images/screenshot-berlin-1.png)
![Screenthot Samples App Berlin 2](docs/images/screenshot-berlin-2.png)
![Screenshot Samples App Berlin 3](docs/images/screenshot-berlin-3.png)
