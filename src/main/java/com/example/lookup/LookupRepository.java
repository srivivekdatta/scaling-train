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

    public List<Stage> find(String paymentId) {
        return List.of(
            new Stage("Message", "The Kafka message is parsed and stored.", query("""
                SELECT message_id AS record_id, status FROM message
                WHERE payment_id = :paymentId ORDER BY message_id
                """, paymentId)),
            new Stage("Transaction", "Transaction records for this payment.", query("""
                SELECT transaction_id AS record_id, status FROM transaction
                WHERE payment_id = :paymentId ORDER BY transaction_id
                """, paymentId)),
            new Stage("Results", "Processing results for this payment.", query("""
                SELECT result_id AS record_id, status FROM results
                WHERE payment_id = :paymentId ORDER BY result_id
                """, paymentId)),
            new Stage("Disposition", "Final disposition records for this payment.", query("""
                SELECT disposition_id AS record_id, status FROM disposition
                WHERE payment_id = :paymentId ORDER BY disposition_id
                """, paymentId))
        );
    }

    private List<Row> query(String sql, String paymentId) {
        return jdbc.query(sql, Map.of("paymentId", paymentId), (rs, index) ->
            new Row(rs.getString("record_id"), rs.getString("status")));
    }

    public record Stage(String title, String description, List<Row> rows) {}
    public record Row(String id, String status) {}
}
