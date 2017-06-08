//: AgileSessionUtility.java
package com.agree.agile.sdk.util;
import java.math.BigDecimal;
import java.util.HashMap;

import com.agile.api.*;
/**
 * Copyright 2015 AdvancedTEK International Corporation, 8F, No.303, Sec. 1, 
 * Fusing S. Rd., Da-an District, Taipei City 106, Taiwan(R.O.C.); Telephone
 * +886-2-2708-5108, Facsimile +886-2-2754-4126, or <http://www.advtek.com.tw/>
 * All rights reserved.
 * @author Loren.Cheng
 * @version 0.1
 */
public class AgileSessionUtility {
	public static final String USERNAME         = UtilConfigReader.readProperty("USERNAME");
	public static final String PASSWORD         = UtilConfigReader.readProperty("PASSWORD");
	public static final String URL              = UtilConfigReader.readProperty("URL");
	
	/**
	 * <p> Create an IAgileSession instance </p>
	 *
	 * @param session
	 * @return IAgileSession
	 * @throws APIException
	 */
	public static IAgileSession getAdminSession() throws APIException {
		AgileSessionFactory factory = AgileSessionFactory.getInstance(URL);
		HashMap params = new HashMap();
		params.put(AgileSessionFactory.USERNAME, USERNAME);
		params.put(AgileSessionFactory.PASSWORD, PASSWORD);
		IAgileSession session = factory.createSession(params);
		return session;
	}
	/**
	 * 
	 * @return
	 * @throws APIException
	 */
	public static IAgileSession getAdminSessionCluster() throws APIException {
		AgileSessionFactory factory = AgileSessionFactory.refreshInstance(URL);
		HashMap params = new HashMap();
		params.put(AgileSessionFactory.USERNAME, USERNAME);
		params.put(AgileSessionFactory.PASSWORD, PASSWORD);
		IAgileSession session = factory.createSession(params);
		return session;
	}
}
///:~