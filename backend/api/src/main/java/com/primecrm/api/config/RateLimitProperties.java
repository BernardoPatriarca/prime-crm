package com.primecrm.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private Limit general = new Limit(300, 60);
    private Limit login = new Limit(10, 60);

    @Getter
    @Setter
    public static class Limit {

        private int capacity;
        private long windowSeconds;

        public Limit() {
        }

        public Limit(int capacity, long windowSeconds) {
            this.capacity = capacity;
            this.windowSeconds = windowSeconds;
        }
    }
}
