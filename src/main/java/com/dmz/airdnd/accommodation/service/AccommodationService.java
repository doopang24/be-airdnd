package com.dmz.airdnd.accommodation.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dmz.airdnd.accommodation.adapter.GeocodingClient;
import com.dmz.airdnd.accommodation.document.AccommodationDocument;
import com.dmz.airdnd.accommodation.domain.Accommodation;
import com.dmz.airdnd.accommodation.dto.FilterCondition;
import com.dmz.airdnd.accommodation.dto.request.AccommodationSearchRequest;
import com.dmz.airdnd.accommodation.dto.response.AccommodationPageResponse;
import com.dmz.airdnd.accommodation.dto.response.CoordinatesDto;
import com.dmz.airdnd.accommodation.mapper.AccommodationMapper;
import com.dmz.airdnd.accommodation.domain.Label;
import com.dmz.airdnd.accommodation.repository.AccommodationRepository;
import com.dmz.airdnd.accommodation.repository.elasticsearch.AccommodationSearchRepository;
import com.dmz.airdnd.common.exception.DuplicateAddressException;
import com.dmz.airdnd.common.exception.ErrorCode;
import com.dmz.airdnd.accommodation.repository.LabelRepository;

import lombok.RequiredArgsConstructor;

import com.dmz.airdnd.accommodation.domain.Address;
import com.dmz.airdnd.accommodation.dto.request.AccommodationCreateRequest;
import com.dmz.airdnd.accommodation.dto.response.AccommodationCreateResponse;
import com.dmz.airdnd.common.aop.RoleCheck;
import com.dmz.airdnd.common.exception.LabelNotFoundException;
import com.dmz.airdnd.user.domain.Role;

/**
 * 숙소 관련 비즈니스 로직을 처리하는 서비스 클래스.
 * MySQL 의 AccommodationRepository 와 Elasticsearch 의 AccommodationSearchRepository 를 모두 사용하여
 * 데이터의 영속성 관리와 검색 기능의 성능 최적화를 동시에 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class AccommodationService {

	private final AccommodationRepository accommodationRepository;
	private final LabelRepository labelRepository;
	private final ElasticsearchRepository<AccommodationDocument, String> elasticsearchRepository;
	private final AccommodationSearchRepository accommodationSearchRepository;
	private final AddressService addressService;
	private final GeocodingClient geocodingClient;

	/*
	 새로운 숙소를 생성합니다.
	 이 메소드는 데이터의 일관성을 위해 주 데이터베이스인 MySQL 에 먼저 숙소 정보를 저장한 후,
	 검색 성능을 위해 Elasticsearch 에도 해당 데이터를 색인하는 'Dual-write' 패턴을 사용합니다.

	 @param request 숙소 생성 요청 DTO
	 @return 생성된 숙소 정보 응답 DTO
	 */
	@RoleCheck(Role.HOST)
	public AccommodationCreateResponse createAccommodation(AccommodationCreateRequest request) {
		try {
			// 1. 외부 API 를 통해 주소를 좌표로 변환 (Geocoding)
			CoordinatesDto coordinates = geocodingClient.lookupCoordinates(request.getBaseAddress());

			// 2. 주소 정보가 DB에 없으면 새로 생성하고, 있으면 기존 주소를 사용 (getOrCreate 패턴)
			Address address = addressService.getOrCreateByFullAddress(
				request.getCountry(),
				request.getBaseAddress(),
				request.getDetailedAddress(),
				coordinates);

			// 3. 요청된 라벨 ID가 모두 유효한지 확인
			List<Label> labels = labelRepository.findAllById(request.getLabelIds());
			if (labels.size() != request.getLabelIds().size()) {
				throw new LabelNotFoundException(ErrorCode.LABEL_NOT_FOUND);
			}

			// 4. 주 데이터베이스(MySQL)에 숙소 정보 저장
			Accommodation newAccommodation = AccommodationMapper.toEntity(request, address, labels);
			Accommodation saved = accommodationRepository.save(newAccommodation);

			// 5. 검색엔진(Elasticsearch)에 숙소 다큐먼트 색인
			AccommodationDocument document = AccommodationMapper.toDocument(saved, address, coordinates);
			elasticsearchRepository.save(document);

			return AccommodationMapper.fromEntity(saved);
		} catch (DataIntegrityViolationException dive) {
			// 주소 UNIQUE 제약 조건 위반 시, 사용자 정의 예외로 변환하여 처리
			throw new DuplicateAddressException(ErrorCode.DUPLICATE_ADDRESS);
		}
	}

	/*
	 MySQL 을 사용하여 다중 필터 조건(가격, 인원수, 날짜 등)으로 숙소를 검색합니다.
	 이 메소드는 QueryDSL 을 통해 동적 쿼리를 생성하여 복잡한 검색 요건을 처리합니다.

	 @param request 숙소 검색 요청 DTO
	 @return 페이징된 숙소 검색 결과
	 */
	@Transactional(readOnly = true)
	public AccommodationPageResponse findFilteredAccommodations(AccommodationSearchRequest request) {
		FilterCondition filterCondition = AccommodationMapper.toCondition(request);
		Pageable pageable = PageRequest.of(request.getPageNumber() - 1, request.getPageSize());

		Page<Accommodation> accommodationPage = accommodationRepository.findFilteredAccommodations(pageable,
			filterCondition);
		return AccommodationMapper.toPageResponse(accommodationPage);
	}

	/*
	 Elasticsearch 를 사용하여 위치 기반으로 주변 숙소를 빠르게 검색합니다.
	 Spring Data Elasticsearch 의 'Query Method' 기능을 활용하여 별도의 쿼리 구현 없이 메소드 이름('findByLocationNear')만으로 geo-distance 쿼리를 실행합니다.

	 @param request 숙소 검색 요청 DTO
	 @return 페이징된 숙소 검색 결과
	 */
	@Transactional(readOnly = true)
	public AccommodationPageResponse findFilterAccommodationByElastic(AccommodationSearchRequest request) {
		// 1. Spring Data Pageable 객체 생성 (사용자 요청 페이지는 1부터, Spring 은 0부터 시작)
		Pageable pageable = PageRequest.of(request.getPageNumber() - 1, request.getPageSize());

		// 2. 검색 중심점(Point)과 반경(Distance) 객체 생성
		Point center = new Point(request.getLongitude(), request.getLatitude());
		Distance radius = new Distance(request.getRadiusKm(), Metrics.KILOMETERS);

		// 3. Spring Data 의 Query Method 를 호출하여 Elasticsearch 에 검색 요청
		Page<AccommodationDocument> documents = accommodationSearchRepository.findByLocationNear(center, radius,
			pageable);

		// 4. 검색 결과(Page<AccommodationDocument>)를 최종 응답 DTO(AccommodationPageResponse)로 변환
		return AccommodationPageResponse.builder()
			.pageNumber(documents.getNumber() + 1) // 사용자에게 보여줄 페이지는 다시 1부터 시작하도록 +1
			.pageSize(documents.getSize())
			.totalElements(documents.getTotalElements())
			.totalPages(documents.getTotalPages())
			.accommodationResponses(
				documents.getContent().stream()
					.map(AccommodationMapper::fromDocument)
					.collect(Collectors.toList())
			)
			.build();
	}
}
