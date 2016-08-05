package com.aic.agile.sdk.api.admin;

import java.util.*;

import com.agile.api.*;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.*;
import com.aic.agile.sdk.util.*;

public class ListExporter {
	
	public static final String USERNAME         = UtilConfigReader.readProperty("USERNAME");
	public static final String PASSWORD         = UtilConfigReader.readProperty("PASSWORD");
	public static final String URL              = UtilConfigReader.readProperty("URL");
	public static String XLS_EXPORT             = "";
	public static final String S_LIST_SHEET     = UtilConfigReader.readProperty("S_LIST_SHEET");
	public static final String C_LIST_SHEET     = UtilConfigReader.readProperty("C_LIST_SHEET");
	public static IAgileSession session = null;
	public static AgileSessionFactory factory;

	public ListExporter() {}
	
	public static void main(String args[]) {
		Logger logger = Logger.getRootLogger();
	    SimpleLayout layout = new SimpleLayout();
	    
	    //the flag to judge the path is right or not.
	    boolean isFile = false;
	    
	    logger.info("Here is some INFO of ListExporter:");
	    
	    while(isFile==false)
	    {
		    java.io.InputStream in = System.in;
		    System.out.println("Please enter the ListExporter file path:");
		    
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
			    		logger.info("The ListExporter file path:" + XLS_EXPORT);
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
			    			    // Get Agile List Library
			    			    IListLibrary lib = admin.getListLibrary();
			    			    // Get all list libraries
			    			    IAdminList list_object = null;
			    	        	IAdminList[] libLists = lib.getAdminLists();
			    	        	
			    	        	for(int i=0; i<libLists.length; i++)
			    	        	{
			    	        		IAdminList libList = libLists[i];
			    	        		
			    	        		if(libList.isListItemsEditable())
			    	        		{
			    	        			logger.info("Start saving libLists Info");
			    	        			
			    	        			String listName = libList.getName();
			    	    				String listApiName = libList.getAPIName();
			    	    				boolean listIsCascaded = libList.isCascaded();
			    	    				
			    	    				logger.info(listName + ":" + listApiName + " ,isCascaded:" + listIsCascaded );
			    	    				
			    	    			    String record = new String();
			    	    			    
			    	    			    if (listIsCascaded)
			    	    			    {
			    	    			    	record = "T" +  ":" + listName + ":" + listApiName;
			    	    			    }
			    	    			    else
			    	    			    {
			    	    			    	record = "F" +  ":" + listName + ":" + listApiName;
			    	    			    }
			    	    			    
			    	    			    logger.info("record:" + record);
			    	    					
			    	        			IAgileList list = libList.getValues();
			    	        			printList(list, 0, record, recordList);
			    	        			
			    	        			logger.info("Finish saving libLists Info");
			    	        		  
			    	        		}
			    	        	}
			    	        	
			    	        	logger.info("----------------------");
			    	        	logger.info("Start saving as Excel ");
			    	        	
			    	        	HSSFWorkbook workbook = new HSSFWorkbook();
			    			    HSSFSheet s_sheet = workbook.createSheet(S_LIST_SHEET);
			    			    HSSFSheet c_sheet = workbook.createSheet(C_LIST_SHEET);
			    			    
			    	        	int s_rownum = 0;
			    	        	int c_rownum = 0;
			    	        	Iterator it = recordList.iterator();
			    	        	
			    	        	String setSListValue ="";
			    	        	String setCListValue ="";
			    	        	
		    	        		int t_scolnum = 0;
		    	        		int t_ccolnum = 0;
		    	        		
		    	        		Row s_row = s_sheet.createRow(s_rownum);
		    	        		Row c_row = c_sheet.createRow(c_rownum);
		    	        		
		    	        		String sTitle = "C_setCellValue:List Name:API Name:List Option:API Name";	
		    	        		String[] tCellValues = sTitle.split(":");	
		    		    		for(int t = 1; t < tCellValues.length; t++)
		    		    		{
		    		    			
		    		    			Cell cell = s_row.createCell(t_scolnum++);
		    		    			cell.setCellValue(tCellValues[t]);
		    		    			setSListValue = setSListValue +tCellValues[t] + " ,";
		    		    				
		    		    		}
		    		    			
		    		    		logger.info("Title:" + setSListValue);		
		    		    		
		    	        		String cTitle = "C_setCellValue:List Name:API Name:Lv1 List Option:API Name:Lv2 List Option:API Name:Lv3 List Option:API Name:Lv4 List Option:API Name";	
		    	        		tCellValues = cTitle.split(":");	
		    		    		for(int t = 1; t < tCellValues.length; t++)
		    		    		{	    		    			
		    		    			Cell cell = c_row.createCell(t_ccolnum++);
		    		    			cell.setCellValue(tCellValues[t]);
		    		    			setCListValue = setCListValue +tCellValues[t] + " ,";
		    		    				
		    		    		}
		    		    			
		    		    		logger.info("Title:" + setCListValue);		
		    	
			    				s_rownum = s_rownum + 1;
			    				c_rownum = c_rownum + 1;
			    	        	
			    	        	while(it.hasNext())
			    	        	{
			    	        		String record = (String)it.next();	        		
			    	        		String[] cellValues = record.split(":");		
			                        	        		
			    	        		if(record.substring(0, 1).toString().trim().equals("T"))
			    	        		{
			    	        			setCListValue ="C_setCellValue: ";
			    	        			
			    	        			c_row = c_sheet.createRow(c_rownum++);
			    	        			
			    		    			int c_colnum = 0;
			    		    			for(int j = 1; j < cellValues.length; j++)
			    		    			{
			    		    				Cell cell = c_row.createCell(c_colnum++);
			    		    				cell.setCellValue(cellValues[j]);
			    		    				setCListValue = setCListValue +cellValues[j] + " ,";
			    		    				
			    		    			}
			    		    			
			    		    			logger.info("setCListValue:" + setCListValue);
			    	        			
			    	        		}
			    	        		else
			    	        		{
			    	        			s_row = s_sheet.createRow(s_rownum++);
			    	        			int s_colnum = 0;
			    	        			setSListValue ="S_setCellValue: ";
			    	        			for(int i = 1; i < cellValues.length; i++)
			    	        			{
			    	        				Cell cell = s_row.createCell(s_colnum++);
			    	        				cell.setCellValue(cellValues[i]);
			    	        				setSListValue = setSListValue +cellValues[i] + " ,";
			    	        			}
			    	        			
			    	        			logger.info("setSListValue" + setSListValue);
			    		    			
			    	        		}		
			    	        	}
			    	        	FileOutputStream out = 
			    	                    new FileOutputStream(new File(XLS_EXPORT));
			    	        	workbook.write(out);
			    	            out.close();
			    	           
			    	            System.out.println("ListExporter done sucessfully... ");		    	            
			    	            logger.info("Excel written successfully");        	
			    	            
			    	        }
			    			catch(Exception e1)
			    			{
			    				logger.error("Error:" + e1.getMessage());
			    			}	
			    		}
			    		catch(Exception ex)
			    		{
			    			logger.error("Error:" + ex.getMessage());
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
		    catch(Exception e)
		    {
		    	logger.error("Error:" + e.getMessage());
			}
		    finally
			{
			    if(session!=null)
			    	session.close();
			}
	    }
    }

	 private static void printList(IAgileList list, int level, String record, ArrayList recordList) throws APIException 
	    {
	    	if (list != null ) 
	    	{
	    		String subRecord = new String();
	    		
	    		if(list.getValue() != null)
	    		{
	    			System.out.println(indent(level*4) + list.getValue() + ":" + list.getAPIName());
	    			subRecord = list.getValue() + ":" + list.getAPIName();
	    			record += ":" + subRecord;
	    		}
	    		
	    		Object[] children = list.getChildren();
	    		if (children != null) 
	    		{
	    			for (int i = 0; i < children.length; ++i) 
	    			{
	    				printList((IAgileList)children[i], level + 1, record, recordList);
	    			}
	    		}
	    		else
	    		{
	    			//System.out.println(record);
	    			recordList.add(record);
	    		}
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
