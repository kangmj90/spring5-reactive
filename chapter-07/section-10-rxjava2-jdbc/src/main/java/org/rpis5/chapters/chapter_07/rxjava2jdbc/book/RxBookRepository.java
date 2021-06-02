package org.rpis5.chapters.chapter_07.rxjava2jdbc.book;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.tuple.Tuple2;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RxBookRepository {
   private static final String SAVE_QUERY =
         "insert into book (id, title, publishing_year) " +
         "values(:id, :title, :publishing_year) " +
         "on duplicate key " +
         "update title=:title, publishing_year=:publishing_year";

   private static final String SELECT_BY_ID =
      "select * from book where id=:id";

   private static final String SELECT_BY_TITLE =
      "select * from book where title=:title";

   /**
    * 라이브러리가 자동으로 쿼리를 생성할 수 없기 때문에 쿼리 직접 작성
    */
   private static final String SELECT_BY_YEAR_BETWEEN =
         "select * from book where " +
         "publishing_year >= :from and publishing_year <= :to";

   private final Database database;

   public Flowable<Book> save(Flowable<Book> books) {
      return books
         .flatMap(book -> save(book).toFlowable());
   }

   public Single<Book> save(Book book) {
      return database
         .update(SAVE_QUERY)
         .parameter("id", book.id())
         .parameter("title", book.title())
         .parameter("publishing_year", book.publishing_year())
         .counts()
         .ignoreElements()
         .andThen(Single.just(book));
   }

   public Flowable<Book> findAll() {
      return database
         .select(Book.class)
         .get();
   }

   public Maybe<Book> findById(String id) {
      return database
         .select(SELECT_BY_ID)
         .parameter("id", id)
         .autoMap(Book.class)
         .firstElement();
   }

   public Maybe<Book> findByTitle(Publisher<String> titlePublisher) {
      return Flowable.fromPublisher(titlePublisher)
         .firstElement()
         .flatMap(title -> database
            .select(SELECT_BY_TITLE)
            .parameter("title", title)
            .autoMap(Book.class)
            .firstElement());
   }

   /**
    * 리액터 프로젝트가 아닌 RxJava2 라이브러리 (Flowable / Single) 의 리액티브 타입
    * rxjava2-jdbc 라이브러리가 내부적으로는 RxJava2 를 사용하고 API를 통해 RxJava 타입을 사용
    * JDBC 드라이버 지원, 일부 트랜잭션 지원
    * 잠재적인 스레드 블로킹을 줄이며 관계형 데이터베이스를 리액티브 스타일로 접근할 수 있게 한다.
    * 하지만 신생 라이브러리에다가 복잡한 리액티브 워크플로 처리가 힘들고, 모든 SQL 쿼리를 직접 작성해야 한다.
    * Flowable<Book> == Flux<Book>
    */
   public Flowable<Book> findByYearBetween(
      Single<Integer> from,
      Single<Integer> to
   ) {
      return Single
         .zip(from, to, Tuple2::new)
         .flatMapPublisher(tuple -> database
            .select(SELECT_BY_YEAR_BETWEEN)
            .parameter("from", tuple._1())
            .parameter("to", tuple._2())
            .autoMap(Book.class));
   }
}
