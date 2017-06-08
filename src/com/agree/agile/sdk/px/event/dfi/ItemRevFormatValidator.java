//: ItemRevFormatValidator.java
package com.agree.agile.sdk.px.event.dfi;
import java.util.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;
/**
 * Item版本格式驗證器.
 * <p>Item發行前檢查New Revision格式是否正確，正確的話才允許流程發行。<br>
 * <p>DFI_PX_CONFIG相關設定:<br>
 * Item Rev Format Validator
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class ItemRevFormatValidator implements IEventAction {
	final String DFI_PX_CONFIG = UtilConfigReader.readProperty("DFI_PX_CONFIG");
	final String SHEET_NAME = "Item Rev Format Validator";
	ActionResult actionResult = null;
	String successLog = "Nothing to do.";
	StringBuffer revFormatErrorLog = new StringBuffer();
	StringBuffer revSeqErrorLog = new StringBuffer();
	Sheet sheet = null;
	
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
 	    	//轉型Change物件
 	    	IChange change = (IChange)object.getDataObject();
 	    	//取得Event Type
 	    	int eventType = object.getEventType();
 	    	//取得Change Type資訊
 	    	String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString(); 
			//取得Change API Prefix(3 digits)
 	    	String changeAPIName = change.getAgileClass().getAPIName();
			String changeAPIPrefix = changeAPIName.substring(0, 3);
 	    	
			//System.out.println("changeType is "+changeType);
			//System.out.println("changeAPIPrefix is "+changeAPIPrefix);
			
			/*
			 * 讀 DFI_PX_CONFIG - Item Rev Format Validator
			 */
			sheet = ExcelUtility.getDataSheet(DFI_PX_CONFIG, SHEET_NAME);
			/*
			 * 取得符合Change API Prefix的Item Revision Format RegEx
			 */
			//Excel Column Header: |Change API Prefix|AI Class API Name|AI Rev Format RegEx|
			List<Row> revFormatRegExSheet = ExcelUtility.filterDataSheet(sheet, 0, changeAPIPrefix);
			
		    /*
		     * 取得Affected Items Table
		     */
		    ITable changeAITable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator<IRow> changeAI = changeAITable.iterator();
		    /*
		     * 檢查Item Rev Format & Rev Order
		     */
			validateItemRevFormat(changeAI, revFormatRegExSheet);
			/*
			 * 產生 Action Result Message
			 */
			//System.out.println("ErrorLog is "+failLog.toString());
			//沒有錯誤
			if(revFormatErrorLog.toString().equals("") && revSeqErrorLog.toString().equals("")) {
	    		//產生成功訊息
				successLog = "Item Revison Validation is OK.";
	    		//Action result is successful.
	    		actionResult = new ActionResult(ActionResult.STRING, successLog);
	    	} 
	    	//如果有錯誤發生
	    	else {
	    		//Combine錯誤訊息
	    		String revErrorLog = "";
	    		//New Rev Format有錯誤
	    		if(!revFormatErrorLog.toString().equals("")) {
		    		//錯誤訊息補上前面說明
		    		revFormatErrorLog.insert(0,"WARNING: Wrong New Revision Format of Affected Items. New Revision Format should be "+ getRevFormatRegEx(revFormatRegExSheet) +"."+
		    				"Affected Items List: [" );
		    		//錯誤訊息補上後面括號
		    		revFormatErrorLog.append("]");
		    		//
		    		revErrorLog += revFormatErrorLog.toString();
    			}
	    		//New Rev Seq有錯誤
	    		if(!revSeqErrorLog.toString().equals("")) {
		    		//錯誤訊息補上前面說明
	    			revSeqErrorLog.insert(0,"WARNING: New Revision should be greater than Old Revision. "+
		    				"Affected Items List: [" );
		    		//錯誤訊息補上後面括號
	    			revSeqErrorLog.append("]");
	    			//
	    			if(!revErrorLog.equals("")) {
	    				revErrorLog += " ； ";
	    			}
	    			revErrorLog += revSeqErrorLog.toString();
	    		}
	    		//拋出錯誤Exception
	    		throw new Exception(revErrorLog);
	    	}
	    } catch(Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
			sheet = null;
		}
		//傳回Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	
	/**
	 * 取得Change物件的屬性值.
	 * @param change Change物件
	 * @param APIName Change屬性API Name
	 * @return Change物件的屬性值      
	 * @throws Exception throw when exception is happened
	 */
	public String getChangeAttributeValue(IChange change, String APIName) throws Exception {
		try {
			//回傳值初始化
			String result = "";
			result = change.getValue(APIName).toString();
			return result;
		} catch(Exception e) {
			throw e;
		}
	}
	
	/**
	 * 驗證Item New Rev是否合法.
	 * @param changeAI Change物件Affected Item Table的Iterator 
	 * @param revFormatRegExSheet DFI_PX_CONFIG的Item Rev Format RegEx列表
	 * @throws Exception throw when exception is happened
	 */
	private void validateItemRevFormat(Iterator<IRow> changeAI, List<Row> revFormatRegExSheet) throws Exception {
		try {
			/*
			 * Loop Affected Item Table
			 */
			while(changeAI.hasNext()) {
				//取得AI info
				IRow ai = changeAI.next();
				String aiNewRev = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_REVISION).getValue()==null? "" : ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_REVISION).getValue().toString();
				String aiOldRev = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_REV).getValue()==null? "" : ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_REV).getValue().toString();
				String aiClassAPIName = ai.getReferent().getAgileClass().getSuperClass().getAPIName();
				String itemNumber = ai.getReferent().getName();
				//debug:如果開帳版本只有大版本，補兩位小版本00
				if(aiOldRev.length()==3) {
					aiOldRev += "00";
				}
				//debug
				//System.out.println("aiNewRev is "+aiNewRev);
				//System.out.println("aiOldRev is "+aiOldRev);
				//初始化Check-Flag (default=false)
				boolean isRegExPassed = false;
				boolean isRevSeqPassed = false;
				/*
				 * 比對此Change Type的所有RegEx，檢查New Rev Format是否符合
				 */
				Iterator<Row> revFormatRegExs = revFormatRegExSheet.iterator();
				//System.out.println("revFormatRegExs size is "+revFormatRegExs.size());
				while(revFormatRegExs.hasNext()) {
					Row revFormatRegEx = revFormatRegExs.next();
					//從DFI_PX_CONFIG Excel 取得 AI Class API Name & Rev Format RegEx
					String valAIClassAPIName = ExcelUtility.getSpecificCellValue(revFormatRegEx, 1);
					String valAIRevFormatRegEx = ExcelUtility.getSpecificCellValue(revFormatRegEx, 2);
					//System.out.println("valAIClassAPIName is "+valAIClassAPIName);
					//System.out.println("valAIRevFormatRegEx is "+valAIRevFormatRegEx);
					
					/*
					 * AI-Class = Parts
					 */
					if(aiClassAPIName.equals(valAIClassAPIName)) {
						/*
						 * 如果Item New Rev不符合DFI版本格式，檢核不通過，加入錯誤清單
						 */
						if(aiNewRev.matches(valAIRevFormatRegEx)) {
							isRegExPassed = true;
							//停止比對RegEx
							break;
						}
					}
					/*
					 * AI-Class = Documents
					 */
					else {
						//Document不比較版本格式
						isRegExPassed = true;
						//停止比對RegEx
						break;
					}
				}
				/*
				 * 如果Old Rev不為空，且New Rev <= Old Rev，檢核不通過，加入錯誤清單
				 */
				if(!aiOldRev.equals("")) {
					//debug
					//System.out.print("aiNewRev.compareTo(aiOldRev) is "+aiNewRev.compareTo(aiOldRev));
					if(aiNewRev.compareTo(aiOldRev) > 0) {
						isRevSeqPassed = true;
					}
				} else {
					isRevSeqPassed = true;
				}
				
				//如果沒有通過Rev Format >> 記錄ItemNo到revFormatErrorLog
				if(!isRegExPassed) {
					revFormatErrorLog.append(itemNumber+"  ");
				}
				
				//如果沒有通過Rev Seq檢查  >> 記錄ItemNo到revSeqErrorLog
				if(!isRevSeqPassed) {
					revSeqErrorLog.append(itemNumber+"  ");
				}
			}
		} catch(Exception e) {
			throw e;
		}
	}
	/**
	 * 回傳合法的New Rev的常規表示式.
	 * @param revFormatRegExSheet 適用這個Change的New Rev Format的DFI_PX_CONFIG的Excel Row
	 * @return 合法的New Rev的常規表示式
	 * @throws Exception throw when exception is happened
	 */
	private String getRevFormatRegEx(List<Row> revFormatRegExSheet) throws Exception {
		try {
			String revFormatRegEx = "";
			Iterator<Row> it = revFormatRegExSheet.iterator();
			while(it.hasNext()) {
				Row row = it.next();
				String valAIRevFormatRegEx = ExcelUtility.getSpecificCellValue(row, 2);
				revFormatRegEx += valAIRevFormatRegEx;
				if(it.hasNext()){
					revFormatRegEx += " | ";
				}
			}
			return revFormatRegEx;
		} catch(Exception e) {
			throw e;
		}
	}
}

///:~
	