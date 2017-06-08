// ===========================================================================
// Copyright 2012 AdvancedTEK International Corporation, 8F, No.303, Sec. 1, 
// Fusing S. Rd., Da-an District, Taipei City 106, Taiwan(R.O.C.); Telephone 
// +886-2-2708-5108, Facsimile +886-2-2754-4126, or <http://www.advtek.com.tw/>. 
// All rights reserved.
// ===========================================================================
// 	Version	|Author			|Comment									
// --------------------------------------------------------------------------
// 	1.0		|Loren Cheng	|Initial
// 	1.1		|Loren Cheng	|If attribute value is null, don't append prefix and suffix string
// ===========================================================================
package com.agree.agile.sdk.px.event;

import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;

import java.lang.*;
import java.sql.*;
import java.util.*;

public class PAD implements IEventAction
{
	public EventActionResult doAction(IAgileSession session, INode actionNode, IEventInfo request) 
	{
		//Variable
		ActionResult actResult = null;
		IChange change = null;
		Connection conn = null;
 	    Statement stat = null;
 	    ResultSet rst = null;
 	    StringBuffer errLog = new StringBuffer(); 
 	    String errLogSep = "_^_";
		String listValueSep = UtilConfigReader.readProperty("list_value_separator");
		//START
		try 
		{
			//Get change obj
			IObjectEventInfo object = (IObjectEventInfo) request;
			change = (IChange)object.getDataObject();
			ITable table = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator it = table.getTableIterator();
			//Handle every affected item
			while(it.hasNext()) 
			{
				String errMsg = "";
				//Get item obj
				IRow row = (IRow)it.next();
				IItem obj = (IItem)row.getReferent();
				String site = row.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_SITES).toString();
			
				//Only handle the item belongs to Subclass of Parts and is common site
				if(obj.getAgileClass().getSuperClass().toString().equalsIgnoreCase("Parts")
					&& site.equals("")) 
				{
					String desc = "";
					
					//Get required info.
					String subclassName = obj.getValue(ItemConstants.ATT_TITLE_BLOCK_PART_TYPE).toString();
					String partNo = obj.getName();
					//Get px cofig: PX_CFG_PAD
					String sqls = 
						" SELECT * FROM PX_CFG_PAD "+
					    " WHERE SUBCLASS_NAME = '"+subclassName+"' "+
						" ORDER BY COMBINE_SEQ ";
					conn = DBUtility.getConnection();
					stat = DBUtility.createStatement(conn);
					rst = SQLUtility.executeQuery(stat,sqls);
					//System.out.println("subclassName="+subclassName);
					//Handle PAD rule
					while(rst.next()) 
					{
						String attObj = rst.getString("COMBINE_ATT_OBJ");
						String attNameRequired = rst.getString("COMBINE_ATT_NAME_REQUIRED");
						String attName = rst.getString("COMBINE_ATT_NAME");
						String attAppend = rst.getString("COMBINE_ATT_APPEND");
						//for non-required column value
						attNameRequired = attNameRequired==null? "" : attNameRequired;
						attAppend = attAppend==null? "" : attAppend;
					   
						//STEP 1: Get attribute value & attribute type
						String attValue = AgileDataObjectUtility.getAttributeValue(session,obj, attObj);
						Integer attType = AgileDataObjectUtility.getAttributeType(session, obj, attObj);
						if(!attValue.equals("")) 
						{
							//STEP 2: if attribute name required is Y
							if(attNameRequired.equalsIgnoreCase("Y")) 
							{
								desc += attName;
							}
							
							//If attribute type is LIST, get list option description
	    	   				if(attType.equals(DataTypeConstants.TYPE_SINGLELIST)) 
	    	   				{
	    	   					attValue = AgileDataObjectUtility.getListOptionDesc(attValue, listValueSep);
	    	   				}
	    	   				desc += attValue;
	    	   				
	    	   				//STEP 3: if attribute append is not null
							if(!attAppend.equals("")) 
							{
								desc += attAppend + " ";
							} 
							else 
							{
								desc += " ";
							}
						}
					}//END:while() for each Affected Item
					//If desc is empty, record error message
		    	   	if(desc.trim().equals("")) 
		    	   	{
		    	   		errLog.append(subclassName + errLogSep + partNo + errLogSep + "PX_CFG_PAD 組態設定沒有["+subclassName+"]的設定。");
		    	   	} 
		    	   	else //Set value of part desc.
		    	   	{
		    	   		obj.setValue(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION, desc.trim());
		    	   	}//One process end.
		    	}
			}//END:while() for all Affected Items
			
			//If errLog is not null, throw Exception
			if(!errLog.toString().equals("")) 
			{
				errLog.insert(0, "PAD failed below:\n");
				throw new Exception(errLog.toString());
			} 
			//Return successful message
			actResult = new ActionResult(ActionResult.STRING, "PAD processed successfully.");
		} 
		catch (APIException ex) 
		{
			actResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} 
		catch(SQLException ex) 
		{
			actResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} 
		catch(Exception ex) 
		{
			actResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} 
		finally 
		{
			DBUtility.closeConnection(conn, stat);
		}
		return new EventActionResult(request, actResult);
	}
}
	