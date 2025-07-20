package com.dmz.airdnd.accommodation.repository;

import java.time.LocalDate;
import java.util.List;

import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.dmz.airdnd.accommodation.domain.Accommodation;
import com.dmz.airdnd.accommodation.domain.QAccommodation;
import com.dmz.airdnd.accommodation.dto.FilterCondition;
import com.dmz.airdnd.accommodation.util.GeoPointFactory;
import com.dmz.airdnd.reservation.domain.QAvailability;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AccommodationRepositoryImpl implements AccommodationRepositoryCustom {
	private final JPAQueryFactory queryFactory;

	@Override
	public Page<Accommodation> findFilteredAccommodations(Pageable pageable, FilterCondition filterCondition) {
		QAccommodation accommodation = QAccommodation.accommodation;
		QAvailability availability = QAvailability.availability;

		double lng = filterCondition.longitude();
		double lat = filterCondition.latitude();
		double radiusKm = filterCondition.radiusKm();
		Integer minPrice = filterCondition.minPrice();
		Integer maxPrice = filterCondition.maxPrice();
		Integer maxGuests = filterCondition.maxGuests();
		List<LocalDate> requestedDates = filterCondition.requestedDates();

		Point userLocation = GeoPointFactory.createPoint(lng, lat);
		String wkt = String.format("POINT(%s %s)", lat, lng);

		List<Accommodation> accommodations = queryFactory
			.selectFrom(accommodation)
			.where(
				minPrice != null ? accommodation.pricePerDay.goe(minPrice) : null,
				maxPrice != null ? accommodation.pricePerDay.loe(maxPrice) : null,
				maxGuests != null ? accommodation.maxGuests.goe(maxGuests) : null,
				JPAExpressions.selectOne()
					.from(availability)
					.where(
						availability.accommodation.eq(accommodation),
						availability.date.in(requestedDates)
					)
					.notExists(),
				Expressions.booleanTemplate(
					"ST_Contains("
						+ "ST_Buffer("
						+ "ST_GeomFromText('" + wkt + "', 4326),"
						+ "{1}"
						+ "),"
						+ "{0}"
						+ ")",
					accommodation.address.location,
					"'" + wkt + "'",
					(int)(radiusKm * 1000)
				)
			)
			.orderBy(accommodation.pricePerDay.asc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		Long total = queryFactory
			.select(accommodation.count())
			.from(accommodation)
			.where(
				minPrice != null ? accommodation.pricePerDay.goe(minPrice) : null,
				maxPrice != null ? accommodation.pricePerDay.loe(maxPrice) : null,
				maxGuests != null ? accommodation.maxGuests.goe(maxGuests) : null,
				JPAExpressions.selectOne()
					.from(availability)
					.where(
						availability.accommodation.eq(accommodation),
						availability.date.in(requestedDates)
					)
					.notExists(),
				Expressions.booleanTemplate(
					"ST_Contains("
						+ "ST_Buffer("
						+ "ST_GeomFromText('" + wkt + "', 4326),"
						+ "{1}"
						+ "),"
						+ "{0}"
						+ ")",
					accommodation.address.location,
					"'" + wkt + "'",
					(int)(radiusKm * 1000)
				)
			)
			.fetchOne();

		long safeTotal = total != null ? total : 0L;

		return new PageImpl<>(accommodations, pageable, safeTotal);
	}
}
