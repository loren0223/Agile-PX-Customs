//: ProductSeriesUpdator.java
package com.agree.agile.sdk.px.event.dfi;
import com.agile.api.*;
import com.agile.px.*;

/**
 * PRP(PDS Document Release Form)�y�{�o���A�NP3.Product Series���ȼg�JList:[DFI] Product Series.
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class ProductSeriesUpdator implements IEventAction {
	ActionResult actionResult = null;
	String actionFlag = null;
	
	/** 
	 * Agile PLM Event PX �i�J�I.
	 * @param session �ϥΪ�Session
	 * @param actionNode Ĳ�o�{������m
	 * @param request Event����
	 */
	public EventActionResult doAction(IAgileSession session, INode actionNode, IEventInfo request) {
		try {
 	    	//���oĲ�oEvent������
 	    	IObjectEventInfo object = (IObjectEventInfo) request;
 	    	//�૬Change����
 	    	IChange change = (IChange)object.getDataObject();
 	    	//�ϧOEvent Type
 	    	//int eventType = object.getEventType();
 	    	//���oChange Type��T
			String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString(); 
			//���oChange API Prefix(3 digits)
 	    	String changeAPIName = change.getAgileClass().getAPIName();
			String changeAPIPrefix = changeAPIName.substring(0, 3);
			
			//System.out.println("changeType is "+changeType);
			//System.out.println("changeAPIPrefix is "+changeAPIPrefix);
			
			/*
			 * ���oPRL.Product Series
			 */
			String productSeries = change.getCell(ChangeConstants.ATT_PAGE_THREE_TEXT01).getValue().toString();
			
			//System.out.println("productSeries is "+productSeries);
			
			/*
			 * �I�sList-[DFI] Product Series
			 */
			IAdmin admin = session.getAdminInstance();
		    IListLibrary listLib = admin.getListLibrary();
		    //Get admin list with API-Name
		    IAdminList productSeriesList = listLib.getAdminList("DFI_PRODUCT_SERIES");
    		
		    //System.out.println("productSeriesList is "+productSeriesList.getAPIName());
		    
			/*
			 * �P�_ PRL.Product Series �O�_�s�b [DFI] Product Series: Yes ����; No �s�W�C
			 */
    		boolean updated = false;
    		IAgileList listValues = productSeriesList.getValues();
    		if(listValues.getChild(productSeries) == null) {
    			listValues.addChild(productSeries);
    			productSeriesList.setValues(listValues);
    			updated =true;
    		}
    		//System.out.println("updated is "+updated);
    		
			//���ͦ��\�T��
    		if(updated)
    			actionResult = new ActionResult(ActionResult.STRING, "Add new product series successfully.("+productSeries+")");
    		else
    			actionResult = new ActionResult(ActionResult.STRING, "Duplicate product series.("+productSeries+")");
		} catch(Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
		}
		//�Ǧ^Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	
}
///:~
	