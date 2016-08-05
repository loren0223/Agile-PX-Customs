package com.aic.agile.sdk.api.export;
import java.util.*;

import com.agile.api.*;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;

import com.aic.agile.sdk.util.*;

public class BatchDataObjectExporter {
	//靜態變數宣告
	public static final String USERNAME = UtilConfigReader.readProperty("USERNAME");
	public static final String PASSWORD = UtilConfigReader.readProperty("PASSWORD");
	public static final String URL = UtilConfigReader.readProperty("URL");
	public static final String XLS_IMPORT = UtilConfigReader.readProperty("DATASHEET");
	public static final String SUBCLASS_SHEET = UtilConfigReader.readProperty("SUBCLASS_SHEET");
	public static final String PAGE_TWO_SHEET = UtilConfigReader.readProperty("PAGE_TWO_SHEET");
	public static IAgileSession session = null;
	public static AgileSessionFactory factory;
	
	public BatchDataObjectExporter() { }
	
	public static void main(String[] args) {
		try {
			//宣告Log4j Logger
			Logger logger = Logger.getRootLogger();
			//
			//建立Agile連線
            session = AgileSessionUtility.getAdminSession();
			//
			IItem item = (IItem) session.getObject(IItem.OBJECT_TYPE, "P00014"); 
			if(item == null) { 
				// throw an error, the part wasn't found
				throw new Exception("No data object found.");
			}
			System.out.println("GET ITEM");
			IDataObject[] expObjs = {item}; 
			String[] filters = {"Default Item Filter","Default Manufacturer Part Filter","Default Manufacturer Filter"}; 
			//
			IExportManager eMgr = (IExportManager) session.getManager(IExportManager.class); 
			try{ 
				byte[] exportData = eMgr.exportData(expObjs, ExportConstants.EXPORT_FORMAT_PDX, filters); 
				if (exportData != null) { 
					String fileName = Long.toString(System.currentTimeMillis()); 
					FileOutputStream outputFile = new FileOutputStream("D:/"+fileName+".pdx"); 
					outputFile.write(exportData); 
					outputFile.close(); 
					System.out.println("Data exported to file: " + fileName);
				}
			} catch(Throwable t) { 
				throw t;
				// error handling
			}
			System.out.println("EXPORT DONE");
		} catch(Exception ex) {
			ex.printStackTrace();
		}
    }
	
	
	
	
}
