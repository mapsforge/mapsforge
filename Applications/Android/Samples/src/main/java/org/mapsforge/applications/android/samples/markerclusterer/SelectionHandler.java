package org.mapsforge.applications.android.samples.markerclusterer;

public interface SelectionHandler<T extends GeoItem> {
	public T getSelectedItem();
	public void setSelectedItem(SelectionHandler<T> sender, T selectedItem);
}
