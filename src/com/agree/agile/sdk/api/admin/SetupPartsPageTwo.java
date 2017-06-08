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

public class SetupPartsPageTwo {
	//靜態變數宣告
	public static final String USERNAME = UtilConfigReader.readProperty("USERNAME");
	public static final String PASSWORD = UtilConfigReader.readProperty("PASSWORD");
	public static final String URL = UtilConfigReader.readProperty("URL");
	public static final String XLS_IMPORT = UtilConfigReader.readProperty("DATASHEET");
	public static final String SUBCLASS_SHEET = UtilConfigReader.readProperty("SUBCLASS_SHEET");
	public static final String PAGE_TWO_SHEET = UtilConfigReader.readProperty("PAGE_TWO_SHEET");
	public static IAgileSession session = null;
	public static AgileSessionFactory factory;
	
	public SetupPartsPageTwo() { }
	
	public static void main(String[] args) {
		//宣告Log4j Logger
		Logger logger = Logger.getRootLogger();
	    //宣告變數
		SimpleLayout layout = new SimpleLayout();
		FileInputStream fileInput = null;
		//POIFSFileSystem poiFileSys = null;
		Workbook workbook = null;
		Sheet subclassSheet = null;
		Sheet pageTwoSheet = null;
		String fileExtension = null;
		//Flag:檔案路徑是否正確
	    boolean isFilepathRight = false;
	    //Default API-Name array of attribute-text
	    String[] textAPIName = {"text01","text02","text03","text04","text05","text06","text07","text08","text09","text10","text11","text12","text13","text14","text15","text16","text17","text18","text19","text20","text21","text22","text23","text24","text25"};
	    //Default API-Name array of attribute-list
	    String[] listAPIName = {"list01","list02","list03","list04","list05","list06","list07","list08","list09","list10","list11","list12","list13","list14","list15","list16","list17","list18","list19","list20","list21","list22","list23","list24","list25"};
	    //Default API-Name array of attribute-numeric
	    String[] numericAPIName = {"numeric01","numeric02","numeric03","numeric04","numeric05","numeric06","numeric07","numeric08","numeric09","numeric10"};
	    //Default API-Name array of attribute-date
	    String[] dateAPIName = {"date01","date02","date03","date04","date05","date06","date07","date08","date09","date10","date11","date12","date13","date14","date15"};
	    //Default API-Name array of attribute-multitext
	    String[] mTextAPIName = {"multiText10","multiText20","multiText30","multiText31","multiText32","multiText33","multiText34","multiText35","multiText36","multiText36","multiText38","multiText39","multiText40","multiText41","multiText42","multiText43","multiText44","multiText45"};
	    //Default API-Name array of attribute-multilist
	    String[] mListAPIName = {"multiList01","multiList02","multiList03"};
	    //Loop index of attribute-text
	    int textIndex = 0;
	    //Loop index of attribute-list
	    int listIndex = 0;
	    //Loop index of attribute-numeric
	    int numericIndex = 0;
	    //Loop index of attribute-date
	    int dateIndex = 0;
	    //Loop index of attribute-multitext
	    int mTextIndex = 0;
	    //Loop index of attribute-multilist
	    int mListIndex = 0;
	    
	    //Logging
	    //System.out.println("Continue to input the required information...");
	    while(isFilepathRight==false) {
	    	//java.io.InputStream in = System.in;
	    	//請輸入Admin Data來源檔案路徑。
	        //System.out.println("Please enter the filepath of Admin Data:");
		    try {
			    //取得檔案
		    	//BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
			    //XLS_IMPORT = bufferRead.readLine().trim();
			    File directory = new File(XLS_IMPORT);
			    //取得副檔名
			    fileExtension = XLS_IMPORT.substring(XLS_IMPORT.lastIndexOf(".")).trim();
		    	//如果檔案不存在
			    if (!directory.exists()) {
		    		System.out.println("Error: File does not exist, please try again!");
					continue;
		        }
			    //如果沒有副檔名,或是副檔名不是.xls/.xlsx
			    if(XLS_IMPORT.lastIndexOf(".")==-1 || !fileExtension.equals(".xls")) {
			    	System.out.println("Error: Filename extension[.xls/.xlsx] is requried, please try again!");
			    	//throw new Exception("Error: [" + XLS_IMPORT + "] filename extension is requried!");
			    	continue;
			    } else {
			    	//if success to read the AttributeImporter path
				    isFilepathRight = true;
			    	//讀Excel檔
			    	fileInput = new FileInputStream(XLS_IMPORT);
				    // POI File System - to Access MS Format File
				    //poiFileSys = new POIFSFileSystem(fileInput);
				    //取得Workbook
				    if(fileExtension.equals(".xls")) {
				    	workbook = new HSSFWorkbook(fileInput);
				    }
				    else if(fileExtension.equals(".xlsx")) {
				    	workbook = new XSSFWorkbook(fileInput);
				    }
				    //取得Sheet
				    //subclassSheet = workbook.getSheet(SUBCLASS_SHEET);
				    pageTwoSheet = workbook.getSheet(PAGE_TWO_SHEET);
			    }
			    
			    /***開始Parts Page Two設定***/
			    //Excel Sheet Schema (Parts.P2)
			    //Column 0:Parts Category List
			    //Column 1:Attribute Name
			    //Column 2:Attribute Description
			    //Column 3:API Name
			    //Column 4:Attribute Type
			    //Column 5:Visible
			    //Column 6:List Name
			    //Column 7:Default Value
			    //Column 8:For Search Criteria
			    //Column 9:Max Length(Text)
			    //Column 10:System Max Length(Text)
			    //Column 11:Character Set(Text)
			    //Column 12:Min Value(Numeric)
			    //Column 13:Max Value(Numeric)
			    //Column 14:Order
			    //Column 15:Site-Specific Field
			    //Column 16:Height
			    //Column 17:Scale(Numeric)
			    //Column 18:Attribute(Constant-like)
			    //Column 19:Base ID
			    //Column 20:Required
			    //Column 21:For Subscribe
			    //Column 22:Display Width
			    //Column 23:Input Width
			    //Column 24:Change Controlled
			    //Column 25:Date Format(Date)
			    
			    try {
					logger.info("**************START**************");
					logger.info("Starting to import admin data of attributes. Filepath:" + XLS_IMPORT);
					//建立Agile連線
		            session = connect(session);
		            //取得Admin Node
				    IAdmin admin = session.getAdminInstance();
				    //取得List Library
				    IListLibrary lib = admin.getListLibrary();	   
				    
			    	//第一列是Title，從第二列開始處理。
				    //String previousSubclassName = "";
				    for (int i=1; i<=pageTwoSheet.getLastRowNum(); i++) {
				    	//取得Row(i)
				    	int xlsRowNo = i+1;
				    	Row row = pageTwoSheet.getRow(i);
				    	if(row==null)
				    		break;
				    	logger.info("Datarow No."+xlsRowNo);
				    	
				    	/***取得資料列的所有欄位資料***/
				    	//String subclassName = getSpecificCellValue(row, (int)0);
				    	String attrName = getSpecificCellValue(row, (int)1);
				    	String desc = getSpecificCellValue(row, (int)2);
				    	String APIName = getSpecificCellValue(row, (int)3);
				    	String attrType = getSpecificCellValue(row, (int)4);
				    	String visible = getSpecificCellValue(row, (int)5);
				    	String listName = getSpecificCellValue(row, (int)6);
				    	String defaultValue = getSpecificCellValue(row, (int)7);
				    	String searchCriteria = getSpecificCellValue(row, (int)8);
				    	String maxLength = getSpecificCellValue(row, (int)9);
				    	//10 System Max Length
				    	String characters = getSpecificCellValue(row, (int)11);
				    	String minValue = getSpecificCellValue(row, (int)12);
				    	String maxValue = getSpecificCellValue(row, (int)13);
				    	//14 Order
				    	//15 Site-Specific Field
				    	//16 Height
				    	String scale = getSpecificCellValue(row, (int)17);
				    	//18 Attribute(Constant-like)
				    	//19 Base ID
				    	String required = getSpecificCellValue(row, (int)20);
				    	String subscribe = getSpecificCellValue(row, (int)21);
				    	//22 Display Width
				    	String inputWidth = getSpecificCellValue(row, (int)23);
				    	String changeControlled = getSpecificCellValue(row, (int)24);
				    	String dateFormat = getSpecificCellValue(row, (int)25);
				    	
				    	//logger.info("\tSubclass Name: " + subclassName);
				    	logger.info("\tAttribute Name: " + attrName);
				    	logger.info("\tAttribute Description: " + desc);
				    	logger.info("\tAPI Name: " + APIName);
				    	logger.info("\tAttribute Type: " + attrType);
				    	logger.info("\tVisible: " + visible);
				    	logger.info("\tList Name: " + listName);
				    	logger.info("\tDefault Value: " + defaultValue);
				    	logger.info("\tFor Search Criteria: " + searchCriteria);	
				    	logger.info("\tMax Length(Text) : " + maxLength);
				    	logger.info("\tCharacter Set: " + characters);
				    	logger.info("\tMin Value: " + minValue);
				    	logger.info("\tMax Value: " + maxValue);
				    	logger.info("\tScale(Numeric: " + scale);
				    	logger.info("\tRequired: " + required);
				    	logger.info("\tFor Subscribe: " + subscribe);
				    	logger.info("\tInput Width: " + inputWidth);
				    	logger.info("\tChange Controlled: " + changeControlled);	    	
				    	logger.info("\tDate Format: " + dateFormat);
				    	
				    	/**
				    	//如果Subclass名稱是空，拋出例外
				    	if(subclassName==null || subclassName.equals("")) {
				    		throw new Exception("Subclass name is required! Row_Index:"+i);
				    	}
				    	**/
				    	/**
				    	//取得Subclass物件類別
				    	IAgileClass agileClass = (IAgileClass)admin.getAgileClass(subclassName);
				    	//如果無法取得，拋出例外 
				    	if(agileClass==null) {
			    			throw new Exception("No such subclass! Subclass_Name:"+subclassName);
			    		}
			    		**/    	
				    	//檢查清單設定是否存在
				    	if(!(listName==null || listName.equals("") || listName.equals("N/A"))) {
				    		//取得清單設定 
				    		IAdminList adminList = lib.getAdminList(listName);
				    		//如果清單設定不存在，拋出例外
				    		if(adminList==null) {
				    			throw new Exception("No such admin list setting! List_Name:"+listName);
				    		}
				    	}
				    	//如果Attribute Name是空，拋出例外
				    	if(attrName==null || attrName.equals("")) {
				    		throw new Exception("Attribute name is required! Row_Index:"+xlsRowNo);
				    	}
				    	//如果Attribute Type是空，拋出例外
				    	if(attrType==null || attrType.equals("")) {
				    		throw new Exception("Attribute type is required! Row_Index:"+xlsRowNo);
				    	}
				    	//如果Visible是空，拋出例外
				    	if(visible==null || visible.equals("")) {
				    		throw new Exception("Visible is required! Row_Index:"+xlsRowNo);
				    	}
				    	//如果Required是空，拋出例外
				    	if(required==null || required.equals("")) {
				    		throw new Exception("Required setting is required! Row_Index:"+xlsRowNo);
				    	}
				    	
				    	/***開始設定Attributes***/
				    	logger.info("Start set attribute properties");
				    	/**
				    	//套用API Name取得子類別第三頁的屬性設定
				    	if(!subclassName.equals(previousSubclassName)) {
				    		previousSubclassName = subclassName;
				    		textIndex = 0;
				    		listIndex = 0;
				    		numericIndex = 0;
				    		dateIndex = 0;
				    		mTextIndex = 0;
				    		mListIndex = 0;
				    	}
				    	**/
				    	if(APIName==null || APIName.equals("")) {
				    		if(attrType.equalsIgnoreCase("text")) {
				    			APIName = textAPIName[textIndex];
				    			textIndex++;
				    		}
				    		if(attrType.equalsIgnoreCase("list")) {
				    			APIName = listAPIName[listIndex];
				    			listIndex++;
				    		}
				    		if(attrType.equalsIgnoreCase("numeric")) {
				    			APIName = numericAPIName[numericIndex];
				    			numericIndex++;
				    		}
				    		if(attrType.equalsIgnoreCase("date")) {
				    			APIName = dateAPIName[dateIndex];
				    			dateIndex++;
				    		}
				    		if(attrType.equalsIgnoreCase("multitext")) {
				    			APIName = mTextAPIName[mTextIndex];
				    			mTextIndex++;
				    		}
				    		if(attrType.equalsIgnoreCase("multilist")) {
				    			APIName = mListAPIName[mListIndex];
				    			mListIndex++;
				    		}
				    	}
				    	
				    	//打開Page Three
				    	//String subclassAPIName = admin.getAgileClass(subclassName).getAPIName();
				    	INode p2node = admin.getNode("PartsClass.PageTwo");
				    	p2node.getProperty(PropertyConstants.PROP_VISIBLE).setValue("Yes");
				    	
				    	//設定屬性
				    	IAttribute attr = admin.getAgileClass("PartsClass").getAttribute("PageTwo."+APIName.trim());
				    	//Disable all warnings
				    	session.disableAllWarnings();
				    	attr.getProperty(PropertyConstants.PROP_NAME).setValue(attrName);
				        logger.info("\t@attribute_name: "+ attrName);
				        attr.getProperty(PropertyConstants.PROP_DESCRIPTION).setValue(desc);
				        logger.info("\t@attribute_desc: "+ desc);
				        attr.getProperty(PropertyConstants.PROP_VISIBLE).setValue(visible);
				        logger.info("\t@visible: "+ visible);
				        
				        //'CreateUser' skips the other processes.
				    	if(APIName.equals("createUser")) continue;
				    		
			    		if(!(changeControlled==null || changeControlled.equals(""))) {
				    		attr.getProperty(PropertyConstants.PROP_CHANGE_CONTROLLED).setValue(changeControlled);
				    		logger.info("\t@changeControlled: "+ changeControlled);
				    	}
				    	attr.getProperty(PropertyConstants.PROP_REQUIRED).setValue(required);
				        logger.info("\t@required: "+ required);
				        
				        //'Heading Type' skips the other processes.
				    	if(attrType.equals("Heading")) continue;
				        
				        if(attrType.equalsIgnoreCase("text") || attrType.equalsIgnoreCase("multitext")) {
					        if(characters!=null && !characters.equals("")) {
					        	attr.getProperty(PropertyConstants.PROP_INCLUDE_CHARACTERS).setValue(characters);
					        	logger.info("\t@characters: "+ characters);
					        }
					        if(maxLength!=null && !maxLength.equals("")) {						        	
					        	attr.getProperty(PropertyConstants.PROP_MAXLENGTH).setValue(maxLength);
					        	logger.info("\t@maxLength: "+ maxLength);
					        }
				        }
				        if(attrType.equalsIgnoreCase("list") || attrType.equalsIgnoreCase("multilist")) {
					        if(listName!=null && !listName.equals("")) {
					        	attr.getProperty(PropertyConstants.PROP_LIST).setValue(lib.getAdminList(listName));
				        		logger.info("\t@list: "+ listName);
					        }
				        }
				        if(attrType.equalsIgnoreCase("numeric")) {
				        	if(scale!=null && !scale.equals("")) {
					        	attr.getProperty(PropertyConstants.PROP_SCALE).setValue(scale);
					        	logger.info("\t@scale: "+ scale);
				        	}
					        if(!(maxValue==null || maxValue.equals("") || maxValue.equals("N/A"))) {
					        	attr.getProperty(PropertyConstants.PROP_MAX_VALUE).setValue(maxValue);
					        	logger.info("\t@ma xValue: "+ maxValue);
					        }
					        if(!(minValue==null || minValue.equals("") || minValue.equals("N/A"))) {
					        	attr.getProperty(PropertyConstants.PROP_MIN_VALUE).setValue(minValue);
					        	logger.info("\t@minValue: "+ minValue);
					        }
				        }
				        if(attrType.equalsIgnoreCase("date")) {
				        	if(dateFormat!=null && !dateFormat.equals("")) {
				        		attr.getProperty(837).setValue(dateFormat);
				        		logger.info("\t@dateFormat: "+ dateFormat);
				        	}
				        }
				        if(!(defaultValue==null || defaultValue.equals("") || defaultValue.equals("N/A"))) {
				        	attr.getProperty(PropertyConstants.PROP_DEFAULTVALUE).setValue(defaultValue);
				        	logger.info("\t@defaultValue: "+ defaultValue);
				        }
				        if(!(subscribe==null || subscribe.equals(""))) {
				        	attr.getProperty(PropertyConstants.PROP_AVAILABLE_FOR_SUBSCRIBE).setValue(subscribe);
				        	logger.info("\t@subscribe: "+ subscribe);
				        }
				        if(!(searchCriteria==null || searchCriteria.equals(""))) {
				        	attr.getProperty(PropertyConstants.PROP_ENABLE_FOR_SEARCH_CRITERIA).setValue(searchCriteria);
				        	logger.info("\t@searchCriteria: "+ searchCriteria);
				        }
				    	if(!(inputWidth==null || inputWidth.equals("") || inputWidth.equals("N/A"))) {
				    		attr.getProperty(864).setValue(inputWidth);
				    		logger.info("\t@inputWidth: "+ inputWidth);
				    	}
				    	/**
				    	if(attrType.equalsIgnoreCase("multitext")) {
					    	if(inputHeight!=null && !inputHeight.equals("")) {
					    		attr.getProperty(865).setValue(inputHeight);
					    		logger.info("@inputHeight: "+ inputHeight);
					    	}
				    	}
				    	**/
				    	//Enable all warnings
				    	session.enableAllWarnings();
				    					    	
				    	logger.info("Parts.PageTwo - AttributeName(APIName):[" + attrName +"("+ APIName +")" + "] OK ");
				    }
				    logger.info("Finish setting attribute ");
				    
		        }
				catch(Exception e1) {
					e1.printStackTrace();
					logger.error("Error: " + e1.getMessage());
			    }
			} 
	        catch(Exception ex) {
	        	logger.error("Error: " + ex.getMessage());
	        }
			finally {
				session.close();
				session = null;
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
	private static IAgileSession connect(IAgileSession session) throws APIException {
		System.setProperty("disable.agile.sessionID.generation", "true");
		factory = AgileSessionFactory.getInstance(URL);
		HashMap params = new HashMap();
		params.put(AgileSessionFactory.USERNAME, USERNAME);
		params.put(AgileSessionFactory.PASSWORD, PASSWORD);
		session = factory.createSession(params);
		return session;
	}
	
	private static String getSpecificCellValue(Row row, int index) throws Exception {
		String cellValue = "";
		Cell cell = row.getCell(index);
		if(cell==null)
			return cellValue;
		int cellType = cell.getCellType();
		switch(cellType) {
			case Cell.CELL_TYPE_STRING:
				cellValue = cell.getStringCellValue().trim();
				break;
			case Cell.CELL_TYPE_NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
                    cellValue = cell.getDateCellValue().toString();
                } else {
                    cellValue = String.valueOf((int)cell.getNumericCellValue());
                }
                break;
			case Cell.CELL_TYPE_BLANK:
				break;
		}
		return cellValue;
	}
	
	
}
