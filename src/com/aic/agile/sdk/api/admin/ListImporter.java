package com.aic.agile.sdk.api.admin;

import java.util.*;

import com.agile.api.*;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.*;
import org.apache.poi.ss.usermodel.Row;

import java.io.*;

import java.io.FileInputStream;
import org.apache.log4j.*;
import com.aic.agile.sdk.util.*;

public class ListImporter {

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
	
	public ListImporter() { }
	
	public static void main(String args[]) {
	    
		//the flag to judge the path is right or not.
		boolean isFile = false;			
		
		logger.info("Here is some INFO of ListImporter:");
		
		//if not key the right file path then show again.
		while(isFile==false)
		{
		    //java.io.InputStream in = System.in;
		    //System.out.println("Please enter the ListImporter file path:");
		    
		    try
		    {
		    	/**
		    	BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
		    	XLS_IMPORT = bufferRead.readLine().trim();

		    	File directory = new File(XLS_IMPORT);
		    	if (!directory.exists()) 
		        {
		    		System.out.println("Notice: the file is not exist! Please enter again.");
					continue;
		        }
		        **/
		    	if(XLS_IMPORT.lastIndexOf(".")==-1)
		    	{
			    	System.out.println("ERROR: Filename extension should be enter!");
			    	throw new Exception("ERROR: [" + XLS_IMPORT + "] filename extension should be enter!");
			    }
			    else
			    {
			    	if(XLS_IMPORT.substring(XLS_IMPORT.lastIndexOf(".")).trim().equals(".xls"))
			    	{
			    		logger.info("The ListImporter file path:" + XLS_IMPORT);
			    		
			    		try 
			    		{
				        	logger.info("**************START**************");
				        	
				        	// Create an IAgileSession instance
				            session = connect(session);
				            // Get Admin instance
						    IAdmin admin = session.getAdminInstance();
						    // Read excel file
						   	FileInputStream xls_input_stream= new FileInputStream(XLS_IMPORT);
						    // POI File System - to Access MS Format File
						    POIFSFileSystem fs = new POIFSFileSystem(xls_input_stream);
						    // Get Excel workbook
						    HSSFWorkbook wb = new HSSFWorkbook(fs);
						    // Get Excel sheet
						    HSSFSheet s_list_sheet = wb.getSheet(S_LIST_SHEET);
						    HSSFSheet c_list_sheet = wb.getSheet(C_LIST_SHEET);
						    // Get Agile List Library
						    IListLibrary lib = admin.getListLibrary();
						    
						    logger.info("Setting up Single_List");
						    //Import Single list from 1st row to the last row.
						    for (int i=1; i<=s_list_sheet.getLastRowNum(); i++)
						    {
						    	
						    	HSSFRow row = s_list_sheet.getRow(i);
						    	HSSFCell cellListName = row.getCell(0);
						    	HSSFCell cellListAPIName = row.getCell(1);
						    	//The 1st List Option Name Index
						        int listOpnNameIndex = 2;
						    	//AdminList
						    	String listName = "";
						    	String listAPIName = "";
						    	
						    	try
						    	{
						    		listName = cellListName.getStringCellValue().trim();
						    		listAPIName = cellListAPIName.getStringCellValue().trim();
						    		
						    		//Check if List exists
						    		IAdminList adminList = lib.getAdminList(listAPIName);
						    		
						    		if(adminList==null) //List not exist
						    		{
						    			HashMap<Integer, Comparable> map = new HashMap<Integer, Comparable>();
						                map.put(IAdminList.ATT_NAME, listName);
						                map.put(IAdminList.ATT_APINAME, listAPIName);
						                map.put(IAdminList.ATT_ENABLED, new Boolean(true));
						                map.put(IAdminList.ATT_CASCADED, new Boolean(false));
						                logger.info("Create new single list name(APIName): " + listName +"("+ listAPIName +")");
						                adminList = lib.createAdminList(map);
						                map.clear();
						    		}
						    		else //List exists, update ListName
						    		{
						    			adminList.setName(listName);
						    			logger.info("Update single list name(APIName): " + listName +"("+ listAPIName +")");
						    		}
						    		//Get value object
						    		IAgileList list = adminList.getValues();            
						            adminList.setValues(list);
						            
						            /**
						             * Check level n if find list with the same apiname
						             * Yes: Update List_Name
						             * No: Create List with the name and apiname
						             */
						    		
						            setupListOption(list, row, listOpnNameIndex);
						            
						            adminList.setValues(list);
						    	
						    	}
						    	catch(Exception singleListE)
						    	{
						    		logger.error("Error: Single List Name(APIName): " + listName + "(" + listAPIName + "): " + singleListE.getMessage());
						    	}
						    	
						    }
						    logger.info("Finish Setting Single_List");
						    logger.info("---------------------------------");
						    
						    logger.info("Setting up Cascaded_List");
						    
						    //Import Cascading list from 1st row to the last row.
						    for (int i=1; i<=c_list_sheet.getLastRowNum(); i++)
						    {
						    	HSSFRow row = c_list_sheet.getRow(i);
						    	HSSFCell cellListName = row.getCell(0);
						    	HSSFCell cellListAPIName = row.getCell(1);
						    	//The 1st List Option Name Index
						        int listOpnNameIndex = 2;
						    	//AdminList
						    	String listName = "";
						    	String listAPIName = "";
						    	
						    	String error ="";
						    	
						    	try
						    	{
						    		listName = cellListName.getStringCellValue().trim();
						    		listAPIName = cellListAPIName.getStringCellValue().trim();
						    		
						    		//Check if List exists
						    		IAdminList adminList = lib.getAdminList(listAPIName);
						    		
						    		if(adminList==null) //List not exist
						    		{
						    			HashMap<Integer, Comparable> map = new HashMap<Integer, Comparable>();
						                map.put(IAdminList.ATT_NAME, listName);
						                map.put(IAdminList.ATT_APINAME, listAPIName);
						                map.put(IAdminList.ATT_ENABLED, new Boolean(true));
						                map.put(IAdminList.ATT_CASCADED, new Boolean(true));
						                adminList = lib.createAdminList(map);
						                logger.info("Create new cascaded list name(APIName): " + listName + "(" + listAPIName + ")");
						                map.clear();
						    		}
						    		else //List exists, update ListName
						    		{
						    			adminList.setName(listName);
						    			logger.info("Update cascaded list name(APIName): " + listName + "(" + listAPIName + ")" );
						    		}
						    		
						    		//Get value object
						    		IAgileList list = adminList.getValues();
						    		
						    		/**
						             * Check level n if find list with the same apiname
						             * Yes: Update List_Name
						             * No: Create List with the name and apiname
						             */
						    		
						            setupListOption(list, row, listOpnNameIndex);
						            
						            adminList.setValues(list);
						    	
						    	}
						    	catch(Exception CascadedListE)
						    	{
						    		logger.error("Error: Cascaded List Name(APIName): " + listName + "(" + listAPIName + "): " + CascadedListE.getMessage());
							    	
						    	}
						    	
							    logger.info("Finish Setting Cascaded_List");
						    }
						    
						    session.close();
						    logger.info("***************END***************");
				        }
			    		catch(Exception ex)
			    		{
			    			ex.printStackTrace();
			    			logger.error("Error:" + ex.getMessage());
			    		}
			    	}
			    	else
			    	{
			    		System.out.println(XLS_IMPORT.substring(XLS_IMPORT.lastIndexOf(".")).trim());
			    		System.out.println("ERROR: Filename extension should be .xls !");
			    		throw new Exception("ERROR: [" + XLS_IMPORT + "] filename extension should be .xls !");
			    	}
			    } 
		        
				//if success to read the ListImporter path
				isFile = true;
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
