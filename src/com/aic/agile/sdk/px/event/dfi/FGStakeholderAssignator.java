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
 * 工作流程簽核自動指派成品利害關係人.
 * <p>根據職能單位(Job Function)、成品料號的客戶(Customer)、成品料號的系列(Series)、<br>
 * Change.Project Type、Change.All Model Change，搜尋PLM系統找到正確的工作流程簽核者。<br>
 * <p>各職能單位人員搜尋條件:<br>
 * 搜尋成品負責業務: User的職能單位(Job Function)=Sales and 負責客戶(Customer)包含成品料號的客戶(Customer)<br>
 * 搜尋成品負責EE: User的職能單位(Job Function)=EE and 負責系列(Series)包含成品料號的系列(Series)<br>
 * 搜尋成品負責PM: User的職能單位(Job Function)=PM and 負責系列(Series)包含成品料號的系列(Series)<br>
 * 搜尋成品負責SE: User的職能單位(Job Function)=SE and 負責系列(Series)包含成品料號的系列(Series)<br>
 * <p>各流程指派規則參考DFI_PX_CONFIG相關設定:<br>
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
	 * Agile PLM Event PX 進入點.
	 * @param session 使用者Session
	 * @param actionNode 觸發程式的位置
	 * @param request Event物件
	 */
	public EventActionResult doAction(IAgileSession session, INode actionNode, IEventInfo request) {
		try {
 	    	//取得觸發Event的物件
 	    	IObjectEventInfo object = (IObjectEventInfo) request;
 	    	//轉型Change物件
 	    	IChange change = (IChange)object.getDataObject();
 	    	//區別Event Type
 	    	int eventType = object.getEventType();
 	    	//取得Event Name
 	    	String eventName = object.getEventName();
 	    	//取得Change Type資訊
			String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString(); 
			//取得Change API Prefix(3 digits)
 	    	String changeAPIName = change.getAgileClass().getAPIName();
			String changeAPIPrefix = changeAPIName.substring(0, 3);
			//debug
			//System.out.println("eventName is "+eventName);
			//System.out.println("changeType is "+changeType);
			//System.out.println("changeAPIPrefix is "+changeAPIPrefix);
			
			/*
			 * 取得Change.Project Type
			 */
			String projectType = change.getCell("PROJECT_TYPE")==null? "" : change.getCell("PROJECT_TYPE").getValue().toString();
			/*
			 * 取得Change.All Model Change
			 */
			String allModelChange = change.getCell("ALL_MODEL_CHANGE")==null? "" : change.getCell("ALL_MODEL_CHANGE").getValue().toString();
			/*
			 * 取得Change.Originator
			 */
			IUser originator = (IUser)change.getCell(ChangeConstants.ATT_COVER_PAGE_ORIGINATOR).getReferent();
			//debug
			//System.out.println("projectType is "+projectType);
			//System.out.println("allModelChange is "+allModelChange);
			//System.out.println("Originator OID is "+originator.getObjectId().toString());
			
			/*
			 * 取得DFI_PX_CONFIG - FG Stakeholder Assignator (FG)
			 * 成品料號子分類前三碼的常規表示式(Regular Expression)
			 * |FG Subclass RegEx|
			 */
			Sheet sheet2 = ExcelUtility.getDataSheet(DFI_PX_CONFIG, SHEET_NAME_2);
			if(sheet2==null) {
				throw new Exception("[DFI_PX_CONFIG - FG Stakeholder Assignator (FG)] can not be found. ");
			}
			String FG_REGEX = sheet2.getRow(1).getCell(0).getStringCellValue();
			
			//取得Affected Items(AI) Table
			ITable changeAITable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			/*
			 * Loop檢查所有AI
			 */
			/* Fix 2/19 ECR做多產品變更，到Sales Approve卻都沒有人
			 * FGStakeholderAssignator多判斷:
   			 * a.成品Customer為空則忽略
   			 * b.成品的Part Type=Part則忽略
   			 * c.成品Series為空則忽略
			 */
			Iterator changeAI = changeAITable.iterator();
			while(changeAI.hasNext()) {
				//取得Affected Item (AI)
				IRow ai = (IRow)changeAI.next();
				//取得AI客戶、成品系列
				String customer = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_P2_LIST14).getValue().toString();
				String series = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_P2_LIST04).getValue().toString();
				//取得AI Part Type
				String partType = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).getValue().toString();
				/*
				 * 判斷是成品類別才處理(750/770/777/790/7A0開頭)
				 */
				if(partType.matches(FG_REGEX)) {
					//成品Customer為空則忽略
					if(customer.equals("")) {
						continue;
					}
					ICustomer customerObj = (ICustomer)ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_P2_LIST14).getReferent();
					String customerNo = customerObj.getCell(CustomerConstants.ATT_GENERAL_INFO_CUSTOMER_NUMBER).getValue().toString();
					//成品Series為空則忽略
					if(series.equals("")) {
						continue;
					}
					//收集成品客戶、成品系列
					customerList.add(customerNo);
					seriesList.add(series);
				}
			}
			/*
			 * 客戶或系列串列為空，拋出Exception中止動作
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
			 * 取得成品負責人的職能單位(Job Function)
			 * DFI_PX_CONFIG - FG Stakeholder Assignator
			 * |Event Name|Change.Project Type|Change.All Model Change|Job Function of Approver (Separator is Comma)|
			 */
			Sheet sheet = ExcelUtility.getDataSheet(DFI_PX_CONFIG, SHEET_NAME);
			//利用Event Subscriber的Event Name、Change.Project Type、Change.All Model Change，篩選出正確的成品負責人的職能單位
			//如果有多個職能單位會以逗號(,)分隔
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
				
				//如果DFI_PX_CONFIG的...
				//Change Project Type 與 Change All Model Change 都是空值，直接取Job Function List
				if(changeProjectType.equals("") && changeAllModelNo.equals("")) {
					jobFunctions = jobFunctionList.split(",");
					break;
				} 
				//Change Project Type不為空，要判斷是否與Change Object.Project Type是否相同
				else if(!changeProjectType.equals("") && changeAllModelNo.equals("")) {
					if(changeProjectType.matches(".*("+projectType+").*")) {
						jobFunctions = jobFunctionList.split(",");
						break;
					}
				} 
				//Change All Model Change不為空，要判斷是否與Change Object.All Model Change是否相同
				else if(changeProjectType.equals("") && !changeAllModelNo.equals("")) {
					if(changeAllModelNo.equals(allModelChange)) {
						jobFunctions = jobFunctionList.split(",");
						break;
					}
				}
				//Change Project Type與Change All Model Change不為空，要判斷是否與Change Object.Project Type, Change Object.All Model Change是否相同
				else if(!changeProjectType.equals("") && !changeAllModelNo.equals("")) {
					if(changeProjectType.matches(".*("+projectType+").*") && changeAllModelNo.equals(allModelChange)) {
						jobFunctions = jobFunctionList.split(",");
						break;
					}
				}
			}
			
			/*
			 * 查詢成品負責人，並加入Approver List
			 */
			if(jobFunctions != null) {
				//debug
				//System.out.println("jobFunctions is "+jobFunctions.toString());
				for(String jobFunction : jobFunctions) {
					//如果Job Function=Sales，根據客戶找業務
					if(jobFunction.equals("Sales")) {
						getFGSales(session, jobFunction, customerList, projectType, originator);
					}
					//如果Job Function=PM,EE,SE，根據產品系列找相關人員(PM/EE/SE/...)
					else if(jobFunction.equals("PM") || jobFunction.equals("EE") || jobFunction.equals("SE")){
						getFGNonSalesStakeholder(session, jobFunction, seriesList);
					} 
					//如果Job Function是$開頭，用$後字串當作API Name，查找Change欄位，取得參照的IUser物件
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
			 * 將成品負責人作為Workflow當前狀態的Approvers
			 */
			//先移除所有Approver，再新增Approver，避免Exception造成中斷
			ISignoffReviewer[] currentApprovers = change.getAllReviewers(change.getStatus(), WorkflowConstants.USER_APPROVER);
			ArrayList<IUser> curApproverList = new ArrayList<IUser>();
			for(ISignoffReviewer approver : currentApprovers) {
				IUser user = (IUser)approver.getReviewer();
				curApproverList.add(user);
			}
			change.removeReviewers(change.getStatus(), curApproverList, null, null, "Remove the current approvers.");
			//新增Approver前，先過濾掉重複的Approver
			approverListNonDuplicate = removeDuplicateApprover(approverList);
			
			/*
			 * 判斷是否有得到Approver List:
			 * 如果有，Add Approver
			 */
			boolean getApproverList = approverListNonDuplicate.isEmpty()==true? false:true;
			//將boolean加入Custom Map
			Map map = new HashMap();
			map.put("getApproverList",getApproverList);
			request.setUserDefinedMap(map);
			//判斷是否有得到Approver List
			if(getApproverList) {
				//新增Approver
				change.addReviewers(change.getStatus(), approverListNonDuplicate, null, null, false, "Add F/G stakeholders.");
				actionResult = new ActionResult(ActionResult.STRING, "Add F/G stakeholder completely.");
			} else {
				actionResult = new ActionResult(ActionResult.STRING, "F/G stakeholder not found.");
			}
			
		} catch(Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
		}
		//傳回Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	
	/**
	 * 取得非業務單位的成品負責人(PM/EE/SE)
	 * @param session 使用者Session
	 * @param jobFunction 職能單位
	 * @param seriesList 系列串列
	 * @throws Exception throw when exception is happened
	 */
	public void getFGNonSalesStakeholder(IAgileSession session, String jobFunction, 
			ArrayList<String> seriesList) throws Exception {
		try {
			//設定查詢條件
			String condition = 
					"SELECT * " +
	        		"FROM " +
	        		"	[UsersClass] " +
	        		"WHERE " +
	        		"	[General Info.Job Function(s)] equal to %0 And "+
	        		"	[General Info.Series] contains any %1 ";
			//建立查詢
	        IQuery query = (IQuery)session.createObject(IQuery.OBJECT_TYPE, condition);
	        //設定查詢條件參數值
	        query.setParams(new Object[]{
	        			jobFunction, 
	        			seriesList.toArray()});
	        //執行查詢
	        ITable results = query.execute();
	        //將查詢的User加入Approvers List
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
	 * 取得成品負責業務(Sales)
	 * @param session 使用者Session
	 * @param jobFunction 職能單位
	 * @param customerList 客戶串列
	 * @param projectType 專案類別(STD/ODM/DEV)
	 * @throws Exception throw when exception is happened
	 */
	public void getFGSales(IAgileSession session, String jobFunction, 
			ArrayList<String> customerList, String projectType, IUser originator) throws Exception {
		try {
			//批次查詢
			Iterator<String> it = customerList.iterator();
			while(it.hasNext()) {
				//取得Customer Number
				String customerNo = it.next();
				//設定查詢條件
				//查詢欄位內容如果是Objects List，必須透過巢狀條件查詢(Using the Nested Criteria to Search for Values in Object Lists)
				String condition = 
						"SELECT * " +
		        		"FROM " +
		        		"	[UsersClass] " +
		        		"WHERE " +
		        		"	[General Info.Job Function(s)] equal to '"+jobFunction+"' And "+
		        		"	[General Info.Customer] contains any ([General Info.Customer Number] equal to '"+customerNo+"') ";
				//建立查詢
		        IQuery query = (IQuery)session.createObject(IQuery.OBJECT_TYPE, condition);
		        //執行查詢
		        ITable results = query.execute();
		        //將查詢的User加入Approvers List
		        Iterator<IRow> it2 = results.iterator();
		        while(it2.hasNext()) {
		        	IUser user = (IUser)(it2.next()).getReferent();
		        	//debug
		        	//System.out.println("Approver User OID is "+user.getObjectId().toString());
		        	//如果 (Project Type=STD) or (Sales是ECR申請人)，忽略不加入Approver List
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
	 * 回傳沒有User重複的Approver清單
	 * @param approverList 根據成品客戶、系列得到的Approver清單
	 * @return 沒有User重複的Approver清單
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
	