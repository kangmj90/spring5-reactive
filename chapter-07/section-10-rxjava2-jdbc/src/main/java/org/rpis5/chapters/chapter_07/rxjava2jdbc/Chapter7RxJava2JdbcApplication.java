package org.rpis5.chapters.chapter_07.rxjava2jdbc;

import io.reactivex.Flowable;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.rpis5.chapters.chapter_07.rxjava2jdbc.book.Book;
import org.rpis5.chapters.chapter_07.rxjava2jdbc.book.RxBookRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 리액티브 응용 프로그램을 블로킹하지 않으면서 JDBC 드라이버를 래핑
 * 전용 스레드 풀과 논블로킹 커넥션 풀 개념 사용으로 클라이언트의 요청은 커넥션을 기다리는 동안 스레드를 차단하지 않음
 * 연결이 가능해지면 쿼리는 실행을 시작하고 스레드를 블로킹
 *
 * 라이브러리가 jdbc의 wrapper일 뿐이기 때문에 jdbc call을 할 때마다 새로운 스레드를 생성해서 blocking 작업을 처리
 * 매번 jdbc call할 때마다 새로운 스레드를 생성하므로 결국 jdbc를 직접 사용할 때와 같은문제에 봉착
 */
@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
public class Chapter7RxJava2JdbcApplication implements CommandLineRunner {

    private final RxBookRepository bookRepository;

    public static void main(String[] args) {
        SpringApplication.run(Chapter7RxJava2JdbcApplication.class, args);
    }

    @Override
    public void run(String... args) {
        reportResults("All books before save:s", bookRepository.findAll());

        bookRepository.save(
            Flowable.just(
                Book.of("Artemis", 2017),
                Book.of("The Expanse: Leviathan Wakes", 2011),
                Book.of("The Expanse: Caliban's War", 2012)
            ))
            .blockingLast();

        reportResults("All books after save:", bookRepository.findAll());

        reportResults("Book with title Artemis: ",
            bookRepository
                .findByTitle(Mono.just("Artemis"))
                .toFlowable());

        reportResults("Book with id='99999999-1967-47a1-aaaa-8399a29866a0':",
            bookRepository
                .findById("99999999-1967-47a1-aaaa-8399a29866a0")
                .toFlowable());

        reportResults("Books from XX century:",
            bookRepository
                .findByYearBetween(Single.just(1900), Single.just(1999)));

        Mono.delay(Duration.ofSeconds(5))
            .subscribe(e -> log.info("Application finished successfully!"));

    }

    private void reportResults(String message, Publisher<Book> books) {
        Flux
            .from(books)
            .map(Book::toString)
            .reduce(
                new StringBuffer(),
                (sb, b) -> sb.append(" - ")
                    .append(b)
                    .append("\n"))
            .doOnNext(sb -> log.info(message + "\n{}", sb))
            .subscribe();
    }
}
