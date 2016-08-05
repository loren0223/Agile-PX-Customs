// ===========================================================================
// Copyright 2012 AdvancedTEK International Corporation, 8F, No.303, Sec. 1, 
// Fusing S. Rd., Da-an District, Taipei City 106, Taiwan(R.O.C.); Telephone 
// +886-2-2708-5108, Facsimile +886-2-2754-4126, or <http://www.advtek.com.tw/>. 
// All rights reserved.
// ===========================================================================
// 	Version	|Author			|Comment									
// --------------------------------------------------------------------------
// 	1.0		|Loren Cheng	|Initial
// ===========================================================================

package com.aic.agile.sdk.px.event;

import java.sql.*;
import java.util.*;

import com.agile.api.*;
import com.agile.px.*;
import com.aic.agile.sdk.util.*;

public class AP implements IEventAction 
{
	public EventActionResult doAction(IAgileSession session, INode actionNode, IEventInfo request) 
	{
		//Variables declaration
		ActionResult actResult = null;
		IChange change = null;
		IProgram program = null;
		IStatus nextStatus = null;
		IObjectEventInfo object = null;
		IDataObject obj = null;
		StringBuffer sb = new StringBuffer();
		
		try 
		{
			//Get data obj 
			object = (IObjectEventInfo) request;
			obj = object.getDataObject();
			
			if(obj.getType() == IChange.OBJECT_TYPE)
			{
				change = (IChange)obj;
				//Get the next status
				nextStatus = change.getDefaultNextStatus();
				//Get the default approvers
				IDataObject[] defaultApprovers = change.getApproversEx(nextStatus);
				//Change to the next status
				change.changeStatus(nextStatus, true, "PX Auto Promote", false, false, null, defaultApprovers, null, false);
				//Return action result
				actResult = new ActionResult(ActionResult.STRING, "Auto Promote Successfully.");
			}
			if(obj.getType() == IProgram.OBJECT_TYPE)
			{
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
		} 
		catch(APIException e) 
		{
			System.out.println("Warning? "+e.isWarning());
			if(e.getErrorCode().equals(ExceptionConstants.API_SEE_MULTIPLE_ROOT_CAUSES)) 
			{
				Throwable[] causes = e.getRootCauses();
				for(int i=0; i<causes.length; i++) 
				{
					sb.append(((APIException)causes[i]).getMessage());
				}
			} 
			else 
			{
				sb.append(e.getMessage());
			}
			
			actResult = new ActionResult(ActionResult.EXCEPTION, new Exception(sb.toString()));
		} 
		catch(Exception ex) 
		{
			actResult = new ActionResult(ActionResult.EXCEPTION, ex);
		}
		return new EventActionResult(request, actResult);
	}


}
