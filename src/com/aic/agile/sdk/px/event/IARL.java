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

public class IARL implements IEventAction 
{
	public EventActionResult doAction(IAgileSession session, INode actionNode, IEventInfo request) 
	{
		ActionResult actResult = null;
		Connection conn = null;
 	    Statement stat = null;
 	    ResultSet rst = null;
 	    StringBuffer errLog = new StringBuffer();                                                     
	
 	    //START
 	    try 
 	    {
 	    	String criteriaValue = "";
 	    	System.out.println("IARL:START----");
			//Get change obj
 	    	IObjectEventInfo object = (IObjectEventInfo) request;
			IChange change = (IChange)object.getDataObject();
			//Get change subclass name
			String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString(); 
			
			//STEP 1: Get px confg: PX_CFG_IARL_CRITERIA
			String sqls = 
				" select * from PX_CFG_IARL_CRITERIA "+
			    " where change_order_name = '"+changeType+"' "+
				" order by CRITERIA_SEQ ";
			conn = DBUtility.getConnection();
	    	stat = DBUtility.createStatement(conn);
	    	System.out.println("IARL:STEP 1 start");
	    	rst = SQLUtility.executeQuery(stat,sqls);
	    	//Handle IARL criteria
	    	while(rst.next())
	    	{                                                
	    		//Get Criteria attributes
	    		String attObj = rst.getString("CRITERIA_ATT_OBJ");
	    		String separator = rst.getString("CRITERIA_SEPARATOR");
	    		String attValue = AgileDataObjectUtility.getAttributeValue(session,change, attObj);
	    		//Set Criteria Value
	    		criteriaValue += attValue + separator;
	    	}
	    	System.out.println("IARL:STEP 1 end");
	    	//rst.close();
	    	
	    	//STEP 2: Get px confg: PX_CFG_IARL_RULE
	    	if(!criteriaValue.equals(""))
	    	{
	    		sqls = 
		    		" select * from PX_CFG_IARL_RULE "+
		    	    " where change_order_name = '"+changeType+"' and criteria_value = '"+criteriaValue+"' ";
		    	System.out.println("IARL:STEP 2 start");
		    	rst = SQLUtility.executeQuery(stat,sqls);
		    	//Handle IARL rule
		    	while(rst.next())
		    	{       
		    		//Get IARL_RULE, NEW_LCP, NEW_REV
		    		String rule = rst.getString("IARL_RULE");
		    		String newLCP = rst.getString("NEW_LCP");
		    		String newRev = rst.getString("NEW_REV");
		    		ITable table = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
					Iterator it = table.getTableIterator();
					//Verify IARL rule for every affected items
					while(it.hasNext())
					{
						IRow row = (IRow)it.next();
						String site = row.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_SITES).toString();
						//Only handle common site items
						if(site.equals(""))
						{
							verifyRule(row,rule,newLCP,newRev);
						}
					}
		    	}
		    	System.out.println("IARL:STEP 2 end");
		    } 
	    	else //if IARL criteria value is null 
	    	{
	    		errLog.append("PX_CFG_IARL_CRITERIA 組態設定沒有["+changeType+"]的設定。");
	    	}
	    	
	    	//if no error log
	    	if(errLog.toString().equals(""))
	    	{
	    		actResult = new ActionResult(ActionResult.STRING, "IARL processed successfully.");
	    		
	    	}
	    	else //if error log is not null
	    	{
				errLog.insert(0,"IARL failed below:\n");
				throw new Exception(errLog.toString());
			}
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
	
	public static boolean verifyRule(IRow row, String rule, String newLCP, String newRev) throws Exception 
	{
		boolean matchRule = false;
		
		try 
		{
			String oldLCP = row.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_LIFECYCLE_PHASE).toString();
			String oldRev = row.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_REV).toString();
			System.out.println("oldRev="+oldRev);
			String[] aryRule = rule.split("=");
			String ruleCondition = aryRule[0].toLowerCase();
			String ruleValue = aryRule[1].toLowerCase();
			//for rule Old_LCP=Preliminary
			ruleValue = ruleValue.equals("preliminary")? "" : ruleValue;
			
			if(ruleCondition.equals("old_lcp")) 
			{
				if(ruleValue.equals(oldLCP.toLowerCase())) 
				{
					setNewLCPRev(row, newLCP, newRev, oldRev);
					matchRule = true;
				}
			} 
			// Avoid none of old rev
			else if
			(ruleCondition.equals("old_rev") && !oldRev.equals("")) 
			{
				if(ruleValue.equals("informal")) 
				{
					if(oldRev.substring(0,1).matches(UtilConfigReader.readProperty("informal_rev_prefix_regex"))) 
					{
						setNewLCPRev(row, newLCP, newRev, oldRev);
						matchRule = true;
					}
				} 
				else if(ruleValue.equals("formal")) 
				{
					if(oldRev.substring(0,1).matches(UtilConfigReader.readProperty("formal_rev_prefix_regex"))) 
					{
						setNewLCPRev(row, newLCP, newRev, oldRev);
						matchRule = true;
					}
				}
			}
		} 
		catch(Exception ex) 
		{
			throw ex;
		}
		return matchRule;
	}
	
	public static void setNewLCPRev(IRow row, String newLCP, String newRev, String oldRev) throws Exception 
	{
		try 
		{
			row.setValue(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE, newLCP);
		
			if(newRev.toLowerCase().equals("old_rev+1"))
				row.setValue(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV, getNextRev(oldRev));
			else
				row.setValue(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV, newRev);
		} 
		catch(APIException ex) 
		{
			if(ex.isWarning()) 
			{  
				// do nothing
			} 
			else 
			{
				throw ex;
			}
		}
	}
	
	public static String getNextRev(String oldRev) throws Exception
	{
		String newRev = null;
		
		try 
		{
			String bigRev = oldRev.substring(0,1);
			String smallRev = oldRev.substring(1,3);
			int iSmallRev = Integer.parseInt(smallRev);
			
			if(iSmallRev+1 <= 99) 
			{
				String nextSmallRev = String.valueOf(iSmallRev+1);
				nextSmallRev = nextSmallRev.length()==1? "0"+nextSmallRev : nextSmallRev;
				newRev = bigRev + nextSmallRev;
			} 
			else if(iSmallRev+1 == 100) 
			{
				String nextBigRev = getNextBigRev(bigRev);
				if(nextBigRev.matches(UtilConfigReader.readProperty("informal_rev_prefix_regex"))) 
				{
					newRev = nextBigRev + "00";
				} 
				else if(nextBigRev.matches(UtilConfigReader.readProperty("formal_rev_prefix_regex"))) 
				{
					newRev = nextBigRev + "01";
				}
			}
		} 
		catch(Exception ex) 
		{
			throw ex;
		}
		return newRev;
	}
	
	public static String getNextBigRev(String bigRev) throws Exception
	{
		String nextBigRev = "";
	
		try 
		{
			if(bigRev.endsWith("Z"))
			{
				throw new Exception("ERROR: Rev-Number ran out of bound!");
			}
			String[] aryBigRev = UtilConfigReader.readProperty("big_rev_seq").split(",");
			for(int i=0; i<aryBigRev.length; i++) 
			{
				String value = aryBigRev[i];
				if(bigRev.equals(value)) 
				{
					nextBigRev = aryBigRev[i+1];
					break;
				}
			}
		} 
		catch(Exception ex) 
		{
			throw ex;
		}
		return nextBigRev;
	}
	
}
	