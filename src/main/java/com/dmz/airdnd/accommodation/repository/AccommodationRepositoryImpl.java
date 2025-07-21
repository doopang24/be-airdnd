package com.dmz.airdnd.accommodation.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.dmz.airdnd.accommodation.domain.Accommodation;
import com.dmz.airdnd.accommodation.domain.QAccommodation;
import com.dmz.airdnd.accommodation.dto.FilterCondition;
import com.dmz.airdnd.reservation.domain.QAvailability;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/*
 AccommodationRepositoryCustom 인터페이스의 QueryDSL 구현체.
 Spring Data의 Query Method만으로 처리하기 어려운 복잡한 동적 검색 쿼리를 담당합니다.
 */
@RequiredArgsConstructor
public class AccommodationRepositoryImpl implements AccommodationRepositoryCustom {
	private final JPAQueryFactory queryFactory;

	/*
	 다양한 필터 조건을 조합하여 숙소를 동적으로 검색하고, 결과를 페이징하여 반환합니다.

	 @param pageable 페이징 정보 (페이지 번호, 페이지 크기)
	 @param filterCondition 검색 필터 조건 (위치, 가격, 인원수, 날짜 등)
	 @return 페이징된 숙소 검색 결과 (Page<Accommodation>)
	 */
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

		// 1. 위치 기반 검색 조건 생성 (MySQL 의 ST_Distance_Sphere 함수 사용)
		// Expressions.booleanTemplate 을 사용하여 JPA 표준이 아닌 Native 함수를 타입-세이프하게 호출합니다.
		String userPointWkt = String.format("POINT(%f %f)", lat, lng);
		BooleanExpression distanceCondition = Expressions.booleanTemplate(
			"ST_Distance_Sphere({0}, ST_GeomFromText({1}, 4326)) <= {2}",
			accommodation.address.location,
			userPointWkt,
			radiusKm * 1000
		);

		// 2. 실제 페이지에 보여줄 숙소 목록 조회 (Content Query)
		// where절의 각 조건은 null 일 경우 무시되어, 필요한 조건만으로 동적 쿼리가 생성됩니다.
		List<Accommodation> accommodations = queryFactory
			.selectFrom(accommodation)
			.where(
				// 가격 조건
				minPrice != null ? accommodation.pricePerDay.goe(minPrice) : null,
				maxPrice != null ? accommodation.pricePerDay.loe(maxPrice) : null,
				// 인원수 조건
				maxGuests != null ? accommodation.maxGuests.goe(maxGuests) : null,
				// 예약 가능 날짜 조건 (요청된 날짜에 예약이 존재하지 않아야 함)
				JPAExpressions.selectOne()
					.from(availability)
					.where(
						availability.accommodation.eq(accommodation),
						availability.date.in(requestedDates)
					)
					.notExists(),
				// 위치 조건
				distanceCondition
			)
			.orderBy(accommodation.pricePerDay.asc())
			.offset(pageable.getOffset()) // 페이징 처리: 시작점
			.limit(pageable.getPageSize()) // 페이징 처리: 가져올 개수
			.fetch();

		// 3. 전체 결과 개수 조회 (Count Query)
		// 페이징 UI(총 페이지 수 등)를 만들기 위해, offset/limit 없이 전체 조건에 맞는 데이터 개수를 별도로 조회합니다.
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
				distanceCondition
			)
			.fetchOne();

		// 4. 조회된 내용(accommodations)과 전체 개수(total)를 사용하여 Page 객체를 생성하여 반환합니다.
		long safeTotal = total != null ? total : 0L;

		return new PageImpl<>(accommodations, pageable, safeTotal);
	}
}
