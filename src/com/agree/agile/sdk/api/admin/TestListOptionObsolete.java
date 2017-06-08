package com.agree.agile.sdk.api.admin;

import java.util.*;

import com.agile.api.*;
import com.agree.agile.sdk.util.*;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.*;
import org.apache.poi.ss.usermodel.Row;

import java.io.*;

import java.io.FileInputStream;
import org.apache.log4j.*;

public class TestListOptionObsolete {

	public static final String USERNAME         = UtilConfigReader.readProperty("USERNAME");
	public static final String PASSWORD         = UtilConfigReader.readProperty("PASSWORD");
	public static final String URL              = UtilConfigReader.readProperty("URL");
	public static String XLS_IMPORT             = UtilConfigReader.readProperty("DATASHEET");
	public static final String S_LIST_SHEET     = UtilConfigReader.readProperty("S_LIST_SHEET");
	public static final String C_LIST_SHEET     = UtilConfigReader.readProperty("C_LIST_SHEET");
	public static IAgileSession session = null;
	public static AgileSessionFactory factory;
	
	public static Logger logger = Logger.getRootLogger();
	public static SimpleLayout layout = new SimpleLayout();
	
	public TestListOptionObsolete() { }
	
	public static void main(String args[]) {
	    try {
	    	logger.info("**************START**************");
        	
        	// Create an IAgileSession instance
            session = connect(session);
            // Get Admin instance
		    IAdmin admin = session.getAdminInstance();
		    // Get Agile List Library
		    IListLibrary lib = admin.getListLibrary();
		    //Check if List exists
    		IAdminList adminList = lib.getAdminList("TEST_OBS");
    		//Get value object
    		IAgileList list = adminList.getValues();
    		Collection listChildren = list.getChildNodes();
    		//If has list children
    		if(listChildren != null && listChildren.size() > 0)
    		{
    			Iterator it = listChildren.iterator();
    			while(it.hasNext()){
    				IAgileList listChildOpn = (IAgileList)it.next();
    				String listChildOpnName = listChildOpn.getValue().toString();
	            	String listChildOpnApiName = listChildOpn.getAPIName();
	            	boolean isObs = listChildOpn.isObsolete();
	            	
	            	logger.info("listChildOpn: "+ listChildOpnName + " listChildOpnApiName: " +listChildOpnApiName+" isObsolete?: "+isObs);
	            	//
	            	listChildOpn.setObsolete(false);
    			}
    		}
    		//Save
    		adminList.setValues(list);
    		
	    }
	    catch(Exception ex) 
	    {
	    	ex.printStackTrace();
	    	logger.error("Error:" + ex.getMessage());
	    }
        finally
		{
		    if(session!=null)
		        session.close();
		}
	}
	
    private static void setupListOption(IAgileList list, HSSFRow row, int listOpnNameIndex) throws APIException 
    {
    	try
    	{
    		Object[] listChildren = list.getChildren();
    		
    		String listOpnName = row.getCell(listOpnNameIndex).getStringCellValue();
    		String listOpnApiName = row.getCell(listOpnNameIndex+1).getStringCellValue();
    		
    		logger.info("listOpnName: "+ listOpnName + " listOpnApiName: " +listOpnApiName);
    		
    		//If has list children
    		if(listChildren != null && listChildren.length > 0)
    		{
	    		boolean found = false;
	    		//Search level n List Options.
	    		for(Object listChild : listChildren)
	            {
	            	IAgileList listChildOpn = (IAgileList)listChild;
	            	String listChildOpnApiName = listChildOpn.getAPIName();
	            	
	            	logger.info("listChildOpn: "+ listChildOpn + " listChildOpnApiName: " +listChildOpnApiName);
	            	
	            	//If find the same API name in level n, update List Option Value
	            	if(listChildOpnApiName.equals(listOpnApiName))
	            	{
	            		found = true;
	            		//update List Option Value
	            		listChildOpn.setValue(listOpnName);
	            		
	            		logger.info(indent(listOpnNameIndex)+"Update List Option: "+listOpnName+", ApiName: "+listOpnApiName);
	            		//Check if Excel has the next level List Option Name
	            		HSSFCell cellListOpnName = row.getCell(listOpnNameIndex+2);
	            		if(cellListOpnName != null && !cellListOpnName.getStringCellValue().equals("")) //Yes
	            		{
	            			//Do the next level List Option setting.
	            			setupListOption(listChildOpn, row, listOpnNameIndex+2);
	            		}
	            		
	            		break;
	            	}
	            }
	    		//If not found the same API name in level n, create new List Option
	    		if(!found)
	    		{
	    			IAgileList listChildOpn = (IAgileList)list.addChild(listOpnName, listOpnApiName);
	    			logger.info(indent(listOpnNameIndex)+"Add List Option: "+listOpnName+", ApiName: "+listOpnApiName);
	    			
	    			//Check if Excel has the next level List Option Name
	        		HSSFCell cellListOpnName = row.getCell(listOpnNameIndex+2);
	        		if(cellListOpnName != null && !cellListOpnName.getStringCellValue().equals("")) //Yes
	        		{
	        			//Do the next level List Option setting.
	        			setupListOption(listChildOpn, row, listOpnNameIndex+2);
	        		}
	    		}
    		}
    		else ////If has no list children, create new List Option
    		{
    			IAgileList listChildOpn = (IAgileList)list.addChild(listOpnName, listOpnApiName);
    			logger.info(indent(listOpnNameIndex)+"Add List Option: "+listOpnName+", ApiName: "+listOpnApiName);
    			
    			//Check if Excel has the next level List Option Name
        		HSSFCell cellListOpnName = row.getCell(listOpnNameIndex+2);
        		if(cellListOpnName != null && !cellListOpnName.getStringCellValue().equals("")) //Yes
        		{
        			//Do the next level List Option setting.
        			setupListOption(listChildOpn, row, listOpnNameIndex+2);
        		}
        	}
    		
    	}
    	catch(Exception e)
    	{
    		logger.error(e.getMessage());
    	}
    }
	
    private static String indent(int level) 
    {
    	if (level <= 0) 
    	{
    		return "";
    	}
    	char c[] = new char[level*2];
    	Arrays.fill(c, ' ');
    	return new String(c);
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
	
}
