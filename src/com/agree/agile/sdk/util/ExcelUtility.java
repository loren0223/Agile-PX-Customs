//: ExcelUtility.java
package com.agree.agile.sdk.util;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

/**
 * Copyright 2015 AdvancedTEK International Corporation, 8F, No.303, Sec. 1, 
 * Fusing S. Rd., Da-an District, Taipei City 106, Taiwan(R.O.C.); Telephone
 * +886-2-2708-5108, Facsimile +886-2-2754-4126, or <http://www.advtek.com.tw/>
 * All rights reserved.
 * @author Loren.Cheng
 * @version 0.1
 */
public class ExcelUtility {
	/**
	 * 
	 * @param filepath
	 * @param sheetName
	 * @return sheet
	 * @throws Exception
	 */
	public static Sheet getDataSheet(String filepath, String sheetName) throws Exception {
		FileInputStream fileInput = null;
		Workbook workbook = null;
		Sheet sheet = null;
		try {
			//讀Excel
			File file = new File(filepath);
			//如果檔案不存在
		    if (!file.exists()) 
	    		throw new Exception("{ERROR}:客製程式組態檔案不存在!!!:"+filepath);
			//讀Excel檔
	    	fileInput = new FileInputStream(file);
		    //取得Workbook
		    workbook = new HSSFWorkbook(fileInput);
		    //取得Sheet
		    sheet = workbook.getSheet(sheetName);
		    //System.out.println("sheet size is "+sheet.getLastRowNum());
		} catch(Exception e) {
			throw e;
		} finally {
			workbook = null;
			fileInput = null;
		}
		return sheet;
	}
	/**
	 * 
	 * @param row
	 * @param index
	 * @return
	 * @throws Exception
	 */
	public static String getSpecificCellValue(Row row, int index) throws Exception {
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
	/**
	 * 
	 * @param sheet
	 * @param filterColIndex
	 * @param filterValue
	 * @return
	 * @throws Exception
	 */
	public static List<Row> filterDataSheet(Sheet sheet, int filterColIndex, String filterValue) throws Exception {
		List<Row> rowList = new ArrayList<Row>();
		try {
			//取得符合條件的Row，加入回傳List
			for(Row row : sheet) {
				for (Cell cell : row) {
					if (cell.getColumnIndex()==filterColIndex && cell.getStringCellValue().equals(filterValue)) {
						Row rowMatched = (Row) cell.getRow();
						if (rowMatched.getRowNum() != 0) { /* Ignore top row */
							rowList.add(rowMatched);
						}
					}                               
				}
			}
		} catch(Exception e) {
			throw e;
		}
		return rowList;
	}

}
///:~
