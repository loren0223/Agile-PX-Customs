//: CustomAutoPromotion.java
package com.aic.agile.sdk.px.event.dfi;

import java.util.*;
import com.agile.api.*;
import com.agile.px.*;
import com.aic.agile.sdk.util.*;

/**
 * �u�@�y�{�۰ʩ��e��.
 * <p>
 * @author Loren.Cheng
 * @version 1.0
 */
public class CustomAutoPromotion implements IEventAction {
	//Variables declaration
	ActionResult actResult = null;
	IChange change = null;
	IProgram program = null;
	IStatus nextStatus = null;
	IObjectEventInfo object = null;
	IDataObject obj = null;
	StringBuffer sb = new StringBuffer();
	/** 
	 * Agile PLM Event PX �i�J�I.
	 * @param session �ϥΪ�Session
	 * @param actionNode Ĳ�o�{������m
	 * @param request Event����
	 */
	public EventActionResult doAction(IAgileSession session, INode actionNode, IEventInfo request) {
		try {
			//���o��ƪ���
			object = (IObjectEventInfo) request;
			obj = object.getDataObject();
			//�������O�OChange
			if(obj.getType() == IChange.OBJECT_TYPE) {
				//�૬Change����
				change = (IChange)obj;
				//���oChange Type
				String changeType = change.getAgileClass().getName();
				//���oChange Number
				String changeNo = change.getName();
				//���o�y�{�U�@��
				nextStatus = change.getDefaultNextStatus();
				
				//Get approvers of the next status
				//ISignoffReviewer[] defaultApprovers = change.getReviewers(nextStatus,WorkflowConstants.USER_APPROVER);
				//Get notify list of the next status
				IDataObject[] notifyList = change.getDefaultNotifyListEx(nextStatus);
				ArrayList notifyListCollection = new ArrayList();
				for(IDataObject notify : notifyList) {
					//�p�Gnotify���� Obj Type �O  User Group
					if(notify.getAgileClass().isSubclassOf(UserGroupConstants.CLASS_USER_GROUPS_CLASS)) {
						IUserGroup group = (IUserGroup)notify;
						ITable table = group.getTable(UserGroupConstants.TABLE_USERS);
						Iterator it = table.iterator();
						//#### SDK Bug Workaround: �P�_User Group�����Ť~�q���A�_�h�|�y��Change Status�L�k�o�X����q�� ####
						if(it.hasNext()) {
							notifyListCollection.add(notify);
						}
						//################################################################################
					} else {
						notifyListCollection.add(notify);
					}	
					//System.out.println("Notify Obj Name is "+notify.getName());
				}
				//debug
				//System.out.println("##### Notify List Size = "+notifyList.length+" #####");
				
				//����Warning
				session.disableAllWarnings();
				//Change to the next status
				change.changeStatus(
						nextStatus, 
						true, 
						"Custom Auto Promotion", 
						true, 
						true, 
						notifyListCollection, 
						null, 
						null,
						null,
						true);
				
				//�^�_
				session.enableAllWarnings();
				//Return action result
				actResult = new ActionResult(ActionResult.STRING, "Auto-Promotion Succeed.");
			}
			/*
			if(obj.getType() == IProgram.OBJECT_TYPE) {
				program = (IProgram)obj;
				//Get the next status
				nextStatus = program.getDefaultNextStatus();
				//System.out.println("Next Status="+nextStatus.getName());
				//Get the default approvers
				IDataObject[] defaultApprovers = program.getApproversEx(nextStatus);
				//Change to the next status
				//System.out.println("Valid operation? "+program.checkValidOperation(OperationConstants.OP_CHANGE_STATUS));
				program.changeStatus(nextStatus, true, "PX Auto Promote", false, false, null, defaultApprovers, null, false);
				//Return action result
				actResult = new ActionResult(ActionResult.STRING, "Auto Promote Successfully.");
			}
			*/
		} catch(APIException e) {
			//System.out.println("Warning? "+e.isWarning());
			if(e.getErrorCode().equals(ExceptionConstants.API_SEE_MULTIPLE_ROOT_CAUSES)) {
				Throwable[] causes = e.getRootCauses();
				for(int i=0; i<causes.length; i++) {
					sb.append(((APIException)causes[i]).getMessage());
				}
			}else{
				sb.append(e.getMessage());
			}
			actResult = new ActionResult(ActionResult.EXCEPTION, new Exception(sb.toString()));
		} catch(Exception ex) {
			actResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} 
		return new EventActionResult(request, actResult);
	}
}
///:~