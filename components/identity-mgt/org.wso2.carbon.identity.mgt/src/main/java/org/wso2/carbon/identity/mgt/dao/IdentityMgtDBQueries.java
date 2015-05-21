package org.wso2.carbon.identity.mgt.dao;

/**
 * This class contains default SQL queries
 */

public class IdentityMgtDBQueries {

    public static final String CHECK_TENANT_ID_EXISTS =
            "SELECT TENANT_ID FROM IDN_TENANT_CONFIG WHERE TENANT_ID=?";

    // STORE Queries
    public static final String STORE_CONFIG_DATA =
            "INSERT "
                    + "INTO IDN_TENANT_CONFIG"
                    + "(TENANT_ID, CONFIG_OBJECT)"
                    + "VALUES (?,?)";

    // LOAD Queries

    public static final String LOAD_CONFIG_DATA =
            "SELECT " + "CONFIG_OBJECT "
                    + "FROM IDN_TENANT_CONFIG "
                    + "WHERE TENANT_ID = ? ";

    // DELETE queries

    public static final String DELETE_CONFIG_DATA =
            "DELETE " + "CONFIG_OBJECT "
                    + "FROM IDN_TENANT_CONFIG "
                    + "WHERE TENANT_ID = ? ";

    // UPDATE queries

    public static final String UPDATE_CONFIG_DATA =
            "UPDATE " + "IDN_TENANT_CONFIG "
                    + "SET CONFIG_OBJECT= ? "
                    + "WHERE TENANT_ID = ? ";
}
