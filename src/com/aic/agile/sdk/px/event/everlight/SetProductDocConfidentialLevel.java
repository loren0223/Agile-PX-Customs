//: SetProductDocConfidentialLevel.java
package com.aic.agile.sdk.px.event.everlight;
import java.util.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import com.agile.api.*;
import com.agile.px.*;
import com.aic.agile.sdk.util.*;
/**
 * 根據D01-Product Document的產品文件類型，變更PageThree.機密等級。
 * <p>EVL_PX_CONFIG:文件描述組成規則設定文件<br>
 * Everlight_Docs_Description_Element.xls<br>
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class SetProductDocConfidentialLevel implements IEventAction {
	final String EVL_PX_CONFIG = UtilConfigReader.readProperty("EVL_PX_CONFIG");
	final String SHEET_NAME = "Docs_Desc_Element";
	final String docDescElementSeparator = "_";
	ActionResult actionResult = null;
	String successLog = "SUCCESS - Confidential Level Configuration Succeed. ";
	Sheet sheet = null;
	IItem doc = null;
	int eventType = 0;
	/** 
	 * Agile PLM Event PX 進入點.
	 * @param session 使用者Session
	 * @param actionNode 觸發程式的位置
	 * @param request Event物件
	 */
	public EventActionResult doAction(IAgileSession session, INode actionNode, IEventInfo request) {
		try {
 	    	//取得觸發Event的物件
 	    	IObjectEventInfo object = (IObjectEventInfo) request;
 	    	//轉型Item物件
 	    	doc = (IItem)object.getDataObject();
 	    	//取得Event Type
 	    	eventType = object.getEventType();
 	    	//取得Document Type資訊
 	    	String docType = doc.getCell(ItemConstants.ATT_TITLE_BLOCK_DOCUMENT_TYPE).getValue().toString(); 
			//debug output
			//System.out.println("docType is "+docType);
			/*
			 * 非D01-Product Document直接結束不處理
			 */
			if(!docType.equals("D01-Product Document產品文件")) {
				actionResult = new ActionResult(ActionResult.STRING, "Nothing to do. (Document Type:"+docType+")");
				//傳回Event Action Result
		 	    return new EventActionResult(request, actionResult);
			}
			/*
			 * 讀取文件描述組成規則 (EVL_PX_CONFIG.Docs_Decription_Element)
			 * Excel Column Header: 文件類型(Subclass Name)|文件細項分類(Attribute Display Name)|文件分類屬性值|Description Element|機密等級
			 */
			sheet = ExcelUtility.getDataSheet(EVL_PX_CONFIG, SHEET_NAME);
			/*
			 * 取得符合觸發Event的物件的Document Type的Elements
			 */
			List<Row> docDescElements = ExcelUtility.filterDataSheet(sheet, 0, docType);
			/*
			 * Loop所有Elements: 根據文件細項分類屬性條件值決定機密等級。
			 */
		    Iterator it = docDescElements.iterator();
		    while(it.hasNext()) {
		    	Row row = (Row)it.next();
		    	//取得文件細項分類屬性名稱、條件值、機密等級
		    	String docCategoryAttrName = ExcelUtility.getSpecificCellValue(row, 1);
		    	String docCategoryAttrValue = ExcelUtility.getSpecificCellValue(row, 2);
		    	String confidentialLevel = ExcelUtility.getSpecificCellValue(row, 4);
		    	//debug
		    	//System.out.println("docCategoryAttrName is "+docCategoryAttrName);
		    	//System.out.println("docCategoryAttrValue is "+docCategoryAttrValue);
		    	//System.out.println("confidentialLevel is "+confidentialLevel);
		    	
		    	//如果文件細項分類屬性名稱、條件值，任何一個是空的，拋出錯誤訊息
		    	if(!docCategoryAttrName.equals("") && docCategoryAttrValue.equals("")) {
		        	throw new Exception("WARNING - DOC_DESC_ELEMENT_CONFIG Lost!(文件細項分類條件值遺失!) (Document Type:"+docType+")");
		        }else if(docCategoryAttrName.equals("") && !docCategoryAttrValue.equals("")) {
		        	throw new Exception("WARNING - DOC_DESC_ELEMENT_CONFIG Lost!(文件細項分類屬性名稱遺失!) (Document Type:"+docType+")");
		        }
		        //如果皆不為空
		        else if(!docCategoryAttrName.equals("") && !docCategoryAttrValue.equals("")) {
		        	//取得觸發Event的物件的文件細項分類屬性
		    		ICell productDocumentType = doc.getCell(docCategoryAttrName);
		    		//如果欄位不存在，拋出警告訊息
		    		if(productDocumentType==null) {
		    			throw new Exception("WARNING - Required Document Attribute does not exist!(產品文件類型屬性不存在!) (Attribute Name:"+docCategoryAttrName+")");
		    		}
		    		//取得物件實際的文件細項分類屬性值
		    		String actualDocCategoryAttrValue = productDocumentType.getValue().toString();
		    		//如果實際值=條件值，變更機密等級
		    		if(actualDocCategoryAttrValue.equals(docCategoryAttrValue)) {
		    			ICell levelCell = doc.getCell(ItemConstants.ATT_PAGE_THREE_LIST05);
		    			IAgileList values = levelCell.getAvailableValues();
		    			values.setSelection(new Object[]{confidentialLevel});
		    			levelCell.setValue(values);
		    			//產生成功的Action Result
		    			actionResult = new ActionResult(ActionResult.STRING, successLog);
		    			//回傳Event Action Result
		    			return new EventActionResult(request, actionResult);
		    		}
		        }
		    }
		} catch(Exception ex) {
	    	//產生失敗的Action Result
	    	actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
	    } finally {
			sheet = null;
		}
		//傳回Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	
}

///:~
	