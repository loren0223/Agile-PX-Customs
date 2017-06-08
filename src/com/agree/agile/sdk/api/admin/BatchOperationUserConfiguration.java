package com.agree.agile.sdk.api.admin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import com.agile.api.*;
import com.agree.agile.sdk.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

public class BatchOperationUserConfiguration {
	
	public static final String USERNAME     = UtilConfigReader.readProperty("USERNAME");
	public static final String PASSWORD     = UtilConfigReader.readProperty("PASSWORD");
	public static final String URL          = UtilConfigReader.readProperty("URL");
	public static final String DB_URL 		= UtilConfigReader.readProperty("DB_URL");
	public static final String DB_USER 		= UtilConfigReader.readProperty("DB_USER");
	public static final String DB_PASSWORD 	= UtilConfigReader.readProperty("DB_PASSWORD");
	public static IAgileSession session = null;
	public static AgileSessionFactory factory;
	
	public BatchOperationUserConfiguration() {}
	
	public static void main(String[] args) {
		Logger logger = Logger.getRootLogger();
		
	    try {
	    	session = connect(session);
	        
	    	//Query all users that need to configure.
	    	int language = UserConstants.ATT_PREFERENCES_LANGUAGE; 
	    	
	    	
	    	//Loop and configure.
	    	
	    	
	    } catch(APIException ex) {
	    	//ex.printStackTrace();
	    	logger.error("API Exception:" + ex.getMessage());
	    } catch(Exception ex) {
		    ex.printStackTrace();
		    logger.error("Exception:" + ex.getMessage());	
		} finally {
		    if(session!=null)
		    	session.close();
		}
	}
	
	/**
	 * <p> Create an IAgileSession instance </p>
	 *
	 * @param session
	 * @return IAgileSession
	 * @throws APIException
	 */
	private static IAgileSession connect(IAgileSession session)
									throws APIException {
		factory = AgileSessionFactory.getInstance(URL);
		HashMap params = new HashMap();
		params.put(AgileSessionFactory.USERNAME, USERNAME);
		params.put(AgileSessionFactory.PASSWORD, PASSWORD);
		session = factory.createSession(params);
		return session;
	}
	
	public static Connection getConnection() throws Exception {
		Connection conn = null;
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
		} catch (Exception e) {
			throw e;
		}
		return conn;
	}
}
