/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.nats.spring.boot.autoconfigure;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Consumer;
import io.nats.client.ErrorListener;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * NatsAutoConfiguration will create a NATS connection from an instance of NatsProperties.
 * A default connection and error handler is provided with basic logging.
 * <p>
 * {@link EnableAutoConfiguration Auto-configuration} for NATS.
 */
@AutoConfiguration
@ConditionalOnClass({Connection.class})
@EnableConfigurationProperties(NatsProperties.class)
public class NatsAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(NatsAutoConfiguration.class);

    /**
     * @return NATS connection created with the provided properties. If no server URL is set the method will return null.
     * @throws IOException              when a connection error occurs
     * @throws InterruptedException     in the unusual case of a thread interruption during connect
     * @throws GeneralSecurityException if there is a problem authenticating the connection
     */
    @Bean
    @ConditionalOnMissingBean
    @Conditional(NatsServerConfiguredCondition.class)
    public @Nullable Connection natsConnection(@Nullable NatsProperties properties,
                                     @Nullable ConnectionListener connectionListener,
                                     @Nullable ErrorListener errorListener)
            throws IOException, InterruptedException, GeneralSecurityException {
        // Defensive for direct programmatic calls; bean creation is already gated by
        // NatsServerConfiguredCondition when Spring creates this bean.
        if (properties == null || !StringUtils.hasText(properties.getServer())) {
            return null;
        }

        try {
            logger.info("auto connecting to NATS with properties - {}", properties);
            Options.Builder builder = properties.toOptionsBuilder();

            builder = builder.connectionListener(connectionListener);

            builder = builder.errorListener(errorListener);

            return Nats.connect(builder.build());
        } catch (Exception e) {
            logger.warn("error connecting to NATS", e);
            throw e;
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public ConnectionListener defaultConnectionListener() {
        return new ConnectionListener() {
            public void connectionEvent(@Nullable Connection conn, Events type) {
                logger.info("NATS connection status changed {}", type);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorListener defaultErrorListener() {
        return new ErrorListener() {
            @Override
            public void slowConsumerDetected(@Nullable Connection conn, @Nullable Consumer consumer) {
                logger.info("NATS connection slow consumer detected");
            }

            @Override
            public void exceptionOccurred(@Nullable Connection conn, Exception exp) {
                logger.warn("NATS connection exception occurred", exp);
            }

            @Override
            public void errorOccurred(@Nullable Connection conn, String error) {
                logger.warn("NATS connection error occurred {}", error);
            }
        };
    }


}
