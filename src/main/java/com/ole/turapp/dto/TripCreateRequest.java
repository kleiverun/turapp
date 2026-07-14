package com.ole.turapp.dto;

public record TripCreateRequest(
   String name,
   String notes,
   String visibility
) {}
