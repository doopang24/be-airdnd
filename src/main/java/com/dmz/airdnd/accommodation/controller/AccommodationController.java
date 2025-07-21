package com.dmz.airdnd.accommodation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dmz.airdnd.accommodation.dto.request.AccommodationCreateRequest;
import com.dmz.airdnd.accommodation.dto.request.AccommodationSearchRequest;
import com.dmz.airdnd.accommodation.dto.response.AccommodationCreateResponse;
import com.dmz.airdnd.accommodation.dto.response.AccommodationPageResponse;
import com.dmz.airdnd.accommodation.service.AccommodationService;
import com.dmz.airdnd.common.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
 숙소(Accommodation)와 관련된 모든 HTTP 요청을 처리하는 컨트롤러.
 클라이언트의 요청을 받아 서비스 계층에 비즈니스 로직 처리를 위임하고, 그 결과를 RESTful API 응답 형식으로 반환합니다.
 */
@RequiredArgsConstructor
@RequestMapping("/api/accommodations")
@RestController
public class AccommodationController {
	private final AccommodationService accommodationService;

	/*
	 POST /api/accommodations
	 새로운 숙소를 등록하는 API 엔드포인트.

	 @param request 클라이언트로부터 받은 숙소 생성 정보. @Valid 를 통해 유효성 검사를 수행합니다.
	 @return 생성된 숙소 정보와 함께 HTTP 201 Created 상태 코드를 반환합니다.
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<AccommodationCreateResponse>> createAccommodation(
		@Valid @RequestBody AccommodationCreateRequest request) {
		AccommodationCreateResponse response = accommodationService.createAccommodation(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	/*
	 GET /api/accommodations
	 다중 필터 조건을 사용하여 숙소를 검색하는 API 엔드포인트. (MySQL 기반)
	 QueryDSL 을 사용한 동적 쿼리를 통해 복잡한 조건(가격, 날짜, 인원수, 위치 등)을 처리합니다.

	 @param request URL 쿼리 파라미터로 전달된 검색 조건. @ModelAttribute 로 DTO 에 바인딩됩니다.
	 @return 페이징 처리된 숙소 목록을 반환합니다.
	 */
	@GetMapping
	public ApiResponse<AccommodationPageResponse> searchAccommodations(
		@Valid @ModelAttribute AccommodationSearchRequest request) {
		return ApiResponse.success(accommodationService.findFilteredAccommodations(request));
	}

	/*
	 GET /api/accommodations/search
	 위치 기반으로 주변 숙소를 검색하는 고성능 API 엔드포인트. (Elasticsearch 기반)
	 빠른 위치 검색이 필요할 때 사용되며, Spring Data Elasticsearch 의 Query Method 를 활용합니다.

	 @param request URL 쿼리 파라미터로 전달된 위치 및 반경 정보.
	 @return 페이징 처리된 숙소 목록을 반환합니다.
	 */
	@GetMapping("/search")
	public ApiResponse<AccommodationPageResponse> searchAccommodationsByElastic(
		@Valid @ModelAttribute AccommodationSearchRequest request) {
		return ApiResponse.success(accommodationService.findFilterAccommodationByElastic(request));
	}
}
