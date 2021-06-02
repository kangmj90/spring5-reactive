package org.rpis5.chapters.chapter_07.jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class BookJdbcRepository {

   @Autowired
   JdbcTemplate jdbcTemplate;

   public Book findById(int id) {
      return jdbcTemplate.queryForObject(
         "SELECT * FROM book WHERE id=?",
         new Object[] { id },
         new BeanPropertyRowMapper<>(Book.class));
   }

   /**
    * JdbcTemplate 의 불편함을 개선한 NamedParameterJdbcTemplate
    * 쿼리에 ? 대신 :search_phrase 처럼 매개변수 전달로 가독성 높임
    */
   public List<Book> findByTitle(String phrase) {
      NamedParameterJdbcTemplate named =
         new NamedParameterJdbcTemplate(jdbcTemplate);
      SqlParameterSource namedParameters
         = new MapSqlParameterSource("search_phrase", phrase);

      String sql = "SELECT * FROM book WHERE title = :search_phrase";

      return named.query(
         sql,
         namedParameters,
         new BeanPropertyRowMapper<>(Book.class));
   }

   /**
    * BookMapper 클래스를 사용
    */
   public List<Book> findAll() {
      return jdbcTemplate.query("SELECT * FROM book", new BookMapper());
   }

   /**
    * ResultSet 를 도메인 엔티티로 변환하는 방법을 지정하는 mapper 클래스
    */
   static class BookMapper implements RowMapper<Book> {

      @Override
      public Book mapRow(ResultSet rs, int rowNum) throws SQLException {
         return new Book(rs.getInt("id"), rs.getString("title"));
      }
   }
}
