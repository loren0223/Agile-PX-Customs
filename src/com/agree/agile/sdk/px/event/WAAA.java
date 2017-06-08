// ===========================================================================
// Copyright 2012 AdvancedTEK International Corporation, 8F, No.303, Sec. 1, 
// Fusing S. Rd., Da-an District, Taipei City 106, Taiwan(R.O.C.); Telephone 
// +886-2-2708-5108, Facsimile +886-2-2754-4126, or <http://www.advtek.com.tw/>. 
// All rights reserved.
// ===========================================================================
// 	Version	|Author			|Comment									
// --------------------------------------------------------------------------
// 	1.0		|Loren Cheng	|Initial
// 	1.1		|Loren Cheng	|New process rule: user/group
//  1.2		|Loren Cheng	|If MGR Id not find, add Admin as approver
// ===========================================================================
package com.agree.agile.sdk.px.event;

import java.sql.*;
import java.util.*;

import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;

public class WAAA implements IEventAction 
{
	public EventActionResult doAction(IAgileSession session, INode actionNode, IEventInfo request) 
	{
		//Declare vars
		ActionResult actResult = null;
		Connection conn = null;
		Statement stat = null;
		ResultSet rst = null;
		Collection approverList = new ArrayList();
		Collection wfApproverList = new ArrayList();
		boolean checkwf = false;
		IChange change = null;
		IProgram program = null;
		
		try 
		{
			//Get change obj
			IObjectEventInfo object = (IObjectEventInfo) request;
			IDataObject obj = object.getDataObject();
			//Check obj type
			String objName = "";
			if(obj.getType() == IChange.OBJECT_TYPE)
			{
				//Get change number
				change = (IChange)obj;
				objName = change.getValue(ChangeConstants.ATT_COVER_PAGE_NUMBER).toString();
			}
			if(obj.getType() == IProgram.OBJECT_TYPE)
			{
				//Get program name
				program = (IProgram)obj;
				objName = program.getValue(ProgramConstants.ATT_GENERAL_INFO_NAME).toString();
			}
			
			//Get event context obj
			IWFChangeStatusEventInfo infoWF = (IWFChangeStatusEventInfo)request;
			//Get WF name, status_from, status_to
			IStatus statusTo = infoWF.getToStatus();         
			String WN = infoWF.getWorkFlow().toString();    
						
			//Get px confg: PX_CFG_WAAA_RULE_ML
			String sqls = 
				" SELECT * FROM PX_CFG_WAAA_RULE_ML " +
				" WHERE WORKFLOW = '"+WN+"' AND STATUS_TO = '"+statusTo+"' ";
			conn = DBUtility.getConnection();
		    stat = DBUtility.createStatement(conn);
		    rst = SQLUtility.executeQuery(stat,sqls);
		    //Debug
		    System.out.println("[ WAAA START ]");
		    System.out.println("Object Name="+objName);
		    System.out.println("WN="+WN);
		    System.out.println("status="+statusTo);
		    //Handle for each WAAA rule
	        while(rst.next()) 
	        {
	        	checkwf = true;
	            //Extract rule setting
	        	String ruleType1 	= rst.getString("RULE_TYPE_1");
	            String ruleValue1 	= rst.getString("RULE_VALUE_1");
	            String ruleType2 	= rst.getString("RULE_TYPE_2");
	            String ruleValue2 	= rst.getString("RULE_VALUE_2");
	            String getMGRLvFrom = rst.getString("GET_MGR_LV_FROM");
	            String getMGRLvTo 	= rst.getString("GET_MGR_LV_TO");
	            ruleType2 		= ruleType2==null? "" : ruleType2;
	            ruleValue2 		= ruleValue2==null? "" : ruleValue2;
	            getMGRLvFrom 	= getMGRLvFrom==null? "" : getMGRLvFrom;
	            getMGRLvTo 		= getMGRLvTo==null? "" : getMGRLvTo;
	            
	            //Debug
	            System.out.println("ruleType1="+ruleType1);
	            System.out.println("ruleValue1="+ruleValue1);
	            
	            //Logic 3.1 / 3.2
	            if(ruleType1.equals("attribute") || ruleType1.equals("originator")) 
	            {
	            	System.out.println("Logic 3.1/3.2");
	            	approverList = getApproverList(session, obj, ruleType1, ruleValue1, getMGRLvFrom, getMGRLvTo);
	            	wfApproverList.addAll(approverList);
	            } 
	            //Logic 3.4 / 3.5
        		if(ruleType1.startsWith("checkAttribute"))
        		{
	            	boolean addApprover = true;
	            	String checkAttributes = ruleType1.substring(ruleType1.indexOf(":")+1);
	            	System.out.println("Checking attributes="+checkAttributes);
            		
	            	//Logic 3.4 / 3.5 (Check attribute value)
	            	if(!checkAttributes.equals("")) 
	            	{
	            		System.out.println("Logic 3.4/3.5");
		            	//Initial boolean(true)
	            		addApprover = true;
	            		//Get checking criteria attributes
	            		String[] attList = checkAttributes.split(",");
	            		//== or <>
	            		String checkEqual = ruleValue1.substring(0,2);
	            		//Get checking value ("null" means empty value)
	            		String checkValue = ruleValue1.substring(2,ruleValue1.length());
	            		System.out.println("checkEqual="+checkEqual);
	            		System.out.println("checkValue="+checkValue);
	            		//Check if every attribute conditions are TRUE
		            	for(int i=0; i<attList.length; i++) 
		            	{
		            		System.out.println("attribute "+(i+1)+" checking...");
		            		String attValue = AgileDataObjectUtility.getAttributeValue(session, obj, attList[i]);
		            		//Case 1: Check if Attribute Value == Checking Value
		            		if(checkEqual.equals("==") && !checkValue.equals("null")) 
		            		{
		            			System.out.println("Check if attribute value == check value");
		            			System.out.println(attValue.equals(checkValue));
			            		//Attribute Value <> Checking Value, no approver adding
		            			if(!attValue.equals(checkValue)) 
		            			{
		            				addApprover = false;
		            				break;
		            			}
		            		}
		            		//Case 2: Check if Attribute Value == Empty Value
		            		if(checkEqual.equals("==") && checkValue.equals("null")) 
		            		{
		            			System.out.println("Check if attribute value == null");
		            			System.out.println(attValue.equals(""));
		            			//Attribute Value <> Empty Value, no approver adding
		            			if(!attValue.equals("")) 
		            			{
		            				addApprover = false;
		            				break;
		            			}
		            		}
		            		//Case 3: Check if Attribute Value <> Checking Value
		            		if(checkEqual.equals("<>") && !checkValue.equals("null")) 
		            		{
		            			System.out.println("Check if attribute value != check value");
		            			System.out.println(!attValue.equals(checkValue));
		            			//Attribute Value == Checking Value, no approver adding
		            			if(attValue.equals(checkValue)) 
		            			{
		            				addApprover = false;
		            				break;
		            			}
		            		}
		            		//Case 4: Check if Attribute Value <> Empty Value
		            		if(checkEqual.equals("<>") && checkValue.equals("null")) 
		            		{
		            			System.out.println("Check if attribute value != null");
		            			System.out.println(!attValue.equals(""));
		            			//Attribute Value == Empty Value, no approver adding
		            			if(attValue.equals("")) 
		            			{
		            				addApprover = false;
		            				break;
		            			}
		            		}
		            	}
		            	//Get approver
	            		if(addApprover) {
	            			//Get approver from rule_type_2
	            			System.out.println("Get approver!");
	            			approverList = getApproverList(session, obj, ruleType2, ruleValue2, getMGRLvFrom, getMGRLvTo);
	            			wfApproverList.addAll(approverList);
	                    }
		            }
        		}//Logic 3.4 / 3.5 end
	            //Logic 3.6 (Check Originator's[Task Owner's] default site)
	            if(ruleType1.equals("checkDefaultSite")) 
            	{
            		System.out.println("Logic 3.6");
            		//Initial boolean(false)
            		boolean addApprover = false;
            		//Get checking criteria attributes
            		//== or <>
            		String checkEqual = ruleValue1.substring(0,2);
            		//Checking criteria (Site)
            		String siteList = ruleValue1.substring(2,ruleValue1.length());
            		//Get originator
            		IUser originator = null;
            		//	Check obj type
            		if(obj.getType() == IChange.OBJECT_TYPE)
            		{
            			originator = (IUser)change.getCell(ChangeConstants.ATT_COVER_PAGE_ORIGINATOR).getReferent();
            		}
            		if(obj.getType() == IProgram.OBJECT_TYPE)
            		{
            			originator = (IUser)program.getCell(ProgramConstants.ATT_GENERAL_INFO_OWNER).getReferent();
            		}
            		//Get originator user site
            	    String userSite = originator.getValue(UserConstants.ATT_GENERAL_INFO_DEFAULT_SITE).toString();
            	    System.out.println("User default site="+userSite);
            	    System.out.println("Check Site List="+siteList);
            	    //Case 1: Originator's default_site 'IN' [Site List]
            		if(checkEqual.equals("==")) 
            		{
            	    	System.out.println("Case 1: Originator's default_site IN [Site List]");
            			if(siteList.indexOf(userSite) >= 0) 
            			{
            				addApprover = true;
            			}
            		}
            		//Case 2: Originator's default_site 'NOT IN' [Site List]
            		if(checkEqual.equals("<>")) 
            		{
            			System.out.println("Case 2: Originator's default_site NOT IN [Site List]");
            			if(siteList.indexOf(userSite) == -1) 
            			{
            				addApprover = true;
            			}
            		}
            		//Get approver
            		if(addApprover) {
            			//Get approver from rule_type_2
            			System.out.println("Get approver!");
            			approverList = getApproverList(session, obj, ruleType2, ruleValue2, getMGRLvFrom, getMGRLvTo);
            			wfApproverList.addAll(approverList);
                    }
            	}
	            //Logic 3.7 (Check Affected Item Type)
	            if(ruleType1.equals("checkAffectedItemType")) 
            	{
            		System.out.println("Logic 3.7");
            		//Initial boolean(false)
            		boolean addApprover = false;
            		//Get checking criteria attributes
            		//== or <>
            		String checkEqual = ruleValue1.substring(0,2);
            		//Checking criteria (Item Type REs)
            		String itemTypeREs = ruleValue1.substring(2,ruleValue1.length());
            		String[] itemTypeREAry = itemTypeREs.split(",");
            		//Get the types of all affected items of Change object
            		ITable affectedItemTable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
            		Iterator it = affectedItemTable.getTableIterator();
            		while(it.hasNext())
            		{
            			IRow row = (IRow)it.next();
            			String itemType = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).getValue().toString();
            			System.out.println("Affected Item Type="+itemType);
            			//Case 1: The type of any Affected Item matches any ItemTypeRE
                		if(checkEqual.equals("==")) 
                		{
                	    	boolean matchItemRE = false;
                	    	System.out.println("Case 1: The type of any Affected Item matches any ItemTypeRE");
                			//Check if Item Type matches any Item Type REs
                	    	for(int i=0; i<itemTypeREAry.length; i++)
                			{
                				String itemTypeRE = itemTypeREAry[i];
                				if(itemType.matches(itemTypeRE))
                				{
                					matchItemRE = true;
                					break;
                				}
                			}
                	    	//If yes, do adding approver.
                			if(matchItemRE)
                			{
                				addApprover = true;
                				break;
                			}
                	    }
                		//Case 2: The type of any Affected Item does not match all ItemTypeREs
                		if(checkEqual.equals("<>")) 
                		{
                			boolean matchItemRE = false;
                			System.out.println("Case 2: The type of any Affected Item does not match all ItemTypeREs");
                			//Check if Item Type does not match all Item Type REs
                			for(int i=0; i<itemTypeREAry.length; i++)
                			{
                				String itemTypeRE = itemTypeREAry[i];
                				System.out.println("Item Type RE["+i+"]="+itemTypeRE);
                				//if(itemType.substring(0,4).matches(itemTypeRE))
                				if(itemType.matches(itemTypeRE))
                				{
                					matchItemRE = true;
                					System.out.println("Match Item RE?"+matchItemRE);
                					break;
                				}
                			}
                			//If no, do adding approver.
                			if(!matchItemRE)
                			{
                				addApprover = true;
                				System.out.println("add approver!");
                				break;
                			}
                			
                		}
            		}
            		//Get approver
            		if(addApprover) {
            			//Get approver from rule_type_2
            			System.out.println("Get approver!");
            			approverList = getApproverList(session, obj, ruleType2, ruleValue2, getMGRLvFrom, getMGRLvTo);
            			wfApproverList.addAll(approverList);
                    }
            	}
                
	        }//End:while()
	        
	        //Check if there are WAAA settings
	        if(!checkwf) 
	        {
	        	throw new Exception("ERROR: No Such Workflow!");
	        }
	        
	        //**************************************
	        // Add approvers into Program Team Table
	        if(obj.getType() == IProgram.OBJECT_TYPE)
	        {
	        	addResouceToTeamTable(session, program, wfApproverList);
	        }
	        //**************************************
	        
	        //Set workflow approvers
	        try 
            {
            	Iterator it = wfApproverList.iterator();
	        	while(it.hasNext())
	        	{
	        		IUser approver = (IUser)it.next();
	        		if(obj.getType() == IChange.OBJECT_TYPE)
	     	        {
	        			change.addApprovers(statusTo, new IUser[]{approver}, null, false, "");
	     	        }
	        		if(obj.getType() == IProgram.OBJECT_TYPE)
	     	        {
	        			program.addApprovers(statusTo, new IUser[]{approver}, null, false, "");
	     	        }
	        	}
            } 
            catch(APIException e) 
            {
            	System.out.println("Warning? "+e.isWarning());
            	//if Duplicate Name Entered exception occurred. Error Code: 113
				if(e.getErrorCode().toString().equals("113")) 
					;// do nothing
				else 
					throw e;
			}
	        
			//Return result
	        actResult = new ActionResult(ActionResult.STRING, "WAAA processed successfully.");
		} 
		catch(APIException ex) 
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
	
	public static void addResouceToTeamTable(IAgileSession session, IProgram program, Collection userList) throws Exception
	{
		try
		{
			//Get the Team table of a program
			ITable team = program.getTable(ProgramConstants.TABLE_TEAM);
			//Get Roles attribute values(use ITable.getAvailableValues)
			IAgileList attRolesValues = team.getAvailableValues(ProgramConstants.ATT_TEAM_ROLES);
			//Add user list into Team table with the Role "Program Team Member"
			for(int i=0; i<userList.size(); i++)
			{
				Iterator it = userList.iterator();
				while(it.hasNext())
				{
					//Get user obj
					IUser user = (IUser)it.next();
					//Set user PPM Roles
					attRolesValues.setSelection(new Object[]{"Program Team Member"});
					Map map = new HashMap();
					map.put(ProgramConstants.ATT_TEAM_NAME, user);
					map.put(ProgramConstants.ATT_TEAM_ROLES, attRolesValues);
					//Add user into Team table
					team.createRow(map);
				}
			}
		}
		catch(APIException ex)
		{
			System.out.println("Warning? "+ex.isWarning());
			//110025 Error message : Some or all of the added users are already on the Team Tab
			if(ex.getErrorCode().toString().equals("110025"))
				;//Do nothing
			else
				throw ex;
		}
		catch(Exception ex)
		{
			throw ex;
		}
	}

	public static Collection getApproverList(IAgileSession session, IDataObject obj, 
		    String ruleType, String ruleValue, String getMGRLvFrom, String getMGRLvTo) throws Exception 
	{
		//Declare vars 
		Collection approverList = new ArrayList();
		String userid = "";
		String attValue = "";
		IUser userObj = null;
		Integer attId = null;
		Integer attType = null;
		
		try 
		{
			//RULE TYPE = attribute
			if(ruleType.equals("attribute")) 
			{
				attType = AgileDataObjectUtility.getAttributeType(session, obj, ruleValue);
				attId = AgileDataObjectUtility.getP3AttributeId(obj, ruleValue);
				//Get attribute value
				if(attType.equals(DataTypeConstants.TYPE_SINGLELIST)) 
				{
					ICell cell = obj.getCell(attId);
					IAgileList cellList = (IAgileList)cell.getValue();
					IAgileList[] cellSelected = cellList.getSelection();
					if(cellSelected != null && cellSelected.length > 0) 
					{
						//Get attribute reference (User object) 
						userObj = (IUser)cellSelected[0].getValue();
						//Get user id
						String userId = userObj.getValue(UserConstants.ATT_GENERAL_INFO_USER_ID).toString();
						//Get user manager obj
						userObj = getMGRUserObj(session, userId, getMGRLvFrom, getMGRLvTo);
						//Add into approver list
						approverList.add(userObj);
					}
				}
				if(attType.equals(DataTypeConstants.TYPE_MULTILIST)) 
				{
					ICell cell = obj.getCell(attId);
					IAgileList cellList = (IAgileList)cell.getValue();
					IAgileList[] cellSelected = cellList.getSelection();
					for(int i=0; i<cellSelected.length; i++) 
					{
						//Get attribute reference (User object) 
						userObj = (IUser)cellSelected[i].getValue();
						//Get user id
						String userId = userObj.getValue(UserConstants.ATT_GENERAL_INFO_USER_ID).toString();
						//Get user manager obj
						userObj = getMGRUserObj(session, userId, getMGRLvFrom, getMGRLvTo);
						//Add into approver list
						approverList.add(userObj);
					}
				}
			} 
			//RULE TYPE = originator
			else if(ruleType.equals("originator")) 
			{
				IUser originator = null;
				if(obj.getType() == IChange.OBJECT_TYPE)
					originator = (IUser)obj.getCell(ChangeConstants.ATT_COVER_PAGE_ORIGINATOR).getReferent();
				if(obj.getType() == IProgram.OBJECT_TYPE)
					originator = (IUser)obj.getCell(ProgramConstants.ATT_GENERAL_INFO_OWNER).getReferent();

				String userId = originator.getValue(UserConstants.ATT_GENERAL_INFO_USER_ID).toString();
				userObj = getMGRUserObj(session, userId, getMGRLvFrom, getMGRLvTo);
				approverList.add(userObj);
			}
			//v1.1 RULE TYPE = user
			else if(ruleType.equals("user")) 
			{
				String[] userIds = ruleValue.split(",");
				for(int i=0; i<userIds.length; i++)
				{
					String userId = userIds[i];
					userObj = getMGRUserObj(session, userId, getMGRLvFrom, getMGRLvTo);
					approverList.add(userObj);
				}
			}
			//v1.1 RULE TYPE = group
			else if(ruleType.equals("group")) 
			{
				String[] groups = ruleValue.split(",");
				for(int i=0; i<groups.length; i++)
				{
					String group = groups[i];
					IUserGroup groupObj = AgileDataObjectUtility.getUserGroupObj(session, group);
					approverList.add(groupObj);
				}
			}
		} 
		catch(APIException ex) 
		{
			throw ex;
		} 
		catch(Exception ex) 
		{
			throw ex;
		} 
		return approverList;
	}
			
	public static IUser getMGRUserObj(IAgileSession session, String userId, 
		String getMGRLvFrom, String getMGRLvTo) throws Exception 
	{
		Connection conn = null;
		Statement stat = null;
		ResultSet rst = null;
		IUser mgrObj = null;
		//Initial user object as himself
		IUser userObj = AgileDataObjectUtility.getUserObj(session, userId);
		//System.out.println("userid="+userId);
		
		//Check & get MGR id
		if(!getMGRLvFrom.equals("") && !getMGRLvTo.equals("")) 
		{
			String sql = 
				"select * from PX_CFG_WAAA_MGR_ML where user_id = '"+userId+"' ";
			try 
			{
				conn = DBUtility.getConnection();
				stat = DBUtility.createStatement(conn);
				rst = SQLUtility.executeQuery(stat,sql);
				if(rst.next()) 
				{
					int from = Integer.parseInt(getMGRLvFrom);
					int to = Integer.parseInt(getMGRLvTo);
					while(from <= to) 
					{
						String mgrId = rst.getString("LV"+from+"_MGR_ID");
						if(mgrId != null) 
						{
							//Get MGR
							mgrObj = AgileDataObjectUtility.getUserObj(session, mgrId);
							break;
						}
						//LV+1
						from++;
					}
					//v1.2 if not get MGR, set PLM Admin as approver
					if(mgrObj == null)
					{
						mgrObj = AgileDataObjectUtility.getUserObj(session, UtilConfigReader.readProperty("PLM_admin_id"));
					}
				}
			} 
			catch(SQLException ex) 
			{
				throw ex; 
			} 
			catch(APIException ex) 
			{
				throw ex;
			}
			catch(Exception ex) 
			{
				throw ex;
			}
			finally 
			{
				DBUtility.closeConnection(conn, stat);
			}
		}
		else //Don't need to get MGR
		{
			mgrObj = userObj;
		}
	    return mgrObj;
	}	
	
}
