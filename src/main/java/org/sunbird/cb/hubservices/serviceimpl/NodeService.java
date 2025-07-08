package org.sunbird.cb.hubservices.serviceimpl;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.sunbird.cb.hubservices.exception.GraphException;
import org.sunbird.cb.hubservices.exception.ValidationException;
import org.sunbird.cb.hubservices.model.Node;
import org.sunbird.cb.hubservices.service.INodeService;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.hubservices.dao.IGraphDao;

import io.micrometer.core.instrument.util.StringUtils;

@Service
public class NodeService implements INodeService {

	private Logger logger = LoggerFactory.getLogger(NodeService.class);

	@Autowired
	private IGraphDao graphDao;

	@Override
	public Boolean connect(Node from, Node to, Map<String, String> relationProperties) throws Exception {

		if (!(Objects.isNull(from) || Objects.isNull(to) || CollectionUtils.isEmpty(relationProperties) || from.getId().equalsIgnoreCase(to.getId())))
		{
			Boolean isNodeFromPresent = graphDao.upsertNode(from);
			Boolean isNodeToPresent = graphDao.upsertNode(to);
			if(isNodeToPresent && isNodeFromPresent) {
				return graphDao.upsertRelation(from, to, relationProperties);
			}
		}
		return  Boolean.FALSE;
	}

	@Override
	public List<Node> getNodes(String id, Map<String, String> relationProperties, Constants.DIRECTION direction,
							   int offset, int size, List<String> attributes) {
		checkParams(id, relationProperties);
		return getNodesWith(id, relationProperties, direction, offset, size, attributes);
	}

	@Override
	public int getNodesCount(String id, Map<String, String> relationProperties, Constants.DIRECTION direction) {
		int count = 0;
		if (StringUtils.isEmpty(id)) {
			throw new ValidationException("id or relation properties cannot be empty");
		}
		try {
			count = graphDao.getNeighboursCount(id, relationProperties, direction);

		} catch (GraphException e) {
			logger.error("Nodes count failed: {}", e);
		}
		return count;
	}

	@Override
	public List<Node> getNodeNextLevel(String id, Map<String, String> relationProperties, int offset,
									   int size) {
		checkParams(id, relationProperties);
		return graphDao.getNeighbours(id, relationProperties, Constants.DIRECTION.OUT, 2, offset, size,
				Arrays.asList(Constants.Graph.ID.getValue()));
	}

	private List<Node> getNodesWith(String id, Map<String, String> relationProperties,
									Constants.DIRECTION direction, int offset, int size, List<String> attributes) {
		return graphDao.getNeighbours(id, relationProperties, direction, 1, offset, size, attributes);
	}

	private void checkParams(String id, Map<String, String> relationProperties) {
		if (StringUtils.isEmpty(id) || CollectionUtils.isEmpty(relationProperties)) {
			throw new ValidationException("id or relation properties cannot be empty");
		}
	}

	@Override
	public Map<String, String> getRelationshipBetweenUsers(String fromUserId, String toUserId) {
		try {
			return graphDao.getRelationshipBetweenUsers(fromUserId, toUserId);
		} catch (GraphException e) {
			logger.error(String.format("Error fetching relationship between %s and %s: %s", fromUserId, toUserId, e));
			return new HashMap<>();
		}
	}

	/**
	 * Finds recommendations for a user based on the provided request parameters.
	 *
	 * @param userId The ID of the user for whom recommendations are to be found.
	 * @param request A map containing request parameters for finding recommendations.
	 * @return A list of maps, each representing a recommendation with relevant details.
	 */
	@Override
	public  List<Map<String, String>> findRecommendationForUser(String userId, Map<String, Object> request) {
		try {
			return graphDao.findRecommendationForUser(userId,request);
		} catch (GraphException e) {
			logger.error(String.format("Error fetching Recommendations for user %s %s", userId, e));
			return new ArrayList<>();
		}
	}

	/**
	 * Finds recommendations for mentors based on the provided request parameters.
	 *
	 * @param userId  The ID of the user for whom mentor recommendations are to be found.
	 * @param request A map containing request parameters for finding mentor recommendations.
	 * @return A list of maps, each representing a mentor recommendation with relevant details.
	 */
	@Override
	public List<Map<String, String>> findRecommendationForMentors(String userId, Map<String, Object> request) {
		try {
			return graphDao.findRecommendationForMentors(userId, request);
		} catch (GraphException e) {
			logger.error(String.format("Error fetching Mentor Recommendations for user %s %s", userId, e));
			return new ArrayList<>();
		}
	}

	/**
	 * Finds blocked users based on the provided request parameters.
	 *
	 * @param userId  The ID of the user for whom blocked users are to be found.
	 * @param request A map containing request parameters for finding blocked users.
	 * @return A list of maps, each representing a blocked user with relevant details.
	 */
	@Override
	public List<Map<String, String>> findBlockedUsers(String userId, Map<String, Object> request) {
		try {
			return graphDao.findBlockedUsers(userId, request);
		} catch (GraphException e) {
			logger.error(String.format("Error fetching Blocked users data %s %s", userId, e));
			return new ArrayList<>();
		}
	}

	/**
	 * Get the count of connections by status for a user.
	 *
	 * @param userId   The ID of the user for whom the connections count is to be fetched.
	 * @param pending  The status of the connections to filter by (e.g., "pending", "accepted").
	 * @param direction The direction of the connection (e.g., incoming, outgoing).
	 * @return A map containing the count of connections by status.
	 */
	@Override
	public Map<String, Integer> getConnectionsCountByStatus(String userId, String pending, Constants.DIRECTION direction) {
		try {
			return graphDao.getConnectionsCountByStatus(userId, pending, direction);
		} catch (GraphException e) {
			logger.error(String.format("Error fetching connections count by status for user %s: %s", userId, e));
		}
		return new HashMap<>();
	}

	/**
	 * Get the count of recommended users for a given user.
	 *
	 * @param userId The ID of the user for whom the count of recommended users is to be fetched.
	 * @return An integer representing the count of recommended users.
	 */
	@Override
	public Integer getCoundForRecommendedUsers(String userId) {
		try {
			return graphDao.getCoundForRecommendedUsers(userId);
		} catch (GraphException e) {
			logger.error(String.format("Error fetching connections count by status for user %s: %s", userId, e));
		}
		return 0;
	}

}