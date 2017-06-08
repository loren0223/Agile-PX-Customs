//: DocsDescValidator.java
package com.agree.agile.sdk.px.event.everlight;
import java.util.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;
/**
 * Change Status��Ĳ�o�A�ˬdAffectedItems.Description�A���o������ĵ�i(WARNING)�y�z�C
 * �p�G���o�{�H�W����A���ĵ�i�T���bChange Status�u�X�����A�ð���Change Status�ʧ@�C
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class DocsDescValidator implements IEventAction {
	ActionResult actionResult = null;
	String successLog = "SUCCESS - Document Description Validation Passed. ";
	String illegalItem = "";
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
 	    	//���oAffected Items Table
 	    	ITable table = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
 	    	//Loop�C��Affected Item
 	    	Iterator it = table.iterator();
 	    	while(it.hasNext()) {
 	    		IRow row = (IRow)it.next();
 	    		//���oItem Number, Item Description, Item Base Class
 	    		String itemNumber = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue().toString();
 	    		String itemDesc = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_DESCRIPTION).getValue().toString();
 	    		IAgileClass spuerClass = row.getReferent().getAgileClass().getSuperClass();
 	    		//�u�ˬdDocument Type��Affected Item��Description�O�_��ĵ�i�T��
 	    		if(spuerClass.getAPIName().equals("DocumentsClass")) {
 	    			//�p�G���A�NItem Number�O���b�H�W�M��
 	    			if(itemDesc.startsWith("WARNING")) {
 	    				if(!illegalItem.equals("")) {
 	    					illegalItem += ", ";
 	    				}
 	    				illegalItem += itemNumber;
 	    			}
 	    		}
 	    	}
 	    	//�p�G�H�W�M��O�Ū�
			if(illegalItem.equals("")) {
				//���ͦ��\��Action Result
				actionResult = new ActionResult(ActionResult.STRING, successLog);
			} 
			//�p�G�H�W�M�椣�O�Ū��A�ߥXĵ�i�T���i��user�A��ܦbChange Status�����C
			else 
			{
				throw new Exception("WARNING - There are documents of invalid description! (Document Number:"+illegalItem+")");
			}
			
	    } catch(Exception ex) {
	    	//���ͥ��Ѫ�Action Result
	    	actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
	    } 
		//�^��Event Action Result
 	    return new EventActionResult(request, actionResult);
	}

}

///:~
	