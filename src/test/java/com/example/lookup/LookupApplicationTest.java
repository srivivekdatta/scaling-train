package com.example.lookup;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:lookup;MODE=Oracle;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Sql(statements = {
    "DROP ALL OBJECTS",
    "CREATE TABLE message (message_id VARCHAR2(30) PRIMARY KEY, payment_id VARCHAR2(128), status VARCHAR2(30))",
    "CREATE TABLE transaction (transaction_id VARCHAR2(30) PRIMARY KEY, payment_id VARCHAR2(128), status VARCHAR2(30))",
    "CREATE TABLE results (result_id VARCHAR2(30) PRIMARY KEY, payment_id VARCHAR2(128), status VARCHAR2(30))",
    "CREATE TABLE disposition (disposition_id VARCHAR2(30) PRIMARY KEY, payment_id VARCHAR2(128), status VARCHAR2(30))",
    "INSERT INTO message VALUES ('MSG-01', 'PAY-A', 'STORED'), ('MSG-02', 'PAY-A', 'STORED')",
    "INSERT INTO transaction VALUES ('TX-01', 'PAY-A', 'COMPLETE')",
    "INSERT INTO results VALUES ('RES-01', 'PAY-A', 'ACCEPTED')",
    "INSERT INTO disposition VALUES ('DSP-01', 'PAY-A', 'SETTLED'), ('DSP-02', 'PAY-B', 'SETTLED')"
})
class LookupApplicationTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void opensWithPaymentIdFormAndNoTimeline() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"paymentId\"")))
            .andExpect(content().string(not(containsString("Processing flow"))));
    }

    @Test
    void rendersAllMatchingRowsInStageOrder() throws Exception {
        mvc.perform(post("/").param("paymentId", "PAY-A"))
            .andExpect(status().isOk())
            .andExpect(content().string(stringContainsInOrder(
                "<h3>Message</h3>", "MSG-01", "MSG-02", "<h3>Transaction</h3>", "TX-01",
                "<h3>Results</h3>", "RES-01", "<h3>Disposition</h3>", "DSP-01")))
            .andExpect(content().string(not(containsString("DSP-02"))))
            .andExpect(content().string(not(containsString("No records found"))));
    }

    @Test
    void rendersDispositionEvenWhenEarlierStagesAreMissing() throws Exception {
        mvc.perform(post("/").param("paymentId", "PAY-B"))
            .andExpect(status().isOk())
            .andExpect(content().string(stringContainsInOrder(
                "<h3>Message</h3>", "No records found", "<h3>Transaction</h3>", "No records found",
                "<h3>Results</h3>", "No records found", "<h3>Disposition</h3>", "DSP-02")))
            .andExpect(content().string(not(containsString("No records found for this Payment ID."))));
    }

    @Test
    void rendersOneEmptyStateWhenEveryTableIsEmpty() throws Exception {
        mvc.perform(post("/").param("paymentId", "unknown"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("No records found for this Payment ID.")))
            .andExpect(content().string(not(containsString("Processing flow"))));
    }
}
