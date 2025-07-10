package org.sunbird.cb.hubservices.network.service;

import org.sunbird.cb.hubservices.model.ConnectionRequest;
import org.sunbird.cb.hubservices.model.SBApiResponse;

public interface UserConnectionService {

    /**
     * Block a user based on the connection request.
     *
     * @param authToken the authentication token of the user making the request
     * @param request   the connection request containing details of the user to be blocked
     * @return SBApiResponse containing the result of the block operation
     */
    SBApiResponse blockUser(String authToken, ConnectionRequest request);

}
