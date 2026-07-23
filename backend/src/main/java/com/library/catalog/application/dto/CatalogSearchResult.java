package com.library.catalog.application.dto;

import java.util.List;

public record CatalogSearchResult(List<CatalogBook> items, int total) {}
