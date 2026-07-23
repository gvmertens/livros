package com.library.catalog.api.dto;

import java.util.List;

public record CatalogSearchResponse(List<CatalogBookResponse> items, int total) {}
