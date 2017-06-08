package com.agree.agile.sdk.api.admin;
import java.util.*;
import com.agile.api.*;
import com.agree.agile.sdk.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GetPartsSubclassID {
	//靜態變數宣告
	public static final String USERNAME = UtilConfigReader.readProperty("USERNAME");
	public static final String PASSWORD = UtilConfigReader.readProperty("PASSWORD");
	public static final String URL = UtilConfigReader.readProperty("URL");
	public static final String XLS_IMPORT = UtilConfigReader.readProperty("DATASHEET");
	public static final String SUBCLASS_SHEET = UtilConfigReader.readProperty("SUBCLASS_SHEET");
	public static final String PAGE_TWO_SHEET = UtilConfigReader.readProperty("PAGE_TWO_SHEET");
	public static IAgileSession session = null;
	public static AgileSessionFactory factory;
	
	public GetPartsSubclassID() { }
	
	public static void main(String[] args) {
		//宣告Log4j Logger
		Logger logger = Logger.getRootLogger();
	    
	    //DB Connection
	    Connection conn = null;
	    Statement queryStat = null, updateStat = null, stat = null;
	    ResultSet rs = null;
	    
	    try {
		    //Get DB Connection
		    conn = SetupPartsSubclassPageTwoVisibility.getConnection();
			queryStat = conn.createStatement();
			rs = queryStat.executeQuery(
		    	"select description, id from nodetable where parentid = '10004' order by description ");
		    while(rs.next()) { //把所有result loop 出來 
		    	String description = rs.getString("description");
		    	String id = rs.getString("id");
		    	System.out.println(id + "," + description);
		    }
		    rs.close();
		} catch(Exception e1) {
			e1.printStackTrace();
			logger.error("Error: " + e1.getMessage());
	    } finally {
			try{
				if(rs!=null)
					rs.close();
				if(queryStat!=null)
					queryStat.close();
				if(updateStat!=null)
					updateStat.close();
				if(conn!=null)
					conn.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
			rs = null;
			queryStat =null;
			updateStat = null;
			conn = null;
		}
    }

}
