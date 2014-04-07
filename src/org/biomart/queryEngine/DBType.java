package org.biomart.queryEngine;

/**
 *
 * @author Syed Haider, Jonathan Guberman
 *
 * these are the query types for different DB platforms as well as web service
 * based querying. Used primarily by Query Engine.
 * All new DB platforms should be added here and the corresponding driver entry
 * should be added in QueryRunner
 */
public enum DBType {
    MYSQL,
    ORACLE,
    POSTGRES,
    MSSQL,
    DB2,
    WS
}
