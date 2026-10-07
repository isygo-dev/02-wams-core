package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;
import software.xdev.vaadin.maps.leaflet.MapContainer;
import software.xdev.vaadin.maps.leaflet.basictypes.LLatLng;
import software.xdev.vaadin.maps.leaflet.layer.raster.LTileLayer;
import software.xdev.vaadin.maps.leaflet.layer.ui.LMarker;
import software.xdev.vaadin.maps.leaflet.map.LMap;
import software.xdev.vaadin.maps.leaflet.registry.LComponentManagementRegistry;
import software.xdev.vaadin.maps.leaflet.registry.LDefaultComponentManagementRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reusable Leaflet map using OpenStreetMap tiles; no map API key is required.
 * OSM's public tile service is best-effort and subject to its usage policy.
 * Popup content is HTML and must be trusted or escaped by the caller.
 */
public class LeafletMap extends Div {

    private static final String OSM_TILE_URL =
            "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
    private static final String OSM_ATTRIBUTION =
            "© <a href=\"https://www.openstreetmap.org/copyright\" target=\"_blank\" "
                    + "rel=\"noopener noreferrer\">OpenStreetMap contributors</a>";
    private static final int MAX_ZOOM = 19;

    private final LComponentManagementRegistry registry;
    private final LMap map;
    private final List<LMarker> markers = new ArrayList<>();
    private transient Registration resizeRegistration;
    private double centerLatitude;
    private double centerLongitude;
    private int zoom = 2;

    public LeafletMap() {
        setWidthFull();
        setHeight("360px");
        addClassName("leaflet-map");

        registry = new LDefaultComponentManagementRegistry(this);
        MapContainer mapContainer = new MapContainer(registry);
        mapContainer.setSizeFull();
        add(mapContainer);

        map = mapContainer.getlMap();
        new LTileLayer(registry, OSM_TILE_URL, MAX_ZOOM, OSM_ATTRIBUTION).addTo(map);
        setCenter(0, 0);
        setZoom(zoom);

        addAttachListener(event -> resizeRegistration = event.getUI().getPage()
                .addBrowserWindowResizeListener(ignored -> map.invalidateSize(false)));
        addDetachListener(event -> removeResizeListener());
    }

    /**
     * Changes the map center while preserving the current zoom.
     */
    public void setCenter(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        centerLatitude = latitude;
        centerLongitude = longitude;
        map.setView(new LLatLng(registry, latitude, longitude), zoom);
    }

    /**
     * Changes the zoom while preserving the current center. OSM's public raster tiles
     * are available through zoom level 19.
     */
    public void setZoom(int zoom) {
        if (zoom < 0 || zoom > MAX_ZOOM) {
            throw new IllegalArgumentException("Zoom must be between 0 and " + MAX_ZOOM);
        }
        this.zoom = zoom;
        map.setView(new LLatLng(registry, centerLatitude, centerLongitude), zoom);
    }

    /**
     * Adds a marker with caller-provided HTML popup content.
     */
    public void addMarker(double latitude, double longitude, String popupHtml) {
        validateCoordinates(latitude, longitude);
        Objects.requireNonNull(popupHtml, "popupHtml must not be null");
        LMarker marker = new LMarker(registry, new LLatLng(registry, latitude, longitude));
        marker.bindPopup(popupHtml).addTo(map);
        markers.add(marker);
    }

    /**
     * Removes all markers currently managed by this map.
     */
    public void clearMarkers() {
        markers.forEach(map::removeLayer);
        markers.clear();
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be finite and between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be finite and between -180 and 180");
        }
    }

    private void removeResizeListener() {
        if (resizeRegistration != null) {
            resizeRegistration.remove();
            resizeRegistration = null;
        }
    }
}
