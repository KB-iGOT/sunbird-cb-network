package org.sunbird.cb.hubservices.util;

public class Constants {

	public static final String TO_VALUE = "toValue";
	public static final String FIELD_KEY = "fieldKey";
	public static final String OFFSET = "offset";
	public static final String LIMIT = "limit";
	public static final String USER_ID = "userId";
	public static final String ACCEPT = "Accept";
	public static final String RESPONSE = "response";
	public static final String OSID = "osid";
	public static final String PROFILE_DETAILS_PROFESSIOANAL_DETAILS = "profileDetails.professionalDetails";
	public static final String PROFILE_DETAILS_EMPLOYMENT_DETAILS = "profileDetails.employmentDetails";
	public static final String PROFILE_DETAILS_PERSONAL_DETAILS = "profileDetails.personalDetails";
	public static final String STATUS = "status";
	public static final String CONNECTIONS = "/connections";
	public static final String CONNECTIONS_PROFILE = "/connections/profile";
	public static final String FIND_RECOMMENDED = "/find/recommended";
	public static final String FIND_SUGGESTS = "/find/suggests";
	public static final String FETCH_REQUESTED = "/fetch/requested";
	public static final String FETCH_REQUESTS_RECEIVED = "/fetch/requests/received";
	public static final String FETCH_ESTABLISHED = "/fetch/established";
	public static final String ADD = "/add";
	public static final String UPDATE = "/update";
	public static final String PAGE_SIZE = "pageSize";
	public static final String PAGE_NO = "pageNo";
	public static final String ADD_OPERATION = "Add";
	public static final String UPDATE_OPERATION = "Update";
	public static final String FAILED = "Failed";
	public static final String FROM_UUID = "fromUUID";
	public static final String TO_UUID = "toUUID";
	public static final String UUID = "UUID";
	public static final String PROPS = "props";
	public static final String VERIFIED_KARMAYOGI = "verifiedKarmayogi";
	public static final String PROFILE_DETAILS_VERIFIED_KARMAYOGI = "profileDetails.verifiedKarmayogi";
	public static final String REDIS_COMMON_KEY = "NETWORK_";
	public static final String QUESTION_ID = "qs_id_";
	public static final String API_REDIS_DELETE = "api.redis.delete";
	public static final String API_REDIS_GET_KEYS = "api.redis.get.keys";
	public static final String API_REDIS_GET_KEYS_VALUE_SET = "api.redis.get.keys&values";
	public static final String SUCCESSFUL = "Successful";
	public static final String UNDER_SCORE = "_";
	public static final String USER_LIST = "userList";
	public static final String KEYSPACE_SUNBIRD = "sunbird";
	public static final String CORE_CONNECTIONS_PER_HOST_FOR_LOCAL = "coreConnectionsPerHostForLocal";
	public static final String CORE_CONNECTIONS_PER_HOST_FOR_REMOTE = "coreConnectionsPerHostForRemote";
	public static final String MAX_CONNECTIONS_PER_HOST_FOR_LOCAl = "maxConnectionsPerHostForLocal";
	public static final String MAX_CONNECTIONS_PER_HOST_FOR_REMOTE = "maxConnectionsPerHostForRemote";
	public static final String MAX_REQUEST_PER_CONNECTION = "maxRequestsPerConnection";
	public static final String HEARTBEAT_INTERVAL = "heartbeatIntervalSeconds";
	public static final String POOL_TIMEOUT = "poolTimeoutMillis";
	public static final String CASSANDRA_CONFIG_HOST = "cassandra.config.host";
	public static final String SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL = "sunbird_cassandra_consistency_level";
	public static final String INSERT_INTO = "INSERT INTO ";
	public static final String DOT = ".";
	public static final String OPEN_BRACE = "(";
	public static final String VALUES_WITH_BRACE = ") VALUES (";
	public static final String QUE_MARK = "?";
	public static final String COMMA = ",";
	public static final String CLOSING_BRACE = ");";
	public static final String EXCEPTION_MSG_FETCH = "Exception occurred while fetching record from ";
	public static final String ID = "id";
	public static final String FIRST_NAME = "firstname";
	public static final String TABLE_USER = "user";
	public static final String CHANNEL = "channel";
	public static final String FULL_NAME = "fullName";
	public static final String FROM_USER = "fromUser";
	public static final String TO_USER = "toUser";
	public static final String DOT_SEPARATOR = ".";
	public static final String SHA_256_WITH_RSA = "SHA256withRSA";
	public static final String ACCESS_TOKEN_PUBLICKEY_BASEPATH = "accesstoken.publickey.basepath";
	public static final String _UNAUTHORIZED = "Unauthorized";
	public static final String SUB = "sub";
	public static final String SSO_REALM = "sso.realm";
	public static final String SSO_URL = "sso.url";
	public static final String ACCESS_TOKEN_IS_EXPIRED = "Access token is expired";
	public static final String ACCESS_TOKEN_VALIDATION_FAILED = "Access token validation is failed";
	public static final String X_AUTH_TOKEN = "x-authenticated-user-token";
	public static final String USER_ID_RQST = "userId";
	public static final String API_VERSION_1 = "1.0";
	public static final String SUCCESS = "success";
	public static final String API_USER_RELATIONSHIP = "api.user.relationship";
	public static final String OK = "OK";
	public static final String SUB_CATEGORY = "subCategory";
	public static final String SUB_TYPE = "subType";
	public static final String USER_IDS = "userIds";
	public static final String MESSAGE = "message";
	public static final String USER_NAME = "userName";
	public static final String PLACE_HOLDERS ="placeholders";
	public static final String DATA = "data";
	public static final String USER_PREFIX = "user:" ;
	public static final String FIRST_NAME_KEY = "first_name";
	public static final String USER_ID_KEY = "user_id";
	public static final String SEND_CONNECTION_REQUEST = "SEND_CONNECTION_REQUEST";
	public static final String ACCEPTED_CONNECTION_REQUEST = "ACCEPTED_CONNECTION_REQUEST";
	public static final String REJECTED_CONNECTION_REQUEST = "REJECTED_CONNECTION_REQUEST";
	public static final String ALERT = "ALERT";
	public static final String FIND_RECOMMENDED_V2 = "/v2/find/recommended";
	public static final String ORGANISATION_ID = "organisationId";
	public static final String DESIGNATION = "designation";
	public static final String SIZE = "size";
	public static final String API_GET_USER_RECOMMENDATIONS_V2 = "api.user.recommendations.v2";
	public static final String FIND_RECOMMENDED_MENTOR = "/find/recommended/mentors";
	public static final String API_USER_MENTOR_RECOMMENDATIONS = "api.user.get.mentor.recommendations";
	public static final String RECOMMENDED_USERS = "recommendedUsers";
	public static final String PROFILE_DETAILS_PROFILE_IMAGE_URL = "profileDetails.profileImageUrl";
	public static final String ORGANISATIONS = "organisations";
	public static final String PROFILE_IMAGE_URL = "profileImageUrl";
	public static final String MENTORS = "mentors";
	public static final String USERS = "users";
	public static final String FETCH_BLOCKED = "/fetch/blocked";
	public static final String API_GET_BLOCKED_USERS= "api.user.blocked";
	public static final String BLOCKED_USERS= "blockedUsers";
	public static final String COUNT = "count";
	public static final String CONNECTION_REQUESTED = "connectionRequested";
	public static final String CONNECTION_RECIEVED = "connectionRecieved";
	public static final String CONNECTION_ESTABLISHED = "connectionEstablished" ;
	public static final String ROLE = "role" ;
	public static final String QUERY = "query" ;
	public static final String FILTERS = "filters";
	public static final String FIELDS = "fields";
	public static final String RESULT = "result";
	public static final String CONTENT = "content";
	public static final String ROOT_ORG_ID ="rootOrgId" ;
	public static final String PROFILE_DETAILS_PROFILE_BANNER_IMAGE_URL = "profileDetails.profileBannerUrl";
	public static final String NEO4J_AUTH_ENABLED = "neo4j.auth.enable";
	public static final String NEO4J_HOST_URL = "neo4j.url";
	public static final String NEO4J_USER_NAME = "neo4j.username";
	public static final String NEO4J_PASSWORD = "neo4j.password";
	public static final String NEO$J_TIMEOUT = "neo.timeout";
	public static final String TOTAL_COUNT = "totalCount";
	public static final String PROFILE_DETAILS = "profiledetails";
	public static final String PROFESSIONAL_DETAILS = "professionalDetails";
	public static final String EMPLOYMENT_DETAILS = "employmentDetails";
	public static final String PROFILE_BANNER_URL = "profileBannerUrl";
	public static final String FIND_TOTAL_CONNECTIONS_COUNT_BY_STATUS = "/user/v1/network/connections/list";
	public static final String REQUEST = "request";
	public static final String STATUS_VALUE = "statusValue";
	public static final String NAME = "name";
	public static final String VALUES = "values";
	public static final String FACETS = "facets";
	public static final String API_GET_TOTAL_CONNECTIONS_COUNT_BY_STATUS = "api.user.get.total.connections.count.by.status";
	public static final String FILTER = "filter";
	public static final String BLOCK_USER = "/block";
	public static final String API_BLOCK_USER = "api.block.user";
	public static final String FETCH_RESULT_CONSTANT = ".fetchResult:";
	public static final String URI_CONSTANT = "URI: ";
	public static final String ROLES = "roles";
	public static final String PROFILE_DETAILS_KEY = "profileDetails";
	public static final String APPROVED = "Approved";
	public static final String REJECTED = "Rejected";
	public static final String BLOCKED = "Blocked";
	public static final String WITHDRAWN ="Withdrawn" ;
	public static final String UNBLOCKED = "Unblocked";
	public static final String API_ONBOARD_NETWORK_HUB_USER = "api.onboard.network.hub.user";
	public static final String USER = "user";
	public static final String PERSONAL_DETAILS = "personalDetails";
	public static final String MOBILE = "mobile";
	public static final String PRIMARY_EMAIL = "primaryEmail";
	public static final String USER_ROLES = "user_roles";
	public static final String USERID_KEY = "userid";
	public static final String SCOPE = "scope";
	public static final String USER_ONBOARDED_NETWORK_HUB ="User onboarded successfully in network hub" ;
	public static final String CREATED_AT = "createdAt" ;
	public static final String UPDATED_AT = "updatedAt" ;
	public static final String REMOVED = "Removed";

	public enum Graph {
		ID("userId"), STATUS(ResponseStatus.STATUS), UUID("UUID"), PROPS("props"), CREATED_AT("createdAt"), CONNECTION_ID("connectionId"),
		UPDATED_AT("updatedAt"), USER_ID("userId");

		private String value;

		private Graph(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}
	}

	private static final String UTIL_CLASS = "Utility class";

	public enum DIRECTION {
		IN, OUT
	}

	public static class Status {
		private Status() {
			throw new IllegalStateException(UTIL_CLASS);
		}

		public static final String APPROVED = "Approved";
		public static final String REJECTED = "Rejected";
		public static final String PENDING = "Pending";
		public static final String DELETED = "Deleted";
		public static final String BLOCKED = "Blocked";
	}

	public static class Message {
		private Message() {
			throw new IllegalStateException(UTIL_CLASS);
		}

		public static final String CONNECTION_EXCEPTION_OCCURED = "Connection exception occurred: {}";
		public static final String FAILED_CONNECTION = "Failed user connections: ";
		public static final String USER_ID_INVALID = "user_id cant be null or empty";
		public static final String ROOT_ORG_INVALID = "rootOrg cant be null or empty";
		public static final String SENT_NOTIFICATION_ERROR = "Notification event send error occurred: {}";
		public static final String SENT_NOTIFICATION_SUCCESS = "Notification event send : {}";
	}

	public static class ResponseStatus {
		private ResponseStatus() {
			throw new IllegalStateException(UTIL_CLASS);
		}

		public static final String SUCCESSFUL = "Successful";
		public static final String FAILED = "Failed";
		public static final String MESSAGE = "message";
		public static final String DATA = "data";
		public static final String STATUS = "status";
		public static final String PAGENO = "pageNo";
		public static final String HASPAGENEXT = "hasNextPage";
		public static final String TOTALHIT = "totalHit";
	}

	public static class Parmeters {
		private Parmeters() {
			throw new IllegalStateException(UTIL_CLASS);
		}

		public static final String ROOT_ORG = "rootOrg";

	}

	public static class Profile {
		private Profile() {
			throw new IllegalStateException(UTIL_CLASS);
		}

		public static final String FIRST_NAME = "firstname";
		public static final String PERSONAL_DETAILS = "personalDetails";
		public static final String HUB_MEMBER = "Hub member";

	}

}
