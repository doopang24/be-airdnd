package com.dmz.airdnd.accommodation.document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.geo.Point;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 Elasticsearch 에 저장될 숙소 도큐먼트의 스키마(매핑)를 정의하는 클래스.
 이 클래스는 'accommodations' 인덱스에 저장될 JSON 도큐먼트의 Java 표현입니다.
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "accommodations") // 이 클래스가 Elasticsearch 도큐먼트임을 나타내고, 'accommodations' 인덱스에 저장되도록 설정합니다.
public class AccommodationDocument {

	// 도큐먼트의 고유 식별자. Elasticsearch 의 '_id' 필드에 매핑됩니다.
	@Id
	private String id;

	/*
	 숙소 이름. 전문 검색(Full-text search)을 위해 'Text' 타입을 사용합니다.
	 'Text' 타입은 Elasticsearch 의 분석기(Analyzer)에 의해 토큰화, 소문자화 등의 과정을 거쳐
	 "멋진 오션뷰" 같은 구문으로도 검색이 가능하게 합니다.
	 */
	@Field(type = FieldType.Text)
	private String name;

	// 숙소 설명. 이름과 마찬가지로 전문 검색을 위해 'Text' 타입을 사용합니다.
	@Field(type = FieldType.Text)
	private String description;

	@Field(type = FieldType.Long)
	private Long pricePerDay;

	// 통화 단위. 'USD', 'KRW' 등 정확한 값으로 필터링, 정렬, 집계(Aggregation)를 해야 하므로 분석 과정을 거치지 않는 'Keyword' 타입을 사용합니다.
	@Field(type = FieldType.Keyword)
	private String currency;

	@Field(type = FieldType.Integer)
	private Integer maxGuests;

	@Field(type = FieldType.Integer)
	private Integer bedCount;

	@Field(type = FieldType.Integer)
	private Integer bedroomCount;

	@Field(type = FieldType.Integer)
	private Integer bathroomCount;

	@Field(type = FieldType.Date)
	private Instant createdAt;

	@Field(type = FieldType.Date)
	private Instant updatedAt;

	/*
	 숙소의 지리적 위치 좌표.
	 @GeoPointField 어노테이션을 통해 위치 기반 검색(geo-distance query)이 가능하도록 설정합니다.
	 */
	@GeoPointField
	private Point location;

	// 국가 정보. 정확한 값으로 필터링해야 하므로 'Keyword' 타입을 사용합니다.
	@Field(type = FieldType.Keyword)
	private String country;

	@Field(type = FieldType.Text)
	private String baseAddress;

	@Field(type = FieldType.Text)
	private String detailedAddress;

	// 숙소 라벨 목록. 정확한 라벨명으로 필터링해야 하므로 'Keyword' 타입을 사용합니다.
	@Field(type = FieldType.Keyword)
	private List<String> labels;

	// 예약 가능한 날짜 목록. 날짜 범위 검색을 위해 'Date' 타입을 사용합니다.
	@Field(type = FieldType.Date, format = DateFormat.date)
	private List<LocalDate> availableDates;
}
