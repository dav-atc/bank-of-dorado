/*
 * Copyright 2020, Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package anthos.samples.bankofanthos.balancereader;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

/**
 * Entry point for the BalanceReader Spring Boot application.
 *
 * Microservice to track the bank balance for each user account.
 */
@SpringBootApplication(exclude = ZipkinAutoConfiguration.class)
public class BalanceReaderApplication {

    private static final Logger LOGGER =
        LogManager.getLogger(BalanceReaderApplication.class);

    private static final String[] EXPECTED_ENV_VARS = {
        "VERSION",
        "PORT",
        "LOCAL_ROUTING_NUM",
        "PUB_KEY_PATH",
        "SPRING_DATASOURCE_URL",
        "SPRING_DATASOURCE_USERNAME",
        "SPRING_DATASOURCE_PASSWORD"
    };

    public static void main(String[] args) {
        // Check that all required environment variables are set.
        for (String v : EXPECTED_ENV_VARS) {
            String value = System.getenv(v);
            if (value == null) {
                LOGGER.fatal(String.format(
                    "%s environment variable not set", v));
                System.exit(1);
            }
        }
        SpringApplication.run(BalanceReaderApplication.class, args);
        LOGGER.log(Level.forName("STARTUP", Level.FATAL.intLevel()),
            String.format("Started BalanceReader service. Log level is: %s",
                LOGGER.getLevel().toString()));

    }

    @PreDestroy
    public void destroy() {
        LOGGER.info("BalanceReader service shutting down");
    }

    /**
     * Initializes Meter Registry with custom CloudWatch configuration
     *
     * @return the MeterRegistry with configuration
     */
    @Bean
    public static MeterRegistry meterRegistry() {
        CloudWatchConfig cloudWatchConfig = new CloudWatchConfig() {
            @Override
            public boolean enabled() {
                boolean enableMetricsExport = true;

                if (System.getenv("ENABLE_METRICS") != null
                    && System.getenv("ENABLE_METRICS").equalsIgnoreCase("false")) {
                    enableMetricsExport = false;
                }

                LOGGER.info(String.format("Enable metrics export: %b",
                    enableMetricsExport));
                return enableMetricsExport;
            }

            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public String namespace() {
                return "BalanceReader";
            }
        };

        return new CloudWatchMeterRegistry(
            cloudWatchConfig,
            Clock.SYSTEM,
            CloudWatchAsyncClient.builder().build()
        );
    }
}
