package com.dmz.airdnd.accommodation.dto.request;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AccommodationSearchRequest {
	@NotNull(message = "경도는 필수 입력값입니다.")
	@DecimalMin(value = "0.0", inclusive = false, message = "경도는 0보다 커야 합니다.")
	@DecimalMax(value = "180.0", message = "경도는 180 이하이어야 합니다.")
	private Double longitude;

	@NotNull(message = "위도는 필수 입력값입니다.")
	@DecimalMin(value = "0.0", inclusive = false, message = "위도는 0보다 커야 합니다.")
	@DecimalMax(value = "90.0", message = "위도는 90 이하이어야 합니다.")
	private Double latitude;

	@NotNull(message = "반경은 필수 입력값입니다.")
	@DecimalMin(value = "0.0", inclusive = false, message = "반경은 0보다 커야 합니다.")
	@DecimalMax(value = "50.0", message = "반경 50km 이상은 찾을 수 없습니다.")
	private Double radiusKm;

	@Min(value = 0, message = "최소 가격은 0 이상이어야 합니다.")
	private Integer minPrice;

	@Min(value = 0, message = "최대 가격은 0 이상이어야 합니다.")
	private Integer maxPrice;

	@Min(value = 1, message = "최대 인원은 1명 이상이어야 합니다.")
	private Integer maxGuests;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate checkIn;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate checkOut;

	@Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
	private Integer pageNumber;

	@Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
	@Max(value = 30, message = "페이지 크기는 30 이하이어야 합니다.")
	private Integer pageSize;
}
