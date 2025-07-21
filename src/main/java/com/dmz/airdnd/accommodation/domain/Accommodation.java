package com.dmz.airdnd.accommodation.domain;

import java.sql.Timestamp;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 숙소 정보를 나타내는 JPA 엔티티 클래스.
 이 클래스는 데이터베이스의 'accommodation' 테이블과 매핑되며,
 시스템의 주 데이터 원천(Source of Truth) 역할을 합니다.
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Accommodation {

	/*
	 숙소의 고유 식별자 (Primary Key).
	 데이터베이스의 AUTO_INCREMENT 전략을 사용하여 자동으로 생성됩니다.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/*
	 숙소의 주소 정보. Address 엔티티와 1:1 관계를 맺습니다.
	 optional = false: 모든 숙소는 반드시 주소를 가져야 함을 의미합니다.
	 cascade = CascadeType.ALL: 숙소가 저장/삭제될 때, 관련된 주소 정보도 함께 저장/삭제됩니다.
	 fetch = FetchType.LAZY: 실제 주소 정보가 필요할 때까지 데이터베이스에서 로딩을 지연시킵니다. (성능 최적화)
	 */
	@OneToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "address_id", nullable = false)
	private Address address;

	/*
	 숙소가 가진 라벨(특징) 목록. Label 엔티티와 다대다(N:M) 관계를 맺습니다.
	 @JoinTable: 'accommodation_label'이라는 중간 테이블을 통해 관계를 매핑합니다.
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "accommodation_label",
		joinColumns = @JoinColumn(name = "accommodation_id"),
		inverseJoinColumns = @JoinColumn(
			name = "label_id",
			columnDefinition = "VARCHAR(50) NOT NULL")
	)
	private List<Label> labels;

	@Column(nullable = false, length = 50)
	private String name;

	// 숙소 설명. 긴 텍스트를 저장할 수 있도록 데이터베이스의 TEXT 타입으로 매핑됩니다.
	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private long pricePerDay;

	@Column(nullable = false, length = 50)
	private String currency;

	@Column(nullable = false)
	private int maxGuests;

	@Column(nullable = false)
	private int bedCount;

	@Column(nullable = false)
	private int bedroomCount;

	@Column(nullable = false)
	private int bathroomCount;

	/*
	 엔티티가 처음 생성될 때의 시간을 자동으로 기록합니다.
	 updatable = false: 생성 시간은 한번 정해지면 변경되지 않아야 합니다.
	 */
	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Timestamp createdAt;

	// 엔티티가 업데이트될 때마다 시간을 자동으로 기록합니다.
	@UpdateTimestamp
	private Timestamp updatedAt;
}
