# Integration guide

This article describes how to integrate the library in your project. Check for current version at Maven badge on main page.

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

#### Java
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

You'll need also the SpatiaLite native library [files](../spatialite-android/natives/lib).

#### Java
```groovy
compile 'org.mapsforge:mapsforge-poi-awt:[CURRENT-VERSION]'
compile 'org.xerial:sqlite-jdbc:3.8.11.2'
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

## JitPack

We support also [JitPack](https://jitpack.io/#mapsforge/mapsforge) for publishing. This can be used for the releases, but it's also useful for integrating SNAPSHOT builds in your application (not available in Maven Central).

For example in order to include the `mapsforge-core` module `master-SNAPSHOT` with Gradle.

Add as repository:
```groovy
maven { url "https://jitpack.io" }
```

And declare as dependency:
```groovy
compile 'com.github.mapsforge.mapsforge:mapsforge-core:master-SNAPSHOT'
```

The same syntax applies for all modules. And with similar way you can declare the dependencies in Maven too.

## Jars

You can find jars (regular and with dependencies) in the [downloads](Downloads.md) section.

Third party jars can be found at their respective sites or in Maven Central repository.
