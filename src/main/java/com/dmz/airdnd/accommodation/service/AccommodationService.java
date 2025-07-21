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

@Service
@RequiredArgsConstructor
public class AccommodationService {

	private final AccommodationRepository accommodationRepository;

	private final LabelRepository labelRepository;

	private final ElasticsearchRepository<AccommodationDocument, String> elasticsearchRepository;

	private final AccommodationSearchRepository accommodationSearchRepository;

	private final AddressService addressService;

	private final GeocodingClient geocodingClient;

	@RoleCheck(Role.HOST)
	public AccommodationCreateResponse createAccommodation(AccommodationCreateRequest request) {
		try {
			CoordinatesDto coordinates = geocodingClient.lookupCoordinates(request.getBaseAddress());
			Address address = addressService.getOrCreateByFullAddress(
				request.getCountry(),
				request.getBaseAddress(),
				request.getDetailedAddress(),
				coordinates);

			List<Label> labels = labelRepository.findAllById(request.getLabelIds());
			if (labels.size() != request.getLabelIds().size()) {
				throw new LabelNotFoundException(ErrorCode.LABEL_NOT_FOUND);
			}

			Accommodation newAccommodation = AccommodationMapper.toEntity(request, address, labels);
			Accommodation saved = accommodationRepository.save(newAccommodation);

			// mysql 에 저장한 후 elastic cloud 에도 저장
			AccommodationDocument document = AccommodationMapper.toDocument(saved, address, coordinates);
			elasticsearchRepository.save(document);

			return AccommodationMapper.fromEntity(saved);
		} catch (DataIntegrityViolationException dive) {
			throw new DuplicateAddressException(ErrorCode.DUPLICATE_ADDRESS);
		}
	}

	@Transactional(readOnly = true)
	public AccommodationPageResponse findFilteredAccommodations(AccommodationSearchRequest request) {
		FilterCondition filterCondition = AccommodationMapper.toCondition(request);
		Pageable pageable = PageRequest.of(request.getPageNumber() - 1, request.getPageSize());

		Page<Accommodation> accommodationPage = accommodationRepository.findFilteredAccommodations(pageable,
			filterCondition);
		return AccommodationMapper.toPageResponse(accommodationPage);
	}

	@Transactional(readOnly = true)
	public AccommodationPageResponse findFilterAccommodationByElastic(AccommodationSearchRequest request) {
		Pageable pageable = PageRequest.of(request.getPageNumber() - 1, request.getPageSize());

		Point center = new Point(request.getLongitude(), request.getLatitude());
		Distance radius = new Distance(request.getRadiusKm(), Metrics.KILOMETERS);

		Page<AccommodationDocument> documents = accommodationSearchRepository.findByLocationNear(center, radius,
			pageable);

		return AccommodationPageResponse.builder()
			.page(documents.getNumber() + 1)
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
