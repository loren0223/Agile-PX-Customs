//: ProductSeriesUpdator.java
package com.agree.agile.sdk.px.event.dfi;
import com.agile.api.*;
import com.agile.px.*;

/**
 * PRP(PDS Document Release Form)流程發行後，將P3.Product Series欄位值寫入List:[DFI] Product Series.
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class ProductSeriesUpdator implements IEventAction {
	ActionResult actionResult = null;
	String actionFlag = null;
	
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
 	    	//區別Event Type
 	    	//int eventType = object.getEventType();
 	    	//取得Change Type資訊
			String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString(); 
			//取得Change API Prefix(3 digits)
 	    	String changeAPIName = change.getAgileClass().getAPIName();
			String changeAPIPrefix = changeAPIName.substring(0, 3);
			
			//System.out.println("changeType is "+changeType);
			//System.out.println("changeAPIPrefix is "+changeAPIPrefix);
			
			/*
			 * 取得PRL.Product Series
			 */
			String productSeries = change.getCell(ChangeConstants.ATT_PAGE_THREE_TEXT01).getValue().toString();
			
			//System.out.println("productSeries is "+productSeries);
			
			/*
			 * 呼叫List-[DFI] Product Series
			 */
			IAdmin admin = session.getAdminInstance();
		    IListLibrary listLib = admin.getListLibrary();
		    //Get admin list with API-Name
		    IAdminList productSeriesList = listLib.getAdminList("DFI_PRODUCT_SERIES");
    		
		    //System.out.println("productSeriesList is "+productSeriesList.getAPIName());
		    
			/*
			 * 判斷 PRL.Product Series 是否存在 [DFI] Product Series: Yes 忽略; No 新增。
			 */
    		boolean updated = false;
    		IAgileList listValues = productSeriesList.getValues();
    		if(listValues.getChild(productSeries) == null) {
    			listValues.addChild(productSeries);
    			productSeriesList.setValues(listValues);
    			updated =true;
    		}
    		//System.out.println("updated is "+updated);
    		
			//產生成功訊息
    		if(updated)
    			actionResult = new ActionResult(ActionResult.STRING, "Add new product series successfully.("+productSeries+")");
    		else
    			actionResult = new ActionResult(ActionResult.STRING, "Duplicate product series.("+productSeries+")");
		} catch(Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
		}
		//傳回Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	
}
///:~
	