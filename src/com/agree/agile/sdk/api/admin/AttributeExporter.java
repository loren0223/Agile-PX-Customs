package com.agree.agile.sdk.api.admin;

import com.agile.api.*;
import com.agree.agile.sdk.util.*;

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

public class AttributeExporter {
	
	public static final String USERNAME         = UtilConfigReader.readProperty("USERNAME");
	public static final String PASSWORD         = UtilConfigReader.readProperty("PASSWORD");
	public static final String URL              = UtilConfigReader.readProperty("URL");
	public static String XLS_EXPORT             = "";
	public static final String ATTRIBUTE_SHEET  = UtilConfigReader.readProperty("ATTRIBUTE_SHEET");
	public static IAgileSession session = null;
	public static AgileSessionFactory factory;

	public AttributeExporter() {
		
		Logger logger = Logger.getRootLogger();
	    SimpleLayout layout = new SimpleLayout();
	    
	    //the flag to judge the path is right or not.
	    boolean isFile = false;
	    
	    logger.info("Here is some INFO of AttributeExporter:");
	    
	    while(isFile==false)
	    {
	    	java.io.InputStream in = System.in;
		    System.out.println("Please enter the AttributeExporter file path:");
		    
		    String name        = "";
		    String APIName     = "";
		    String desc        = "";
		    String characters  = "";
		    String maxLength   = "";
		    String list        = "";
		    String scale       = "";
		    String maxValue    = "";
		    String minValue    = "";
		    String dateFormat  = "";
		    String visible     = "";
		    String required    = "";
		    String defaultValue= "" ;
		    String subscribe   = "";
		    String inputWidth   = ""; 
		    String inputHeight  = "";
		    String changeControlled = "";
		    String searchCriteria ="";
		    
		    
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
			    		logger.info("The AttributeExporter file path:" + XLS_EXPORT);
			    		
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
		
							    // Get the Parts class
							    IAgileClass PartsClass = admin.getAgileClass(ItemConstants.CLASS_PARTS_CLASS );
							    // Get the Item subclass 
							    IAgileClass[] subclasses = PartsClass.getSubclasses();
							    
							    logger.info("Start saving attribute Info ");
							    
							    try
							    {
	
						    		String title = "SubClass (物料子分類):Name (欄位名稱):API Name (API索引):Description (欄位說明):";
					    			title = title +"Characters (字元集) Text/MultiText only:MaxLength (最大長度) Text/MultiText only:";
					    			title = title +"List (選單) List/MultiList only:Scale (精確度) Numeric only:Max Value (最大值) Numeric only:";
					    			title = title +"Min Value (最小值) Numeric only:Date Time Format (日期格式) Date only:Visible(顯示):";
					    			title = title +"Required (必填欄位):DefaultValue (欄位預設值):Available for Subscribe (允許訂閱):";
					    			title = title +"Enable for Search Criteria (套用於自訂搜尋BasicMode):Input Width (輸入格寬度):";
					    			title = title +"Input Height(輸入格高度) MultiText only:Change controlled (變更控管):";
					    			title = title +"PAD Required (是否組成品名):PAD Order (品名組成順序):PAD Value Prefix (品名組成區段前綴):";
					    			title = title +"PAD Value Suffix (品名組成區段後綴)";
					    			recordList.add(title);
							    	
							    	for (int i = 0; i < subclasses.length; ++i) 
							    	{
							    		String nameSubclasses = subclasses[i].getName();
							    		logger.info("SubClass: " + nameSubclasses);				    		
							    								    		
							    		//Get required attributes for Page Three
							    		ITableDesc page3 = subclasses[i].getTableDescriptor(TableTypeConstants.TYPE_PAGE_THREE);
							    		if (page3 != null) 
							    		{						    			
							    								    			
							    			IAttribute[] attrs = page3.getAttributes();
							    			for (int j = 0; j < attrs.length; j++) 
							    			{
							    				String record = new String();
							    				
							    				IAttribute attr = attrs[j];		
							    									    								    				
							    				//get Attribute dataType
							    				Integer dataType = attr.getDataType();
							    				logger.info("dataType:" + dataType);
							    				
							    				//get Property dataType
							    				IProperty attTypeProperty = attr.getProperty(PropertyConstants.PROP_TYPE);					    				
							    				String attType =   attTypeProperty.getValue().toString();
							    				logger.info("attType:" + attType);
							    				
							    				/*
							    				 *Show attr all property					    				
							    				
							    				IProperty[] property = attr.getProperties();
							    				for (int p=0; p< property.length; p++)
							    				{
							    					Integer pid = (Integer) property[p].getId();
							    					String pName = property[p].getName();
							    					Integer ptype = property[p].getDataType();
							    					logger.info("pid:" + pid + " ,pAPIName:"+ pName+ " ,ptype: "+ ptype);
							    				}
							    				 */
							    				
							    				//Ignore Property DataType = Heading, Money
							    				if(!attType.toUpperCase().equals("MONEY") && !attType.toUpperCase().equals("HEADING"))
							    				{						    									    				
								    				//Get Property Name
							    					name = attr.getName();
								    				logger.info("Name: " + name );
								    				//Get Property APIName
								    				APIName = attr.getAPIName();
								    				logger.info("-APIName: " + APIName );		    					
								    				//Get Property Description
								    				desc = (String) attr.getProperty(PropertyConstants.PROP_DESCRIPTION).getValue();
								    				logger.info("-Description: " + desc );			    					
							    									    					
								    				//Property type = TEXT / MULTITEXT
								    				if(dataType.equals(DataTypeConstants.TYPE_STRING))
								    				{
								    					//Get Property Characters
								    					characters =  attr.getProperty(PropertyConstants.PROP_INCLUDE_CHARACTERS).getValue().toString();  					
								    					characters = characters==null? "" : characters.toString();
								    					logger.info("-Characters: " + characters );	
								    					//Get Property MaxLength
								    					maxLength = (String) attr.getProperty(PropertyConstants.PROP_MAXLENGTH).getValue().toString();
								    					maxLength = maxLength==null? "" : maxLength.toString();
								    					logger.info("-MaxLength: " + maxLength );
								    					//Get Property IputWidth
									    				inputWidth = attr.getProperty(864).getValue().toString();
									    				inputWidth = inputWidth==null? "" : inputWidth.toString();
									    				logger.info("-inputWidth: " + inputWidth);							    				
								    				}
								    				
								    				if(attType.toUpperCase().equals("MULTITEXT"))
								    				{
									    				//Get Property inputHeight
									    				inputHeight = attr.getProperty(865).getValue().toString();
									    				inputHeight = inputHeight==null? "" : inputHeight.toString();
								    				}
								    				logger.info("-InputHeight: " + inputHeight);
								    				
								    				//Property type = List /MulitList
								    				if(dataType.equals(DataTypeConstants.TYPE_SINGLELIST)||dataType.equals(DataTypeConstants.TYPE_MULTILIST))
								    				{
								    					list = (String) attr.getProperty(PropertyConstants.PROP_LIST).getValue().toString();
								    					list = list==null? "" : list.toString();
								    				}					    				
								    				logger.info("-list: " + list );
							    					
								    				//Property type = Numeric
								    				if(attType.toUpperCase().equals("NUMERIC")) 
								    				{
								    					//Get Property Scale
								    					Object scaleO = attr.getProperty(419).getValue();  					
								    					scale = scaleO==null? "" : scaleO.toString();
								    					if(scale.lastIndexOf(".")!=-1)
								    					{
								    						scale =scale.substring(0, scale.lastIndexOf("."));
								    					}
								    					//Get Property MaxValue							    					
								    					Object maxValueO = attr.getProperty(418).getValue();
								    					maxValue = maxValueO==null? "" : maxValueO.toString();
								    					//去除有小數點部分
								    					if(maxValue.lastIndexOf(".")!=-1)
								    					{
								    						maxValue =maxValue.substring(0, maxValue.lastIndexOf("."));
								    					}
								    					//Get Property MinValue
								    					Object minValueO = attr.getProperty(417).getValue();
								    					minValue = minValueO==null? "" : minValueO.toString();
								    					//去除有小數點部分
								    					if(minValue.lastIndexOf(".")!=-1)
								    					{
								    						minValue =minValue.substring(0, minValue.lastIndexOf("."));
								    					}
								    				}
								    				logger.info("-scale: " + scale );
								    				logger.info("-maxValue: " + maxValue );
								    				logger.info("-minValue: " + minValue );
							    					
								    				//Property type = Date
								    				if(attType.toUpperCase().equals("DATE"))
								    				{
								    					dateFormat = attr.getProperty(837).getValue().toString();
								    					dateFormat = dateFormat==null? "" : dateFormat.toString();
								    				}
								    				logger.info("-dateFormat: " + dateFormat);
								    					
								    				//Get Property Visible
								    				Object visibleO = attr.getProperty(PropertyConstants.PROP_VISIBLE).getValue();
								    				visible = visibleO==null? "" : visibleO.toString();
								    				logger.info("-visible: " + visible);					    				
								    				//Get Attribute Required
								    				required = attr.getProperty(PropertyConstants.PROP_REQUIRED).getValue().toString();
								    				logger.info("-required: " + required);
								    				//Get Attribute DefaultValue
								    				defaultValue = attr.getProperty(PropertyConstants.PROP_DEFAULTVALUE).getValue().toString();
								    				logger.info("-defaultValue: " + defaultValue);
								    				//Get Attribute Subscribe
								    				subscribe = attr.getProperty(PropertyConstants.PROP_AVAILABLE_FOR_SUBSCRIBE).getValue().toString();
								    				logger.info("-subscribe: " + subscribe);					    				
								    				//Get Attribute ChangeControlled
								    				changeControlled = attr.getProperty(PropertyConstants.PROP_CHANGE_CONTROLLED).getValue().toString();
								    				logger.info("-changeControlled: " + changeControlled);
								    				//Get Attribute Enable for Search Criteria
								    				searchCriteria = attr.getProperty(PropertyConstants.PROP_ENABLE_FOR_SEARCH_CRITERIA).getValue().toString();
								    				logger.info("-searchCriteria: " + searchCriteria);
							    				
								    				record = nameSubclasses + ":" + name + ":" + APIName + ":" + desc + ":" ;
								    				record= record + characters + ":" + maxLength + ":" ;
								    				record= record + list + ":" + scale + ":" + maxValue + ":";
								    				record= record + minValue + ":" + dateFormat + ":" + visible + ":";
								    				record= record + required + ":" + defaultValue + ":" + subscribe + ":" ;
								    				record= record + searchCriteria + ":" +inputWidth + ":" ;
								    				record= record + inputHeight + ":" + changeControlled;
									    			
								    				logger.info("record:" + record);
								    				//save record into recordList
								    				recordList.add(record);
								    				
								    				//set data null;
								    				name        = "";
								    			    APIName     = "";
								    			    desc        = "";
								    			    characters  = "";
								    			    maxLength   = "";
								    			    list        = "";
								    			    scale       = "";
								    			    maxValue    = "";
								    			    minValue    = "";
								    			    dateFormat  = "";
								    			    visible     = "";
								    			    required    = "";
								    			    defaultValue= "" ;
								    			    subscribe   = "";
								    			    inputWidth   = ""; 
								    			    inputHeight  = "";
								    			    changeControlled = "";
								    			    searchCriteria ="";
								    			    
							    				}
							    			}
							    			
								    		logger.info("----------------------");
							    	       	logger.info("Start saving as Excel ");
							    	        	
							    	        HSSFWorkbook workbook = new HSSFWorkbook();
							    	        HSSFSheet sheet = workbook.createSheet(ATTRIBUTE_SHEET);
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
							    	       
							    	        System.out.println("AttributeExpoter done sucessfully... ");						    	            
							    	        logger.info("Excel written successfully"); 
							    		}
							    	}				    				
							    }
							    catch(Exception ex)
							    {	
							    	ex.printStackTrace();
							    	logger.error("Error:" + ex.getMessage());
							    }
					        }
						    catch(Exception e1)
				    		{
						    	e1.printStackTrace();
						    	logger.error("Error:" + e1.getMessage());
				    		}
			    		}
				    	catch(Exception e2)
				    	{
				    		e2.printStackTrace();
				    		logger.error("Error:" + e2.getMessage());
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
