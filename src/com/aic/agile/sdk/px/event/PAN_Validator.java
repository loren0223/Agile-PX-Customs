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

public class PAN_Validator implements IEventAction
{
	public EventActionResult doAction(IAgileSession session, INode actionNode, IEventInfo request) 
	{
		//Variable
		ActionResult actResult = null;
		IChange change = null;
		StringBuffer errLog = new StringBuffer();
		String errLogSep = "_^_";
		
		//START
		try 
		{
			//Get change obj
			IObjectEventInfo object = (IObjectEventInfo) request;
			change = (IChange)object.getDataObject();
			//Get change affected items
			ITable table = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator it = table.getTableIterator();
			//Handle every items
			while(it.hasNext()) 
			{
				String errMsg = "";
				//Get item
				IRow row = (IRow)it.next();
				IItem obj = (IItem)row.getReferent();
				String site = row.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_SITES).toString();
				
				//Only handle Parts subclass
				if(obj.getAgileClass().getSuperClass().toString().equalsIgnoreCase("Parts")
					&& site.equals("")) 
				{
					//Get obj subclass
					String sn = obj.getValue(ItemConstants.ATT_TITLE_BLOCK_PART_TYPE).toString();
				    //Get obj number
			    	String partNo = obj.getName();
			    	
			    	//Validate part number length less than or equal 20
			    	if(partNo.length() > 20) 
			    	{
			    		errLog.append("料號編碼長度超過20:"+partNo+errLogSep);
			    	}
			    	//Validate part number is formal number
			    	if(partNo.startsWith("T")) 
			    	{
			    		errLog.append("暫時料號未轉成正式料號:"+partNo+errLogSep);
			    	}
			    }
			}//End: while()
			
			//if no error message
			if(errLog.toString().equals("")) 
			{
				actResult = new ActionResult(ActionResult.STRING, "PAN validation successfully.");
			} 
			//if there are error messages
			else 
			{
				errLog.insert(0, "PAN validation fail: ");
				//Throw exception to stop event action
				throw new Exception(errLog.toString());
			}
		} 
		catch(APIException ae) 
		{
			actResult = new ActionResult(ActionResult.EXCEPTION, ae);
		} 
		catch(Exception ex) 
		{
			actResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} 
		
		return new EventActionResult(request, actResult);
	}
}
