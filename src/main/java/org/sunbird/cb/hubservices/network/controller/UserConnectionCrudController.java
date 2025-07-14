package org.sunbird.cb.hubservices.network.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.cb.hubservices.model.ConnectionRequest;
import org.sunbird.cb.hubservices.model.Response;
import org.sunbird.cb.hubservices.model.SBApiResponse;
import org.sunbird.cb.hubservices.network.service.UserConnectionService;
import org.sunbird.cb.hubservices.util.Constants;

@RestController
@RequestMapping(Constants.CONNECTIONS)
public class UserConnectionCrudController {

	@Autowired
	private UserConnectionService userConnectionService;

	@PostMapping(Constants.ADD)
	public ResponseEntity<Response> add(@RequestBody ConnectionRequest request) {
		Response response = userConnectionService.addUserConnection(request, Constants.ADD_OPERATION);
		return new ResponseEntity<>(response, (HttpStatus) response.get(Constants.STATUS));
	}

	@PostMapping(Constants.UPDATE)
	public ResponseEntity<Response> update(@RequestBody ConnectionRequest request) {
		Response response = userConnectionService.updateUserConnection(request);
		return new ResponseEntity<>(response, (HttpStatus) response.get(Constants.STATUS));
	}

	@PostMapping(Constants.BLOCK_USER)
	public ResponseEntity<SBApiResponse> block(
			@RequestHeader(value = Constants.X_AUTH_TOKEN, required = true) String authToken,
			@RequestBody ConnectionRequest request) {
		SBApiResponse response = userConnectionService.blockUser(authToken, request);
		return new ResponseEntity<>(response, response.getResponseCode());
	}
}