package com.isp.lg.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserTypeSchemaUpdater implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public UserTypeSchemaUpdater(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        Integer colCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'user_type'",
                Integer.class
        );
        if (colCount == null || colCount == 0) {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN user_type VARCHAR(16) NOT NULL DEFAULT 'LOCAL' AFTER mobile");
        }
        jdbcTemplate.execute("UPDATE users SET user_type = 'LOCAL' WHERE user_type IS NULL OR user_type = ''");
    }
}
