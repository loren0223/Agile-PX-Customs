//: DocsDescGenerator.java
package com.agree.agile.sdk.px.event.everlight;
import java.util.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;
/**
 * 根據EVL文件描述組成規則，自動產生文件描述.
 * <p>當文件的Event Type是Create Object、Update Title Block、Save As，觸發DocsDescGenerator產生文件描述。
 * 產生文件描述後，必須判斷其他文件的文件描述是否重複。如有重複要提示警告訊息告知user，警告訊息會直接記錄在Description欄位。<br>
 * <p>EVL_PX_CONFIG:文件描述組成規則設定文件<br>
 * Everlight_Docs_Description_Element.xls<br>
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class DocsDescGenerator implements IEventAction {
	final String EVL_PX_CONFIG = UtilConfigReader.readProperty("EVL_PX_CONFIG");
	final String SHEET_NAME = "Docs_Desc_Element";
	final String docDescElementSeparator = "_";
	ActionResult actionResult = null;
	String successLog = "SUCCESS - Document Description Generation Succeed. ";
	Sheet sheet = null;
	String docDesc = "";
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
 	    	//取得Document Number
 	    	String docNumber = doc.getCell(ItemConstants.ATT_TITLE_BLOCK_NUMBER).getValue().toString();
 	    	//取得Document Type資訊
 	    	String docType = doc.getCell(ItemConstants.ATT_TITLE_BLOCK_DOCUMENT_TYPE).getValue().toString(); 
			//debug output
			//System.out.println("docType is "+docType);
			/*
			 * 如果Event Type是Save As，取得另存的New Object，執行後續的文件描述變更作業
			 */
			if(eventType==EventConstants.EVENT_SAVE_AS_OBJECT) {
				ISaveAsEventInfo saveAs = (ISaveAsEventInfo) request;
				//取得New Object Number
				docNumber = saveAs.getNewNumber();
				//取得New Object 
				doc = (IItem)session.getObject(ItemConstants.CLASS_DOCUMENTS_CLASS, docNumber);
				//取的New Object Type
				docType = doc.getCell(ItemConstants.ATT_TITLE_BLOCK_DOCUMENT_TYPE).getValue().toString();
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
			 * Loop所有Elements
			 * 		判斷觸發Event的物件是否符合文件細項分類條件
			 * 			如果文件細項分類屬性、條件值都是空的，忽略文件細項分類屬性值判斷，直接套用Description Element組成文件描述，Loop停止.
			 * 			如果有符合文件細項分類條件，則按照Description Element組成文件描述，Loop停止.
			 * Loop結束
			 * 		結果沒有產生文件描述，拋出警告訊息告知user。警告訊息會直接記錄在Description欄位。
			 */
		    Iterator it = docDescElements.iterator();
		    while(it.hasNext()) {
		    	Row row = (Row)it.next();
		    	//取得文件細項分類屬性、條件值、Description Element
		    	String docCategoryAttrName = ExcelUtility.getSpecificCellValue(row, 1);
		    	String docCategoryAttrValue = ExcelUtility.getSpecificCellValue(row, 2);
		    	String docDescElement = ExcelUtility.getSpecificCellValue(row, 3);
		    	//debug
		    	//System.out.println("docCategoryAttrName is "+docCategoryAttrName);
		    	//System.out.println("docCategoryAttrValue is "+docCategoryAttrValue);
		    	//System.out.println("docDescElement is "+docDescElement);
		    	//如果文件細項分類屬性、條件值都是空的
		        if(docCategoryAttrName.equals("") && docCategoryAttrValue.equals("")) {
		    		docDesc = generateDocDesc(doc, docDescElement);
		    		break;
		    	}
		        //如果文件細項分類屬性、條件值，任何一個是空的，拋出警告訊息告知user。警告訊息會直接記錄在Description欄位。
		        else if(!docCategoryAttrName.equals("") && docCategoryAttrValue.equals("")) {
		        	throw new Exception("WARNING - DOC_DESC_ELEMENT_CONFIG Lost!(文件細項分類條件值遺失!) (Document Type:"+docType+")");
		        }else if(docCategoryAttrName.equals("") && !docCategoryAttrValue.equals("")) {
		        	throw new Exception("WARNING - DOC_DESC_ELEMENT_CONFIG Lost!(文件細項分類屬性名稱遺失!) (Document Type:"+docType+")");
		        }
		        //如果皆不為空
		        else 
		        {
		    		//取得觸發Event的物件的文件細項分類屬性
		    		ICell cell = doc.getCell(docCategoryAttrName);
		    		//如果欄位不存在，拋出警告訊息告知user。警告訊息會直接記錄在Description欄位。
		    		if(cell==null) {
		    			throw new Exception("WARNING - Required Document Attribute does not exist!(文件細項分類屬性不存在!) (Attribute Name:"+docCategoryAttrName+")");
		    		}
		    		//取得物件實際的文件細項分類屬性值
		    		String actualDocCategoryAttrValue = cell.getValue().toString();
		    		//如果實際值=條件值，按照Description Element組成文件描述
		    		if(actualDocCategoryAttrValue.equals(docCategoryAttrValue)) {
		    			docDesc = generateDocDesc(doc, docDescElement);
		    			break;
		    		}
		    	}
		    }
		    
			/*
			 * 如果有產生文件描述
			 */
			if(!docDesc.equals("")) {
	    		//判斷其他文件的文件描述是否重複
				String existingDocName = checkExistingDocOfDuplicateDocDesc(session, docNumber, docDesc);
				//如果有，拋出警告訊息告知user。警告訊息會直接記錄在Description欄位。
				if(!existingDocName.equals("")) {
					throw new Exception("WARNING - Duplicate Document Description! (Document Type:"+docType+", Existing Document:"+existingDocName+")");
				}
				//變更觸發Event的物件的文件描述
				doc.setValue(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION, docDesc);
				//產生成功的Action Result
	    		actionResult = new ActionResult(ActionResult.STRING, successLog);
	    	} 
	    	/*
	    	 * 如果沒有產生文件描述...
	    	 */
			else 
	    	{
				//拋出警告訊息告知user。警告訊息會直接記錄在Description欄位。
    			throw new Exception("WARNING - There Is No Valid Document Description Element in EVL-PX-CONFIG!");
	    	}
	    } catch(Exception ex) {
	    	//產生失敗的Action Result
	    	actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
	    	//將警告訊息直接記錄在物件的Description欄位。
	    	try {
	    		doc.setValue(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION, ex.getMessage());
	    	} catch(APIException apiEx) {
	    		apiEx.printStackTrace();
	    	}
	    } finally {
			sheet = null;
		}
		//回傳 Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	
	/**
	 * 根據Description Element組成文件描述.
	 * @param doc 觸發Event的Document物件
	 * @param docDescElement 文件描述組成規則
	 * @return 文件描述
	 * @throws Exception throw when exception is happened
	 */
	public String generateDocDesc(IItem doc, String docDescElement) throws Exception {
		String result = "";
		try {
			//用文件描述分隔字元分解文件描述組成規則
			String[] elements = docDescElement.split(docDescElementSeparator);
			//按照先後順序組成文件描述
			for(String element : elements) {
				//如果回傳結果不是空的，就在字串後面加一個分隔字元
				if(!result.equals("")) {
					result += docDescElementSeparator;
				}
				//尋找子組成規則字串是否有成對的中括號符號([])
				int head = element.indexOf("[");
				int foot = element.indexOf("]");
				//如果有，取得[]中的字串當作屬性名稱，取得Document物件的屬性值，再用屬性值取代[]中的字串。
				if(head!=-1 && foot!=-1) {
					//取得屬性名稱
					String attrName = element.substring(head+1, foot);
					//debug
					//System.out.println("#### Attribute Name is "+attrName+" ####");
					//取得屬性欄位
					ICell cell = doc.getCell(attrName);
					//如果文件描述組成欄位無法取得，拋出警告訊息告知user。警告訊息會直接記錄在Description欄位。
					if(cell==null) {
						throw new Exception("WARNING - Required Document Attribute does not exist!(Attribute Name:"+attrName+")");
					}
					//取得屬性值
					String attrValue = cell.getValue().toString();
					//debug
					//System.out.println("#### Attribute Value is "+attrValue+" ####");
					//如果文件描述組成欄位的值是空的，拋出警告訊息告知user。警告訊息會直接記錄在Description欄位。
					if(attrValue.equals("")) {
						throw new Exception("WARNING - Required Document Attribute Value is empty!(Attribute Name:"+attrName+")");
					}
					//串聯文件描述字串 (用屬性值取代[]中的字串)
					result += element.substring(0,head) + attrValue + element.substring(foot+1,element.length());
				}
				//如果沒有，直接用子組成規則字串當作文件描述
				else {
					//串聯文件描述字串
					result += element;
				}
			}
			//回傳結果
			return result;
		} catch(Exception e) {
			throw e;
		}
	}
	
	/**
	 * 
	 * 判斷其他文件的文件描述是否重複。如果有，回傳文件編號。
	 * @param session Agile User Session
	 * @param docNumber 觸發Event的文件編號
	 * @param docDesc 組成的文件描述
	 * @return 相同文件描述的文件編號
	 * @throws Exception throw when exception is happened
	 */
	public String checkExistingDocOfDuplicateDocDesc(IAgileSession session, String docNumber, String docDesc) throws Exception {
		String result = "";
		try {
			//設定查詢條件(相同描述的文件)
			String condition = 
					"SELECT * " +
	        		"FROM " +
	        		"	[DocumentsClass] " +
	        		"WHERE " +
	        		"	[Title Block.Description] equal to %0 ";
			//建立查詢物件
	        IQuery query = (IQuery)session.createObject(IQuery.OBJECT_TYPE, condition);
	        //設定查詢條件值
	        query.setParams(new Object[]{docDesc});
	        //執行查詢得到結果
	        ITable results = query.execute();
	        //紀錄查詢到的文件編號
	        Iterator<IRow> it = results.iterator();
	        while(it.hasNext()) {
	        	//查詢到的文件物件
	        	IItem item = (IItem)(it.next()).getReferent();
	        	//取得文件編號
	        	String itemNumber = item.getName();
	        	//只記錄不同於"觸發Event的文件物件"的文件編號
	        	if(!itemNumber.equals(docNumber)) {
	        		if(!result.equals("")) {
		        		result += ",";
		        	}
	        		//紀錄文件編號(逗號分隔)
		        	result += item.getName();
	        	}
	        }
	        return result;
		} catch(Exception ex) {
			throw ex;
		}
	}

}

///:~
	