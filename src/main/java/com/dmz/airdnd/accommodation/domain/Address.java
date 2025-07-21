package com.dmz.airdnd.accommodation.domain;

import org.locationtech.jts.geom.Point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 주소 정보를 나타내는 JPA 엔티티.
 여러 숙소가 동일한 주소를 공유할 수 있도록 하여 데이터 중복을 방지하고, 데이터베이스 정규화를 만족시키는 역할을 합니다.
 */
@Entity
@Table(
	name = "address",
	// '기본 주소'와 '상세 주소'의 조합이 항상 고유하도록 데이터베이스 레벨에서 제약 조건을 설정합니다.
	// 이를 통해 AddressService 의 get-or-create 로직의 데이터 일관성을 보장하고,
	// 동일한 주소가 중복으로 생성되는 것을 원천적으로 방지합니다.
	uniqueConstraints = @UniqueConstraint(
		name = "uk_address_base_detail",
		columnNames = {"base_address", "detailed_address"}
	)
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Address {

	/*
	 주소의 고유 식별자 (Primary Key).
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String country;

	@Column(length = 255, nullable = false)
	private String baseAddress;

	@Column(length = 255)
	private String detailedAddress;

	/*
	 주소의 지리적 위치 좌표.
	 columnDefinition 을 통해 데이터베이스에 POINT 타입과 공간 참조 시스템 ID(SRID) 4326 (WGS 84)을 명시적으로 지정합니다.
	 이를 통해 MySQL 의 공간 검색 기능을 정확하게 사용할 수 있습니다.
	 */
	@Column(nullable = false, columnDefinition = "POINT SRID 4326 NOT NULL")
	private Point location;
}

