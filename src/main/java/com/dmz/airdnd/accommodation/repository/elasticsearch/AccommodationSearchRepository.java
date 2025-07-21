package com.dmz.airdnd.accommodation.repository.elasticsearch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Repository;

import com.dmz.airdnd.accommodation.document.AccommodationDocument;

/*
 Elasticsearch 를 위한 숙소 검색 리포지토리 인터페이스.
 Spring Data Elasticsearch 의 ElasticsearchRepository 를 상속받아 기본적인 CRUD 기능을 자동으로 제공받습니다.
 */
@Repository
public interface AccommodationSearchRepository extends ElasticsearchRepository<AccommodationDocument, String> {

	/*
	 Spring Data Elasticsearch 의 'Query Method' 기능을 사용하여 위치 기반 검색을 수행합니다.
	 메소드 이름을 분석하여, 주어진 지점(Point)을 중심으로 특정 거리(Distance) 내에 있는
	 모든 숙소 도큐먼트를 찾아 페이징하여 반환하는 geo_distance 쿼리를 자동으로 생성합니다.

	 @param location 검색의 중심이 될 지리적 좌표 (Point)
	 @param distance 중심점으로부터의 최대 거리 (Distance)
	 @param pageable 페이징 및 정렬 정보
	 @return 페이징된 숙소 도큐먼트 목록 (Page<AccommodationDocument>)
	 */
	Page<AccommodationDocument> findByLocationNear(Point location, Distance distance, Pageable pageable);
}
