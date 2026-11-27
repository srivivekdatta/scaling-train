package com.example.lookup;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LookupRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public LookupRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Group> find(String id) {
        return List.of(
            new Group("Payments", query("""
                SELECT payment_id AS record_id, status FROM payment
                WHERE lookup_id = :id ORDER BY payment_id
                """, id)),
            new Group("Transactions", query("""
                SELECT transaction_id AS record_id, status FROM transaction
                WHERE lookup_id = :id ORDER BY transaction_id
                """, id)),
            new Group("Messages", query("""
                SELECT message_id AS record_id, status FROM message
                WHERE lookup_id = :id ORDER BY message_id
                """, id))
        );
    }

    private List<Row> query(String sql, String id) {
        return jdbc.query(sql, Map.of("id", id), (rs, index) ->
            new Row(rs.getString("record_id"), rs.getString("status")));
    }

    public record Group(String title, List<Row> rows) {}
    public record Row(String id, String status) {}
}
