# Integration guide

Clone the project and run the Gradle task `publishToMavenLocal`.

Add the repository:

```groovy
repositories {
    mavenLocal()
}
```

## Map

```groovy
implementation 'org.mapsforge:mapsforge-core:master-SNAPSHOT@jar'
implementation 'org.mapsforge:mapsforge-map:master-SNAPSHOT@jar'
implementation 'org.mapsforge:mapsforge-map-reader:master-SNAPSHOT@jar'
implementation 'org.mapsforge:mapsforge-themes:master-SNAPSHOT@jar'
```

### Android

```groovy
implementation 'org.mapsforge:mapsforge-map-android:master-SNAPSHOT@jar'
implementation 'com.caverock:androidsvg:1.4'
```

### Desktop

```groovy
implementation 'org.mapsforge:mapsforge-map-awt:master-SNAPSHOT@jar'
implementation 'guru.nidi.com.kitfox:svgSalamander:1.1.3'
implementation 'net.sf.kxml:kxml2:2.3.0'
```

## POI

```groovy
implementation 'org.mapsforge:mapsforge-core:master-SNAPSHOT@jar'
implementation 'org.mapsforge:mapsforge-poi:master-SNAPSHOT@jar'
```

### Android

```groovy
implementation 'org.mapsforge:mapsforge-poi-android:master-SNAPSHOT@jar'
implementation 'androidx.sqlite:sqlite-bundled:2.7.0'
```

### Desktop

```groovy
implementation 'org.mapsforge:mapsforge-poi-awt:master-SNAPSHOT@jar'
implementation 'org.xerial:sqlite-jdbc:3.53.4.0'
```
