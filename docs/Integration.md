# Integration guide

This article describes how to integrate the library in your project, with Gradle / Maven / Jars or use SNAPSHOT builds.

Current version is [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.mapsforge/mapsforge/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.mapsforge/mapsforge)

## Gradle

### Map

#### Core
```groovy
compile 'org.mapsforge:mapsforge-core:[CURRENT-VERSION]'
compile 'org.mapsforge:mapsforge-map:[CURRENT-VERSION]'
compile 'org.mapsforge:mapsforge-map-reader:[CURRENT-VERSION]'
compile 'net.sf.kxml:kxml2:2.3.0'
```

#### Android
```groovy
compile 'org.mapsforge:mapsforge-map-android:[CURRENT-VERSION]'
compile 'com.caverock:androidsvg:1.2.2-beta-1'
```

Optionally:
```groovy
compile('org.mapsforge:mapsforge-map-android-extras:[CURRENT-VERSION]') {
    transitive = false
}
```

#### Desktop
```groovy
compile 'org.mapsforge:mapsforge-map-awt:[CURRENT-VERSION]'
compile 'com.kitfox.svg:svg-salamander:1.0'
```

### POI

#### Core
```groovy
compile 'org.mapsforge:mapsforge-core:[CURRENT-VERSION]'
compile 'org.mapsforge:mapsforge-poi:[CURRENT-VERSION]'
```

#### Android
```groovy
compile 'org.mapsforge:mapsforge-poi-android:[CURRENT-VERSION]'
compile 'org.mapsforge:spatialite-android:[CURRENT-VERSION]'
```

You'll need also the SpatiaLite native library [files](../spatialite-android/natives).

#### Desktop
```groovy
compile 'org.mapsforge:mapsforge-poi-awt:[CURRENT-VERSION]'
compile 'org.xerial:sqlite-jdbc:3.8.11.2'
```

## Snapshots

We publish regularly SNAPSHOT builds to Sonatype OSS Repository Hosting.

You need to add the repository:
```groovy
repositories {
    maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}
```

And declare the dependencies like:
```groovy
compile 'org.mapsforge:mapsforge-core:master-SNAPSHOT'
...
```

## Maven

The dependencies for Maven are declared in a similar way. For example:

```xml
<dependency>
    <groupId>org.mapsforge</groupId>
    <artifactId>mapsforge-core</artifactId>
    <version>[CURRENT-VERSION]</version>
</dependency>
```

## Jars

You can find release and snapshot jars (regular and with dependencies) in [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.mapsforge%22) and [Sonatype OSS Repository Hosting](https://oss.sonatype.org/content/repositories/snapshots/org/mapsforge/).

Third party jars can be found at their respective sites or in Maven Central repository.
