[![Maven Central](https://img.shields.io/maven-central/v/org.mapsforge/mapsforge-core.svg)](https://repo1.maven.org/maven2/org/mapsforge/)
[![](https://jitpack.io/v/mapsforge/mapsforge.svg)](https://jitpack.io/#mapsforge/mapsforge)

# Integration guide

This article describes how to integrate the library in your project with Maven and [JitPack](https://jitpack.io/#mapsforge/mapsforge).

## Maven

Package: `org.mapsforge`

## JitPack
 
Package: `com.github.mapsforge.mapsforge`

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

## Map

```groovy
implementation '[PACKAGE]:mapsforge-core:[CURRENT-VERSION]'
implementation '[PACKAGE]:mapsforge-map:[CURRENT-VERSION]'
implementation '[PACKAGE]:mapsforge-map-reader:[CURRENT-VERSION]'
implementation '[PACKAGE]:mapsforge-themes:[CURRENT-VERSION]'
```

### Android

```groovy
implementation '[PACKAGE]:mapsforge-map-android:[CURRENT-VERSION]'
implementation 'com.caverock:androidsvg:1.4'
```

### Desktop

```groovy
implementation '[PACKAGE]:mapsforge-map-awt:[CURRENT-VERSION]'
implementation 'guru.nidi.com.kitfox:svgSalamander:1.1.3'
implementation 'net.sf.kxml:kxml2:2.3.0'
```

## POI

```groovy
implementation '[PACKAGE]:mapsforge-core:[CURRENT-VERSION]'
implementation '[PACKAGE]:mapsforge-poi:[CURRENT-VERSION]'
```

### Android

```groovy
implementation '[PACKAGE]:mapsforge-poi-android:[CURRENT-VERSION]'
```

### Desktop

```groovy
implementation '[PACKAGE]:mapsforge-poi-awt:[CURRENT-VERSION]'
implementation 'org.xerial:sqlite-jdbc:3.43.0.0'
```

## Snapshots

We publish SNAPSHOT builds to Sonatype OSS Repository Hosting.

```groovy
repositories {
    maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}
```

```groovy
implementation 'org.mapsforge:mapsforge-core:master-SNAPSHOT'
```

For checking latest snapshot on every build:
```groovy
configurations.all {
    resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
}
```

## Jars

You can find release and snapshot jars on [Maven Central](https://repo1.maven.org/maven2/org/mapsforge/) and [Sonatype OSS Repository Hosting](https://oss.sonatype.org/content/repositories/snapshots/org/mapsforge/).
