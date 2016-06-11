# Integration guide

This article describes how to integrate mapsforge library in your project. Check for latest version at Maven badge on main page.

## Gradle

### 1. Map

#### 1.1 Core
```groovy
compile 'org.mapsforge:mapsforge-core:0.6.1'
compile 'org.mapsforge:mapsforge-map:0.6.1'
compile 'org.mapsforge:mapsforge-map-reader:0.6.1'
compile 'net.sf.kxml:kxml2:2.3.0'
```

#### 1.2 Android
```groovy
compile 'org.mapsforge:mapsforge-map-android:0.6.1'
compile 'com.caverock:androidsvg:1.2.2-beta-1'
```

Optionally:
```groovy
compile('org.mapsforge:mapsforge-map-android-extras:0.6.1') {
    transitive = false
}
```

#### 1.3 Java
```groovy
compile 'org.mapsforge:mapsforge-map-awt:0.6.1'
compile 'com.kitfox.svg:svg-salamander:1.0'
```

### 2. POI

#### 2.1 Core
```groovy
compile 'org.mapsforge:mapsforge-core:0.6.1'
compile 'org.mapsforge:mapsforge-poi:0.6.1'
```

#### 2.2 Android
```groovy
compile 'org.mapsforge:mapsforge-poi-android:0.6.1'
compile 'org.mapsforge:spatialite-android:0.6.1'
```

You'll need also the SpatiaLite native library [files](../spatialite-android/libs).

#### 2.3 Java
```groovy
compile 'org.mapsforge:mapsforge-poi-awt:0.6.1'
compile 'org.xerial:sqlite-jdbc:3.8.11.2'
```

## Maven

The dependencies for Maven are declared in a similar way. For example:

```xml
<dependency>
    <groupId>org.mapsforge</groupId>
    <artifactId>mapsforge-core</artifactId>
    <version>0.6.1</version>
</dependency>
```

## JitPack

We support also [JitPack](https://jitpack.io/#mapsforge/mapsforge) for publishing Mapsforge. This can be used for the releases, but it's also useful for integrating SNAPSHOT builds in your application (not available in Maven central).

For example in order to include the `mapsforge-core` module `master-SNAPSHOT` with Gradle.

Add as repository:
```groovy
maven { url "https://jitpack.io" }
```

And declare as dependency:
```groovy
compile 'com.github.mapsforge.mapsforge:mapsforge-core:master-SNAPSHOT'
```

The same syntax applies for all Mapsforge modules. And with similar way you can declare the dependencies in Maven too.

## Jars

You can find Mapsforge (regular and with dependencies) jars in the [downloads](Downloads.md) section.

External dependencies jars can be found at their respective sites or in Maven central repository.
