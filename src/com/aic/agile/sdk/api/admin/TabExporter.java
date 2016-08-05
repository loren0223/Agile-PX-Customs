package com.aic.agile.sdk.api.admin;

import com.agile.api.*;
import com.aic.agile.sdk.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class TabExporter {
	
	public static final String USERNAME         = UtilConfigReader.readProperty("USERNAME");
	public static final String PASSWORD         = UtilConfigReader.readProperty("PASSWORD");
	public static final String URL              = UtilConfigReader.readProperty("URL");
	public static String XLS_EXPORT             = "";
	public static final String TAB_SHEET  = UtilConfigReader.readProperty("TAB_SHEET");
	public static IAgileSession session = null;
	public static AgileSessionFactory factory;
	
	public TabExporter(){
		
		Logger logger = Logger.getRootLogger();
	    SimpleLayout layout = new SimpleLayout();
	    
	    //the flag to judge the path is right or not.
	    boolean isFile = false;
	    
	    logger.info("Here is some INFO of TabExporter:");
	    
	    while(isFile==false)
	    {
	    	java.io.InputStream in = System.in;
	    	System.out.println("Please enter the TabExporter file path:");
	    	try
	    	{
	    	    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
	    	   	XLS_EXPORT = bufferRead.readLine().trim();
	    	   	
	    	   	String path = XLS_EXPORT.substring(0, XLS_EXPORT.lastIndexOf("/"));
	    	   	
	    	   	if(XLS_EXPORT.lastIndexOf("/")==-1)
			    {
			    	System.out.println("Notice: the file path is not found! Please enter again:");
					continue;
			    }
	    	   	
	    	   	File directory = new File(path);
		    	if (!directory.exists()) 
		        {
		    		System.out.println("Notice: the file path is not found! Please enter again:");
					continue;
		        }
		    
	    	   	if(XLS_EXPORT.lastIndexOf(".")==-1)
	    	   	{
	    	    	System.out.println("ERROR: Filename extension should be enter!");
	    	    	throw new Exception("ERROR: [" + XLS_EXPORT + "] filename extension should be enter!");
	    	    }
	    	    else
	    	    {
	    	    	if(XLS_EXPORT.substring(XLS_EXPORT.lastIndexOf(".")).trim().equals(".xls"))
	    	    	{
	    	    		logger.info("The TabExporter file path:" + XLS_EXPORT);
					    try
					    {
					    	ArrayList<String> recordList = new ArrayList<String>(); 
						    try
							{
						    	logger.info("**************START**************");
						    	// Create an IAgileSession instance
					            session = connect(session);
					            // Get Admin instance
							    IAdmin admin = session.getAdminInstance();
							    
							    String title = "SubClass:API Name:Visible";
				    			recordList.add(title);
						        
							    // Get AgileClasses node
							    INode node= admin.getNode(NodeConstants.NODE_AGILE_CLASSES);
							    
							    // Get parts subclass node
						    	INode nodeParts = (INode) node.getChildNode("Parts");
						    	INode DSubclass = (INode) nodeParts.getChildNode("User-Defined Subclasses");	
						    	
						    	// Get all subclass nodes
						    	Collection subclassNode = DSubclass.getChildNodes();
						       	for (Iterator it = subclassNode.iterator();it.hasNext();)
						    	{
						    		String record = new String();
						    		
						    		INode obj = (INode) it.next();
						    		String subclass = obj.getName().toString();
						    		String subclassName = obj.getAPIName().toString();
						    		logger.info("--subclassNode:" + obj + " / " + subclassName);
						    		
						 		    //Get subclass Page Two
							    	INode p2 = admin.getNode(subclassName + ".PageTwo"); // Fully qualified API name
							    	
							    	//Get visible value
							    	IProperty visible = p2.getProperty(PropertyConstants.PROP_VISIBLE);
							    	String visibleS = visible.getValue().toString();	    	
							    	logger.info("--visible:" + visibleS);	
							    	
							    	record =subclass + ":" + subclassName + ":" + visibleS;
							    	logger.info("record:" + record);
							    	recordList.add(record);
						    	}
						    	   	
						    	logger.info("----------------------");
				    	       	logger.info("Start saving as Excel ");
				    	        	
				    	        HSSFWorkbook workbook = new HSSFWorkbook();
				    	        HSSFSheet sheet = workbook.createSheet(TAB_SHEET);
				    	       	int rownum = 0;			
				    	       	
				    	       	Iterator it = recordList.iterator();
				    	       	
				    	       	while(it.hasNext())
			    	        	{
				    	       		String record = (String)it.next();	        		
			    	        		String[] cellValues = record.split(":");
			    	        		
					    	       	Row row = sheet.createRow(rownum++);
					    	        String setValue ="";
			    	        		
			    		    		int colnum = 0;
			    		    		for(int j = 0; j < cellValues.length; j++)
			    		    		{
			    		    			Cell cell = row.createCell(colnum++);
			    		    			cell.setCellValue(cellValues[j]);
			    		    			setValue = setValue +cellValues[j] + " ,";
			    		    			
			    		    		}
			    		    		logger.info("setValue:" + setValue);
			    	        	}	
		    		    		FileOutputStream out = 
				    	                   new FileOutputStream(new File(XLS_EXPORT));
				    	       	workbook.write(out);
				    	        out.close();
				    	        
				    	        System.out.println("TabExpoter done sucessfully... ");			    	            
				    	        logger.info("Excel written successfully"); 
							    
							    if(XLS_EXPORT.lastIndexOf(".")==-1)
							    {
							    	System.out.println("ERROR: Filename extension should be enter!");
							    	throw new Exception("ERROR: [" + XLS_EXPORT + "] filename extension should be enter!");
							    }
							    else
							    {
							    	
							    }
							}
						    catch(Exception e)
						    {
						    	e.printStackTrace();
						    }
					    }
					    catch(Exception ex)
					    {
					    	ex.printStackTrace();
					    }
			    	}  
					else
					{
						System.out.println(XLS_EXPORT.substring(XLS_EXPORT.lastIndexOf(".")).trim());
					    System.out.println("ERROR: Filename extension should be .xls !");
						throw new Exception("ERROR: [" + XLS_EXPORT + "] filename extension should be .xls !");
					 }
			    }
			    
			    isFile = true;
			}
		    catch(Exception e3)
			{
				e3.printStackTrace();
				logger.error("Error:" + e3.getMessage());
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
