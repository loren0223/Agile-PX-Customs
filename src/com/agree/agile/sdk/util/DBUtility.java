//: DBUtility.java
package com.agree.agile.sdk.util;
import java.sql.*;
/**
 * Copyright 2015 AdvancedTEK International Corporation, 8F, No.303, Sec. 1, 
 * Fusing S. Rd., Da-an District, Taipei City 106, Taiwan(R.O.C.); Telephone
 * +886-2-2708-5108, Facsimile +886-2-2754-4126, or <http://www.advtek.com.tw/>
 * All rights reserved.
 * @author Loren.Cheng
 * @version 0.1
 */
public class DBUtility {
	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public static Connection getConnection() throws Exception {
		Connection conn = null;
		try {
			//Get DB connection properties
			String agileDBDriver = UtilConfigReader.readProperty("px_db_driver");
			String agileDBUrl = UtilConfigReader.readProperty("px_db_url");
			String agileDBUsername = UtilConfigReader.readProperty("px_db_username");
			String agileDBPassword = UtilConfigReader.readProperty("px_db_password");
			//Register JDBC driver
			Class.forName(agileDBDriver);
			//Get DB Connection
			conn = DriverManager.getConnection(agileDBUrl, agileDBUsername, agileDBPassword);
			conn.setAutoCommit(true);
		} catch(SQLException ex) {
			throw ex;
		} catch(Exception ex) {
			throw ex;
		}
	    return conn;
	}
	/**
	 * 
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public static Statement createStatement(Connection conn) throws Exception {
		Statement state = null;
		try {
			state = conn.createStatement();
		} catch(SQLException ex) {
			throw ex;
		} catch(Exception ex) {
			throw ex;
		}
    	return state;
	}
	/**
	 * 
	 * @param conn
	 * @param state
	 */
	public static void closeConnection(Connection conn, Statement state) {
		try {
			state.close();
			conn.close();
		} catch(SQLException ex) {
			ex.printStackTrace();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
}
///:~