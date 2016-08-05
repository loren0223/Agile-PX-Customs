package com.aic.agile.sdk.api.admin;

import java.sql.Connection;
import com.aic.agile.sdk.util.*;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import com.agile.api.*;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

public class UserPasswordRestter {
	
	public static final String USERNAME     = UtilConfigReader.readProperty("USERNAME");
	public static final String PASSWORD     = UtilConfigReader.readProperty("PASSWORD");
	public static final String URL          = UtilConfigReader.readProperty("URL");
	public static final String DB_URL 		= UtilConfigReader.readProperty("DB_URL");
	public static final String DB_USER 		= UtilConfigReader.readProperty("DB_USER");
	public static final String DB_PASSWORD 	= UtilConfigReader.readProperty("DB_PASSWORD");
	public static IAgileSession session = null;
	public static AgileSessionFactory factory;
	
	public UserPasswordRestter() {}
	
	public static void main(String[] args) {
		Logger logger = Logger.getRootLogger();
		//DB Connection
	    Connection conn = null;
	    PreparedStatement queryStat = null, updateStat = null, stat = null;
	    ResultSet rs = null;
	    
	    try {
	    	//取得Agile DB connection
		    conn = getConnection();
	    	//取得Agile 'admin' session 
	        session = connect(session);
	        //取得所有LDAP user_id list
		    String loginid = null;
		    queryStat = conn.prepareStatement(
		    		"SELECT loginid FROM agileuser WHERE comments LIKE ? "+
		    		"OR loginid NOT IN ('admin','ifsuser','superadmin','agileuser') ");
		    queryStat.setString(1, "User added from LDAP"+"%");
		    rs = queryStat.executeQuery();
		    //把所有result loop 出來
    		while(rs.next()) {  
    			loginid = rs.getString("loginid");
    			//取得User object
    	        IUser user = (IUser)session.getObject(UserConstants.CLASS_USER, loginid);
    	        //重新設定User Password = agile9
    	        user.changeLoginPassword(null, "agile9");
    		}
    		rs.close();
    		queryStat.close();
    		conn.close();
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
