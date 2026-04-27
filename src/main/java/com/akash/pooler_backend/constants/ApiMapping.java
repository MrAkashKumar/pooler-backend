package com.akash.pooler_backend.constants;

/**
 * @author Akash Kumar
 */
public class ApiMapping {

    private ApiMapping(){
        throw new IllegalArgumentException("Illegal Argument Exception");
    }

    /**
     * REQUEST API
     */
    public static final String PUBLIC_API = "/api/v1/public";
    public static final String ADMIN_API = "/api/v1/admin";
    public static final String AUDIT_API = "/api/v1/audit";
    public static final String SESSIONS_API = "/api/v1/sessions";
    public static final String USERS_API = "/api/v1/users";

    /**
     * USER API
     */
    public static final String CREATE_USER = "/me";
    public static final String CHANGE_PASSWORD = "/me/change-password";
    public static final String GET_USER = "/user";
    public static final String GET_ALL_USER = "/user";

    /**
     * AUTHENTICATION API
     */
    public static final String CREATE_AUTH = "/authentication";
    public static final String LOGIN = "/login";
    public static final String LOGOUT = "/logout";

    /**
     * ADMIN API
     */
    public static final String ADMIN_LOGOUT = "/logout";

    /**
     * User Session
     */
    public static final String USER_SESSION_ID = SESSIONS_API + "/{sessionId}";
    public static final String USER_TOKEN_INFO = SESSIONS_API + "/token-info";

    /**
     * AUDIT API
     */
    public static final String AUDIT_ = "/logout";

    /**
     * HEALTH
     */
    public static final String HEALTH = "/health";
    public static final String VERSION = "/version";





}
