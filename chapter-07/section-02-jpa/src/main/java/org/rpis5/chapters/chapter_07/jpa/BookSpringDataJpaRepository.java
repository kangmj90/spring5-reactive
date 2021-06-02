package org.rpis5.chapters.chapter_07.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 리액티브 프로그래밍에서는 사용하지 않는 가정들을 기반으로 설계되었고 코드 베이스 리팩토링의 어려움
 * 스프링 데이터 JPA 모듈을 리액티브 방식으로 이용하려면 JDBC, JPA 및 JPA 공급자를 포함하여 기본 레이어도 함꼐 리액티브 방식으로 동작해야하는데
 * 이러한 이유로 JPA 의 리액티브화 된 것은 없음 (없을 예정)
 */
@Repository
public interface BookSpringDataJpaRepository
   extends PagingAndSortingRepository<Book, Integer> {

   Iterable<Book> findByIdBetween(int lower, int upper);

   @Query("SELECT b FROM Book b WHERE " +
          "LENGTH(b.title) = (SELECT MIN(LENGTH(b2.title)) FROM Book b2)")
   Iterable<Book> findShortestTitle();
}

