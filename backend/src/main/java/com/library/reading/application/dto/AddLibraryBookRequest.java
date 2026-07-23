package com.library.reading.application.dto;

import com.library.reading.domain.ReadingStatus;

public record AddLibraryBookRequest(String googleBooksId, ReadingStatus status) {}
