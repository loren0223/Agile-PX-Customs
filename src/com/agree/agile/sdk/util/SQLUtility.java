//: SQLUtility.java
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
public class SQLUtility {
	/**
	 * 
	 * @param state
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public static ResultSet executeQuery(Statement state, String sql) throws SQLException {
		try {
			ResultSet rs = state.executeQuery(sql);
			return rs;
		} catch(SQLException ex) {
			throw ex;
		}
	}
	/**
	 * 
	 * @param state
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public static int executeUpdate(Statement state, String sql) throws SQLException {
		int rs = 0;
		try {
			rs = state.executeUpdate(sql);
			return rs;
		} catch(SQLException se) {
			throw se;
		}
	}
	
}
///:~