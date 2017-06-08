//: UtilConfigReader.java
package com.agree.agile.sdk.util;
import java.util.*;
/**
 * Copyright 2015 AdvancedTEK International Corporation, 8F, No.303, Sec. 1, 
 * Fusing S. Rd., Da-an District, Taipei City 106, Taiwan(R.O.C.); Telephone
 * +886-2-2708-5108, Facsimile +886-2-2754-4126, or <http://www.advtek.com.tw/>
 * All rights reserved.
 * @author Loren.Cheng
 * @version 0.1
 */
public class UtilConfigReader {
	private static final ResourceBundle config = ResourceBundle.getBundle("UtilConfig");
	/**
	 * 
	 * @param name
	 * @return
	 */
	public static String readProperty(String name) {
    	String value = "";
        value = config.getString(name);
    	return value;
    }
}
///:~