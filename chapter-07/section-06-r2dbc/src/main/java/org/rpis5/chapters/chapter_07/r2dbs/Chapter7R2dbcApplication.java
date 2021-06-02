package org.rpis5.chapters.chapter_07.r2dbs;

import io.r2dbc.client.R2dbc;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.function.TransactionalDatabaseClient;
import org.springframework.data.r2dbc.core.TransactionalDatabaseClient;

import java.time.Duration;
import java.util.Arrays;

/**
 * ATTENTION: Test requires running Docker Engine!
 *
 * Application may fail on the first start-up due to timeout for Docker image download.
 * Please, retry a few times in case of failures.
 *
 * 공식 문서 번역 : https://godekdls.github.io/Spring%20Data%20R2DBC/contents/
 *
 * Blocking 문제를 Reactive 하게 접근하여 Non-blocking 하게 동작하게 한 것이어서
 * DB 자체를 Non-blocking 하게 한 것이 아닌 client 접근 부분만 Non-blocking 하게 한 것
 * 따라서 DB 내부에서는 Lock 이 걸릴 것이기 때문에 근본적으로는 ... DB 의 병목 현상을 해결해야 한다
 *
 * 현재 비동기를 지원하는 DB는 R2DBC 기준으로 mssql, postgre, h2 (mysql 은 예정)
 * 기존 jdbc나 jpa > mvc,  nio를 지원하는 api가 있는 DB > webflux
 *
 * https://happyer16.tistory.com/entry/스프링-blocking-vs-non-blocking-R2DBC-vs-JDBC-WebFlux-vs-Web-MVC
 * R2DBC + Webflux가 높은 동시성때는 좋은 선택이다.
 *  요청당 CPU를 조금 사용함
 *  용청당 메모리가 조금 필요함
 *  응답시간이 빠름
 *  처리량도 높아짐
 *  JAR도 가벼워짐
 * 대략 200 concurrenct request 밑으로는 Spring Web MVC + JDBC가 좋은 선택
 */
@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
@Import({
    PostgresConfiguration.class,
    InfrastructureConfiguration.class
})
public class Chapter7R2dbcApplication implements CommandLineRunner {

    private final PostgresqlConnectionFactory pgConnectionFactory;
    private final TransactionalDatabaseClient databaseClient;
    private final BookRepository bookRepository;

    public static void main(String[] args) {
        SpringApplication.run(Chapter7R2dbcApplication.class, args);
    }

    @Override
    public void run(String... args) {
        databaseClient.execute()
            .sql("create table book (id integer, title varchar(50), publishing_year integer);")
            .fetch()
            .rowsUpdated()
            .doOnSuccess(count -> log.info("Database schema created"))
            .block();

        // Manual data insert
        databaseClient.execute()
            .sql("insert into book (id, title, publishing_year) values (4, 'Yellow Mars', 2009);")
            .fetch()
            .rowsUpdated()
            .doOnSuccess(count -> log.info("Manual book insert, inserted {}", count))
            .block();

        databaseClient.inTransaction(session ->
            session.execute()
                .sql("select * from book where id = 4")
                .as(Book.class)
                .fetch()
                .all())
            .subscribe(b -> log.info("Book from transaction: {}", b));

        // Note: Repo is in alpha, no ID is generated
        bookRepository.saveAll(Arrays.asList(
            new Book("Blue Mars", 2009),
            new Book("Red Mars", 1998),
            new Book("Pink Mars", 2009)
        ))
            .count()
            .doOnSuccess(count -> log.info("{} books inserted", count))
            .block(Duration.ofSeconds(2));

        bookRepository.findById(1)
            .doOnSuccess(b -> log.info("Book with id=1: {}", b))
            .block();

        bookRepository.findAll()
            .doOnNext(book -> log.info("Book: {}", book))
            .count()
            .doOnSuccess(count -> log.info("Database contains {} books", count))
            .block();

        log.info("The latest books in the DB:");
        bookRepository.findTheLatestBooks()
            .doOnNext(book -> log.info("Book: {}", book))
            .count()
            .doOnSuccess(count -> log.info("Database contains {} latest books", count))
            .block();

        log.info("Using raw R2DBC Client");
        // Using raw client
        R2dbc r2dbc = new R2dbc(pgConnectionFactory);

        /**
         * inTransaction 트랜잭션 생성
         * handle 리액티브 커넥션의 인스턴스를 래핑. SQL 실행
         * execute sql 문장과 쿼리 매개변수 받기 → 영향 받은 행의 수 반환
         * thenMany 이후 다시 작업 시작
         * mapResult Flux 타입 반환
         *
         * R2DBC Client 는 리액티브 스타일의 연쇄형 API 제공
         */
        r2dbc.inTransaction(handle ->
            handle
                .execute("insert into book (id, title, publishing_year) " +
                        "values ($1, $2, $3)",
                    20, "The Sands of Mars", 1951)
                .doOnNext(inserted -> log.info("{} rows was inserted into DB", inserted))
        ).thenMany(r2dbc.inTransaction(handle ->
            handle.select("SELECT title FROM book")
                .mapResult(result ->
                    result.map((row, rowMetadata) ->
                        row.get("title", String.class)))))
//                .subscribe(elem -> log.info(" - Title: {}", elem));
            .doOnNext(elem -> log.info(" - Title: {}", elem))
            .blockLast();

        log.info("Application finished successfully!");
    }

}
