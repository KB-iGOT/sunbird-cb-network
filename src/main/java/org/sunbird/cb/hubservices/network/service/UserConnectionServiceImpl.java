package org.sunbird.cb.hubservices.network.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

    /**
     * CWE-840 – Business Logic Errors
     * Defines which target statuses are considered known/valid for an update request.
     */
    private static final Set<String> VALID_UPDATE_STATUSES = new HashSet<>(Arrays.asList(
            Constants.Status.APPROVED,
            Constants.Status.REJECTED,
            Constants.Status.WITHDRAWN,
            Constants.Status.REMOVED,
            Constants.Status.BLOCKED,
            Constants.Status.UNBLOCKED
    ));

    /**
     * CWE-840 – Business Logic Errors
     * Strict server-side state-machine: maps each current (persisted) status to the
     * set of statuses that are permitted as the next state.
     *
     * Allowed transitions:
     *   PENDING   → APPROVED  (recipient accepts)
     *   PENDING   → REJECTED  (recipient rejects)
     *   PENDING   → WITHDRAWN (sender withdraws before approval)
     *   APPROVED  → REMOVED   (either party removes the connection)
     *   APPROVED  → BLOCKED   (either party blocks the other)
     *   BLOCKED   → UNBLOCKED (blocker lifts the block)
     *
     * Terminal states with no outgoing transitions: REJECTED, WITHDRAWN, REMOVED, UNBLOCKED.
     */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = new HashMap<>();
    static {
        ALLOWED_TRANSITIONS.put(Constants.Status.PENDING, new HashSet<>(Arrays.asList(
                Constants.Status.APPROVED,
                Constants.Status.REJECTED,
                Constants.Status.WITHDRAWN
        )));
        ALLOWED_TRANSITIONS.put(Constants.Status.APPROVED, new HashSet<>(Arrays.asList(
                Constants.Status.REMOVED,
                Constants.Status.BLOCKED
        )));
        ALLOWED_TRANSITIONS.put(Constants.Status.BLOCKED, new HashSet<>(Arrays.asList(
                Constants.Status.UNBLOCKED
        )));
    }

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    AccessTokenValidator accessTokenValidator;

    @Autowired
    RedisCacheMgr redisCacheMgr;

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


    public Response updateUserConnection(ConnectionRequest request) {
        Response response = new Response();
        String fromUserId = request.getUserIdFrom();
        String toUserId = request.getUserIdTo();
        String requestedStatus = request.getStatus();

        // ── CWE-840: Enforce strict server-side state validation ──────────────────

        // 1. Reject requests with missing mandatory fields
        if (StringUtils.isAnyEmpty(fromUserId, toUserId, requestedStatus)) {
            logger.warn("updateUserConnection: Rejected – missing required field(s). "
                    + "fromUserId={} toUserId={} status={}", fromUserId, toUserId, requestedStatus);
            response.put(Constants.ResponseStatus.MESSAGE,
                    "userIdFrom, userIdTo and status are required fields.");
            response.put(Constants.ResponseStatus.STATUS, HttpStatus.BAD_REQUEST);
            return response;
        }

        // 2. Reject unknown / arbitrary status values (prevents injection of invented states)
        if (!VALID_UPDATE_STATUSES.contains(requestedStatus)) {
            logger.warn("updateUserConnection: Rejected – unrecognised status '{}' supplied by fromUserId={}",
                    requestedStatus, fromUserId);
            response.put(Constants.ResponseStatus.MESSAGE,
                    "Invalid status value: '" + requestedStatus + "'.");
            response.put(Constants.ResponseStatus.STATUS, HttpStatus.BAD_REQUEST);
            return response;
        }

        // 3. Re-read the current persisted state from the graph before approving any action
        Map<String, String> currentRelationship =
                connectionService.getRelationshipBetweenUsers(fromUserId, toUserId);
        String currentStatus = (currentRelationship != null)
                ? currentRelationship.get(Constants.Graph.STATUS.getValue())
                : null;

        if (StringUtils.isEmpty(currentStatus)) {
            logger.warn("updateUserConnection: Rejected – no existing connection found between "
                    + "fromUserId={} and toUserId={}", fromUserId, toUserId);
            response.put(Constants.ResponseStatus.MESSAGE,
                    "No existing connection found between the specified users.");
            response.put(Constants.ResponseStatus.STATUS, HttpStatus.BAD_REQUEST);
            return response;
        }

        // 4. Validate the transition against the allowed state-machine
        Set<String> allowedNextStates = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowedNextStates == null || !allowedNextStates.contains(requestedStatus)) {
            logger.warn("updateUserConnection: Rejected – illegal state transition '{}' → '{}' "
                            + "for fromUserId={} toUserId={}",
                    currentStatus, requestedStatus, fromUserId, toUserId);
            response.put(Constants.ResponseStatus.MESSAGE,
                    String.format("State transition from '%s' to '%s' is not permitted.",
                            currentStatus, requestedStatus));
            response.put(Constants.ResponseStatus.STATUS, HttpStatus.BAD_REQUEST);
            return response;
        }

        // 5. Log every approved state-change for audit / monitoring purposes
        logger.info("updateUserConnection: State transition approved – '{}' → '{}' "
                        + "for fromUserId={} toUserId={}",
                currentStatus, requestedStatus, fromUserId, toUserId);

        // ── Original logic (unchanged) ────────────────────────────────────────────
        request.setUpdatedAt(new Date().toString());
        response = connectionService.upsert(request, Constants.UPDATE_OPERATION);

        if (Constants.APPROVED.equalsIgnoreCase(requestedStatus)) {
            redisCacheMgr.deleteKeysByName(RedisCacheMgr.APPROVED_OP_KEYS_TO_CLEAR, fromUserId, toUserId);
        } else if (Constants.REJECTED.equalsIgnoreCase(requestedStatus)) {
            redisCacheMgr.deleteKeysByName(RedisCacheMgr.REJECTED_OP_KEYS_TO_CLEAR, fromUserId, toUserId);
        } else if (Constants.BLOCKED.equalsIgnoreCase(requestedStatus)) {
            redisCacheMgr.deleteKeysByName(RedisCacheMgr.BLOCKED_OP_KEYS_TO_CLEAR, fromUserId, toUserId);
        } else if (Constants.WITHDRAWN.equalsIgnoreCase(requestedStatus)) {
            redisCacheMgr.deleteKeysByName(RedisCacheMgr.WITHDRAWN_OP_KEYS_TO_CLEAR, fromUserId, toUserId);
        } else if (Constants.UNBLOCKED.equalsIgnoreCase(requestedStatus)) {
            redisCacheMgr.deleteKeysByName(RedisCacheMgr.UNBLOCKED_OP_KEYS_TO_CLEAR, fromUserId);
        } else if (Constants.REMOVED.equalsIgnoreCase(requestedStatus)) {
            redisCacheMgr.deleteKeysByName(RedisCacheMgr.REMOVED_OP_KEYS_TO_CLEAR, fromUserId, toUserId);
        }
        redisCacheMgr.deleteKeysByName(RedisCacheMgr.RECOMMENDED_USER_COUNT_KEYS, fromUserId, toUserId);
        return response;
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
