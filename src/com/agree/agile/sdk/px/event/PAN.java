// ===========================================================================
// Copyright 2012 AdvancedTEK International Corporation, 8F, No.303, Sec. 1, 
// Fusing S. Rd., Da-an District, Taipei City 106, Taiwan(R.O.C.); Telephone 
// +886-2-2708-5108, Facsimile +886-2-2754-4126, or <http://www.advtek.com.tw/>. 
// All rights reserved.
// ===========================================================================
// 	Version	|Author			|Comment									
// --------------------------------------------------------------------------
// 	1.0		|Loren Cheng	|Initial
// 	1.1		|Loren Cheng	|For Meiloon new requirement: attribute value substring 
// 	1.2		|Loren Cheng	|Only handle parts that is not NPP and never release
// 	1.3		|Loren Cheng	|Handle the sharing serial no that used by different subclasses
// 	1.4		|Loren Cheng	|Fix bug: null pointer exception: forwardSerialNo()
//  1.5		|Loren Cheng	|Fix bug: Modify the check criteria that if part ever released 
// ===========================================================================
package com.agree.agile.sdk.px.event;
import java.sql.*;
import java.util.*;

import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;

public class PAN implements IEventAction
{
	public EventActionResult doAction(IAgileSession session, INode actionNode, IEventInfo request) 
	{
		//Variable
		ActionResult actResult = null;
		IChange change = null;
		Connection conn = null;
 	    Statement state = null;
 	    ResultSet rst = null;
 	    StringBuffer errLog = new StringBuffer();
		String errLogSep = "_^_";
		String listValueSep = UtilConfigReader.readProperty("list_value_separator");
		//Get Non-PAN-Part list
		Vector nppCode = new Vector();
		String noPANPart = UtilConfigReader.readProperty("non_pan_part");
		String npp[] = noPANPart.split(",");
		for(int i=0; i<npp.length; i++) 
		{
			nppCode.add(npp[i]);
		}
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
				//Get item information
				IRow row = (IRow)it.next();
				IItem obj = (IItem)row.getReferent();
				String site = row.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_SITES).toString();
				ITable changeHistory = obj.getTable(ItemConstants.TABLE_CHANGEHISTORY);
				int changeHistories = changeHistory.size();
				//v1.5
				Map revisions = obj.getRevisions();
				boolean released = checkItemEverReleasedOrNot(revisions);
				System.out.println("Item released: "+released);
				
				//Only handle the item belongs to Subclass of Parts and is common site
				if(obj.getAgileClass().getSuperClass().toString().equalsIgnoreCase("Parts")
					&& site.equals("")) 
				{
					String subclassName = obj.getValue(ItemConstants.ATT_TITLE_BLOCK_PART_TYPE).toString();
				    //For Meiloon
					String partCode = subclassName.substring(0,subclassName.indexOf("_"));
				    //v1.2 Only handle the parts that is not Non-PNP and never released 
				    if(!nppCode.contains(partCode) && released == false) 
				    {
				    	String newNo = "";
				    	//Get obj number
				    	String partNo = obj.getName();
				    	//Get px config: PX_CFG_PAN
				    	String sqls = 
				    		" select * from PX_CFG_PAN " + 
				    		" where SUBCLASS_NAME = '"+subclassName+"'" + 
				    		" order by combine_seq ";
				    	conn = DBUtility.getConnection();
			    	   	state = DBUtility.createStatement(conn);
			    	   	rst = SQLUtility.executeQuery(state,sqls);
			    	   	//Handle PAN config
			    	   	while(rst.next()) 
			    	   	{
			    	   		String dataType = rst.getString("COMBINE_DATA_TYPE");
			    	   		String dataValue = rst.getString("COMBINE_DATA_VALUE");
			    	   		String dataComment = rst.getString("COMBINE_DATA_COMMENT");
			    	   		String dataComment2 = rst.getString("COMBINE_DATA_COMMENT_2");
			    	   		String dataComment3 = rst.getString("COMBINE_DATA_COMMENT_3");
			    	   		dataComment2 = dataComment2==null? "" : dataComment2;
			    	   		dataComment3 = dataComment3==null? "" : dataComment3.toLowerCase();
			    	   		int panLength = dataComment2==""? 0 : Integer.parseInt(dataComment2);
			    	   		
			    	   		//**DATA TYPE = string**
			    	   		if(dataType.equals("string")) 
			    	   		{
			    	   			newNo += dataValue;
			    	   		}
			    	   		//**DATA TYPE = attribute**
			    	   		if(dataType.equals("attribute")) 
			    	   		{
			    	   			String attValue = AgileDataObjectUtility.getAttributeValue(session, obj, dataValue);
			    	   			Integer attType = AgileDataObjectUtility.getAttributeType(session, obj, dataValue);
			    	   			//Debug
			    	   			//System.out.println("attribute type="+dataValue);
			    	   			//System.out.println("attribute value="+value);
			    	   			//If attribute value not null
			    	   			if(!attValue.equals("")) 
			    	   			{
			    	   				//If attribute type is LIST
			    	   				if(attType.equals(DataTypeConstants.TYPE_SINGLELIST)) 
			    	   				{
			    	   					attValue = AgileDataObjectUtility.getListOptionCode(attValue, listValueSep);
			    	   				}
			    	   				//Version 1.1 for Meiloon
			    	   				//If attribute type is TEXT
			    	   				if(attType.equals(DataTypeConstants.TYPE_STRING) || 
			    	   					attType.equals(DataTypeConstants.TYPE_INTEGER) || 
			    	   					attType.equals(DataTypeConstants.TYPE_DOUBLE) ) 
			    	   				{
			    	   					if(!dataComment2.equals(""))
			    	   					{
			    	   						if(attValue.length() >= panLength)
			    	   						{
			    	   							if(dataComment3.equals("forward"))
			    	   							{
			    	   								attValue = attValue.substring(0, panLength);
			    	   							}
			    	   							else if(dataComment3.equals("backward"))
			    	   							{
			    	   								attValue = attValue.substring(attValue.length()-panLength, attValue.length());
			    	   							}
			    	   						}
			    	   						else if(attValue.length() < panLength)
			    	   						{
			    	   							attValue = checkSerialNoLength(attValue, panLength);
			    	   						}
			    	   					}
			    	   				}
			    	   				//Combine attribute value
			    	   				newNo += attValue;
			    	   			} 
			    	   			//If attribute value is null
			    	   			else
			    	   			{
			    	   				//0823: Meiloon allows PAN column value is empty value.
			    	   				//errMsg += errLogSep + "çµ„æ?æ¬„ä?["+dataComment+"]æ²’æ?è³‡æ?;";
			    	   			}
			    	   		}
			    	   		//**DATA TYPE = serial**
			    	   		if(dataType.equals("serial")) 
			    	   		{
		    	   				//Get serial no.
		    	   				String serialNo = getSerialNo(subclassName, partNo, obj);
			    	   			//if serial no. is null
		    	   				if(serialNo.equals(""))	
		    	   				{
			    	   				errMsg += errLogSep + "PX_CFG_PAN_SN çµ„æ?è¨­å?æ²’æ?["+subclassName+"]?„æ?æ°´ç¢¼è¨­å?;";
			    	   			} 
		    	   				else //if serial no. return 
		    	   				{
			    	   				newNo += serialNo;
			    	   			}
		    	   			}
			    	   	}//End: while() for each affected item
			    	   	
			    	   	//If error message is not null
			    	   	if(!errMsg.equals("")) 
			    	   	{
			    	   		errLog.append(subclassName + errLogSep + partNo + errMsg);
			    	   	} 
			    	   	//if newNo is null
			    	   	else if(newNo.equals("")) 
			    	   	{
			    	   		errLog.append(subclassName + errLogSep + partNo + errLogSep + "PX_CFG_PAN çµ„æ?è¨­å?æ²’æ?["+subclassName+"]?„è¨­å®šã?");
			    	   	}
			    	   	//Set new number of part (Upper Case)
			    	   	else 
			    	   	{
			    	   		obj.setValue(ItemConstants.ATT_TITLE_BLOCK_NUMBER, newNo.toUpperCase());
			    	   	}//One process end.
				    }
				}
			}//End: while() for all affected items
			
			//if no error message
			if(errLog.toString().equals("")) 
			{
				actResult = new ActionResult(ActionResult.STRING, "PAN processed successfully.");
			} 
			else 
			{
				errLog.insert(0, "PAN failed below:\n");
				throw new Exception(errLog.toString());
			}
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
			DBUtility.closeConnection(conn, state);
		}
		return new EventActionResult(request, actResult);
	}
	
	public static String getSerialNo(String subclassName, String partNo, IItem obj) throws Exception 
	{
		String result = "";
		Connection conn = null;
		Statement state = null;
		ResultSet rs = null;
		
		try 
		{
			conn = DBUtility.getConnection();
			state = DBUtility.createStatement(conn);
			//Get px config: PX_CFG_PAN_SN
			rs = SQLUtility.executeQuery(state, 
				" SELECT serial_no, part_tempno_prefix, serial_no_length, serial_no_sharing " + 
			    " FROM PX_CFG_PAN_SN " + 
				" WHERE subclass_name = '"+subclassName+"' ");
			//Handle serial no. rule
			if(rs.next()) 
			{
				//Get current serial no.
				String serialNo = rs.getString("serial_no");
				String tempPartNoPrefix = rs.getString("part_tempno_prefix");
				int serialNoLength = Integer.parseInt(rs.getString("serial_no_length"));
				String serialNoSharing = rs.getString("serial_no_sharing");
				
				//v1.4
				serialNoSharing = serialNoSharing==null? "" : serialNoSharing;
				
				//If part no. is temporary
				if(partNo.startsWith(tempPartNoPrefix)) 
				{
					//Check serial no length
					result = checkSerialNoLength(serialNo, serialNoLength);
					//Serial no. + 1
					forwardSerialNo(subclassName, serialNoSharing);
					//Record serial no. on Part page two
					obj.setValue(ItemConstants.ATT_PAGE_TWO_TEXT01, result);
					
				}
				//If part no. is not temporary
				else 
				{
					//Get the assigned SN from Part page two
					result = obj.getValue(ItemConstants.ATT_PAGE_TWO_TEXT01).toString();
				}
			}
		} 
		catch(SQLException se) 
		{ 
			throw se; 
		} 
		catch(Exception ex) 
		{ 
			throw ex; 
		} 
		finally 
		{ 
			DBUtility.closeConnection(conn, state); 
		}
		return result;
	}

	public static void forwardSerialNo(String subclassName, String serialNoSharing) throws Exception 
	{
		Connection conn = null;
		Statement state = null;
		int rs = 0;
		
		try 
		{
			conn = DBUtility.getConnection();
			state = DBUtility.createStatement(conn);
			//STEP 1: Serial No + 1
			rs = SQLUtility.executeUpdate(state, 
				" UPDATE PX_CFG_PAN_SN "+
				" SET serial_no = serial_no+1 "+
				" WHERE subclass_name = '"+subclassName+"' ");
			//STEP 2: Update the serial no of other sharing subclass
			if(!serialNoSharing.equals(""))
			{
				String[] sn = serialNoSharing.split(",");
				for(int i=0; i<sn.length; i++)
				{
					SQLUtility.executeUpdate(state, 
						" UPDATE PX_CFG_PAN_SN "+
						" SET serial_no = (SELECT serial_no FROM PX_CFG_PAN_SN WHERE subclass_name = '"+subclassName+"') "+
						" WHERE subclass_name = '"+sn[i]+"' ");
				}
			}
		} 
		catch(SQLException ex) 
		{
			throw ex;
		}
		catch(Exception ex) 
		{
			throw ex;
		}
		finally 
		{
			DBUtility.closeConnection(conn, state);
		}
	}
	
	public static String checkSerialNoLength(String serialNo, int length) throws Exception
	{
		try 
		{
			int snLength = serialNo.length();
			for(int i=0; i<length-snLength; i++) 
			{
				serialNo = "0"+serialNo;
			}
		} 
		catch(Exception ex) 
		{
			throw ex;
		} 
		return serialNo;
	}
	
	public static boolean checkItemEverReleasedOrNot(Map revisions) throws Exception
	{
		// Get the set view of the map
        Set set = revisions.entrySet();
        // Get an iterator for the set
        Iterator it = set.iterator();
        // Iterate through the revisions and set each revision value
        while (it.hasNext()) 
        {
       	 	Map.Entry entry = (Map.Entry)it.next();
       	 	String rev = (String)entry.getValue();
       	 	System.out.println("Rev : " + rev + "....");
       	 	
       	 	if(!rev.equalsIgnoreCase("Introductory") && !rev.matches("[(].*[)]"))
       	 		return true;
        }
        return false;
	}
}
