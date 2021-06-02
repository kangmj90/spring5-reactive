package org.rpis5.chapters.chapter_07.wrapped_sync;

import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import static reactor.function.TupleUtils.function;

/**
 * 블로킹 메서드를 래핑하는 방식은 JPA 지연 로드를 사용하지 못할 가능성이 큼
 * 트랜잭션을 지원하려면 rxjava2-jdbc 라이브러리와 비슷한 추가 작업 필요
 * 모든 트랜잭션이 하나 이상의 블로킹 호출을 하지 않도록 블로킹 작업 래팽
 */
@Component
public class RxBookRepository extends
   ReactiveCrudRepositoryAdapter<Book, Integer, BookJpaRepository> {

   public RxBookRepository(
      BookJpaRepository delegate,
      Scheduler scheduler
   ) {
      super(delegate, scheduler);
   }

   public Flux<Book> findByIdBetween(
      Publisher<Integer> lowerPublisher,
      Publisher<Integer> upperPublisher
   ) {
      return Mono.zip(
         Mono.from(lowerPublisher),
         Mono.from(upperPublisher)
      ).flatMapMany(
         function((lower, upper) ->
            Flux
               .fromIterable(delegate.findByIdBetween(lower, upper))
               .subscribeOn(scheduler)
         ))
         .subscribeOn(scheduler);
   }

   public Flux<Book> findShortestTitle() {
      return Mono.fromCallable(delegate::findShortestTitle)
         .subscribeOn(scheduler)
         .flatMapMany(Flux::fromIterable);
   }
}
