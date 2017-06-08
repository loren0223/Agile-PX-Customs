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

public class SetupPartsSubclassPageTwoVisibility {
	//�R�A�ܼƫŧi
	public static final String USERNAME = UtilConfigReader.readProperty("USERNAME");
	public static final String PASSWORD = UtilConfigReader.readProperty("PASSWORD");
	public static final String URL = UtilConfigReader.readProperty("URL");
	public static final String XLS_IMPORT = UtilConfigReader.readProperty("DATASHEET");
	public static final String SUBCLASS_SHEET = UtilConfigReader.readProperty("SUBCLASS_SHEET");
	public static final String PAGE_TWO_SHEET = UtilConfigReader.readProperty("PAGE_TWO_SHEET");
	public static final String DB_URL = UtilConfigReader.readProperty("DB_URL");
	public static final String DB_USER = UtilConfigReader.readProperty("DB_USER");
	public static final String DB_PASSWORD = UtilConfigReader.readProperty("DB_PASSWORD");
	public static IAgileSession session = null;
	public static AgileSessionFactory factory;
	
	public SetupPartsSubclassPageTwoVisibility() { }
	
	public static void main(String[] args) {
		//�ŧiLog4j Logger
		Logger logger = Logger.getRootLogger();
	    //�ŧi�ܼ�
		SimpleLayout layout = new SimpleLayout();
		FileInputStream fileInput = null;
		//POIFSFileSystem poiFileSys = null;
		Workbook workbook = null;
		Sheet subclassSheet = null;
		Sheet pageTwoSheet = null;
		String fileExtension = null;
		//Flag:�ɮ׸��|�O�_���T
	    boolean isFilepathRight = false;
	    //Map:Subclass/Category/API Name
	    Map subclassMap = new HashMap();
	    //DB Connection
	    Connection conn = null;
	    Statement queryStat = null, updateStat = null, stat = null;
	    ResultSet rs = null;
	    
	    //Logging
	    //System.out.println("Continue to input the required information...");
	    while(isFilepathRight==false) {
	    	//java.io.InputStream in = System.in;
	    	//�п�JAdmin Data�ӷ��ɮ׸��|�C
	        //System.out.println("Please enter the filepath of Admin Data:");
		    try {
			    //���o�ɮ�
		    	//BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
			    //XLS_IMPORT = bufferRead.readLine().trim();
			    File directory = new File(XLS_IMPORT);
			    //���o���ɦW
			    fileExtension = XLS_IMPORT.substring(XLS_IMPORT.lastIndexOf(".")).trim();
		    	//�p�G�ɮפ��s�b
			    if (!directory.exists()) {
		    		System.out.println("Error: File does not exist, please try again!");
					continue;
		        }
			    //�p�G�S�����ɦW,�άO���ɦW���O.xls/.xlsx
			    if(XLS_IMPORT.lastIndexOf(".")==-1 || !fileExtension.equals(".xls")) {
			    	System.out.println("Error: Filename extension[.xls/.xlsx] is requried, please try again!");
			    	//throw new Exception("Error: [" + XLS_IMPORT + "] filename extension is requried!");
			    	continue;
			    } else {
			    	//if success to read the AttributeImporter path
				    isFilepathRight = true;
			    	//ŪExcel��
			    	fileInput = new FileInputStream(XLS_IMPORT);
				    // POI File System - to Access MS Format File
				    //poiFileSys = new POIFSFileSystem(fileInput);
				    //���oWorkbook
				    if(fileExtension.equals(".xls")) {
				    	workbook = new HSSFWorkbook(fileInput);
				    }
				    else if(fileExtension.equals(".xlsx")) {
				    	workbook = new XSSFWorkbook(fileInput);
				    }
				    //���oSheet
				    subclassSheet = workbook.getSheet(SUBCLASS_SHEET);
				    pageTwoSheet = workbook.getSheet(PAGE_TWO_SHEET);
			    }
			    
			    /***�}�lSubclass Page Two�]�w***/
			    try {
					logger.info("**************START**************");
					logger.info("Starting to set page two of subclass. Filepath:" + XLS_IMPORT);
					//�إ�Agile�s�u
		            session = connect(session);
		            //���oAdmin Node
				    IAdmin admin = session.getAdminInstance();
				    //���oList Library
				    IListLibrary lib = admin.getListLibrary();	   
				    //Get DB Connection
				    conn = getConnection();
				    
			    	//���oSubclass Category,Name,API Name
				    //�Ĥ@�C�OTitle�A�q�ĤG�C�}�l�B�z�C
				    //Excel Sheet Schema (Parts.Subclass)
				    //Column 0:Part Category
				    //Column 2:Code
				    //Column 3:Part Subclass
				    //Column 4:Description
				    //Column 5:API Name
				    //Column 7:Subclass ID
				    for (int i=1; i<=subclassSheet.getLastRowNum(); i++) {
				    	//���oRow(i)
				    	int xlsRowNo = i+1;
				    	Row row = subclassSheet.getRow(i);
				    	logger.info("Subclass Datarow No."+xlsRowNo);
				    	
				    	/***���o��ƦC���Ҧ������***/
				    	String partCategory = getSpecificCellValue(row, (int)0);
				    	//String code = getSpecificCellValue(row, (int)2);
				    	String partSubclass = getSpecificCellValue(row, (int)3);
				    	//String description = getSpecificCellValue(row, (int)4);
				    	String partAPIName = getSpecificCellValue(row, (int)5);
				    	String subclassID = getSubclassId(conn, queryStat, rs, partAPIName);
				    	
				    	logger.info("\tPart Category: " + partCategory);
				    	//logger.info("\tCode: " + code);
				    	logger.info("\tPart Subclass: " + partSubclass);
				    	//logger.info("\tDescription: " + description);
				    	logger.info("\tPart API Name: " + partAPIName);
				    	logger.info("\tSubclass ID: " + subclassID);
				    	
				    	//�p�GPart Category�O�šA�ߥX�ҥ~
				    	if(partCategory==null || partCategory.equals("")) {
				    		throw new Exception("Part Category is required! Row_Index:"+xlsRowNo);
				    	}
				    	//�p�GPart Subclass�O�šA�ߥX�ҥ~
				    	if(partSubclass==null || partSubclass.equals("")) {
				    		throw new Exception("Part Subclass is required! Row_Index:"+xlsRowNo);
				    	}
				    	/**
				    	//�p�GAPI Name�O�šA�ߥX�ҥ~
				    	if(partAPIName==null || partAPIName.equals("")) {
				    		throw new Exception("API Name is required! Row_Index:"+xlsRowNo);
				    	}
				    	**/
				    	//�p�GSubclass ID�O�šA�ߥX�ҥ~
				    	if(subclassID==null || subclassID.equals("")) {
				    		throw new Exception("Subclass ID is required! Row_Index:"+xlsRowNo);
				    	}
				    	
				    	
  			    	    //���oSubclass Page Two
				    	//�Ĥ@�C�OTitle�A�q�ĤG�Ҷ}�l
				    	//Excel Sheet Schema (Parts.Page Two)
					    //Column 0:Part Categories that Visible=Yes
					    //Column 1:Attribute Name
					    //Column 2:Attribute Description
					    //Column 3:API Name
				    	//Column 19:Base ID
				    	for(int j=1; j<=pageTwoSheet.getLastRowNum(); j++) {
				    		//���oRow(i)
					    	int xlsRowNoJ = j+1;
					    	Row rowj = pageTwoSheet.getRow(j);
					    	if(rowj==null)
					    		break;
					    	logger.info("\tPage Two Datarow No."+xlsRowNoJ);
					    	
					    	/***���o��ƦC���Ҧ������***/
					    	String partCategoryList = getSpecificCellValue(rowj, (int)0);
					    	String attrName = getSpecificCellValue(rowj, (int)1);
					    	//String attrDesc = getSpecificCellValue(rowj, (int)2);
					    	//String attrAPIName = getSpecificCellValue(rowj, (int)3);
					    	String attrBaseID = getSpecificCellValue(rowj, (int)19);
					    	
					    	logger.info("\t\tPart Category List: " + partCategoryList);
					    	logger.info("\t\tAttribute Name: " + attrName);
					    	//logger.info("\t\tAttribute Description: " + attrDesc);
					    	//logger.info("\t\tAttribute API Name: " + attrAPIName);
					    	logger.info("\t\tAttribute Base ID: " + attrBaseID);
					    						    	
					    	//�p�GAttribute Name�O�šA�ߥX�ҥ~
					    	if(attrName==null || attrName.equals("")) {
					    		throw new Exception("Attribute Name is required! Row_Index:"+xlsRowNoJ);
					    	}
					    	/**
					    	//�p�GAPI Name�O�šA�ߥX�ҥ~
					    	if(attrAPIName==null || attrAPIName.equals("")) {
					    		throw new Exception("Attribute API Name is required! Row_Index:"+xlsRowNoJ);
					    	}**/
					    	//�p�GAttribute Base Id�O�šA�ߥX�ҥ~
					    	if(attrBaseID==null || attrBaseID.equals("")) {
					    		throw new Exception("Attribute Base ID is required! Row_Index:"+xlsRowNoJ);
					    	}
					    	
					    	/**
					    	 * Connect Oracle database and get propertytable data
					    	 */
					    	//Get propertytabe data
						    String parentID = null;
						    queryStat = conn.createStatement();
				    		rs = queryStat.executeQuery(
				    			"select max(a.parentid) parentid from "+
				    				"(select * from propertytable where value = '"+attrBaseID+"' and propertyid = 953) a, "+
				    				"(select * from propertytable where value = '"+subclassID+"') b "+
				    			"where a.parentid = b.parentid ");
				    		while(rs.next()) { //��Ҧ�result loop �X�� 
				    			parentID = rs.getString("parentid");
				    		}
				    		rs.close();
				    		queryStat.close();
					    	/**
					    	 * ��p2 attr�n���:
					    	 * 		�ˬdpropertytable�O�_���]�w:
					    	 * 			�� --> �]��1
					    	 * 			�S�� --> ���B�z
					    	**/
					    	if(partCategoryList.lastIndexOf(partCategory)!=-1) {
					    		if(parentID != null) {
					    			updateStat = conn.createStatement();
					    			updateStat.executeUpdate(
					    				"update propertytable set value = '1' where parentid = "+parentID+" and propertyid = 9 ");
					    			updateStat.close();
					    		}
					    	}
					    	
					    	/**
					    	 * ��p2 attr���n���:
					    	 * 		�ˬdpropertytable�O�_���]�w:
					    	 * 			�� --> �]��0
					    	 * 			�S�� --> 
					    	 * 				nodetable�W�[�@������
					    	 * 					ID= MAX(ID<2000000000)+1
					    	 * 					ParentID=�T�w2000009631
					    	 * 					Description=Base ID
					    	 * 					OBJTYPE=�T�w275
					    	 * 					Inherit=�T�w0
					    	 * 					HelpID=�T�w0
					    	 * 					Version=0
					    	 * 					Name=Base ID
					    	 * 					Created=sysdate
					    	 * 					Last_upd=sysdate
					    	 *  			propertytable�W�[�������
					    	 *  				ID= MAX(ID)+5, MAX(ID)+4, MAX(ID)+3, MAX(ID)+2, MAX(ID)+1
					    	 *  				ParentID=Nodetable ID
					    	 *  				Readonly=0,0,0,0,0
					    	 *  				AttType=2,2,2,4,2
					    	 *  				DataType=1,1,1,1,1
					    	 *  				Selection=0,0,0,451,0
					    	 *  				Visible=1,1,1,1,1
					    	 *  				PropertyID=954,953,955,9,925
					    	 *  				Value=�T�w810,BaseID,SubclassID,0(�����),(��)
					    	 *  				Created=sysdate
					    	 *  				Last_upd=sysdate
					    	 *  			����nodetable,propertytable sequence no.
					    	 */
					    	if(partCategoryList.lastIndexOf(partCategory)==-1) {
					    		if(parentID != null) {
					    			updateStat = conn.createStatement();
					    			updateStat.executeUpdate(
					    				"update propertytable set value = '0' where parentid = "+parentID+" and propertyid = 9 ");
					    			updateStat.close();
					    		} else {
					    			//Get the next ID of nodetable for subclassP1P2attributes
					    			int nextNodeId = 0;
					    			queryStat = conn.createStatement();
					    			rs = queryStat.executeQuery("select max(id)+1 next_id from nodetable where id < 2000000000 ");
							    	while(rs.next()) { //��Ҧ�result loop �X�� 
							    		nextNodeId = rs.getInt("next_id");
							    	}
					    			rs.close();
					    			queryStat.close();
					    			//Get the MAX propertytable ID
					    			int maxPropertyId = 0;
					    			queryStat = conn.createStatement();
					    			rs = queryStat.executeQuery("select max(id) max_id from propertytable ");
							    	while(rs.next()) { //��Ҧ�result loop �X�� 
							    		maxPropertyId = rs.getInt("max_id");
							    	}
					    			rs.close();
					    			queryStat.close();
					    			//Batch update 
					    			conn.setAutoCommit(false);
					    			updateStat = conn.createStatement();
					    			//nodetable�W�[�@������
					    			updateStat.addBatch(
					    				"insert into nodetable (id, parentid, description, objtype, inherit, helpid, version, name, created, last_upd) "+
					    			    "values ("+nextNodeId+", 2000009631, '"+attrBaseID+"', 275, 0, 0, 0, '"+attrBaseID+"', sysdate, sysdate) ");
					    			//propertytable�W�[�������
					    			updateStat.addBatch(
					    				"insert into propertytable (id, parentid, readonly, atttype, datatype, selection, visible, propertyid, value, created, last_upd) "+
					    				"values ("+(maxPropertyId+5)+", "+nextNodeId+", 0, 2, 1, 0, 1, 954, '810', sysdate, sysdate) ");
					    			updateStat.addBatch(
						    			"insert into propertytable (id, parentid, readonly, atttype, datatype, selection, visible, propertyid, value, created, last_upd) "+
						    			"values ("+(maxPropertyId+4)+", "+nextNodeId+", 0, 2, 1, 0, 1, 953, '"+attrBaseID+"', sysdate, sysdate) ");
					    			updateStat.addBatch(
						    			"insert into propertytable (id, parentid, readonly, atttype, datatype, selection, visible, propertyid, value, created, last_upd) "+
						    			"values ("+(maxPropertyId+3)+", "+nextNodeId+", 0, 2, 1, 0, 1, 955, '"+subclassID+"', sysdate, sysdate) ");
					    			updateStat.addBatch(
						    			"insert into propertytable (id, parentid, readonly, atttype, datatype, selection, visible, propertyid, value, created, last_upd) "+
						    			"values ("+(maxPropertyId+2)+", "+nextNodeId+", 0, 2, 1, 0, 1, 9, '0', sysdate, sysdate) ");
					    			updateStat.addBatch(
						    			"insert into propertytable (id, parentid, readonly, atttype, datatype, selection, visible, propertyid, created, last_upd) "+
						    			"values ("+(maxPropertyId+1)+", "+nextNodeId+", 0, 2, 1, 0, 1, 925, sysdate, sysdate) ");
					    			updateStat.executeBatch();
					    			conn.commit();
					    			conn.setAutoCommit(true);
					    			updateStat.close();
					    			/**
					    			//����Nodetable,Propertytable��Sequence No.
					    			conn.setAutoCommit(false);
					    			stat = conn.createStatement();
					    			stat.addBatch("drop sequence SEQNODETABLE ");
					    			stat.addBatch("create sequence SEQNODETABLE minvalue 1 maxvalue 999999999999999999999999999 increment by 20 cache 20 noorder nocycle start with "+nextNodeId);
					    			stat.addBatch("drop sequence seqpropertytable ");
					    			stat.addBatch("create sequence seqpropertytable minvalue 1 maxvalue 999999999999999999999999999 increment by 20 cache 20 noorder nocycle start with "+(maxPropertyId+5));
					    			stat.executeBatch();
					    			conn.commit();
					    			conn.setAutoCommit(true);
					    			stat.close();
					    			**/
					    		}
					    		
					    	}
					    		
				    	}
				    	logger.info("Set subclass page two: [ " + partSubclass + " ](ID:" + subclassID +")" + " ] OK ");
				    }
				    //����Nodetable,Propertytable��Sequence No.
				    //	(1)Get the MAX ID of nodetable for subclassP1P2attributes
	    			int maxNodeTableId = 0;
	    			queryStat = conn.createStatement();
	    			rs = queryStat.executeQuery("select max(id) max_id from nodetable where id < 2000000000 ");
			    	while(rs.next()) { //��Ҧ�result loop �X�� 
			    		maxNodeTableId = rs.getInt("max_id");
			    	}
	    			rs.close();
	    			queryStat.close();
	    			//	(2)Get the MAX propertytable ID
	    			int maxPropertyTableId = 0;
	    			queryStat = conn.createStatement();
	    			rs = queryStat.executeQuery("select max(id) max_id from propertytable ");
			    	while(rs.next()) { //��Ҧ�result loop �X�� 
			    		maxPropertyTableId = rs.getInt("max_id");
			    	}
	    			rs.close();
	    			queryStat.close();
	    			//	(3)����SEQUENCE NO
				    conn.setAutoCommit(false);
	    			stat = conn.createStatement();
	    			stat.addBatch("drop sequence SEQNODETABLE ");
	    			stat.addBatch("create sequence SEQNODETABLE minvalue 1 maxvalue 999999999999999999999999999 increment by 20 cache 20 noorder nocycle start with "+maxNodeTableId);
	    			stat.addBatch("drop sequence seqpropertytable ");
	    			stat.addBatch("create sequence seqpropertytable minvalue 1 maxvalue 999999999999999999999999999 increment by 20 cache 20 noorder nocycle start with "+maxPropertyTableId);
	    			stat.executeBatch();
	    			conn.commit();
	    			conn.setAutoCommit(true);
	    			stat.close();
				    
	    			logger.info("Finish setting. ");
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
	
	public static Connection getConnection() throws Exception {
		Connection conn = null;
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
		} catch (Exception e) {
			throw e;
		}
		return conn;
	}
	
	public static String getSubclassId(Connection conn, Statement queryStat, ResultSet rs, String partAPIName) throws Exception {
		String subclassId = "";
		try {
			queryStat = conn.createStatement();
			rs = queryStat.executeQuery("select id from nodetable where parentid = 10004 and name = '"+partAPIName+"' ");
	    	while(rs.next()) { //��Ҧ�result loop �X�� 
	    		subclassId = rs.getString("id");
	    	}
			rs.close();
			queryStat.close();
		} catch (Exception e) {
			throw e;
		}
		return subclassId;
	}
}
