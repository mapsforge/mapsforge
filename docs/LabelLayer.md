The New Label Layer
===================

The new LabelLayer is responsible for drawing labels and icons onto a map. This effectively splits the responsibility of drawing a map into the TileRendererLayer, which will now only draw ways and areas (essentially items that will appear the same when a map is rotated), and the LabelLayer which will draw labels, road-names and icons (everything that needs to have a fixed angle relative to the screen).

Problems in 0.4.x
-----------------

In the current, 0.4.x, we have have a couple of issues, that are either unresolvable with the current approach or the implementation has so many problems that they are difficult to solve without a complete rewrite. More specifically:

  * No support for map rotation. Since labels were drawn at a fixed angle onto a tile, rotating the tile would also rotate the labels. 
  * Complex implementation: since labels were drawn onto tiles, it had to be carefully where labels overlapped tile boundaries. In these cases, labels had to be drawn onto adjacent tiles. The implementation for this was very complex and had several errors, most notably that it could not account for tiles being purged from a cache and redrawn. One result were labels wrongly clipped at tile boundaries.
  * Overwriting for parallel roads: the label placement algorithm was not applied to roads, resulting in road names overwriting each other.
  * No priority, a random first-come/first-serve algorithm for label placement, often obscuring more important place names.
  * A related problem in the TileRendererLayer is that through the extensive use of local variables it is impossible to multi-thread that layer. 

The new approach
----------------

  * Labels are drawn separately and directly onto the canvas. This will make it possible to rotate the underlying tiles and keep labels horizontal. Rotation is now implemented.
  * Labels are not tiled anymore, therefore clipping at tile boundaries has been eliminated and the complex accounting for tile dependencies could be removed. 
  * Road labels are now part of the placement algorithm.
  * There is now a priority element on captions, symbols, line-symbols and pathtestext. The higher the priority, the earlier the element is drawn onto the map, ensuring that labels with higher priority are visible.
  * In the DatabaseRenderer a number of local variables have been removed. This work is not yet complete, but it is a step towards multi-threading here.

As with other changes introduced, this is an incremental change that leaves user code as much unaffected as possible, but it also makes the implementation a little tricky. 

  * The labels are retrieved from the map database in a tile by tile fashion. There is no way to retrieve labels by location. And as every single item has to be passed through the rendertheme mechanism this retrieval is relatively expensive.
  * The labels need to be redrawn for every redraw of the map. No everything can be done on the main UI thread. Computing the label placement is relatively expensive.

The new implementation addresses this in the following way:
  * All map elements to be placed on the LabelLayer are derived from MapElementContainer in mapsforge-core. Technically, this should go into mapsforge-map, but for the PointTextContainer we need different implementations for Android/Awt, which requires a factory method through the GraphicFactory. A MapElementContainer knows how to draw itself onto the Canvas. 
  * Label retrieval hooks into the retrieval by the TileRendererLayer that was previously responsible for drawing the labels. This eliminates a double reading of the map file -- however, at the expense of some complex operations.
  * The label data is then stored in the TileBasedLabelStore, a LRU cache that is organized according to tiles. 
  * The TileBasedLabelStore precomputes the layout for the last requested area. This assumes that the area does not change very often, as it is the case with usual map panning. 
  * Whenever the visible tile set changes, the LayoutCalculator thread recomputes the layout and, when finished, requires the LayerLabel to be redrawn.

Notes for users
===============

TileRendererLayer:
------------------

The TileRendererLayer now takes an additional argument of the TileBasedLabelStore, which receives the labels that the DatabaseRenderer produces. Alongside the TileRendererLayer we need the LabelLayer.

See the examples in [mapsforge-samples-android](https://github.com/mapsforge/mapsforge/tree/master/mapsforge-samples-android).

If you do not want any labels, pass null for the TileBasedLabelStore to the TileRendererLayer.

Rendertheme Change:
-------------------
The priority attribute has been added to caption, pathText, lineSymbol and symbol. The default priority is 0, higher priorities are rendered first, lower priority elements later if space is still available. 

Remaining Problems:
-------------------

  * Memory as usual: mapsforge sails close to OOM all the time and keeping the label data for all the visible tiles in memory can cause an app to OOM. Finding the right balance is difficult.
