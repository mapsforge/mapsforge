package org.mapsforge.map.layer.hills;

/** should implement equals */
public interface DemFolder {
    Iterable<DemFolder> subs();
    Iterable<DemFile> files();
}
