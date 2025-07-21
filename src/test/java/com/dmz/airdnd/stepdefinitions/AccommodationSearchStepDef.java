package com.dmz.airdnd.stepdefinitions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.UriComponentsBuilder;

import com.dmz.airdnd.accommodation.repository.AccommodationRepository;
import com.dmz.airdnd.common.auth.AuthService;
import com.dmz.airdnd.common.auth.jwt.JwtUtil;
import com.dmz.airdnd.fixture.TestAccommodationFactory;
import com.dmz.airdnd.fixture.TestUserFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class AccommodationSearchStepDef {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private AccommodationRepository accommodationRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JwtUtil jwtUtil;

	private Map<String, String> requestParams = new HashMap<>();

	private ResultActions resultActions;

	private String accessToken;

	@After
	public void tearDown() {
		accommodationRepository.deleteAll();
	}

	@Given("여러 숙소 데이터가 사전에 등록되어 있다")
	public void 여러_숙소가_시스템에_등록되어_있다() {
		accommodationRepository.saveAll(TestAccommodationFactory.createTestAccommodationList());
	}

	@Given("User 사용자로 로그인이 되어있다.")
	public void User_사용자로_로그인이_되어있다() {
		accessToken = jwtUtil.generateAccessToken(TestUserFactory.createTestUser(1L));
	}

	@When("사용자가 다음 조건으로 필터링을 건다.:")
	public void 사용자가_조건으로_필터링을_걸고_검색한다(DataTable dataTable) {
		Map<String, String> filter = dataTable.asMaps(String.class, String.class).get(0);

		requestParams.putAll(filter);
	}

	@When("다음 페이지네이션 조건을 건다.:")
	public void 다음_페이지네이션_조건을_건다(DataTable dataTable) {
		Map<String, String> pagination = dataTable.asMaps(String.class, String.class).get(0);

		requestParams.putAll(pagination);
	}

	@When("숙소 검색을 요청한다.")
	public void 숙소_검색을_요청한다() throws Exception {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/accommodations");
		addQueryParamIfPresent(builder, "longitude", "경도");
		addQueryParamIfPresent(builder, "latitude", "위도");
		addQueryParamIfPresent(builder, "radiusKm", "반경");
		addQueryParamIfPresent(builder, "minPrice", "최소 가격");
		addQueryParamIfPresent(builder, "maxPrice", "최대 가격");
		addQueryParamIfPresent(builder, "maxGuests", "최대 인원");
		addQueryParamIfPresent(builder, "checkIn", "체크인");
		addQueryParamIfPresent(builder, "checkOut", "체크아웃");
		addQueryParamIfPresent(builder, "pageNumber", "페이지");
		addQueryParamIfPresent(builder, "pageSize", "페이지 크기");

		String uri = builder.build().toUriString();

		resultActions = mockMvc.perform(
			get(uri)
				.accept(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + accessToken)
		);
	}

	private void addQueryParamIfPresent(UriComponentsBuilder builder, String paramKey, String mapKey) {
		String value = requestParams.get(mapKey);
		if (value != null && !value.isBlank()) {
			builder.queryParam(paramKey, value);
		}
	}

	@Then("페이지 번호는 {string}, 페이지 크기는 {string}, 총 페이지 수는 {string}이어야 한다.")
	public void 페이지_정보를_검증한다(String page, String size, String totalPages) throws Exception {
		resultActions
			.andExpect(jsonPath("$.data.pageNumber").value(Integer.parseInt(page)))
			.andExpect(jsonPath("$.data.pageSize").value(Integer.parseInt(size)))
			.andExpect(jsonPath("$.data.totalPages").value(Integer.parseInt(totalPages)));
	}

	@Then("다음 정보의 숙소들이 반환되어야 한다.:")
	public void 다음_이름의_숙소들이_반환되어야_한다(DataTable dataTable) throws Exception {
		List<Map<String, String>> accommodations = dataTable.asMaps(String.class, String.class);

		for (int i = 0; i < accommodations.size(); i++) {
			Map<String, String> accommodation = accommodations.get(i);
			String expectedName = accommodation.get("이름");
			int expectedPrice = Integer.parseInt(accommodation.get("가격"));

			resultActions
				.andExpect(jsonPath("$.data.accommodationResponses[" + i + "].name").value(expectedName))
				.andExpect(jsonPath("$.data.accommodationResponses[" + i + "].pricePerDay").value(expectedPrice));
		}
	}
}
