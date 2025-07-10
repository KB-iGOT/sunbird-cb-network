package org.sunbird.cb.hubservices.network.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cb.hubservices.common.auth.AccessTokenValidator;
import org.sunbird.cb.hubservices.common.util.ProjectUtil;
import org.sunbird.cb.hubservices.model.ConnectionRequest;
import org.sunbird.cb.hubservices.model.SBApiResponse;
import org.sunbird.cb.hubservices.serviceimpl.ConnectionService;
import org.sunbird.cb.hubservices.serviceimpl.ProfileService;
import org.sunbird.cb.hubservices.util.Constants;

import java.util.Date;

@Service
public class UserConnectionServiceImpl implements UserConnectionService {

    private Logger logger = LoggerFactory.getLogger(UserConnectionServiceImpl.class);

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    AccessTokenValidator accessTokenValidator;

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

}
