# Changelog

## Next version

- `ThemeCallback.getColor` method [#1737](https://github.com/mapsforge/mapsforge/pull/1737)
- Map writer performance improvements [#1741](https://github.com/mapsforge/mapsforge/pull/1741)
- Map theme improvements
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.27.0)

## Version 0.26.1 (2025-09-15)

- Render themes: restore `display="always"` [#1736](https://github.com/mapsforge/mapsforge/pull/1736)
  - Add `display="order"`
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.26.1)

## Version 0.26.0 (2025-09-14)

- MBTiles raster tile source (Android) [#1687](https://github.com/mapsforge/mapsforge/pull/1687)
- `ThemeCallback.getText` method [#1700](https://github.com/mapsforge/mapsforge/pull/1700)
- Dark and Indigo map themes [#1704](https://github.com/mapsforge/mapsforge/issues/1704)
- Motorider map theme improvements [#1483](https://github.com/mapsforge/mapsforge/issues/1483)
- Biker map theme improvements [#1674](https://github.com/mapsforge/mapsforge/issues/1674)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.26.0)

## Version 0.25.0 (2025-04-04)

- Hillshading tile layer [#1672](https://github.com/mapsforge/mapsforge/pull/1672)
- POI: Arabic normalization [#1680](https://github.com/mapsforge/mapsforge/pull/1680)
- Motorider map theme improvements [#1483](https://github.com/mapsforge/mapsforge/issues/1483)
- Biker map theme [#1674](https://github.com/mapsforge/mapsforge/issues/1674)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.25.0)

## Version 0.24.1 (2025-03-07)
 
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.24.1)

## Version 0.24.0 (2025-03-03)

- Multi-map deduplicate optimization [#1635](https://github.com/mapsforge/mapsforge/pull/1635)
- Label direct rendering `CachedMapDataStoreLabelStore` [#1646](https://github.com/mapsforge/mapsforge/pull/1646)
- Color filter theme resources `ThemeCallback.getBitmap` [#1622](https://github.com/mapsforge/mapsforge/pull/1622)
- Android 32-bit color option [#1656](https://github.com/mapsforge/mapsforge/pull/1656)
  - `Parameters.ANDROID_32BIT_COLOR`
- Motorider map theme improvements [#1483](https://github.com/mapsforge/mapsforge/issues/1483)
- Rename `MapDataStore.readLabels` to `readNamedItems` [#1640](https://github.com/mapsforge/mapsforge/pull/1640)
  - Rename `MapFile.Selector.LABELS` to `NAMED`
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.24.0)

## Version 0.23.0 (2025-01-06)

- Map rotation: marker billboard option [#1563](https://github.com/mapsforge/mapsforge/pull/1563)
- Map label / symbol improvements [#1579](https://github.com/mapsforge/mapsforge/pull/1579) [#1586](https://github.com/mapsforge/mapsforge/pull/1586) [#1588](https://github.com/mapsforge/mapsforge/pull/1588)
- Adaptive hillshading and improvements
  - [#1561](https://github.com/mapsforge/mapsforge/pull/1561) [#1592](https://github.com/mapsforge/mapsforge/pull/1592) [#1597](https://github.com/mapsforge/mapsforge/pull/1597) [#1605](https://github.com/mapsforge/mapsforge/pull/1605)
- Map rendering performance improvements [#1599](https://github.com/mapsforge/mapsforge/pull/1599)
- Multi-map `MapDataStore.setPriority` [#1582](https://github.com/mapsforge/mapsforge/pull/1582)
- Group of layers with z-order `ZOrderGroupLayer` [#1603](https://github.com/mapsforge/mapsforge/pull/1603)
- Motorider map theme improvements [#1483](https://github.com/mapsforge/mapsforge/issues/1483)
- Remove `IMapViewPosition`, use `MapViewPosition` [#1591](https://github.com/mapsforge/mapsforge/pull/1591)
- Remove `Filter`, use `ThemeCallback` [#1573](https://github.com/mapsforge/mapsforge/pull/1573)
- Move `MapViewerTemplate` in samples [#1616](https://github.com/mapsforge/mapsforge/pull/1616)
- Move preferences in samples [#1617](https://github.com/mapsforge/mapsforge/pull/1617)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.23.0)

## Version 0.22.0 (2024-10-10)

- Map rotation [#1491](https://github.com/mapsforge/mapsforge/pull/1491) [#1493](https://github.com/mapsforge/mapsforge/pull/1493)
- Rotation gesture [#1495](https://github.com/mapsforge/mapsforge/issues/1495)
- Fractional zoom [#1492](https://github.com/mapsforge/mapsforge/pull/1492)
  - `Parameters.FRACTIONAL_ZOOM`
- Pre-cache map tiles (+-zoom, margin) [#1507](https://github.com/mapsforge/mapsforge/issues/1507)
- Hillshading new algorithms and improvements
  - [#1521](https://github.com/mapsforge/mapsforge/pull/1521) [#1534](https://github.com/mapsforge/mapsforge/pull/1534) [#1537](https://github.com/mapsforge/mapsforge/pull/1537) [#1543](https://github.com/mapsforge/mapsforge/pull/1543) [#1548](https://github.com/mapsforge/mapsforge/pull/1548) [#1550](https://github.com/mapsforge/mapsforge/pull/1550)
- Hillshading increase default magnitude [#1540](https://github.com/mapsforge/mapsforge/issues/1540)
- Hillshading fix at 0 lat / lon [#1497](https://github.com/mapsforge/mapsforge/issues/1497)
- Motorider map theme [#1483](https://github.com/mapsforge/mapsforge/issues/1483)
- Render themes: exclusive / except negation [#1524](https://github.com/mapsforge/mapsforge/pull/1524)
- `mapsforge-themes` change package [#1135](https://github.com/mapsforge/mapsforge/issues/1135)
  - Rename `InternalRenderTheme` to `MapsforgeThemes`
- Enable multithreaded map rendering
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.22.0)

## Version 0.21.0 (2024-03-15)

- POI writer: way-filtering option [#1457](https://github.com/mapsforge/mapsforge/pull/1457)
- Display model: line, text scale options [#1445](https://github.com/mapsforge/mapsforge/pull/1445)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.21.0)

## Version 0.20.0 (2023-08-15)

- POI: **v3** Android without external libs [#1411](https://github.com/mapsforge/mapsforge/pull/1411)
- Polygon layer: support holes [#1432](https://github.com/mapsforge/mapsforge/pull/1432)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.20.0)

## Version 0.19.0 (2023-04-26)

- Fix incorrect handling of nested tags [#1366](https://github.com/mapsforge/mapsforge/pull/1366)
- Render themes: `curve` parameter [#1371](https://github.com/mapsforge/mapsforge/pull/1371)
- Render themes: `text-transform` parameter [#1372](https://github.com/mapsforge/mapsforge/pull/1372)
- Render themes: `text-wrap-width` parameter [#1379](https://github.com/mapsforge/mapsforge/pull/1379)
- Render themes: `symbol-orientation`, `text-orientation` parameters [#1393](https://github.com/mapsforge/mapsforge/pull/1393)
- Android text wrap improvements [#1392](https://github.com/mapsforge/mapsforge/pull/1392)
- Hillshading: Android scoped storage [#1338](https://github.com/mapsforge/mapsforge/pull/1338)
- Performance improvements [#1367](https://github.com/mapsforge/mapsforge/pull/1367) [#1369](https://github.com/mapsforge/mapsforge/pull/1369)
- POI: sort by distance [#1402](https://github.com/mapsforge/mapsforge/pull/1402)
- POI writer: add all entities by default [#1382](https://github.com/mapsforge/mapsforge/pull/1382)
- POI writer: add normalized name by default [e2eb716](https://github.com/mapsforge/mapsforge/commit/e2eb716936e32d9eced0012de79c3eb6b7577668)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.19.0)

## Version 0.18.0 (2022-06-18)

- Multi-map deduplicate optimization [#1288](https://github.com/mapsforge/mapsforge/pull/1288)
- Validate coordinates option [#1294](https://github.com/mapsforge/mapsforge/pull/1294)
  - `Parameters.VALIDATE_COORDINATES`
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.18.0)

## Version 0.17.0 (2022-01-03)

- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.17.0)

## Version 0.16.0 (2021-05-27)

- Android: scoped storage map / theme example [#1186](https://github.com/mapsforge/mapsforge/pull/1186)
- Render theme from zip archive [#1186](https://github.com/mapsforge/mapsforge/pull/1186)
- Render themes: custom resource providers [#1186](https://github.com/mapsforge/mapsforge/pull/1186)
- Nautical unit adapter with feet [#1188](https://github.com/mapsforge/mapsforge/pull/1188)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.16.0)

## Version 0.15.0 (2021-01-01)

- Android: scoped storage map example [#1170](https://github.com/mapsforge/mapsforge/pull/1170)
- Render theme from Android content providers [#1169](https://github.com/mapsforge/mapsforge/pull/1169)
- Render themes: symbol and lineSymbol position attribute [#1172](https://github.com/mapsforge/mapsforge/pull/1172)
- Hillshading from zipped HGT [#1181](https://github.com/mapsforge/mapsforge/pull/1181)
- AssetsRenderTheme: stream improvements [#1167](https://github.com/mapsforge/mapsforge/pull/1167)
- Map frame buffer improvements [#1178](https://github.com/mapsforge/mapsforge/pull/1178)
  - `Parameters.FRAME_BUFFER_HA3`
- Symbol scale option [#1179](https://github.com/mapsforge/mapsforge/pull/1179)
  - `Parameters.SYMBOL_SCALING`
- Layer scroll event option [#1163](https://github.com/mapsforge/mapsforge/pull/1163)
  - `Parameters.LAYER_SCROLL_EVENT`
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.15.0)

## Version 0.14.0 (2020-08-25)

- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.14.0)

## Version 0.13.0 (2020-01-12)

- Fix marker placement on Android 9+ [#1138](https://github.com/mapsforge/mapsforge/issues/1138)
- Fix hillshading on Android 8+ [#1131](https://github.com/mapsforge/mapsforge/pull/1131)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.13.0)

## Version 0.12.0 (2019-09-17)

- Curved path text rendering [#1112](https://github.com/mapsforge/mapsforge/pull/1112)
- POI writer: normalize names option [#1123](https://github.com/mapsforge/mapsforge/pull/1123)
- Layer groups implementation [#1116](https://github.com/mapsforge/mapsforge/issues/1116)
- Android 10 compatibility [#1120](https://github.com/mapsforge/mapsforge/issues/1120)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.12.0)

## Version 0.11.0 (2019-03-25)

- Render themes: PNG scaling [#1090](https://github.com/mapsforge/mapsforge/issues/1090)
- Polyline scaled width [#1088](https://github.com/mapsforge/mapsforge/issues/1088)
- Hillshading in style menu [#1095](https://github.com/mapsforge/mapsforge/issues/1095)
- `ThemeCallback.getColor` refactor [#912](https://github.com/mapsforge/mapsforge/issues/912)
- Parent tiles rendering quality option [#1102](https://github.com/mapsforge/mapsforge/pull/1102)
  - `Parameters.PARENT_TILES_RENDERING`
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.11.0)

## Version 0.10.0 (2018-08-28)

- Mapsforge maps **v5**: custom tag keys [#1041](https://github.com/mapsforge/mapsforge/issues/1041)
- Read & render polygon label/symbol position [#1064](https://github.com/mapsforge/mapsforge/issues/1064)
- Map writer: polygon label/symbol centroid [#1061](https://github.com/mapsforge/mapsforge/issues/1061)
- POI: SQLite Android Bindings [#1079](https://github.com/mapsforge/mapsforge/issues/1079)
- POI: offline address search [#1063](https://github.com/mapsforge/mapsforge/issues/1063)
- POI writer: tag keys as categories [#1062](https://github.com/mapsforge/mapsforge/pull/1062)
- POI writer: latest Java compatibility [#1083](https://github.com/mapsforge/mapsforge/issues/1083)
- MyLocationOverlay implementation [#1035](https://github.com/mapsforge/mapsforge/issues/1035)
- Polyline / Polygon rendering optimizations [#1057](https://github.com/mapsforge/mapsforge/issues/1057)
- Tile sources with api keys [#1028](https://github.com/mapsforge/mapsforge/issues/1028)
- Render theme fallback internal resources [#1026](https://github.com/mapsforge/mapsforge/issues/1026)
- MapViewPosition interface [#1044](https://github.com/mapsforge/mapsforge/pull/1044)
- Android 9 compatibility [#1066](https://github.com/mapsforge/mapsforge/issues/1066)
- JTS (LocationTech) [#1027](https://github.com/mapsforge/mapsforge/issues/1027)
- SVG Salamander (JitPack) [#1078](https://github.com/mapsforge/mapsforge/issues/1078)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.10.0)

## Version 0.9.1 (2018-01-04)

- Gradle fix transitive dependencies [#1009](https://github.com/mapsforge/mapsforge/issues/1009)
- Deprecate mapsforge-map-android-extras [#1021](https://github.com/mapsforge/mapsforge/issues/1021)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.9.1)

## Version 0.9.0 (2017-12-03)

- Mapsforge maps **v5**: variable tag values [#1006](https://github.com/mapsforge/mapsforge/pull/1006)
- Mapsforge maps **v5**: implicit relations of building parts [#1014](https://github.com/mapsforge/mapsforge/pull/1014)
- Map frame buffer improvements [#977](https://github.com/mapsforge/mapsforge/issues/977)
- Hillshading improvements [#923](https://github.com/mapsforge/mapsforge/issues/923)
- Polyline overlay touch events [#998](https://github.com/mapsforge/mapsforge/issues/998)
- Polygon overlay touch events [#999](https://github.com/mapsforge/mapsforge/issues/999)
- Circle overlay touch events [#996](https://github.com/mapsforge/mapsforge/issues/996)
- MapFile supports FileChannel as input [#982](https://github.com/mapsforge/mapsforge/issues/982)
- XmlPullParser different implementations [#974](https://github.com/mapsforge/mapsforge/issues/974)
- Desktop: fix blurred map view [#978](https://github.com/mapsforge/mapsforge/issues/978)
- POI: **v2** with multiple categories [#950](https://github.com/mapsforge/mapsforge/issues/950)
- POI: multiple patterns in search [#988](https://github.com/mapsforge/mapsforge/issues/988)
- POI: add non-closed ways [#947](https://github.com/mapsforge/mapsforge/issues/947)
- POI: add geo tagging [#946](https://github.com/mapsforge/mapsforge/issues/946)
- POI: add named entities option [#949](https://github.com/mapsforge/mapsforge/issues/949)
- POI: fix multiple POI categories [#940](https://github.com/mapsforge/mapsforge/issues/940)
- Feature parameters [#994](https://github.com/mapsforge/mapsforge/issues/994)
- Writers: Osmosis 0.46 with protobuf 3 [#1002](https://github.com/mapsforge/mapsforge/issues/1002)
- Gradle 4 / Android plugin 3 support [#1009](https://github.com/mapsforge/mapsforge/issues/1009)
- Internal render themes various improvements [#857](https://github.com/mapsforge/mapsforge/issues/857)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.9.0)

## Version 0.8.0 (2017-03-18)

- Android fix hardware acceleration [#919](https://github.com/mapsforge/mapsforge/issues/919) [#613](https://github.com/mapsforge/mapsforge/issues/613)
- Hillshading from HGT digital elevation model data [#923](https://github.com/mapsforge/mapsforge/issues/923)
- Android tile cache folder option [#913](https://github.com/mapsforge/mapsforge/issues/913)
- Android SVG cache folder option [#914](https://github.com/mapsforge/mapsforge/issues/914)
- Desktop tile cache creation utility [#915](https://github.com/mapsforge/mapsforge/issues/915)
- Desktop MapView custom listeners [#935](https://github.com/mapsforge/mapsforge/issues/935)
- Map writer: multiple threads option (default 1) [#920](https://github.com/mapsforge/mapsforge/issues/920)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.8.0)

## Version 0.7.0 (2016-12-30)

- Multithreaded rendering improvements [#591](https://github.com/mapsforge/mapsforge/issues/591) [#806](https://github.com/mapsforge/mapsforge/issues/806)
- Fix map disappearance at large zoom levels [#572](https://github.com/mapsforge/mapsforge/issues/572)
- Map writer: polygon label position enhancements [#886](https://github.com/mapsforge/mapsforge/issues/886)
- Map writer: house numbers (nodes) include at zoom 17 [#895](https://github.com/mapsforge/mapsforge/issues/895)
- Map writer: simplification max zoom option [#892](https://github.com/mapsforge/mapsforge/issues/892)
- POI writer: allow empty categories option [#883](https://github.com/mapsforge/mapsforge/issues/883)
- Multiple maps with different zoom levels [#911](https://github.com/mapsforge/mapsforge/issues/911)
- Map rotation (external) in library [#637](https://github.com/mapsforge/mapsforge/issues/637)
- Group layer implementation [#860](https://github.com/mapsforge/mapsforge/issues/860)
- SVG symbols customization [#858](https://github.com/mapsforge/mapsforge/issues/858)
- Map scale bar size scaling [#861](https://github.com/mapsforge/mapsforge/issues/861)
- Group marker example [#905](https://github.com/mapsforge/mapsforge/issues/905)
- Deprecate water tiles rendering [#640](https://github.com/mapsforge/mapsforge/issues/640)
- mapsforge-themes module [#848](https://github.com/mapsforge/mapsforge/issues/848)
- New default internal render theme [#903](https://github.com/mapsforge/mapsforge/issues/903)
- Internal render themes new SVG resources [#904](https://github.com/mapsforge/mapsforge/issues/904)
- Render theme resources optional location prefixes [#847](https://github.com/mapsforge/mapsforge/issues/847)
- Render theme from input stream [#872](https://github.com/mapsforge/mapsforge/issues/872)
- SpatiaLite natives published artifacts [#849](https://github.com/mapsforge/mapsforge/issues/849)
- SNAPSHOT builds publish to Sonatype OSSRH [#873](https://github.com/mapsforge/mapsforge/issues/873)
- Deprecate Maven build [#852](https://github.com/mapsforge/mapsforge/issues/852)
- Deprecate CI server [#877](https://github.com/mapsforge/mapsforge/issues/877)
- Drop of `dev` branch
- Internal render themes various improvements [#857](https://github.com/mapsforge/mapsforge/issues/857)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.7.0)

## Version 0.6.1 (2016-06-11)

- [POI Search](POI.md) **v1** [#728](https://github.com/mapsforge/mapsforge/issues/728)
- Render Theme v5: pathText repeat options [#473](https://github.com/mapsforge/mapsforge/issues/473)
- Render Theme v5: scale options [#814](https://github.com/mapsforge/mapsforge/issues/814)
- Render Theme v5: deprecate 'symbol-scaling' option [#815](https://github.com/mapsforge/mapsforge/issues/815)
- Map writer improved area heuristics [#788](https://github.com/mapsforge/mapsforge/issues/788) [#795](https://github.com/mapsforge/mapsforge/issues/795)
- Reverse Geocoding example [#727](https://github.com/mapsforge/mapsforge/issues/727)
- LabelLayer enhanced implementation [#763](https://github.com/mapsforge/mapsforge/issues/763)
- Street name rendering improvements [#644](https://github.com/mapsforge/mapsforge/issues/644)
- Color filters in map rendering [#778](https://github.com/mapsforge/mapsforge/issues/778)
- Night mode example [#777](https://github.com/mapsforge/mapsforge/issues/777)
- Layers redraw option [#817](https://github.com/mapsforge/mapsforge/issues/817)
- Zoom level limits central handling [#646](https://github.com/mapsforge/mapsforge/issues/646)
- Simplified tile layer creation [#833](https://github.com/mapsforge/mapsforge/issues/833)
- Android gestures options [#705](https://github.com/mapsforge/mapsforge/issues/705)
- Java onTap layer listener [#774](https://github.com/mapsforge/mapsforge/issues/774)
- Online tile source enhancements [#823](https://github.com/mapsforge/mapsforge/issues/823)
- Diagnostic layers improvements [#821](https://github.com/mapsforge/mapsforge/issues/821)
- Android samples hardware acceleration preference [#825](https://github.com/mapsforge/mapsforge/issues/825)
- Library integration [documentation](Integration.md) [#781](https://github.com/mapsforge/mapsforge/issues/781)
- Jar with dependencies building [#767](https://github.com/mapsforge/mapsforge/issues/767)
- Sample applications reorganization [#724](https://github.com/mapsforge/mapsforge/issues/724)
- Code formatting improvements [#782](https://github.com/mapsforge/mapsforge/issues/782)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.6.1)

## Version 0.6.0 (2015-11-25)

- Mapsforge maps **v4**: multilingual names [#624](https://github.com/mapsforge/mapsforge/issues/624)
- Writer language improved parsing [#663](https://github.com/mapsforge/mapsforge/issues/663)
- MapDataStore extensibility [#668](https://github.com/mapsforge/mapsforge/issues/668)
- Android gestures total overhaul [#688](https://github.com/mapsforge/mapsforge/issues/688)
- Android gesture: fling [#219](https://github.com/mapsforge/mapsforge/issues/219)
- Android gesture: quick scale [#651](https://github.com/mapsforge/mapsforge/issues/651)
- Marker clustering example [#669](https://github.com/mapsforge/mapsforge/issues/669)
- BoundingBox enhancements [#660](https://github.com/mapsforge/mapsforge/issues/660)
- Path enhancements [#676](https://github.com/mapsforge/mapsforge/issues/676)
- Customizable zoom controls [#700](https://github.com/mapsforge/mapsforge/issues/700)
- Android View overlay [#680](https://github.com/mapsforge/mapsforge/issues/680)
- Simplification of MapView API [#662](https://github.com/mapsforge/mapsforge/issues/662)
- AWT library reorganization [#714](https://github.com/mapsforge/mapsforge/issues/714)
- XSD repositioning for online validation [#672](https://github.com/mapsforge/mapsforge/issues/672)
- Android 6 / SDK 23 / Runtime permission support [#704](https://github.com/mapsforge/mapsforge/issues/704)
- Map creation with coastlines [documentation](MapCreation.md)
- Fix invalid number of way nodes [#645](https://github.com/mapsforge/mapsforge/issues/645)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.6.0)

## Version 0.5.2 (2015-08-23)

- Multithreaded map rendering [#73](https://github.com/mapsforge/mapsforge/issues/73)
- Hardware acceleration improvements [#613](https://github.com/mapsforge/mapsforge/issues/613)
- Touch gesture improvements and fixes [#616](https://github.com/mapsforge/mapsforge/issues/616)
- Scale gesture through focal point [#626](https://github.com/mapsforge/mapsforge/issues/626)
- Geographic grid layer improvements [#574](https://github.com/mapsforge/mapsforge/issues/574)
- Map writer tag-mapping improvements [#339](https://github.com/mapsforge/mapsforge/issues/339) [#586](https://github.com/mapsforge/mapsforge/issues/586)
- Render themes improvements [#608](https://github.com/mapsforge/mapsforge/issues/608) [#615](https://github.com/mapsforge/mapsforge/issues/615)
- Scale bar enhancements [#597](https://github.com/mapsforge/mapsforge/issues/597)
- Zoom controls enhancements [#598](https://github.com/mapsforge/mapsforge/issues/598)
- Map buffer size adjustable [#619](https://github.com/mapsforge/mapsforge/issues/619)
- MyLocationOverlay moved in Samples [#657](https://github.com/mapsforge/mapsforge/issues/657)
- External Map Rotation example improvements [#637](https://github.com/mapsforge/mapsforge/issues/637)
- LabelLayer improvements [#595](https://github.com/mapsforge/mapsforge/issues/595) [#639](https://github.com/mapsforge/mapsforge/issues/639) [#642](https://github.com/mapsforge/mapsforge/issues/642)
- Simplified cleanup operation [#620](https://github.com/mapsforge/mapsforge/issues/620)
- Minor improvements and bug fixes
- [Solved issues](https://github.com/mapsforge/mapsforge/issues?q=is%3Aclosed+milestone%3A0.5.2)

## Version 0.5.1 (2015-03-18)

- Support for more general map data sources through [MapDataStore interface](MapDataStore.md)
- Persistent Caching.
- SVG rendering for Java with SVG Salamander
- Internal rendertheme (osmarender) using SVG symbols by default
- Resource memory/file cache: common clear methods
- Documentation updates.
- Various bug fixes.
- Geographic grid layer.
- External Map Rotation example in Samples.

## Version 0.5.0 (2014-12-08)

- Rendertheme V4.
- Scalebar now with the option of using two units at the same time. New unit: nautical.
- Line breaks in labels: this is the most fragile change at the moment as it does not play well with the label placement algorithm.
- Improved tile caching: bugs fixed and optional threading for better performance.
- TileStoreLayer using the standard TMS directory layout of zoomlevel/y/x.
- New SVG library having better compliance with SVG spec.
- New render theme xml parser for better performance.
- Caching of rendered SVG symbols & delayed rendering.
- Simplify map writer plugin installation in Osmosis.
- Fixes to Rendertheme V4 with most notably improved tiling of area shaders.
- Rendertheme V4: added display directive to force symbols to appear.
- Rendertheme V4: added optional map-background-outside to blank out non-map areas.
- Introduced MapViewerTemplate for easier development of Android apps and simplified the Android Samples app.
- Android Lollipop support, various upgrades to latest software, Java 8 (for maven only so far).
- Map Reader fix for long ways.
- Map Writer fix to clip multi-polygons and area detection.
- Cache calculation improvements.
- Changes to support the new standard for encoding land/sea area.
- Fix for #332 (invalid geometries in map writer).
- Fix for #549 (ordering of label drawing).
- Improved documentation integrated into git.
- Move to github.

## Version 0.4.3 (2014-05-10)

Version 0.4.X is what during a period was known as the 'rescue' branch. It is based on an extensive rewrite of the mapsforge library that is incompatible with the previous releases. The mapfile format however has not changed and map files from 0.3 can still be used.

- Better modular design of the entire project.
- Support for Java-only applications, see the SwingMapViewer subproject.
- Map views are not directly coupled to activities any more and can be used with modern Android design concepts.
- More flexible overlay model.
- Tightened-up memory handling.
- Support for device screen scaling.
- Support for SVG files as map icons.
- Gradle as build system in addition to Maven.
- Updated icons of default render theme.
- Available from Maven Central.
- Use mapsforge svg-android.

For a demonstration of the capabilities refer to the Samples app.

## Version 0.3.0 (2012-03-18)

- New version of the binary map file format, see [Specification Binary Map File](Specification-Binary-Map-File.md).
- New render theme API for custom map styles. ([issue 31](https://github.com/mapsforge/mapsforge/issues#issue/31))
- Allow for custom tile download classes. ([issue 204](https://github.com/mapsforge/mapsforge/issues#issue/204))
- Position of the zoom controls is now configurable. ([issue 155](https://github.com/mapsforge/mapsforge/issues#issue/155))
- The map scale bar supports metric and imperial units. ([issue 167](https://github.com/mapsforge/mapsforge/issues#issue/167))
- A preferred language can now be set for the map-writer. ([issue 90](https://github.com/mapsforge/mapsforge/issues#issue/90))
- Introduced maven as build system for the mapsforge project.
- Major refactoring of the package structure in the map library.
- Moved the map-reader code to a new, Android-independent subproject.
- Minor improvements and bug fixes.

## Version 0.2.4 (2011-06-28)

- AdvancedMapViewer now supports all location providers.
- Snap to position can be toggled if current position is shown.
- Support for multi-polygons in Overlay API.
- Support for long press events in Overlay API.
- Added metadata API for map files.
- Minor improvements and bug fixes.

## Version 0.2.3 (2011-05-12)

- License change from GPL3 to LGPL3.
- Added animations to the MapView when zooming in and out.
- The text size of the map renderer can be adjusted at runtime.
- Rendering of symbols along ways, used for example on one-way roads.
- The maximum zoom level of the MapView may now be set at runtime.
- Minor improvements and bug fixes.

## Version 0.2.2 (2011-03-22)

- Improved the overlay API and implemented some new classes.
- Better rendering of water areas and coastline situations.
- The tag list of the mapfile-writer can now be configured via an XML file.
- Added the possibility to render areas with a background pattern.
- Integrated support for piste maps features.
- Added a method to take a screenshot of the currently displayed map.
- The minimum zoom level of the MapView may now be set at runtime.
- Started to make the map tile cache on the memory card persistent.
- Minor improvements and bug fixes.

## Version 0.2.1 (2011-02-10)

- Optimized binary map format, reduces the file size by 15-20%.
- Rewritten overlay implementation with many new features and bug fixes.
- Zoom level can now be changed by multi-tap and double-tap gestures.
- The mapfile-writer supports hard disk as temporary storage device.
- The mapfile-writer depends no longer on the JTS library and works faster.
- Added support for a couple of OpenStreetMap tags.
- Better rendering of way names.
- Better rendering of coastline and water areas.
- Minor improvements and bug fixes.

## Version 0.2.0 (2010-11-22)

- New binary map format for more detailed and faster map rendering.
- Overlay API to display points, ways and polygons on top of the map.
- Better label and symbol placement with collision avoidance.
- The MapView now supports multi-touch and new tile download modes.
- First release of the Osmosis plugin to generate binary map files.
- Minor improvements and bug fixes.

## Version 0.1.0 (2010-06-27)

- First public release, contains the android.map package.
