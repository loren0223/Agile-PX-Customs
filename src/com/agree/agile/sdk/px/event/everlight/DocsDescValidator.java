//: DocsDescValidator.java
package com.agree.agile.sdk.px.event.everlight;
import java.util.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;
/**
 * Change Status時觸發，檢查AffectedItems.Description，不得有任何警告(WARNING)描述。
 * 如果有發現違規物件，顯示警告訊息在Change Status彈出視窗，並停止Change Status動作。
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class DocsDescValidator implements IEventAction {
	ActionResult actionResult = null;
	String successLog = "SUCCESS - Document Description Validation Passed. ";
	String illegalItem = "";
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
 	    	//取得Affected Items Table
 	    	ITable table = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
 	    	//Loop每個Affected Item
 	    	Iterator it = table.iterator();
 	    	while(it.hasNext()) {
 	    		IRow row = (IRow)it.next();
 	    		//取得Item Number, Item Description, Item Base Class
 	    		String itemNumber = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue().toString();
 	    		String itemDesc = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_DESCRIPTION).getValue().toString();
 	    		IAgileClass spuerClass = row.getReferent().getAgileClass().getSuperClass();
 	    		//只檢查Document Type的Affected Item的Description是否有警告訊息
 	    		if(spuerClass.getAPIName().equals("DocumentsClass")) {
 	    			//如果有，將Item Number記錄在違規清單
 	    			if(itemDesc.startsWith("WARNING")) {
 	    				if(!illegalItem.equals("")) {
 	    					illegalItem += ", ";
 	    				}
 	    				illegalItem += itemNumber;
 	    			}
 	    		}
 	    	}
 	    	//如果違規清單是空的
			if(illegalItem.equals("")) {
				//產生成功的Action Result
				actionResult = new ActionResult(ActionResult.STRING, successLog);
			} 
			//如果違規清單不是空的，拋出警告訊息告知user，顯示在Change Status視窗。
			else 
			{
				throw new Exception("WARNING - There are documents of invalid description! (Document Number:"+illegalItem+")");
			}
			
	    } catch(Exception ex) {
	    	//產生失敗的Action Result
	    	actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
	    } 
		//回傳Event Action Result
 	    return new EventActionResult(request, actionResult);
	}

}

///:~
	