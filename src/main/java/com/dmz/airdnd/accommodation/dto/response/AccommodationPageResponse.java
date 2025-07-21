package com.dmz.airdnd.accommodation.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AccommodationPageResponse {
	private int pageNumber;
	private int pageSize;
	private int totalPages;
	private long totalElements;
	private List<AccommodationResponse> accommodationResponses;
}
