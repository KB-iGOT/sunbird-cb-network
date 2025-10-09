package org.sunbird.cb.hubservices.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;

public interface CassandraConnectionManager {
	/**
	 * Method to get the cassandra session oject on basis of keyspace name provided
	 * .
	 *
	 * @param keyspaceName
	 * @return Session
	 */
    CqlSession getSession(String keyspaceName);
}
