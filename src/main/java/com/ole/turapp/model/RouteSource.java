package com.ole.turapp.model;

/**
 * Hvor ruten kommer fra — styrer hvordan klientene viser den:
 * PLANNED-ruter vises i rutelisten og kan følges; GPX_IMPORT-ruter er
 * bakgrunnsnett (tur- og friluftsruter) som kun tegnes på kartet.
 */
public enum RouteSource {
    PLANNED,
    GPX_IMPORT
}
