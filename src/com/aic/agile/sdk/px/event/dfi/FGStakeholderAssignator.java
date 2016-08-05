//: FGApproverAssignator.java
package com.aic.agile.sdk.px.event.dfi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.agile.api.*;
import com.agile.px.*;
import com.aic.agile.sdk.util.ExcelUtility;
import com.aic.agile.sdk.util.UtilConfigReader;

/**
 * �u�@�y�{ñ�֦۰ʫ������~�Q�`���Y�H.
 * <p>�ھ�¾����(Job Function)�B���~�Ƹ����Ȥ�(Customer)�B���~�Ƹ����t�C(Series)�B<br>
 * Change.Project Type�BChange.All Model Change�A�j�MPLM�t�Χ�쥿�T���u�@�y�{ñ�̡֪C<br>
 * <p>�U¾����H���j�M����:<br>
 * �j�M���~�t�d�~��: User��¾����(Job Function)=Sales and �t�d�Ȥ�(Customer)�]�t���~�Ƹ����Ȥ�(Customer)<br>
 * �j�M���~�t�dEE: User��¾����(Job Function)=EE and �t�d�t�C(Series)�]�t���~�Ƹ����t�C(Series)<br>
 * �j�M���~�t�dPM: User��¾����(Job Function)=PM and �t�d�t�C(Series)�]�t���~�Ƹ����t�C(Series)<br>
 * �j�M���~�t�dSE: User��¾����(Job Function)=SE and �t�d�t�C(Series)�]�t���~�Ƹ����t�C(Series)<br>
 * <p>�U�y�{�����W�h�Ѧ�DFI_PX_CONFIG�����]�w:<br>
 * FG Stakeholder Assignator<br>
 * FG Stakeholder Assignator (FG)<br>
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class FGStakeholderAssignator implements IEventAction {
	final String DFI_PX_CONFIG = UtilConfigReader.readProperty("DFI_PX_CONFIG");
	final String SHEET_NAME = "FG Stakeholder Assignator";
	final String SHEET_NAME_2 = "FG Stakeholder Assignator (FG)";
	//final String FG_REGEX = "(750|770|777|790|7A0).+";
	ActionResult actionResult = null;
	ArrayList<String> customerList = new ArrayList<String>();
	ArrayList<String> seriesList = new ArrayList<String>();
	ArrayList<IUser> approverList = new ArrayList<IUser>();
	ArrayList<IUser> approverListNonDuplicate = new ArrayList<IUser>();
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
 	    	//�ϧOEvent Type
 	    	int eventType = object.getEventType();
 	    	//���oEvent Name
 	    	String eventName = object.getEventName();
 	    	//���oChange Type��T
			String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString(); 
			//���oChange API Prefix(3 digits)
 	    	String changeAPIName = change.getAgileClass().getAPIName();
			String changeAPIPrefix = changeAPIName.substring(0, 3);
			//debug
			//System.out.println("eventName is "+eventName);
			//System.out.println("changeType is "+changeType);
			//System.out.println("changeAPIPrefix is "+changeAPIPrefix);
			
			/*
			 * ���oChange.Project Type
			 */
			String projectType = change.getCell("PROJECT_TYPE")==null? "" : change.getCell("PROJECT_TYPE").getValue().toString();
			/*
			 * ���oChange.All Model Change
			 */
			String allModelChange = change.getCell("ALL_MODEL_CHANGE")==null? "" : change.getCell("ALL_MODEL_CHANGE").getValue().toString();
			/*
			 * ���oChange.Originator
			 */
			IUser originator = (IUser)change.getCell(ChangeConstants.ATT_COVER_PAGE_ORIGINATOR).getReferent();
			//debug
			//System.out.println("projectType is "+projectType);
			//System.out.println("allModelChange is "+allModelChange);
			//System.out.println("Originator OID is "+originator.getObjectId().toString());
			
			/*
			 * ���oDFI_PX_CONFIG - FG Stakeholder Assignator (FG)
			 * ���~�Ƹ��l�����e�T�X���`�W��ܦ�(Regular Expression)
			 * |FG Subclass RegEx|
			 */
			Sheet sheet2 = ExcelUtility.getDataSheet(DFI_PX_CONFIG, SHEET_NAME_2);
			if(sheet2==null) {
				throw new Exception("[DFI_PX_CONFIG - FG Stakeholder Assignator (FG)] can not be found. ");
			}
			String FG_REGEX = sheet2.getRow(1).getCell(0).getStringCellValue();
			
			//���oAffected Items(AI) Table
			ITable changeAITable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			/*
			 * Loop�ˬd�Ҧ�AI
			 */
			/* Fix 2/19 ECR���h���~�ܧ�A��Sales Approve�o���S���H
			 * FGStakeholderAssignator�h�P�_:
   			 * a.���~Customer���ūh����
   			 * b.���~��Part Type=Part�h����
   			 * c.���~Series���ūh����
			 */
			Iterator changeAI = changeAITable.iterator();
			while(changeAI.hasNext()) {
				//���oAffected Item (AI)
				IRow ai = (IRow)changeAI.next();
				//���oAI�Ȥ�B���~�t�C
				String customer = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_P2_LIST14).getValue().toString();
				String series = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_P2_LIST04).getValue().toString();
				//���oAI Part Type
				String partType = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).getValue().toString();
				/*
				 * �P�_�O���~���O�~�B�z(750/770/777/790/7A0�}�Y)
				 */
				if(partType.matches(FG_REGEX)) {
					//���~Customer���ūh����
					if(customer.equals("")) {
						continue;
					}
					ICustomer customerObj = (ICustomer)ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_P2_LIST14).getReferent();
					String customerNo = customerObj.getCell(CustomerConstants.ATT_GENERAL_INFO_CUSTOMER_NUMBER).getValue().toString();
					//���~Series���ūh����
					if(series.equals("")) {
						continue;
					}
					//�������~�Ȥ�B���~�t�C
					customerList.add(customerNo);
					seriesList.add(series);
				}
			}
			/*
			 * �Ȥ�Ψt�C��C���šA�ߥXException����ʧ@
			 */
			if(customerList.isEmpty()) {
				throw new Exception("WARNING! - No F/G customer information. ");
			}
			if(seriesList.isEmpty()) {
				throw new Exception("WARNING! - No F/G series information. ");
			}
			//debug
			/*
			Object[] cusObjs = customerList.toArray();
			Object[] serObjs = seriesList.toArray();
			for(Object cusObj : cusObjs) {
				System.out.println("Customer is "+cusObj.toString());
			}
			for(Object serObj : serObjs) {
				System.out.println("Series is "+serObj.toString());
			}
			*/
			
			/*
			 * ���o���~�t�d�H��¾����(Job Function)
			 * DFI_PX_CONFIG - FG Stakeholder Assignator
			 * |Event Name|Change.Project Type|Change.All Model Change|Job Function of Approver (Separator is Comma)|
			 */
			Sheet sheet = ExcelUtility.getDataSheet(DFI_PX_CONFIG, SHEET_NAME);
			//�Q��Event Subscriber��Event Name�BChange.Project Type�BChange.All Model Change�A�z��X���T�����~�t�d�H��¾����
			//�p�G���h��¾����|�H�r��(,)���j
			List<Row> list = ExcelUtility.filterDataSheet(sheet, 0, eventName);
			Iterator<Row> it = list.iterator();
			String[] jobFunctions = null; //Job Function List Separator is comma(,)
			//Loop all specific-event rules
			while(it.hasNext()) {
				Row row = it.next();
				String changeProjectType = ExcelUtility.getSpecificCellValue(row, 1);
				String changeAllModelNo = ExcelUtility.getSpecificCellValue(row, 2);
				String jobFunctionList = ExcelUtility.getSpecificCellValue(row, 3);
				//debug
				//System.out.println("changeProjectType is "+changeProjectType);
				//System.out.println("changeAllModelNo is "+changeAllModelNo);
				//System.out.println("jobFunctionList is "+jobFunctionList);
				
				//�p�GDFI_PX_CONFIG��...
				//Change Project Type �P Change All Model Change ���O�ŭȡA������Job Function List
				if(changeProjectType.equals("") && changeAllModelNo.equals("")) {
					jobFunctions = jobFunctionList.split(",");
					break;
				} 
				//Change Project Type�����šA�n�P�_�O�_�PChange Object.Project Type�O�_�ۦP
				else if(!changeProjectType.equals("") && changeAllModelNo.equals("")) {
					if(changeProjectType.matches(".*("+projectType+").*")) {
						jobFunctions = jobFunctionList.split(",");
						break;
					}
				} 
				//Change All Model Change�����šA�n�P�_�O�_�PChange Object.All Model Change�O�_�ۦP
				else if(changeProjectType.equals("") && !changeAllModelNo.equals("")) {
					if(changeAllModelNo.equals(allModelChange)) {
						jobFunctions = jobFunctionList.split(",");
						break;
					}
				}
				//Change Project Type�PChange All Model Change�����šA�n�P�_�O�_�PChange Object.Project Type, Change Object.All Model Change�O�_�ۦP
				else if(!changeProjectType.equals("") && !changeAllModelNo.equals("")) {
					if(changeProjectType.matches(".*("+projectType+").*") && changeAllModelNo.equals(allModelChange)) {
						jobFunctions = jobFunctionList.split(",");
						break;
					}
				}
			}
			
			/*
			 * �d�ߦ��~�t�d�H�A�å[�JApprover List
			 */
			if(jobFunctions != null) {
				//debug
				//System.out.println("jobFunctions is "+jobFunctions.toString());
				for(String jobFunction : jobFunctions) {
					//�p�GJob Function=Sales�A�ھګȤ��~��
					if(jobFunction.equals("Sales")) {
						getFGSales(session, jobFunction, customerList, projectType, originator);
					}
					//�p�GJob Function=PM,EE,SE�A�ھڲ��~�t�C������H��(PM/EE/SE/...)
					else if(jobFunction.equals("PM") || jobFunction.equals("EE") || jobFunction.equals("SE")){
						getFGNonSalesStakeholder(session, jobFunction, seriesList);
					} 
					//�p�GJob Function�O$�}�Y�A��$��r���@API Name�A�d��Change���A���o�ѷӪ�IUser����
					else if(jobFunction.startsWith("$")) {
						IUser user = (IUser)change.getCell(jobFunction.substring(1)).getReferent();
						approverList.add(user);
					}
				}
			}
			//debug
			/*
			Object[] apprObjs = approverList.toArray();
			for(Object apprObj : apprObjs) {
				System.out.println("Approver is "+((IUser)apprObj).getName());
			}
			*/
			
			/*
			 * �N���~�t�d�H�@��Workflow��e���A��Approvers
			 */
			//�������Ҧ�Approver�A�A�s�WApprover�A�קKException�y�����_
			ISignoffReviewer[] currentApprovers = change.getAllReviewers(change.getStatus(), WorkflowConstants.USER_APPROVER);
			ArrayList<IUser> curApproverList = new ArrayList<IUser>();
			for(ISignoffReviewer approver : currentApprovers) {
				IUser user = (IUser)approver.getReviewer();
				curApproverList.add(user);
			}
			change.removeReviewers(change.getStatus(), curApproverList, null, null, "Remove the current approvers.");
			//�s�WApprover�e�A���L�o�����ƪ�Approver
			approverListNonDuplicate = removeDuplicateApprover(approverList);
			
			/*
			 * �P�_�O�_���o��Approver List:
			 * �p�G���AAdd Approver
			 */
			boolean getApproverList = approverListNonDuplicate.isEmpty()==true? false:true;
			//�Nboolean�[�JCustom Map
			Map map = new HashMap();
			map.put("getApproverList",getApproverList);
			request.setUserDefinedMap(map);
			//�P�_�O�_���o��Approver List
			if(getApproverList) {
				//�s�WApprover
				change.addReviewers(change.getStatus(), approverListNonDuplicate, null, null, false, "Add F/G stakeholders.");
				actionResult = new ActionResult(ActionResult.STRING, "Add F/G stakeholder completely.");
			} else {
				actionResult = new ActionResult(ActionResult.STRING, "F/G stakeholder not found.");
			}
			
		} catch(Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
		}
		//�Ǧ^Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	
	/**
	 * ���o�D�~�ȳ�쪺���~�t�d�H(PM/EE/SE)
	 * @param session �ϥΪ�Session
	 * @param jobFunction ¾����
	 * @param seriesList �t�C��C
	 * @throws Exception throw when exception is happened
	 */
	public void getFGNonSalesStakeholder(IAgileSession session, String jobFunction, 
			ArrayList<String> seriesList) throws Exception {
		try {
			//�]�w�d�߱���
			String condition = 
					"SELECT * " +
	        		"FROM " +
	        		"	[UsersClass] " +
	        		"WHERE " +
	        		"	[General Info.Job Function(s)] equal to %0 And "+
	        		"	[General Info.Series] contains any %1 ";
			//�إ߬d��
	        IQuery query = (IQuery)session.createObject(IQuery.OBJECT_TYPE, condition);
	        //�]�w�d�߱���Ѽƭ�
	        query.setParams(new Object[]{
	        			jobFunction, 
	        			seriesList.toArray()});
	        //����d��
	        ITable results = query.execute();
	        //�N�d�ߪ�User�[�JApprovers List
	        Iterator<IRow> it = results.iterator();
	        while(it.hasNext()) {
	        	IUser user = (IUser)(it.next()).getReferent();
	        	approverList.add(user);
	        }
		} catch(Exception ex) {
			throw ex;
		}
	}
	/**
	 * ���o���~�t�d�~��(Sales)
	 * @param session �ϥΪ�Session
	 * @param jobFunction ¾����
	 * @param customerList �Ȥ��C
	 * @param projectType �M�����O(STD/ODM/DEV)
	 * @throws Exception throw when exception is happened
	 */
	public void getFGSales(IAgileSession session, String jobFunction, 
			ArrayList<String> customerList, String projectType, IUser originator) throws Exception {
		try {
			//�妸�d��
			Iterator<String> it = customerList.iterator();
			while(it.hasNext()) {
				//���oCustomer Number
				String customerNo = it.next();
				//�]�w�d�߱���
				//�d����줺�e�p�G�OObjects List�A�����z�L�_������d��(Using the Nested Criteria to Search for Values in Object Lists)
				String condition = 
						"SELECT * " +
		        		"FROM " +
		        		"	[UsersClass] " +
		        		"WHERE " +
		        		"	[General Info.Job Function(s)] equal to '"+jobFunction+"' And "+
		        		"	[General Info.Customer] contains any ([General Info.Customer Number] equal to '"+customerNo+"') ";
				//�إ߬d��
		        IQuery query = (IQuery)session.createObject(IQuery.OBJECT_TYPE, condition);
		        //����d��
		        ITable results = query.execute();
		        //�N�d�ߪ�User�[�JApprovers List
		        Iterator<IRow> it2 = results.iterator();
		        while(it2.hasNext()) {
		        	IUser user = (IUser)(it2.next()).getReferent();
		        	//debug
		        	//System.out.println("Approver User OID is "+user.getObjectId().toString());
		        	//�p�G (Project Type=STD) or (Sales�OECR�ӽФH)�A�������[�JApprover List
		        	if(projectType.equals("STD") || user.getObjectId().toString().equals(originator.getObjectId().toString())) {
		        		continue;
		        	} else {
		        		approverList.add(user);
		        	}	
		        }
			}
		} catch(Exception ex) {
			throw ex;
		}
	}
	/**
	 * �^�ǨS��User���ƪ�Approver�M��
	 * @param approverList �ھڦ��~�Ȥ�B�t�C�o�쪺Approver�M��
	 * @return �S��User���ƪ�Approver�M��
	 * @throws Exception throw when exception is happened
	 */
	private ArrayList<IUser> removeDuplicateApprover(ArrayList<IUser> approverList) throws Exception {
		ArrayList<IUser> userList = new ArrayList<IUser>();
		try {
			Iterator<IUser> it = approverList.iterator();
			while(it.hasNext()) {
				IUser user = it.next();
				if(!userList.contains(user)) {
					userList.add(user);
				}
			}
			return userList;
		} catch(Exception e) {
			throw e;
		}
	}
}
///:~
	