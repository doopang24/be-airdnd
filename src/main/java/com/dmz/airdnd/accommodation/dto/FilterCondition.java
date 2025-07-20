package com.dmz.airdnd.accommodation.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;

@Builder
public record FilterCondition(
	double longitude,
	double latitude,
	double radiusKm,
	Integer minPrice,
	Integer maxPrice,
	Integer maxGuests,
	List<LocalDate> requestedDates
) {
}
