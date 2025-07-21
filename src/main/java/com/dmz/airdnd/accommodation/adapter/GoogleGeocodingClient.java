package com.dmz.airdnd.accommodation.adapter;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.dmz.airdnd.accommodation.dto.response.CoordinatesDto;
import com.dmz.airdnd.accommodation.dto.response.GeocodeResponse;
import com.dmz.airdnd.common.exception.ErrorCode;
import com.dmz.airdnd.common.exception.GeocodingException;

import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 GeocodingClient 인터페이스의 Google API 구현체.
 어댑터 패턴을 사용하여 외부 시스템(Google Geocoding API)의 구현细节을 애플리케이션의 나머지 부분과 분리합니다.
 이 클래스는 주소 문자열을 위도/경도 좌표로 변환하는 책임을 가집니다.
*/
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleGeocodingClient implements GeocodingClient {

	// 매직 스트링을 피하고 코드의 가독성과 유지보수성을 높이기 위해 상수를 사용합니다.
	private static final String GOOGLE_GEOCODING_URL = "https://maps.googleapis.com/maps/api/geocode/json";
	private static final String STATUS_OK = "OK";
	private static final String URI_ADDRESS = "address";
	private static final String URI_KEY = "key";
	private static final String URI_LANGUAGE = "language";
	private static final String LANGUAGE_VALUE_KO = "ko";

	// Spring 의 RestTemplate 을 사용하여 외부 API 와 HTTP 통신을 수행합니다.
	private final RestTemplate restTemplate;

	// API 키는 보안과 유연성을 위해 코드에 하드코딩하지 않고, 외부 설정 파일에서 주입받습니다.
	@Value("${GOOGLE_MAPS_API_KEY}")
	private String apiKey;

	/*
	 주어진 주소 문자열을 실제 지리적 좌표(위도, 경도)로 변환합니다.

	 @param baseAddress 변환할 주소 문자열
	 @return 위도, 경도 정보를 담은 DTO
	 @throws GeocodingException API 호출 실패 또는 유효하지 않은 응답 시 발생
	 */
	@Override
	public CoordinatesDto lookupCoordinates(String baseAddress) {
		// 1. UriComponentsBuilder 를 사용하여 안전하게 요청 URI 를 생성합니다.
		URI coordinateRequestUri = generateUri(baseAddress);
		GeocodeResponse geocodeResponse;

		try {
			// 2. RestTemplate을 사용하여 외부 API를 호출하고, 응답 JSON을 GeocodeResponse 객체로 자동 변환합니다.
			geocodeResponse = restTemplate.getForObject(coordinateRequestUri, GeocodeResponse.class);
		} catch (RestClientException e) {
			// 3. 네트워크 오류 등 API 호출 중 발생하는 모든 예외를 잡아, 서비스에 특화된 예외로 변환하여 던집니다.
			//    이를 통해 서비스 레이어는 외부 시스템의 구체적인 예외 상황을 알 필요가 없어집니다.
			throw new GeocodingException(ErrorCode.GEOCODING_FAILED);
		}

		// 4. API 응답이 유효한지 검증합니다. (null 체크, 상태 코드 확인, 결과 존재 여부 확인)
		//    이러한 방어적인 코드는 NullPointerException 을 방지하고 시스템의 안정성을 높입니다.
		if (geocodeResponse == null || !STATUS_OK.equals(geocodeResponse.getStatus()) || geocodeResponse.getResults()
			.isEmpty()) {
			log.warn("유효하지 않은 GeocodeResponse: {}", geocodeResponse);
			throw new GeocodingException(ErrorCode.GEOCODING_FAILED);
		}

		// 5. 유효한 응답에서 필요한 좌표 정보만 추출하여 반환합니다.
		GeocodeResponse.Location location = geocodeResponse.getResults().get(0).getGeometry().getLocation();

		return new CoordinatesDto(location.getLatitude(), location.getLongitude());
	}

	/*
	 Google Geocoding API 요청을 위한 URI를 생성하는 헬퍼 메소드.
	 UriComponentsBuilder 를 사용하면 파라미터의 URL 인코딩을 자동으로 처리해주어 안전합니다.

	 @param baseAddress 인코딩할 주소 문자열
	 @return 완성된 URI 객체
	 */
	private URI generateUri(String baseAddress) {
		return UriComponentsBuilder
			.fromHttpUrl(GOOGLE_GEOCODING_URL)
			.queryParam(URI_ADDRESS, baseAddress)
			.queryParam(URI_KEY, apiKey)
			.queryParam(URI_LANGUAGE, LANGUAGE_VALUE_KO) // 결과를 한글로 받기 위한 설정
			.encode(StandardCharsets.UTF_8) // 한글 주소가 깨지지 않도록 UTF-8로 인코딩
			.build()
			.toUri();
	}
}
