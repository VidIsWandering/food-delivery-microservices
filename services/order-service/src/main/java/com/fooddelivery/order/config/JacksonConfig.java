package com.fooddelivery.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson configuration for JSON serialization.
 *
 * IMPORTANT: This enforces snake_case for ALL JSON output.
 * - Java field: totalAmount → JSON: total_amount
 * - Java field: createdAt  → JSON: created_at
 *
 * This matches the API_STYLE_GUIDE.md naming convention.
 *
 * @see docs/development/API_STYLE_GUIDE.md#7-naming-conventions
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // snake_case: totalAmount → total_amount
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        // Java 8 date/time support (Instant → ISO 8601 string)
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
