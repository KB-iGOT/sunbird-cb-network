package org.sunbird.cb.hubservices.network.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cb.hubservices.cache.RedisCacheMgr;
import org.sunbird.cb.hubservices.common.auth.AccessTokenValidator;
import org.sunbird.cb.hubservices.common.util.ProjectUtil;
import org.sunbird.cb.hubservices.model.ConnectionRequest;
import org.sunbird.cb.hubservices.model.Response;
import org.sunbird.cb.hubservices.model.SBApiResponse;
import org.sunbird.cb.hubservices.serviceimpl.ConnectionService;
import org.sunbird.cb.hubservices.util.Constants;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class UserConnectionServiceImpl implements UserConnectionService {

    private Logger logger = LoggerFactory.getLogger(UserConnectionServiceImpl.class);

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    AccessTokenValidator accessTokenValidator;

    @Autowired
    RedisCacheMgr redisCacheMgr;

    @Value("${connection.allowed.transitions.pending:Approved,Rejected,Withdrawn}")
    private String pendingAllowedTransitions;

    @Value("${connection.allowed.transitions.approved:Removed,Blocked}")
    private String approvedAllowedTransitions;

    @Value("${connection.allowed.transitions.blocked:Unblocked}")
    private String blockedAllowedTransitions;

    private static final Set<String> VALID_UPDATE_STATUSES = new HashSet<>(Arrays.asList(
            Constants.Status.APPROVED,
            Constants.Status.REJECTED,
            Constants.Status.WITHDRAWN,
            Constants.Status.REMOVED,
            Constants.Status.BLOCKED,
            Constants.Status.UNBLOCKED
    ));

    private final Map<String, Set<String>> ALLOWED_TRANSITIONS = new HashMap<>();

    @Autowired
    private void initAllowedTransitions() {
        ALLOWED_TRANSITIONS.clear();
        ALLOWED_TRANSITIONS.put(Constants.Status.PENDING, parseAllowedTransitions(pendingAllowedTransitions));
        ALLOWED_TRANSITIONS.put(Constants.Status.APPROVED, parseAllowedTransitions(approvedAllowedTransitions));
        ALLOWED_TRANSITIONS.put(Constants.Status.BLOCKED, parseAllowedTransitions(blockedAllowedTransitions));
    }

    private Set<String> parseAllowedTransitions(String transitionsCsv) {
        Set<String> transitions = new HashSet<>();
        if (StringUtils.isEmpty(transitionsCsv)) {
            return transitions;
        }
        for (String transition : transitionsCsv.split(",")) {
            String normalized = StringUtils.capitalize(StringUtils.lowerCase(StringUtils.trim(transition)));
            if (StringUtils.isNotEmpty(normalized)) {
                transitions.add(normalized);
            }
        }
        return transitions;
    }

    /**
     * This method is used to block a user.
     *
     * @param authToken the authentication token of the user
     * @param request the connection request containing details of the user to be blocked
     * @return SBApiResponse containing the status and message of the operation
     */
    @Override
    public SBApiResponse blockUser(String authToken, ConnectionRequest request) {
        SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_BLOCK_USER);
        String userId = "";
        try {
            userId = accessTokenValidator.fetchUserIdFromAccessToken(authToken, response);
            if (StringUtils.isEmpty(userId)) {
                return response;
            }
            if (validateRequestBodyForBlockedUser(request, response)) {
                return response;
            }
            request.setStatus(Constants.Status.BLOCKED);
            request.setCreatedAt(new Date().toString());
            redisCacheMgr.deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + Constants.USER_COUNT + Constants.UNDER_SCORE + userId);
            return connectionService.blockUser(request,authToken);
        } catch (Exception e) {
            logger.error(String.format("UserConnectionServiceImpl : blockUser : Error while blocking the user %s %s", userId, e));
            response.getParams().setStatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
            response.getParams().setErrmsg("Error while blocking the user");
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        }
    }

    /**
     * Validates the request body for blocking a user.
     *
     * @param request the connection request containing details of the user to be blocked
     * @param response the response object to set error messages and status
     * @return true if validation fails, false otherwise
     */
    private boolean validateRequestBodyForBlockedUser(ConnectionRequest request, SBApiResponse response) {
        if (StringUtils.isEmpty(request.getConnectionId())) {
            response.getParams().setStatus(HttpStatus.BAD_REQUEST.toString());
            response.getParams().setErrmsg("Connection ID is required to block a user");
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return true;
        }
        if (StringUtils.isEmpty(request.getUserIdTo())) {
            response.getParams().setStatus(HttpStatus.BAD_REQUEST.toString());
            response.getParams().setErrmsg("User ID to block is required");
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return true;
        }
        if (StringUtils.isEmpty(request.getUserIdFrom())) {
            response.getParams().setStatus(HttpStatus.BAD_REQUEST.toString());
            response.getParams().setErrmsg("User ID from is required");
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return true;
        }
        if (request.getUserIdFrom().equals(request.getUserIdTo())) {
            response.getParams().setStatus(HttpStatus.BAD_REQUEST.toString());
            response.getParams().setErrmsg("User ID from and User ID to cannot be the same");
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return true;
        }
        return false;
    }
    @Override
    public Response updateUserConnection(ConnectionRequest request) {
        Response response = new Response();
        String fromUserId = request.getUserIdFrom();
        String toUserId = request.getUserIdTo();
        String status = request.getStatus();

        Map<String, String> currentRelationship =
                connectionService.getRelationshipBetweenUsers(fromUserId, toUserId);

        if (MapUtils.isEmpty(currentRelationship)
                || StringUtils.isEmpty(currentRelationship.get(Constants.Graph.STATUS.getValue()))) {
            logger.warn(Constants.Message.NO_CONNECTION_FOUND_LOG, fromUserId, toUserId);
            response.put(Constants.ResponseStatus.MESSAGE,
                    Constants.Message.NO_CONNECTION_FOUND);
            response.put(Constants.ResponseStatus.STATUS, HttpStatus.BAD_REQUEST);
            return response;
        }

        String currentStatus = currentRelationship.get(Constants.Graph.STATUS.getValue());
        String validationError = validateStatusTransition(currentStatus, status);
        if (StringUtils.isNotEmpty(validationError)) {
            logger.warn(Constants.Message.INVALID_TRANSITION_LOG,
                    currentStatus, status, fromUserId, toUserId);
            response.put(Constants.ResponseStatus.MESSAGE, validationError);
            response.put(Constants.ResponseStatus.STATUS, HttpStatus.BAD_REQUEST);
            return response;
        }
        request.setUpdatedAt(new Date().toString());
        response = connectionService.upsert(request, Constants.UPDATE_OPERATION);

        if (Constants.APPROVED.equalsIgnoreCase(status)) {
            redisCacheMgr.deleteKeysByName(RedisCacheMgr.APPROVED_OP_KEYS_TO_CLEAR, fromUserId, toUserId);
        } else if (Constants.REJECTED.equalsIgnoreCase(status)) {
            redisCacheMgr.deleteKeysByName(RedisCacheMgr.REJECTED_OP_KEYS_TO_CLEAR, fromUserId, toUserId);
        } else if (Constants.BLOCKED.equalsIgnoreCase(status)) {
            redisCacheMgr.deleteKeysByName(RedisCacheMgr.BLOCKED_OP_KEYS_TO_CLEAR, fromUserId, toUserId);
        } else if (Constants.WITHDRAWN.equalsIgnoreCase(status)) {
            redisCacheMgr.deleteKeysByName(RedisCacheMgr.WITHDRAWN_OP_KEYS_TO_CLEAR, fromUserId, toUserId);
        } else if (Constants.UNBLOCKED.equalsIgnoreCase(status)) {
            redisCacheMgr.deleteKeysByName(RedisCacheMgr.UNBLOCKED_OP_KEYS_TO_CLEAR, fromUserId);
        } else if (Constants.REMOVED.equalsIgnoreCase(status)) {
            redisCacheMgr.deleteKeysByName(RedisCacheMgr.REMOVED_OP_KEYS_TO_CLEAR, fromUserId, toUserId);
        }
        redisCacheMgr.deleteKeysByName(RedisCacheMgr.RECOMMENDED_USER_COUNT_KEYS, fromUserId, toUserId);
        return response;
    }

    private String validateStatusTransition(String currentStatus, String requestedStatus) {
        String normalizedCurrentStatus = StringUtils.capitalize(StringUtils.lowerCase(currentStatus));
        String normalizedRequestedStatus = StringUtils.capitalize(StringUtils.lowerCase(requestedStatus));

        if (!VALID_UPDATE_STATUSES.contains(normalizedRequestedStatus)) {
            return String.format("Invalid status '%s'", requestedStatus);
        }

        if (Constants.REJECTED.equalsIgnoreCase(normalizedCurrentStatus)
                && Constants.APPROVED.equalsIgnoreCase(normalizedRequestedStatus)) {
            return Constants.Message.REJECTED_REQUEST_CANNOT_BE_APPROVED;
        }

        Set<String> allowedNextStates = ALLOWED_TRANSITIONS.get(normalizedCurrentStatus);
        if (allowedNextStates == null
                || !allowedNextStates.contains(normalizedRequestedStatus)) {
            return String.format("State transition from '%s' to '%s' is not allowed", currentStatus, requestedStatus);
        }


        return null; // valid transition
    }

    @Override
    public Response addUserConnection(ConnectionRequest request, String addOperation) {
        request.setStatus(Constants.Status.PENDING);
        request.setCreatedAt(new Date().toString());
        Response response = connectionService.upsert(request, Constants.ADD_OPERATION);
        String fromUserId = request.getUserIdFrom();
        String toUserId = request.getUserIdTo();
        redisCacheMgr.deleteKeysByName(RedisCacheMgr.ADD_USER_OP_KEYS_TO_CLEAR, fromUserId, toUserId);
        return response;
    }
}
