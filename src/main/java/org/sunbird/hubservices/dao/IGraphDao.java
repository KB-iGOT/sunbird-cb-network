package org.sunbird.hubservices.dao;

import java.util.List;
import java.util.Map;

import org.sunbird.cb.hubservices.model.Node;
import org.sunbird.cb.hubservices.util.Constants;

public interface IGraphDao {

	public Boolean upsertNode(Node node) throws Exception ;

	public Boolean upsertRelation(Node nodeFrom, Node nodeTo, Map<String, String> relationProperties) throws Exception;

	public int getNeighboursCount(String UUID, Map<String, String> relationProperties, Constants.DIRECTION direction);

	public List<Node> getNeighbours(String UUID, Map<String, String> relationProperties, Constants.DIRECTION direction,
			int level, int offset, int limit, List<String> attributes);

	Map<String, String> getRelationshipBetweenUsers(String fromUserId, String toUserId);

	/**
	 * Finds recommendations for a user based on the provided request parameters.
	 *
	 * @param userId The ID of the user for whom recommendations are to be found.
	 * @param request A map containing request parameters for finding recommendations.
	 * @return A list of maps, each representing a recommendation with relevant details.
	 */
	List<Map<String, String>> findRecommendationForUser(String userId, Map<String, Object> request);

	/**
	 * Finds recommendations for mentors based on the provided request parameters.
	 *
	 * @param userId The ID of the user for whom mentor recommendations are to be found.
	 * @param request A map containing request parameters for finding mentor recommendations.
	 * @return A list of maps, each representing a mentor recommendation with relevant details.
	 */
	List<Map<String, String>> findRecommendationForMentors(String userId, Map<String, Object> request);
}
