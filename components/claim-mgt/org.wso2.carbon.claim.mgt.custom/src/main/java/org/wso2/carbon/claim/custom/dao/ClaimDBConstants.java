/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.claim.custom.dao;


public class ClaimDBConstants {

    public static final String ADD_DIALECT_SQL = "INSERT INTO UM_DIALECT (UM_DIALECT_URI, " +
            "UM_TENANT_ID) VALUES (?, ?)";
    public static final String ADD_CLAIM_SQL = "INSERT INTO UM_CLAIM(UM_DIALECT_ID, UM_CLAIM_URI, " +
            "UM_TENANT_ID) VALUES (?,?,?)";

    public static final String DELETE_CLAIM_SQL = "DELETE FROM UM_CLAIM WHERE UM_CLAIM_URI=? AND " +
            "UM_DIALECT_ID=(SELECT UM_ID FROM UM_DIALECT WHERE UM_DIALECT_URI=? " +
            "AND UM_TENANT_ID=?) AND UM_TENANT_ID=?";

    public static final String DELETE_CLAIM_RELATION_SQL = "DELETE FROM UM_CLAIM_MAPPING WHERE " +
            "UM_LOCAL_CLAIM_ID=? OR UM_ADDITIONAL_CLAIM_ID=? AND UM_TENANT_ID=?";

    public static final String DELETE_CLAIM_META_DATA_SQL = "DELETE FROM UM_CLAIM_META_DATA WHERE " +
            "UM_CLAIM_ID=? AND UM_TENANT_ID=?";

    public static final String DELETE_MAPPED_ATTRIBUTE_SQL = "DELETE FROM UM_CLAIM_MAPPED_ATTRIBUTE WHERE " +
            "UM_CLAIM_ID=? AND " +
            "UM_TENANT_ID=?";

    public static final String GET_ALL_DIALECT_SQL = "SELECT * FROM UM_DIALECT";

    public static final String GET_ALL_CLAIM_META_DATA_SQL = "SELECT * FROM UM_CLAIM_META_DATA";

    public static final String ADD_CLAIM_MAPPED_ATTRIBUTE_SQL = "INSERT INTO UM_CLAIM_MAPPED_ATTRIBUTE(UM_CLAIM_ID, " +
            "UM_MAPPED_ATTRIBUTE_DOMAIN, UM_MAPPED_ATTRIBUTE,UM_TENANT_ID) VALUES (?,?,?,?)";

    public static final String ADD_CLAIM_RELATION_SQL = "INSERT INTO UM_CLAIM_MAPPING(UM_LOCAL_CLAIM_ID, " +
            "UM_ADDITIONAL_CLAIM_ID," +
            "UM_TENANT_ID) VALUES (?,?,?)";

    public static final String ADD_CLAIM_META_DATA_SQL = "INSERT INTO UM_CLAIM_META_DATA(UM_CLAIM_ID, " +
            "UM_KEY,UM_VALUE,UM_TENANT_ID) VALUES (?,?,?,?)";


    public static final String GET_ALL_CLAIMS_SQL = "(SELECT ABC.UM_DIALECT_URI AS UM_DIALECT_URI, ABC.UM_CLAIM_URI, " +
            "ABC.UM_CLAIM_ID, M.UM_MAPPED_ATTRIBUTE, M.UM_MAPPED_ATTRIBUTE_DOMAIN " +
            "FROM (SELECT D.UM_DIALECT_URI AS UM_DIALECT_URI, C.UM_CLAIM_URI AS UM_CLAIM_URI, " +
            "C.UM_ID AS UM_CLAIM_ID " +
            "FROM UM_DIALECT D, UM_CLAIM C WHERE D.UM_TENANT_ID =? AND C.UM_TENANT_ID=? " +
            "AND D.UM_ID=C.UM_DIALECT_ID) ABC INNER JOIN UM_CLAIM_MAPPED_ATTRIBUTE M ON ABC.UM_CLAIM_ID=M" +
            ".UM_CLAIM_ID) UNION " +
            "(SELECT TEMP.UM_DIALECT_URI, TEMP.UM_CLAIM_URI, TEMP.UM_CLAIM_ID, CL.UM_CLAIM_URI, TEMP" +
            ".UM_MAPPED_ATTRIBUTE_DOMAIN " +
            "FROM (SELECT XYZ.UM_DIALECT_URI AS UM_DIALECT_URI, XYZ.UM_CLAIM_URI, XYZ.UM_CLAIM_ID AS UM_CLAIM_ID, M" +
            ".UM_MAPPED_ATTRIBUTE, " +
            "M.UM_MAPPED_ATTRIBUTE_DOMAIN FROM (SELECT D.UM_DIALECT_URI AS UM_DIALECT_URI, C.UM_CLAIM_URI AS " +
            "UM_CLAIM_URI, C.UM_ID AS UM_CLAIM_ID," +
            "CM.UM_LOCAL_CLAIM_ID AS UM_LOCAL_CLAIM_ID FROM UM_DIALECT D,UM_CLAIM C, UM_CLAIM_MAPPING CM WHERE D" +
            ".UM_ID=C.UM_DIALECT_ID AND D.UM_TENANT_ID =? AND" +
            " C.UM_TENANT_ID=? AND C.UM_ID = CM.UM_ADDITIONAL_CLAIM_ID) XYZ INNER JOIN UM_CLAIM_MAPPED_ATTRIBUTE M ON" +
            " XYZ.UM_LOCAL_CLAIM_ID=M.UM_CLAIM_ID) TEMP, UM_CLAIM_MAPPING CM," +
            " UM_CLAIM CL WHERE TEMP.UM_CLAIM_ID = CM.UM_ADDITIONAL_CLAIM_ID AND CM.UM_LOCAL_CLAIM_ID = CL.UM_ID)";


    public static final String GET_CLAIM_ID_SQL = "SELECT UM_ID FROM UM_CLAIM WHERE " +
            "UM_CLAIM_URI=? AND UM_TENANT_ID=?";

    public static final String GET_META_DATA_FOR_CLIAM_SQL = "SELECT * FROM UM_CLAIM_META_DATA WHERE " +
            "UM_CLAIM_ID=?";


    public static final String GET_CLAIMS_FOR_DIALECTT_SQL = "SELECT UM_CLAIM_URI FROM " +
            "UM_CLAIM, UM_DIALECT WHERE UM_CLAIM.UM_DIALECT_ID=UM_DIALECT.UM_ID AND UM_DIALECT_URI=? " +
            "AND UM_CLAIM.UM_TENANT_ID=? AND UM_DIALECT.UM_TENANT_ID=?";

    public static final String GET_DIALECT_ID_SQL = "SELECT UM_ID FROM UM_DIALECT WHERE " +
            "UM_DIALECT_URI=? AND UM_TENANT_ID=?";
    public static final String COUNT_DIALECTS = "SELECT COUNT(UM_ID) FROM UM_DIALECT WHERE " +
            "UM_TENANT_ID=?";


    public static final String DELETE_DIALECT = "DELETE FROM UM_DIALECT WHERE UM_DIALECT_URI=?";

    private ClaimDBConstants() {
    }
}
