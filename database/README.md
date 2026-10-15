MySQL tables are created/updated by Hibernate from the JPA entities at startup. For production, replace `ddl-auto: update` with Flyway/Liquibase migrations and set `ddl-auto: validate`.
