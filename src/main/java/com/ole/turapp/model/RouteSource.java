package com.ole.turapp.model;

/**
 * Where the route comes from — controls how clients display it:
 * PLANNED routes are shown in the route list and can be followed; GPX_IMPORT
 * routes are background trail networks (hiking and outdoor trails) that are
 * only drawn on the map.
 */
public enum RouteSource {
    PLANNED,
    GPX_IMPORT
}
