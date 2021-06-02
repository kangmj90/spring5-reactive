package org.rpis5.chapters.chapter_07.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * CrudRepository 상속하여 기본 CRUD 연산을 지원하는 메소드 사용 가능
 *
 * 스프링 데이터 JDBC는 블로킹 API인 JDBC 를 필수로 사용하기 때문에 리액티브 스택에 적합하지 않음
 * 현재 접근 방식
 *    JdbcRepository(Spring data JDBC) → JdbcTemplate(Spring JDBC) → JDBC API → 블로킹 드라이버 → DB
 * 리액티브로 전환될 접근 방식
 *    ReactiveR2dbcRepository → DatabaseClient → R2DBC SPI → 리액티브 드라이버 → DB
 */
@Repository
public interface BookSpringDataJdbcRepository
   extends CrudRepository<Book, Integer> {

   /**
    * 스프링 데이터 JDBC 사용으로 스프링 JDBC 와 달리 ResultSet 변환을 하지 않음
    * 결과로 List 를 반환하는데 이 동작으로 인해 클라이어트는 전체 쿼리가 도착할 때 까지 블로킹 됨
    */
   @Query("SELECT * FROM book WHERE LENGTH(title) = " +
          "(SELECT MAX(LENGTH(title)) FROM book)")
   List<Book> findByLongestTitle();

   /**
    * 내부 구현과 데이터베이스 자체가 이 동작을 지원하는 경우에는 기본 구현에 따라
    * 데이터베이스가 쿼리를 실행하는 동안에도 API 가 첫 번째 원소를 처리하도록 할 수 있다.
    */
   @Query("SELECT * FROM book WHERE LENGTH(title) = " +
          "(SELECT MIN(LENGTH(title)) FROM book)")
   Stream<Book> findByShortestTitle();

   /**
    * repository 가 스프링 프레임워크의 비동기 모드를 활용
    * CompletableFuture 를 반환하므로 클라이언트 스레드는 결과를 기다리는 동안 블로킹되지 않는다.
    * 하지만 JDBC 가 블로킹 방식으로 동작하기 때문에 내부적으로 스레드는 락(lock)이 걸림
    */
   @Async
   @Query("SELECT * FROM book b " +
      "WHERE b.title = :title")
   CompletableFuture<Book> findBookByTitleAsync(
      @Param("title") String title);

   /**
    * CompletableFuture 과 Stream 의 결합
    * 클라이언트 스레드는 첫 번째 행이 도착할 때까지 블로킹 되지 않으며 결과 집합이 청크 형태로 전달
    * 하지만 첫 번째 실행에서 메인 스레드는 블로킹 되어야 하고 클라이언트의 스레드는 다음 데이터 청크를 검색할 때 블로킹하게 됨
    * 리액티브 지원 없이 JDBC 를 사용해 얻을 수 있는 최상의 방법
    */
   @Async
   @Query("SELECT * FROM book b " +
      "WHERE b.id > :fromId AND b.id < :toId")
   CompletableFuture<Stream<Book>> findBooksByIdBetweenAsync(
      @Param("fromId") Integer from,
      @Param("toId") Integer to);
}
