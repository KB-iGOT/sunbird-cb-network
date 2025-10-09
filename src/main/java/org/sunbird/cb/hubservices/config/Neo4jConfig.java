
package org.sunbird.cb.hubservices.config;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sunbird.cb.hubservices.exception.GraphException;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.cb.hubservices.util.PropertiesCache;
import org.neo4j.driver.exceptions.AuthenticationException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;

@Configuration
public class Neo4jConfig {

	private Logger logger = LoggerFactory.getLogger(Neo4jConfig.class);

	@Bean
	public Driver Neo4jDriver() {
		try {
			String uri = PropertiesCache.getInstance().getProperty(Constants.NEO4J_HOST_URL);
			String user = PropertiesCache.getInstance().getProperty(Constants.NEO4J_USER_NAME);
			String pass = PropertiesCache.getInstance().getProperty(Constants.NEO4J_PASSWORD);
			int maxPoolSize = Optional
					.ofNullable(PropertiesCache.getInstance().getProperty(Constants.NEO4J_MAX_POOL_SIZE))
					.map(Integer::parseInt)
					.orElse(150);
			int connectionAcquisitionTimeout = Optional.ofNullable(
					PropertiesCache.getInstance().getProperty(Constants.NEO4J_CONNECTION_ACQUISITION_TIMEOUT))
					.map(Integer::parseInt)
					.orElse(90);
			int connectionTimeout = Optional
					.ofNullable(PropertiesCache.getInstance().getProperty(Constants.NEO4J_CONNECTION_TIMEOUT))
					.map(Integer::parseInt)
					.orElse(5);
			int connectionLivenessCheckTimeout = Optional
					.ofNullable(PropertiesCache.getInstance()
							.getProperty(Constants.NEO4J_CONNECTION_LIVENESS_CHECK_TIMEOUT))
					.map(Integer::parseInt)
					.orElse(30);
			Config config = Config.builder()
					.withMaxConnectionPoolSize(maxPoolSize)
					.withConnectionAcquisitionTimeout(connectionAcquisitionTimeout, TimeUnit.SECONDS)
					.withConnectionTimeout(connectionTimeout, TimeUnit.SECONDS)
					.withConnectionLivenessCheckTimeout(connectionLivenessCheckTimeout, TimeUnit.SECONDS)
					.build();
			if (Boolean.parseBoolean(PropertiesCache.getInstance().getProperty(Constants.NEO4J_AUTH_ENABLED))) {
				return GraphDatabase.driver(uri, AuthTokens.basic(user, pass), config);
			} else {
				// If authentication is not enabled, use the default driver without credentials
				return GraphDatabase.driver(uri, config);
			}
		} catch (AuthenticationException | ServiceUnavailableException e) {
			logger.error("Failed to initialize Neo4J connection. Exception: ", e);
			throw new GraphException(e.code(), e.getMessage());
		}
	}
}
