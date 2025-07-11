package org.sunbird.cb.hubservices.network.service;

import org.sunbird.cb.hubservices.model.ConnectionRequest;
import org.sunbird.cb.hubservices.model.Response;
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


    /**
     * Updates the user connection based on the provided connection request.
     *
     * @param request the connection request containing details to update the user connection
     * @return Response containing the result of the update operation
     */
    Response updateUserConnection(ConnectionRequest request);

    /**
     * Adds a user connection based on the provided connection request and operation type.
     *
     * @param request      the connection request containing details to add the user connection
     * @param addOperation the operation type for adding the connection (e.g., "add", "request")
     * @return Response containing the result of the add operation
     */
    Response addUserConnection(ConnectionRequest request, String addOperation);
}
