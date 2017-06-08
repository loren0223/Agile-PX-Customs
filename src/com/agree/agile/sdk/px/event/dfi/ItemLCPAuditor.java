//: ItemLCPAuditor.java
package com.agree.agile.sdk.px.event.dfi;
import java.util.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;
/**
 * Item生命週期(LCP)的設定與檢核.
 * <p>根據DFI規則，當Event Type是Update Table(Affected Items)或是Extend Action Menu，設定Item生命週期；
 * 當Event Type是Change Status for Workflow，檢查Item生命週期，通過才允許進入下一關。<br>
 * <p>DFI_PX_CONFIG相關設定:<br>
 * Item LCP Auditor<br>
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class ItemLCPAuditor implements IEventAction {
	final String DFI_PX_CONFIG = UtilConfigReader.readProperty("DFI_PX_CONFIG");
	final String SHEET_NAME = "Item LCP Auditor";
	ActionResult actionResult = null;
	String successLog = "Nothing to do.";
	StringBuffer errorLog = new StringBuffer();
	Sheet sheet = null;
	
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
 	    	//取得Event Type
 	    	int eventType = object.getEventType();
 	    	//取得Change Type資訊
 	    	String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString(); 
			//取得Change API Prefix(3 digits)
 	    	String changeAPIName = change.getAgileClass().getAPIName();
			String changeAPIPrefix = changeAPIName.substring(0, 3);
 	    	//debug output
			//System.out.println("changeType is "+changeType);
			//System.out.println("changeAPIPrefix is "+changeAPIPrefix);
			
			/*
			 * 讀 DFI_PX_CONFIG.Item LCP Auditor
			 */
			sheet = ExcelUtility.getDataSheet(DFI_PX_CONFIG, SHEET_NAME);
			/*
			 * 取得符合Change API Prefix的LCP Rule
			 */
			//Excel Column Header: | Change API Prefix | AI Class API Name | AI LCP Specific Criteria | AI Released LCP |
			List<Row> lcpRuleSheet = ExcelUtility.filterDataSheet(sheet, 0, changeAPIPrefix);
			
		    /*
		     * 取得Affected Items Table
		     */
		    ITable changeAITable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator<IRow> changeAI = changeAITable.iterator();
		    
			/*
			 * Event Type is "Update Table" and Event Action is "Add Row/Update Row"
			 */
			if(eventType == EventConstants.EVENT_UPDATE_TABLE) {
				//Change Type不是 ECR 才執行設定 AI Released LCP 的動作
				if(!changeAPIPrefix.equals("ECR")) {
					//取得Update Table Action
					IUpdateTableEventInfo updateTableEventInfo = (IUpdateTableEventInfo) request;
					IEventDirtyTable eventDirtyTable = updateTableEventInfo.getTable();
					Iterator<IEventDirtyRowUpdate> eventDirty = eventDirtyTable.iterator();
					IEventDirtyRowUpdate eventDirtyRowUpdate = eventDirty.next();
					int updateTableAction = eventDirtyRowUpdate.getAction();
					//debug output
					//System.out.println("Update Table Action is "+updateTableAction);
					//只有Update Table Action=Add-Row才往下執行指定LCP的動作
					if(updateTableAction == EventConstants.DIRTY_ROW_ACTION_ADD) {
						executeItemLCPAudit(changeAI, lcpRuleSheet, eventType, change);
					}
				}
			} 
			/* 
			 * Event Type is "Change Status for Workflow"
			 */
			else if(eventType == EventConstants.EVENT_CHANGE_STATUS_FOR_WORKFLOW) {
				executeItemLCPAudit(changeAI, lcpRuleSheet, eventType, null);
			} 
			/*
			 * Event Type is "Extend Actions Menu"
			 */
			else if(eventType == EventConstants.EVENT_EXTEND_ACTIONS_MENU) {
				executeItemLCPAudit(changeAI, lcpRuleSheet, eventType, change);
			}
			
			
			/*
			 * 產生Event  Action Result Message
			 */
			//debug output
			//System.out.println("ErrorLog is "+failLog.toString());
			/*
			 * 沒有記錄任何錯誤
			 */
	    	if(errorLog.toString().equals("")) {
	    		//如果是Update Table Event，產生適當的設定訊息。
	    		if(eventType == EventConstants.EVENT_UPDATE_TABLE) {
	    			//產生成功訊息(自動指派生命週期)
	    			successLog = "Auto-assign a lifecycle phase of affected items successfully.";
	    		//如果是Change Status Event，產生適當的檢核訊息。
	    		} else if(eventType == EventConstants.EVENT_CHANGE_STATUS_FOR_WORKFLOW) {
	    			//產生成功訊息(生命週期檢核)
					successLog = "Audit of the lifecycle phase of affected items is passed.";
	    		}
	    		//產生成功的Action Result
	    		actionResult = new ActionResult(ActionResult.STRING, successLog);
	    	} 
	    	/*
	    	 * 如果有紀錄錯誤發生
	    	 */
	    	else {
    			//取得LCP Rule完整表示式
	    		String[] LCPCriteriaNameValue = getLCPCriteriaNameValue(lcpRuleSheet);
	    		//錯誤訊息最前面補上說明
	    		errorLog.insert(0,"WARNING! - The "+LCPCriteriaNameValue[0]+" of affected items should be "+LCPCriteriaNameValue[1]+". "+
	    				"These affected items can not step this workflow: [" );
	    		//錯誤訊息後面補上括號
	    		errorLog.append("]");
	    		//取代特定字串
	    		String errMsg = errorLog.toString().replace("OLD_LCP", "old lifecycle phase").replace("LCP", "lifecycle phase");
	    		//拋出Exception
	    		throw new Exception(errMsg);
	    	}
	    } catch(Exception ex) {
	    	//產生失敗的Action Result
	    	actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
			sheet = null;
		}
		//傳回Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	/**
	 * 檢查Item的(舊)生命週期是否符合工作流程的條件限制.
	 * @param AIOldLCP AI舊生命週期
	 * @param AILCP AI生命週期
	 * @param LCPRuleAILCPCriteria AI生命週期檢核條件，例如OLD_LCP=EBPM
	 * @return true表示通過檢查，false表示沒通過
	 * @throws Exception throw when exception is happened
	 */
	public boolean matchAILCPCriteria(String AIOldLCP, String AILCP, String LCPRuleAILCPCriteria) throws Exception {
		try {
			//回傳值初始化=false
			boolean result = false;
			//如果LCP Criteria是空的，直接回傳true
			if(LCPRuleAILCPCriteria.equals(""))
				return true;
			//解析LCP Criteria
			String criteriaName = LCPRuleAILCPCriteria.split("=")[0];
			String criteriaValue = LCPRuleAILCPCriteria.split("=")[1];
			//如果LCP criteria value = null，轉換成空字串
			criteriaValue = criteriaValue.equals("null")? "" : criteriaValue;
			//System.out.println("criteriaName/criteriaValue is "+criteriaName+"/"+criteriaValue);
			//取得實際值
			String acturalValue = "";
			if(criteriaName.equals("OLD_LCP")) {
				acturalValue = AIOldLCP;
			} else if(criteriaName.equals("LCP")) {
				acturalValue = AILCP;
			}
			//System.out.println("acturalValue is "+acturalValue);
			//如果實際值=條件值，回傳值=true
			if(acturalValue.equals(criteriaValue)) {
				result = true;
			}
			//回傳結果	
			return result;
		} catch(Exception e) {
			throw e;
		}
	}
	
	/**
	 * 取得Change屬性值
	 * @param change Change物件
	 * @param APIName 屬性API Name
	 * @return 回傳屬性質
	 * @throws Exception throw when exception is happened
	 */
	public String getChangeAttributeValue(IChange change, String APIName) throws Exception {
		try {
			//回傳值初始化
			String result = "";
			result = change.getValue(APIName).toString();
			return result;
		} catch(Exception e) {
			throw e;
		}
	}
	/**
	 * 取得Item LCP Audit Rule的AI LCP Criteria
	 * @param listLCPRule Item LCP Rule串列
	 * @return 檢查生命週期或是舊生命週期/合法生命週期清單
	 * @throws Exception throw when exception is happened
	 */
	public String[] getLCPCriteriaNameValue(List<Row> listLCPRule) throws Exception {
		try {
			//初始化回傳值
			String[] result = new String[2];
			
			String criteriaName = "";
			String criteriaValue = "";
			//Loop LCP Rule List產生回傳值
			Iterator<Row> itListLCPRule = listLCPRule.iterator();
			while(itListLCPRule.hasNext()) {
				Row LCPRule = itListLCPRule.next();
				String criteriaNameValue = ExcelUtility.getSpecificCellValue(LCPRule, 2);
				//debug:2015/12/31:只有PartsClass才提示訊息
				String aiClassAPIName = ExcelUtility.getSpecificCellValue(LCPRule, 1);
				if(aiClassAPIName.equals("PartsClass")) {
					//Left Operand of LCP Criteria 
					criteriaName = criteriaNameValue.split("=")[0];
					//Right Operand of LCP Criteria, separated by comma.
					criteriaValue += criteriaNameValue.split("=")[1];
					if(itListLCPRule.hasNext())
						criteriaValue += ",";
				}
			}
			//設定回傳值
			result[0] = criteriaName;
			result[1] = criteriaValue;
			return result;
		} catch(Exception e) {
			throw e;
		}
	}
	
	/**
	 * 執行Item生命週期設定或檢核.
	 * @param changeAI Change受影響物件的集合
	 * @param lcpRuleSheet 工作流程發行Item的生命週期規則
	 * @param eventType Event事件類型
	 * @param change Change物件
	 * @throws Exception throw when exception is happened
	 */
	public void executeItemLCPAudit(Iterator<IRow> changeAI, List<Row> lcpRuleSheet, int eventType, IChange change) throws Exception {
		try {
			/*
			 * Loop Affected Item Table
			 */
			while(changeAI.hasNext()) {
				/*
				 * 取得AI info.
				 */
				IRow ai = changeAI.next();
				//Bug fix 2016/1/6: ECR沒有Old Lifecycle Phase, 如果Cell.getValue()會出現NullPointerException. 
				//String aiOldLCP = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_LIFECYCLE_PHASE).getValue()==null? "" : ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_LIFECYCLE_PHASE).getValue().toString();
				String aiOldLCP = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_LIFECYCLE_PHASE)==null? "" : ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_LIFECYCLE_PHASE).getValue().toString();
				String aiLCP = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE).getValue().toString();
				String aiClassAPIName = ai.getReferent().getAgileClass().getSuperClass().getAPIName();
				String itemNumber = ai.getReferent().getName();
				//debug output
				/*
				System.out.println("aiOldLCP is "+aiOldLCP);
				System.out.println("aiLCP is "+aiLCP);
				System.out.println("aiClassAPIName is "+aiClassAPIName);
				System.out.println("itemNumber is "+itemNumber);
				*/
				//初始化isPassed (default=false)
				boolean isPassed = false;
				/*
				 * 比對此工作流程的所有LCP Rule，檢查AI LCP是否符合LCP Rule
				 */
				Iterator<Row> lcpRules = lcpRuleSheet.iterator();
				//System.out.println("listLCPRule size is "+listLCPRule.size());
				while(lcpRules.hasNext()) {
					Row lcpRule = lcpRules.next();
					String lcpRuleAIClassAPIName = ExcelUtility.getSpecificCellValue(lcpRule, 1);
					String lcpRuleAILCPCriteria = ExcelUtility.getSpecificCellValue(lcpRule, 2);
					String lcpRuleAIReleasedLCP = ExcelUtility.getSpecificCellValue(lcpRule, 3);
					//debug output
					/*
					System.out.println("lcpRuleAIClassAPIName is "+lcpRuleAIClassAPIName);
					System.out.println("lcpRuleAILCPCriteria is ["+lcpRuleAILCPCriteria+"]");
					System.out.println("lcpRuleAIReleasedLCP is "+lcpRuleAIReleasedLCP);
					*/
					/*
					 * AI-Class符合LCP Rule
					 */
					if(aiClassAPIName.equals(lcpRuleAIClassAPIName)) {
						/*
						 * AI-OldLCP符合LCP Rule
						 */
						if(matchAILCPCriteria(aiOldLCP, aiLCP, lcpRuleAILCPCriteria)) {
							/*
							 * Event Type=Update Table(Affected Item) or Extend Action Menu, 設定ITEM LCP
							 */
							if(eventType == EventConstants.EVENT_UPDATE_TABLE || 
									eventType == EventConstants.EVENT_EXTEND_ACTIONS_MENU) {
								//決定AI Released LCP
								String aiReleasedLCP = lcpRuleAIReleasedLCP.startsWith("$")? getChangeAttributeValue(change, lcpRuleAIReleasedLCP.substring(1)) : lcpRuleAIReleasedLCP;
								//指定AI Released LCP
								ICell changeAILCPCell = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE);
								changeAILCPCell.setValue(aiReleasedLCP);
								isPassed = true;
								////System.out.println("Update LCP as "+AIReleasedLCP);
								//停止比對LCP Rule
								break;
							} 
							/*
							 * Event Type=Change Status for Workflow, 過關 
							 */
							else if(eventType == EventConstants.EVENT_CHANGE_STATUS_FOR_WORKFLOW) {
								isPassed = true;
								//停止比對LCP Rule
								break;
							}
						}	
					}	
				}
				/*
				 * 如果沒有通過LCP Rule檢查，記錄ItemNo到ErrorLog
				 */
				if(!isPassed) {
					errorLog.append(itemNumber+"  ");
				}
			}
		} catch(Exception e) {
			throw e;
		}
	}
}

///:~
	