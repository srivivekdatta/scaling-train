package com.example.lookup;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:lookup;MODE=Oracle;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Sql(statements = {
    "DROP ALL OBJECTS",
    "CREATE TABLE payment (payment_id VARCHAR2(30) PRIMARY KEY, lookup_id VARCHAR2(128), status VARCHAR2(30))",
    "CREATE TABLE transaction (transaction_id VARCHAR2(30) PRIMARY KEY, lookup_id VARCHAR2(128), status VARCHAR2(30))",
    "CREATE TABLE message (message_id VARCHAR2(30) PRIMARY KEY, lookup_id VARCHAR2(128), status VARCHAR2(30))",
    "INSERT INTO payment VALUES ('PAY-01', 'A', 'PAID'), ('PAY-02', 'A', 'PENDING')",
    "INSERT INTO transaction VALUES ('TX-01', 'B', 'COMPLETE')",
    "INSERT INTO message VALUES ('MSG-01', 'A', 'SENT')"
})
class LookupApplicationTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void rendersMultipleRowsAndAnEmptySection() throws Exception {
        mvc.perform(post("/").param("id", "A"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("PAY-01")))
            .andExpect(content().string(containsString("PAY-02")))
            .andExpect(content().string(containsString("MSG-01")))
            .andExpect(content().string(containsString("<h2>Transactions</h2>")))
            .andExpect(content().string(containsString("No matching records.")))
            .andExpect(content().string(not(containsString("TX-01"))));
    }

    @Test
    void rendersRecordsWhenOnlyOneTableMatches() throws Exception {
        mvc.perform(post("/").param("id", "B"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("TX-01")))
            .andExpect(content().string(containsString("<h2>Messages</h2>")))
            .andExpect(content().string(not(containsString("PAY-01"))));
    }

    @Test
    void rendersOneEmptyStateWhenEveryTableIsEmpty() throws Exception {
        mvc.perform(post("/").param("id", "unknown"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("No records found for this ID.")))
            .andExpect(content().string(not(containsString("<section>"))));
    }
}
