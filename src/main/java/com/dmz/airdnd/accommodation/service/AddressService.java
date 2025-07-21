package com.dmz.airdnd.accommodation.service;

import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import com.dmz.airdnd.accommodation.adapter.GeocodingClient;
import com.dmz.airdnd.accommodation.domain.Address;
import com.dmz.airdnd.accommodation.dto.response.CoordinatesDto;
import com.dmz.airdnd.accommodation.repository.AddressRepository;
import com.dmz.airdnd.accommodation.util.GeoPointFactory;

import lombok.RequiredArgsConstructor;

/*
 주소(Address) 데이터의 생성과 조회를 책임지는 서비스 클래스.
 동일한 주소 데이터가 중복으로 저장되는 것을 방지하고, 주소 관련 로직을 중앙에서 관리하여 데이터의 일관성과 재사용성을 높이는 역할을 합니다.
 */
@Service
@RequiredArgsConstructor
public class AddressService {

	private final AddressRepository addressRepository;

	/*
	 주어진 주소 정보로 데이터베이스에서 기존 주소를 찾거나, 없으면 새로 생성하여 반환합니다. (get-or-create 패턴)
	 이 로직을 통해 여러 숙소가 동일한 주소 객체를 공유하게 되어 데이터 정규화를 만족시키고, 불필요한 데이터 중복을 막아 저장 공간을 효율적으로 사용합니다.
	 @param country 국가
	 @param baseAddress 기본 주소 (e.g., "서울시 강남구 테헤란로")
	 @param detailedAddress 상세 주소 (e.g., "427, 10층")
	 @param coordinates 외부 API 를 통해 변환된 위도, 경도 좌표
	 @return DB에 저장되거나 조회된 Address 엔티티
	 */
	public Address getOrCreateByFullAddress(String country, String baseAddress, String detailedAddress,
		CoordinatesDto coordinates) {
		// 상세 주소가 null 이거나 공백일 경우, 일관성을 위해 빈 문자열로 통일합니다.
		String detail = StringUtils.trimToEmpty(detailedAddress);
		Point geometryPoint = findPointFromBaseAddress(coordinates);

		// 기본 주소와 상세 주소를 기준으로 DB 에서 기존 주소 정보를 찾습니다.
		// 기존의 주소 중에서 일치하는게 없다면 새로운 Address 객체를 build() 하여 반환합니다.
		return addressRepository.findByBaseAddressAndDetailedAddress(baseAddress, detail)
			.orElseGet(() -> Address.builder()
				.country(country)
				.baseAddress(baseAddress)
				.detailedAddress(detail)
				.location(geometryPoint)
				.build()
			);
	}

	/*
	 좌표(CoordinatesDto)를 JTS 의 Point 객체로 변환합니다.
	 이 메소드는 JTS 라이브러리의 표준 좌표 순서인 (경도, 위도)를 따릅니다.

	 @param coordinates 위도, 경도 정보를 담은 DTO
	 @return JTS 라이브러리의 Point 객체
	 */
	private Point findPointFromBaseAddress(CoordinatesDto coordinates) {
		double latitude = coordinates.latitude();
		double longitude = coordinates.longitude();
		return GeoPointFactory.createPoint(longitude, latitude);
	}
}
