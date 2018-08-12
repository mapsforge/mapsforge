# Integration guide

This article describes how to integrate the library in your project, with Gradle / Maven / Jars or SNAPSHOT builds.

Current version is [![Maven Central](https://img.shields.io/maven-central/v/org.mapsforge/mapsforge-core.svg)](https://search.maven.org/search?q=g:org.mapsforge)

## Gradle

### Map

#### Core
```groovy
implementation 'org.mapsforge:mapsforge-core:[CURRENT-VERSION]'
implementation 'org.mapsforge:mapsforge-map:[CURRENT-VERSION]'
implementation 'org.mapsforge:mapsforge-map-reader:[CURRENT-VERSION]'
implementation 'org.mapsforge:mapsforge-themes:[CURRENT-VERSION]'
implementation 'net.sf.kxml:kxml2:2.3.0'
```

#### Android
```groovy
repositories {
    maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}

implementation 'org.mapsforge:mapsforge-map-android:[CURRENT-VERSION]'
implementation 'com.caverock:androidsvg:1.3-SNAPSHOT'
```

#### Desktop
```groovy
implementation 'org.mapsforge:mapsforge-map-awt:[CURRENT-VERSION]'
implementation 'com.metsci.ext.com.kitfox.svg:svg-salamander:0.1.19'
```

### POI

#### Core
```groovy
implementation 'org.mapsforge:mapsforge-core:[CURRENT-VERSION]'
implementation 'org.mapsforge:mapsforge-poi:[CURRENT-VERSION]'
```

#### Android
```groovy
implementation 'org.mapsforge:mapsforge-poi-android:[CURRENT-VERSION]'
implementation 'org.mapsforge:spatialite-android:[CURRENT-VERSION]'
implementation 'org.mapsforge:spatialite-android:[CURRENT-VERSION]:natives-armeabi'
implementation 'org.mapsforge:spatialite-android:[CURRENT-VERSION]:natives-armeabi-v7a'
implementation 'org.mapsforge:spatialite-android:[CURRENT-VERSION]:natives-x86'
```

#### Desktop
```groovy
implementation 'org.mapsforge:mapsforge-poi-awt:[CURRENT-VERSION]'
implementation 'org.xerial:sqlite-jdbc:3.20.0'
```

## Snapshots

We publish SNAPSHOT builds to Sonatype OSS Repository Hosting.

You need to add the repository:
```groovy
repositories {
    maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}
```

And declare the dependencies like:
```groovy
implementation 'org.mapsforge:mapsforge-core:master-SNAPSHOT'
...
```

For checking latest snapshot on every build:
```groovy
configurations.all {
    resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
}
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

You can find release and snapshot jars (regular and with dependencies) in [Maven Central](https://search.maven.org/search?q=g:org.mapsforge) and [Sonatype OSS Repository Hosting](https://oss.sonatype.org/content/repositories/snapshots/org/mapsforge/).

Third party jars can be found at their respective sites or in Maven Central repository.
