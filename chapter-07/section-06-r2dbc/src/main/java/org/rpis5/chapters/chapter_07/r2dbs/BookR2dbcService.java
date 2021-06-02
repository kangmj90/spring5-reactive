package org.rpis5.chapters.chapter_07.r2dbs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author kangminjeong
 * @since 2021. 5. 31.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class BookR2dbcService {
    private final BookR2dbcRepository repository;

    public void example() {

        /**
         * findAll()할 경우 기존에 Optional<List<Book>> 혹은 List<Book> 로 넘어오던 부분이 Flux 로 변경
         */
        Flux<Book> allBook = repository.findAll();
        List<Book> blocked = allBook.collectList().block();

        blocked.forEach(item -> log.info("book : {}", item));
    }
}
