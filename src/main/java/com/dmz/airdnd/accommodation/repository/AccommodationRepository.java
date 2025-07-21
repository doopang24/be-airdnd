package com.dmz.airdnd.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dmz.airdnd.accommodation.domain.Accommodation;

/*
 숙소(Accommodation) 엔티티에 대한 데이터 접근을 담당하는 메인 리포지토리 인터페이스.
 서비스 계층에서는 이 인터페이스를 통해 모든 숙소 데이터 관련 작업을 수행합니다.

 이 인터페이스는 두 가지 다른 인터페이스를 상속받아 그 기능을 통합합니다.
 1. JpaRepository: `save()`, `findById()`, `findAll()` 등 기본적인 CRUD 기능을 자동으로 제공합니다.
 2. AccommodationRepositoryCustom: QueryDSL 을 사용하여 구현된 복잡한 동적 검색 기능을 제공합니다.
 Spring Data JPA 는 애플리케이션 시작 시점에 `AccommodationRepositoryCustom`의 구현체인 `AccommodationRepositoryImpl`을 자동으로 찾아, 이 인터페이스의 기능과 합쳐진 프록시 객체를 생성합니다.
 서비스 계층에서는 이 하나의 리포지토리 인터페이스만으로 모든 데이터 접근 로직을 깔끔하게 사용할 수 있습니다.

 @see org.springframework.data.jpa.repository.JpaRepository
 @see com.dmz.airdnd.accommodation.repository.AccommodationRepositoryCustom
 @see com.dmz.airdnd.accommodation.repository.AccommodationRepositoryImpl
 */
public interface AccommodationRepository extends JpaRepository<Accommodation, Long>, AccommodationRepositoryCustom {
}
