package com.dmz.airdnd.accommodation.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.dmz.airdnd.accommodation.domain.Accommodation;
import com.dmz.airdnd.accommodation.dto.request.AccommodationSearchRequest;
import com.dmz.airdnd.accommodation.dto.response.AccommodationPageResponse;
import com.dmz.airdnd.accommodation.repository.AccommodationRepository;
import com.dmz.airdnd.fixture.TestAccommodationFactory;

@ExtendWith(MockitoExtension.class)
class AccommodationServiceTest {

	@InjectMocks
	private AccommodationService accommodationService;

	@Mock
	private AccommodationRepository accommodationRepository;

	@Test
	@DisplayName("필터링과 페이징이 포함된 요청을 받고 필터링과 페이징 된 결과를 반환한다.")
	void testAccommodationService() {
		// given
		Accommodation accommodation = TestAccommodationFactory.createTestAccommodation1();

		AccommodationSearchRequest request = AccommodationSearchRequest.builder()
			.longitude(127.027778)
			.latitude(37.498056)
			.minPrice(10000)
			.maxPrice(50000)
			.maxGuests(4)
			.checkIn(LocalDate.parse("2023-10-01"))
			.checkOut(LocalDate.parse("2023-10-05"))
			.pageNumber(1)
			.pageSize(3)
			.build();

		Pageable pageable = PageRequest.of(request.getPageNumber() - 1, request.getPageSize());

		Page<Accommodation> accommodationPage = new PageImpl<>(
			List.of(accommodation),
			pageable,
			1 // total count
		);

		when(accommodationRepository.findFilteredAccommodations(any(), any()))
			.thenReturn(accommodationPage);
		// when
		AccommodationPageResponse response = accommodationService.findFilteredAccommodations(request);

		// then
		assertThat(response.getPageNumber()).isEqualTo(1);
		assertThat(response.getPageSize()).isEqualTo(3);
		assertThat(response.getTotalPages()).isEqualTo(1);
		assertThat(response.getTotalElements()).isEqualTo(1);
		assertThat(response.getAccommodationResponses().get(0).getName()).isEqualTo(accommodation.getName());
	}
}
