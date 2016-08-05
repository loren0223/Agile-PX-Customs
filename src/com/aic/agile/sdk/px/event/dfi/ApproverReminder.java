//: ApproverReminder.java
package com.aic.agile.sdk.px.event.dfi;
import java.util.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import com.agile.api.*;
import com.agile.px.*;
import com.aic.agile.sdk.util.*;
/**
 * Approver未完成工作提醒。
 * <p>當Approver執行Approve動作時，執行Audit Status/Audit Release動作，檢查是否有未完成的工作。
 * 如果沒有，就讓Approve動作完成；如果有，則顯示Audit結果，並終止Approve動作完成。<br>
 * 
 * Event Type:Approve
 * Trigger Type:Pre
 * Error Handling Rule:Stop
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class ApproverReminder implements IEventAction {
	ActionResult actionResult = null;
	String successLog = "Nothing to do.";
	StringBuffer errMessage = new StringBuffer();
	
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
 	    	//int eventType = object.getEventType();
 	    	//取得Change Type資訊
 	    	//String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString(); 
			//取得Change API Prefix(3 digits)
 	    	//String changeAPIName = change.getAgileClass().getAPIName();
			//String changeAPIPrefix = changeAPIName.substring(0, 3);
 	    	//取得Workflow Name (API Name)
 	    	String wfName = change.getWorkflow().getAPIName();
 	    	//取得Current Status Name (API Name)
 	    	String stName = change.getStatus().getAPIName();
 	    	//debug output
			//System.out.println("wfName is "+wfName);
			//System.out.println("stName is "+stName);
			
 	    	/*
 	    	 * 準備 Change.audit(boolean auditRelease)
 	    	 * ----------------------------------------
 	    	 * auditRelease=true: "Current Status.Pass Release Audit=yes" or "Next Status Type=Release"
 	    	 */
 	    	boolean auditRelease = false;
 	    	//取得Admin身份
            session = AgileSessionUtility.getAdminSession();
            //取得Admin Node
		    IAdmin admin = session.getAdminInstance();
 	    	//取得Status Criteria Node
		    INode statusCriteria = AgileNodeUtility.getStatusCriteriaNode(admin, wfName, stName);
		    //取得所有Criteria，檢查Pass Release Audit參數。如果有任何Criteria的參數值是Yes，執行audit時auditRelease參數=true。
		    Collection criterias = statusCriteria.getChildNodes();
		    Iterator it1 = criterias.iterator();
		    while(it1.hasNext()) {
		    	INode criteria = (INode)it1.next();
		    	String passReleaseAudit = criteria.getProperty(PropertyConstants.PROP_PASS_RELEASE_AUDIT).getValue().toString();
		    	if(passReleaseAudit.equals("Yes")) {
		    		auditRelease = true;
		    		break;
		    	}
		    }
		    //如果auditRelease是false，檢查Next Statue的Status Type:
		    //	如果Type是Released，auditRelease參數=true
		    //--------------------------------------------------------------------------------
		    //NOTE: User要有Next Status權限，否則會取不到Next Status物件。
		    if(!auditRelease) {
			    IStatus nextStatus = change.getDefaultNextStatus();
			    //System.out.println("Next Status Name is "+nextStatus.getName());
			    if(nextStatus.getStatusType() == StatusConstants.TYPE_RELEASED) {
			    	auditRelease = true;
		    	}
		    }
		    //System.out.println("auditRelease is "+auditRelease);
		    /*
		     * 執行Change.audit(boolean auditRelease)
		     * -------------------------------------------
		     * 顯示Audit Error Message給User知道。
		     * 忽略Audit Warning Message，不顯示。
		     */
			Map results = change.audit(auditRelease);
			// Get the set view of the map 
			Set set = results.entrySet(); 
			// Get an iterator for the set 
			Iterator it = set.iterator(); 
			// Iterate through the cells and print each cell name and exception 
			while (it.hasNext()) { 
				Map.Entry entry = (Map.Entry)it.next(); 
				ICell cell = (ICell)entry.getKey(); 
				if(cell != null) { 
					//System.out.println("Cell : " + cell.getName());
					errMessage.append("<p>").append("Related Attribute: "+cell.getName());
				} else {
					//System.out.println("Cell : No associated data cell"); 
				} 
				//Iterate through exceptions for each map entry. 
				//(There can be multiple exceptions for each data cell.) 
				Iterator jt = ((Collection)entry.getValue()).iterator(); 
				while (jt.hasNext()) { 
					APIException e = (APIException)jt.next();
					//過濾掉尚未簽核的錯誤訊息"Not all approvers responded"
					//過濾掉警告訊息Warning
					if(!e.isWarning() && !e.getMessage().contains("Not all approvers responded"))  {
						//System.out.println("Exception : " + e.getMessage());
						errMessage.append("<p>").append(e.getMessage());
					}
				} 
			}
			//如果有Audit Error Message，拋出Exception
			if(!errMessage.toString().equals("")) {
				throw new Exception(errMessage.toString());
			}
			//如果沒有，顯示Event Action成功訊息
			actionResult = new ActionResult(ActionResult.STRING, "Pass ApproverReminder.");
	    } catch(Exception ex) {
	    	//產生失敗的Action Result
	    	actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
			
		}
		//傳回Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	
}

///:~
	