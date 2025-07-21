package com.dmz.airdnd.accommodation.mapper;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;

import com.dmz.airdnd.accommodation.document.AccommodationDocument;
import com.dmz.airdnd.accommodation.domain.Accommodation;
import com.dmz.airdnd.accommodation.domain.Address;
import com.dmz.airdnd.accommodation.domain.Label;
import com.dmz.airdnd.accommodation.domain.LabelType;
import com.dmz.airdnd.accommodation.dto.FilterCondition;
import com.dmz.airdnd.accommodation.dto.request.AccommodationCreateRequest;
import com.dmz.airdnd.accommodation.dto.request.AccommodationSearchRequest;
import com.dmz.airdnd.accommodation.dto.response.AccommodationPageResponse;
import com.dmz.airdnd.accommodation.dto.response.AccommodationResponse;
import com.dmz.airdnd.accommodation.dto.response.AddressResponse;
import com.dmz.airdnd.accommodation.dto.response.CoordinatesDto;
import com.dmz.airdnd.accommodation.dto.response.LabelResponse;
import com.dmz.airdnd.accommodation.dto.response.AccommodationCreateResponse;
import com.dmz.airdnd.common.exception.ErrorCode;
import com.dmz.airdnd.common.exception.InvalidFilterConditionException;

/*
 애플리케이션의 각 계층(DTO, Entity, Document) 간의 데이터 변환을 책임지는 중앙 매퍼 클래스.
 이 클래스를 통해 객체 생성 로직을 중앙에서 관리하고, 각 계층의 독립성을 보장합니다.
 */
public class AccommodationMapper {

	private static final int MONTHS_TO_ADD = 3;

	/*
	 숙소 생성 요청 DTO(AccommodationCreateRequest)를 JPA 엔티티(Accommodation)로 변환합니다.
	 Controller 에서 받은 데이터를 데이터베이스에 저장하기 위한 형태로 가공합니다.
	 */
	public static Accommodation toEntity(AccommodationCreateRequest request, Address address, List<Label> labels) {
		return Accommodation.builder()
			.name(request.getName())
			.description(request.getDescription())
			.pricePerDay(request.getPricePerDay())
			.currency(request.getCurrency())
			.maxGuests(request.getMaxGuests())
			.bedCount(request.getBedCount())
			.bedroomCount(request.getBedroomCount())
			.bathroomCount(request.getBathroomCount())
			.address(address)
			.labels(labels)
			.build();
	}

	/*
	 JPA 엔티티(Accommodation)를 클라이언트 응답용 DTO(AccommodationResponse)로 변환합니다.
	 데이터베이스의 원본 데이터를 클라이언트에게 필요한 형태로 가공하여 전달합니다.
	 */
	public static AccommodationResponse toResponse(Accommodation accommodation) {
		return AccommodationResponse.builder()
			.id(accommodation.getId())
			.addressResponse(toResponse(accommodation.getAddress()))
			.labelResponses(toResponses(accommodation.getLabels()))
			.name(accommodation.getName())
			.description(accommodation.getDescription())
			.pricePerDay(accommodation.getPricePerDay())
			.currency(accommodation.getCurrency())
			.maxGuests(accommodation.getMaxGuests())
			.bedCount(accommodation.getBedCount())
			.bedroomCount(accommodation.getBedroomCount())
			.bathroomCount(accommodation.getBathroomCount())
			.createdAt(accommodation.getCreatedAt())
			.updatedAt(accommodation.getUpdatedAt())
			.build();
	}

	public static AddressResponse toResponse(Address address) {
		Point location = address.getLocation();
		return AddressResponse.builder()
			.country(address.getCountry())
			.baseAddress(address.getBaseAddress())
			.detailedAddress(address.getDetailedAddress())
			.latitude(location.getX())
			.longitude(location.getY())
			.build();
	}

	public static List<LabelResponse> toResponses(List<Label> labels) {
		return labels.stream().map(label -> new LabelResponse(label.getId(), label.getName())).toList();
	}

	public static AccommodationCreateResponse fromEntity(Accommodation accommodation) {
		return AccommodationCreateResponse.builder()
			.id(accommodation.getId())
			.address(formatFullAddress(accommodation.getAddress()))
			.labels(accommodation.getLabels())
			.name(accommodation.getName())
			.description(accommodation.getDescription())
			.pricePerDay(accommodation.getPricePerDay())
			.currency(accommodation.getCurrency())
			.maxGuests(accommodation.getMaxGuests())
			.bedCount(accommodation.getBedCount())
			.bedroomCount(accommodation.getBedroomCount())
			.bathroomCount(accommodation.getBathroomCount())
			.createdAt(accommodation.getCreatedAt())
			.build();
	}

	private static String formatFullAddress(Address address) {
		String base = address.getBaseAddress();
		String detail = address.getDetailedAddress();
		if (detail != null && !detail.isEmpty()) {
			return base + " " + detail;
		}
		return base;
	}

	/*
	 JPA 엔티티(Accommodation)를 Elasticsearch 도큐먼트(AccommodationDocument)로 변환합니다.
	 MySQL 의 원본 데이터를 검색엔진에 색인하기 위한 형태로 가공합니다.
	 이 과정에서 JTS 의 Point 를 Spring Data 의 Point 로 변환하는 등, 검색에 최적화된 데이터 구조를 만듭니다.
	 */
	public static AccommodationDocument toDocument(Accommodation accommodation, Address address,
		CoordinatesDto coordinates) {
		org.springframework.data.geo.Point location = new org.springframework.data.geo.Point(coordinates.longitude(),
			coordinates.latitude());
		LocalDate startDate = LocalDate.now();
		LocalDate endDate = startDate.plusMonths(MONTHS_TO_ADD);
		List<LocalDate> availableDates = startDate
			.datesUntil(endDate)
			.toList();

		return AccommodationDocument.builder()
			.id(String.valueOf(accommodation.getId()))
			.name(accommodation.getName())
			.description(accommodation.getDescription())
			.pricePerDay(accommodation.getPricePerDay())
			.currency(accommodation.getCurrency())
			.maxGuests(accommodation.getMaxGuests())
			.bedCount(accommodation.getBedCount())
			.bedroomCount(accommodation.getBedroomCount())
			.bathroomCount(accommodation.getBathroomCount())
			.createdAt(accommodation.getCreatedAt().toInstant())
			.updatedAt(accommodation.getUpdatedAt() != null ? accommodation.getUpdatedAt().toInstant() : null)
			.location(location)
			.country(address.getCountry())
			.baseAddress(address.getBaseAddress())
			.detailedAddress(address.getDetailedAddress())
			.labels(accommodation.getLabels().stream()
				.map(Label::getName)
				.collect(Collectors.toList()))
			.availableDates(availableDates)
			.build();
	}

	/*
	 Elasticsearch 도큐먼트(AccommodationDocument)를 클라이언트 응답용 DTO(AccommodationResponse)로 변환합니다.
	 검색 결과를 클라이언트에게 보여주기 위한 형태로 가공합니다.
	 */
	public static AccommodationResponse fromDocument(AccommodationDocument document) {
		AddressResponse addressResponse = AddressResponse.builder()
			.country(document.getCountry())
			.baseAddress(document.getBaseAddress())
			.detailedAddress(document.getDetailedAddress())
			.latitude(document.getLocation().getX())
			.longitude(document.getLocation().getY())
			.build();

		return AccommodationResponse.builder()
			.id(Long.valueOf(document.getId()))
			.addressResponse(addressResponse)
			.labelResponses(
				document.getLabels().stream()
					.map(labelName -> new LabelResponse(LabelType.fromDisplayName(labelName), labelName))
					.collect(Collectors.toList())
			)
			.name(document.getName())
			.description(document.getDescription())
			.pricePerDay(document.getPricePerDay())
			.currency(document.getCurrency())
			.maxGuests(document.getMaxGuests())
			.bedCount(document.getBedCount())
			.bedroomCount(document.getBedroomCount())
			.bathroomCount(document.getBathroomCount())
			.createdAt(Timestamp.from(document.getCreatedAt()))
			.updatedAt(document.getUpdatedAt() != null ? Timestamp.from(document.getUpdatedAt()) : null)
			.build();
	}

	/*
	 JPA 의 Page<Accommodation> 객체를 클라이언트 응답용 DTO(AccommodationPageResponse)로 변환합니다.
	 페이징 관련 메타데이터(총 페이지 수, 현재 페이지 등)를 포함하여 변환합니다.
	 */
	public static AccommodationPageResponse toPageResponse(Page<Accommodation> accommodationPage) {
		return AccommodationPageResponse.builder()
			.pageNumber(accommodationPage.getNumber() + 1)
			.pageSize(accommodationPage.getSize())
			.totalElements(accommodationPage.getTotalElements())
			.totalPages(accommodationPage.getTotalPages())
			.accommodationResponses(accommodationPage.getContent()
				.stream()
				.map(AccommodationMapper::toResponse)
				.collect(Collectors.toList()))
			.build();
	}

	/*
	 검색 요청 DTO(AccommodationSearchRequest)를 내부 검색 조건 객체(FilterCondition)로 변환합니다.
	 이 과정에서 날짜 범위, 가격 범위 등 검색 조건에 대한 유효성 검사를 함께 수행합니다.
	 */
	public static FilterCondition toCondition(AccommodationSearchRequest request) {
		LocalDate checkIn = request.getCheckIn();
		LocalDate checkOut = request.getCheckOut();

		if (checkIn != null && checkOut != null) {
			if (!checkIn.isBefore(checkOut)) {
				throw new InvalidFilterConditionException(ErrorCode.INVALID_DATE_RANGE);
			}
		}

		if (request.getMinPrice() != null && request.getMaxPrice() != null) {
			if (request.getMinPrice() > request.getMaxPrice()) {
				throw new InvalidFilterConditionException(ErrorCode.INVALID_MIN_MAX_PRICE);
			}
		}

		List<LocalDate> requestedDates =
			(checkIn != null && checkOut != null) ? checkIn.datesUntil(checkOut).toList() : List.of();

		return FilterCondition.builder()
			.longitude(request.getLongitude())
			.latitude(request.getLatitude())
			.radiusKm(request.getRadiusKm())
			.minPrice(request.getMinPrice())
			.maxPrice(request.getMaxPrice())
			.maxGuests(request.getMaxGuests())
			.requestedDates(requestedDates)
			.build();
	}
}
