package com.aic.agile.sdk.api.admin;

import java.util.*;
import com.aic.agile.sdk.util.*;

import com.agile.api.*;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.*;

import java.io.*;

public class TabImporter {

	public static final String USERNAME         = UtilConfigReader.readProperty("USERNAME");
	public static final String PASSWORD         = UtilConfigReader.readProperty("PASSWORD");
	public static final String URL              = UtilConfigReader.readProperty("URL");
	public static String XLS_IMPORT             = "";
	public static final String TAB_SHEET  = UtilConfigReader.readProperty("TAB_SHEET");
	public static IAgileSession session = null;
	public static AgileSessionFactory factory;
	
	public TabImporter(){
		
		Logger logger = Logger.getRootLogger();
	    SimpleLayout layout = new SimpleLayout();
	    
	    //the flag to judge the path is right or not.
	  	boolean isFile = false;		
	    
	    logger.info("Here is some INFO of TabImporter:");
	    
	    while(isFile==false)
	    {
	    
		    java.io.InputStream in = System.in;
		    System.out.println("Please enter the TabImporter file path:");
			
		    try
			{
			    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
			    XLS_IMPORT = bufferRead.readLine().trim();
			    
				File directory = new File(XLS_IMPORT);
		    	if (!directory.exists()) 
		        {
		    		System.out.println("Notice: the file is not exist! Please enter again.");
					continue;
		        }
			    
			    if(XLS_IMPORT.lastIndexOf(".")==-1)
			    {
			    	System.out.println("ERROR: Filename extension should be enter!");
			    	throw new Exception("ERROR: [" + XLS_IMPORT + "] filename extension should be enter!");
			    }
			    else
			    {
			    	if(XLS_IMPORT.substring(XLS_IMPORT.lastIndexOf(".")).trim().equals(".xls"))
			    	{
			    		logger.info("The TabImporter file path:" + XLS_IMPORT);
			    			    
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
						    HSSFSheet tab_sheet = wb.getSheet(TAB_SHEET);
						    
						    logger.info("Start setting subclass Page Two");
						    try
						    {
							    for (int i=1; i<=tab_sheet.getLastRowNum(); i++)  //1st row is the title.
							    {
							    	HSSFRow row = tab_sheet.getRow(i);
							    	String subClass = row.getCell(0).getStringCellValue().trim();
							    	
							    	if(subClass==null||subClass.equals(""))
							    		break;
							    	
							    	//check the Parts subclass is exist in system
							    	IAgileClass partClass = (IAgileClass)admin.getAgileClass(subClass);
							    	if(partClass==null) //partClass not exist
						    		{
						    			throw new Exception("The subclass: " + subClass + " not exist! ");
						    		}    	
					
							       	//Get settings of rows
							    	logger.info("subClass: " + subClass );		    	    	
							    	String APIName = row.getCell(1).getStringCellValue().trim();
							    	logger.info("--APIName: " + APIName );
							    	String visible = row.getCell(2).getStringCellValue().trim();
							    	logger.info("--Visible: " + visible );
							    	
							    	logger.info("Start set subclass PageTwo properties");
							    	
							    	//Get subclass Page Two node
							    	INode p2 =  admin.getNode(APIName+ ".PageTwo");
							    	
							    	//Set p2 visible
							    	p2.getProperty(PropertyConstants.PROP_VISIBLE).setValue(visible);
							    	
							    	logger.info("Subclass:" + subClass + " (" + APIName +" ): " + visible );
							    }
								logger.info("Finish setting Tab ");
								session.close();
							}
							catch(Exception e)
							{
								e.printStackTrace();
						    	logger.error("Error: " + e.getMessage());
							}
				        }
						catch(Exception e1)
				    	{
							e1.printStackTrace();
							logger.error("Error: " + e1.getMessage());				    	
				    	}
			    	}
			    	else
			    	{
			    		System.out.println(XLS_IMPORT.substring(XLS_IMPORT.lastIndexOf(".")).trim());
			    		System.out.println("ERROR: Filename extension should be .xls !");
			    		throw new Exception("ERROR: [" + XLS_IMPORT + "] filename extension should be .xls !");
			    	}
			    } 
			    
			    //if success to read the TabImporter path
			    isFile = true;
			}
		    catch(Exception ex)
		    {
		    	logger.error("Error: " + ex.getMessage());
		    }
		    finally
		    {
		    	if(session!=null)
		    		session.close();
		    }
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
}


