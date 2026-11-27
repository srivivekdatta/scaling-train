# Oracle record lookup

A Spring Boot and Thymeleaf app that traces a Payment ID across four Oracle tables. The timeline shows Message (parsed from Kafka and stored), Transaction, Results, and Disposition in that order. Each table is queried independently with a bound parameter. The page shows all matching rows and flags stages with no records. If every table is empty for the Payment ID, the page shows one empty-state message. The Message step is based on stored database rows.

## Build and run

Use JDK 21 and the included Gradle Wrapper.

```sh
./gradlew test bootJar
export SPRING_DATASOURCE_URL='jdbc:oracle:thin:@//oracle.example.com:1521/APPDB'
# Set SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD securely.
./gradlew bootRun
```

Open http://localhost:8080. The tests exercise JDBC and Thymeleaf together using H2 in Oracle compatibility mode. Verify the mapped queries on Oracle before deployment.

## Map the Oracle schema

Update `LookupRepository.java` to match your schema. The starter assumes text `PAYMENT_ID` columns and the following projections.

| Table | Primary key shown | Other field shown |
|---|---|---|
| MESSAGE | MESSAGE_ID | STATUS |
| TRANSACTION | TRANSACTION_ID | STATUS |
| RESULTS | RESULT_ID | STATUS |
| DISPOSITION | DISPOSITION_ID | STATUS |

Qualify the tables with their schema as needed. For a numeric ID, validate and parse it before binding it as a numeric JDBC parameter. If a child table is reachable by foreign key, use its actual relationship in the query. For example, if messages carry a transaction foreign key:

```sql
SELECT m.message_id AS record_id, m.status
FROM message m
WHERE EXISTS (
    SELECT 1 FROM transaction t
    WHERE t.transaction_id = m.transaction_id AND t.payment_id = :paymentId
)
ORDER BY m.message_id
```

Check the four queries with an ID present in only one or two tables, an ID with multiple rows in a table, and an ID absent from all tables.

## Deploy on OpenShift

1. Run `./gradlew test bootJar`, then build and push the image using `podman build -t YOUR_IMAGE .` and `podman push YOUR_IMAGE`.
2. In the target project, create ConfigMap `oracle-lookup-db` with a `url` key. Create Secret `oracle-lookup-db` with `username` and `password` keys for a read-only Oracle account.
3. Replace the image reference in `openshift/app.yaml`, then run `oc apply -f openshift/app.yaml` and `oc rollout status deployment/oracle-lookup`.
4. For a cluster-local check, run `oc port-forward svc/oracle-lookup 8080:8080`. To make the UI available to other users, expose the Service through your organization's authenticated ingress.

The deployment checks application liveness and includes database health in readiness.
