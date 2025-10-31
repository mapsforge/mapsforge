[![](https://jitpack.io/v/mapsforge/mapsforge.svg)](https://jitpack.io/#mapsforge/mapsforge)

# Integration guide

This article describes how to integrate the library in your project with [JitPack](https://jitpack.io/#mapsforge/mapsforge).

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

## Map

```groovy
implementation 'com.github.mapsforge.mapsforge:mapsforge-core:[CURRENT-VERSION]@jar'
implementation 'com.github.mapsforge.mapsforge:mapsforge-map:[CURRENT-VERSION]@jar'
implementation 'com.github.mapsforge.mapsforge:mapsforge-map-reader:[CURRENT-VERSION]@jar'
implementation 'com.github.mapsforge.mapsforge:mapsforge-themes:[CURRENT-VERSION]@jar'
```

### Android

```groovy
implementation 'com.github.mapsforge.mapsforge:mapsforge-map-android:[CURRENT-VERSION]@jar'
implementation 'com.caverock:androidsvg:1.4'
```

### Desktop

```groovy
implementation 'com.github.mapsforge.mapsforge:mapsforge-map-awt:[CURRENT-VERSION]@jar'
implementation 'guru.nidi.com.kitfox:svgSalamander:1.1.3'
implementation 'net.sf.kxml:kxml2:2.3.0'
```

## POI

```groovy
implementation 'com.github.mapsforge.mapsforge:mapsforge-core:[CURRENT-VERSION]@jar'
implementation 'com.github.mapsforge.mapsforge:mapsforge-poi:[CURRENT-VERSION]@jar'
```

### Android

```groovy
implementation 'com.github.mapsforge.mapsforge:mapsforge-poi-android:[CURRENT-VERSION]@jar'
```

### Desktop

```groovy
implementation 'com.github.mapsforge.mapsforge:mapsforge-poi-awt:[CURRENT-VERSION]@jar'
implementation 'org.xerial:sqlite-jdbc:3.43.0.0'
```

## Snapshots

See the instructions on [JitPack](https://jitpack.io/#mapsforge/mapsforge).
