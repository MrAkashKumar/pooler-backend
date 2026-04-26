package com.akash.pooler_backend.constants;

/**
 * @author Akash Kumar
 */
public class ApiMapping {

    private ApiMapping(){
        throw new IllegalArgumentException("Illegal Argument Exception");
    }

    /**
     * USER API
     */
    public static final String CREATE_USER = "/user";
    public static final String UPDATE_USER = "/user";
    public static final String GET_USER = "/user";
    public static final String GET_ALL_USER = "/user";

    /**
     * AUTHENTICATION API
     */
    public static final String CREATE_AUTH = "/authentication";
    public static final String LOGIN = "/login";
    public static final String LOGOUT = "/logout";

}
