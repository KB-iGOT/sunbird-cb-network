package org.sunbird.cb.hubservices.common.util;

import java.util.*;

import org.joda.time.DateTime;
import org.springframework.http.HttpStatus;
import org.sunbird.cb.hubservices.model.SBApiResponse;
import org.sunbird.cb.hubservices.model.SunbirdApiRespParam;
import org.sunbird.cb.hubservices.util.Constants;

/**
 * This class will contains all the common utility methods.
 *
 * @author Manzarul
 */
public class ProjectUtil {

    public static SBApiResponse createDefaultResponse(String api) {
        SBApiResponse response = new SBApiResponse();
        response.setId(api);
        response.setVer(Constants.API_VERSION_1);
        response.setParams(new SunbirdApiRespParam(UUID.randomUUID().toString()));
        response.getParams().setStatus(Constants.SUCCESS);
        response.setResponseCode(HttpStatus.OK);
        response.setTs(DateTime.now().toString());
        return response;
    }
}
