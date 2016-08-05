//: CheckFGStakeholderExistOrNot.java
package com.aic.agile.sdk.px.event.dfi;

import java.util.*;
import com.agile.api.*;
import com.agile.px.*;
import com.aic.agile.sdk.util.*;

/**
 * �T�{�O�_�����F/G Stakeholder.
 * <p>
 * @author Loren.Cheng
 * @version 1.0
 */
public class CheckFGStakeholderExistOrNot implements IEventAction {
	//Variables declaration
	ActionResult actionResult = null;
	StringBuffer sb = new StringBuffer();
	/** 
	 * Agile PLM Event PX �i�J�I.
	 * @param session �ϥΪ�Session
	 * @param actionNode Ĳ�o�{������m
	 * @param request Event����
	 */
	public EventActionResult doAction(IAgileSession session, INode actionNode, IEventInfo request) {
		try {
			//���oEventInfo Custom Map(Name=getApproverList, Value Type is boolean)
			Map map = request.getUserDefinedMap();
			boolean getApproverList = (boolean)map.get("getApproverList");
			//If getApproverList = true, throw Exception("Stop Custom Auto Promotion")
			//Else false, Action Result is Successful.
			if(getApproverList) {
				throw new Exception("Stop Custom Auto Promotion.");
			} else {
				actionResult = new ActionResult(ActionResult.STRING, "Continue Custom Auto Promotion.");
			}
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
			actionResult = new ActionResult(ActionResult.EXCEPTION, new Exception(sb.toString()));
		} catch(Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} 
		return new EventActionResult(request, actionResult);
	}
}
///:~