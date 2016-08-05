//: ApproverReminder.java
package com.aic.agile.sdk.px.event.dfi;
import java.util.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import com.agile.api.*;
import com.agile.px.*;
import com.aic.agile.sdk.util.*;
/**
 * Approver�������u�@�����C
 * <p>��Approver����Approve�ʧ@�ɡA����Audit Status/Audit Release�ʧ@�A�ˬd�O�_�����������u�@�C
 * �p�G�S���A�N��Approve�ʧ@�����F�p�G���A�h���Audit���G�A�òפ�Approve�ʧ@�����C<br>
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
 	    	//���oEvent Type
 	    	//int eventType = object.getEventType();
 	    	//���oChange Type��T
 	    	//String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString(); 
			//���oChange API Prefix(3 digits)
 	    	//String changeAPIName = change.getAgileClass().getAPIName();
			//String changeAPIPrefix = changeAPIName.substring(0, 3);
 	    	//���oWorkflow Name (API Name)
 	    	String wfName = change.getWorkflow().getAPIName();
 	    	//���oCurrent Status Name (API Name)
 	    	String stName = change.getStatus().getAPIName();
 	    	//debug output
			//System.out.println("wfName is "+wfName);
			//System.out.println("stName is "+stName);
			
 	    	/*
 	    	 * �ǳ� Change.audit(boolean auditRelease)
 	    	 * ----------------------------------------
 	    	 * auditRelease=true: "Current Status.Pass Release Audit=yes" or "Next Status Type=Release"
 	    	 */
 	    	boolean auditRelease = false;
 	    	//���oAdmin����
            session = AgileSessionUtility.getAdminSession();
            //���oAdmin Node
		    IAdmin admin = session.getAdminInstance();
 	    	//���oStatus Criteria Node
		    INode statusCriteria = AgileNodeUtility.getStatusCriteriaNode(admin, wfName, stName);
		    //���o�Ҧ�Criteria�A�ˬdPass Release Audit�ѼơC�p�G������Criteria���ѼƭȬOYes�A����audit��auditRelease�Ѽ�=true�C
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
		    //�p�GauditRelease�Ofalse�A�ˬdNext Statue��Status Type:
		    //	�p�GType�OReleased�AauditRelease�Ѽ�=true
		    //--------------------------------------------------------------------------------
		    //NOTE: User�n��Next Status�v���A�_�h�|������Next Status����C
		    if(!auditRelease) {
			    IStatus nextStatus = change.getDefaultNextStatus();
			    //System.out.println("Next Status Name is "+nextStatus.getName());
			    if(nextStatus.getStatusType() == StatusConstants.TYPE_RELEASED) {
			    	auditRelease = true;
		    	}
		    }
		    //System.out.println("auditRelease is "+auditRelease);
		    /*
		     * ����Change.audit(boolean auditRelease)
		     * -------------------------------------------
		     * ���Audit Error Message��User���D�C
		     * ����Audit Warning Message�A����ܡC
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
					//�L�o���|��ñ�֪����~�T��"Not all approvers responded"
					//�L�o��ĵ�i�T��Warning
					if(!e.isWarning() && !e.getMessage().contains("Not all approvers responded"))  {
						//System.out.println("Exception : " + e.getMessage());
						errMessage.append("<p>").append(e.getMessage());
					}
				} 
			}
			//�p�G��Audit Error Message�A�ߥXException
			if(!errMessage.toString().equals("")) {
				throw new Exception(errMessage.toString());
			}
			//�p�G�S���A���Event Action���\�T��
			actionResult = new ActionResult(ActionResult.STRING, "Pass ApproverReminder.");
	    } catch(Exception ex) {
	    	//���ͥ��Ѫ�Action Result
	    	actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
			
		}
		//�Ǧ^Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	
}

///:~
	