package com.dmz.airdnd.accommodation.mapper;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Point;

import com.dmz.airdnd.accommodation.document.AccommodationDocument;
import com.dmz.airdnd.accommodation.domain.Accommodation;
import com.dmz.airdnd.accommodation.domain.Address;
import com.dmz.airdnd.accommodation.domain.Label;
import com.dmz.airdnd.accommodation.domain.LabelType;
import com.dmz.airdnd.accommodation.dto.request.AccommodationCreateRequest;
import com.dmz.airdnd.accommodation.dto.response.AccommodationResponse;
import com.dmz.airdnd.accommodation.dto.response.AddressResponse;
import com.dmz.airdnd.accommodation.dto.response.CoordinatesDto;
import com.dmz.airdnd.accommodation.dto.response.LabelResponse;
import com.dmz.airdnd.accommodation.dto.response.AccommodationCreateResponse;

public class AccommodationMapper {

	private static final int MONTHS_TO_ADD = 3;

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

	public static AccommodationDocument toDocument(Accommodation accommodation, Address address,
		CoordinatesDto coordinates) {
		org.springframework.data.geo.Point location = new org.springframework.data.geo.Point(coordinates.longitude(),
			coordinates.latitude());
		// 6개월
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
}
