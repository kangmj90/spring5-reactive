package org.rpis5.chapters.chapter_07.r2dbs;

import org.springframework.stereotype.Repository;

/**
 * @author kangminjeong
 * @since 2021. 5. 31.
 */

@Repository
public interface BookR2dbcRepository extends R2dbcRepository<Book, Integer>{
}
