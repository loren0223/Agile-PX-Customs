//: ItemLCPAuditor.java
package com.agree.agile.sdk.px.event.dfi;
import java.util.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;
/**
 * Item�ͩR�g��(LCP)���]�w�P�ˮ�.
 * <p>�ھ�DFI�W�h�A��Event Type�OUpdate Table(Affected Items)�άOExtend Action Menu�A�]�wItem�ͩR�g���F
 * ��Event Type�OChange Status for Workflow�A�ˬdItem�ͩR�g���A�q�L�~���\�i�J�U�@���C<br>
 * <p>DFI_PX_CONFIG�����]�w:<br>
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
 	    	//���oEvent Type
 	    	int eventType = object.getEventType();
 	    	//���oChange Type��T
 	    	String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString(); 
			//���oChange API Prefix(3 digits)
 	    	String changeAPIName = change.getAgileClass().getAPIName();
			String changeAPIPrefix = changeAPIName.substring(0, 3);
 	    	//debug output
			//System.out.println("changeType is "+changeType);
			//System.out.println("changeAPIPrefix is "+changeAPIPrefix);
			
			/*
			 * Ū DFI_PX_CONFIG.Item LCP Auditor
			 */
			sheet = ExcelUtility.getDataSheet(DFI_PX_CONFIG, SHEET_NAME);
			/*
			 * ���o�ŦXChange API Prefix��LCP Rule
			 */
			//Excel Column Header: | Change API Prefix | AI Class API Name | AI LCP Specific Criteria | AI Released LCP |
			List<Row> lcpRuleSheet = ExcelUtility.filterDataSheet(sheet, 0, changeAPIPrefix);
			
		    /*
		     * ���oAffected Items Table
		     */
		    ITable changeAITable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator<IRow> changeAI = changeAITable.iterator();
		    
			/*
			 * Event Type is "Update Table" and Event Action is "Add Row/Update Row"
			 */
			if(eventType == EventConstants.EVENT_UPDATE_TABLE) {
				//Change Type���O ECR �~����]�w AI Released LCP ���ʧ@
				if(!changeAPIPrefix.equals("ECR")) {
					//���oUpdate Table Action
					IUpdateTableEventInfo updateTableEventInfo = (IUpdateTableEventInfo) request;
					IEventDirtyTable eventDirtyTable = updateTableEventInfo.getTable();
					Iterator<IEventDirtyRowUpdate> eventDirty = eventDirtyTable.iterator();
					IEventDirtyRowUpdate eventDirtyRowUpdate = eventDirty.next();
					int updateTableAction = eventDirtyRowUpdate.getAction();
					//debug output
					//System.out.println("Update Table Action is "+updateTableAction);
					//�u��Update Table Action=Add-Row�~���U������wLCP���ʧ@
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
			 * ����Event  Action Result Message
			 */
			//debug output
			//System.out.println("ErrorLog is "+failLog.toString());
			/*
			 * �S���O��������~
			 */
	    	if(errorLog.toString().equals("")) {
	    		//�p�G�OUpdate Table Event�A���;A���]�w�T���C
	    		if(eventType == EventConstants.EVENT_UPDATE_TABLE) {
	    			//���ͦ��\�T��(�۰ʫ����ͩR�g��)
	    			successLog = "Auto-assign a lifecycle phase of affected items successfully.";
	    		//�p�G�OChange Status Event�A���;A���ˮְT���C
	    		} else if(eventType == EventConstants.EVENT_CHANGE_STATUS_FOR_WORKFLOW) {
	    			//���ͦ��\�T��(�ͩR�g���ˮ�)
					successLog = "Audit of the lifecycle phase of affected items is passed.";
	    		}
	    		//���ͦ��\��Action Result
	    		actionResult = new ActionResult(ActionResult.STRING, successLog);
	    	} 
	    	/*
	    	 * �p�G���������~�o��
	    	 */
	    	else {
    			//���oLCP Rule�����ܦ�
	    		String[] LCPCriteriaNameValue = getLCPCriteriaNameValue(lcpRuleSheet);
	    		//���~�T���̫e���ɤW����
	    		errorLog.insert(0,"WARNING! - The "+LCPCriteriaNameValue[0]+" of affected items should be "+LCPCriteriaNameValue[1]+". "+
	    				"These affected items can not step this workflow: [" );
	    		//���~�T���᭱�ɤW�A��
	    		errorLog.append("]");
	    		//���N�S�w�r��
	    		String errMsg = errorLog.toString().replace("OLD_LCP", "old lifecycle phase").replace("LCP", "lifecycle phase");
	    		//�ߥXException
	    		throw new Exception(errMsg);
	    	}
	    } catch(Exception ex) {
	    	//���ͥ��Ѫ�Action Result
	    	actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
			sheet = null;
		}
		//�Ǧ^Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	/**
	 * �ˬdItem��(��)�ͩR�g���O�_�ŦX�u�@�y�{�����󭭨�.
	 * @param AIOldLCP AI�¥ͩR�g��
	 * @param AILCP AI�ͩR�g��
	 * @param LCPRuleAILCPCriteria AI�ͩR�g���ˮֱ���A�ҦpOLD_LCP=EBPM
	 * @return true��ܳq�L�ˬd�Afalse��ܨS�q�L
	 * @throws Exception throw when exception is happened
	 */
	public boolean matchAILCPCriteria(String AIOldLCP, String AILCP, String LCPRuleAILCPCriteria) throws Exception {
		try {
			//�^�ǭȪ�l��=false
			boolean result = false;
			//�p�GLCP Criteria�O�Ū��A�����^��true
			if(LCPRuleAILCPCriteria.equals(""))
				return true;
			//�ѪRLCP Criteria
			String criteriaName = LCPRuleAILCPCriteria.split("=")[0];
			String criteriaValue = LCPRuleAILCPCriteria.split("=")[1];
			//�p�GLCP criteria value = null�A�ഫ���Ŧr��
			criteriaValue = criteriaValue.equals("null")? "" : criteriaValue;
			//System.out.println("criteriaName/criteriaValue is "+criteriaName+"/"+criteriaValue);
			//���o��ڭ�
			String acturalValue = "";
			if(criteriaName.equals("OLD_LCP")) {
				acturalValue = AIOldLCP;
			} else if(criteriaName.equals("LCP")) {
				acturalValue = AILCP;
			}
			//System.out.println("acturalValue is "+acturalValue);
			//�p�G��ڭ�=����ȡA�^�ǭ�=true
			if(acturalValue.equals(criteriaValue)) {
				result = true;
			}
			//�^�ǵ��G	
			return result;
		} catch(Exception e) {
			throw e;
		}
	}
	
	/**
	 * ���oChange�ݩʭ�
	 * @param change Change����
	 * @param APIName �ݩ�API Name
	 * @return �^���ݩʽ�
	 * @throws Exception throw when exception is happened
	 */
	public String getChangeAttributeValue(IChange change, String APIName) throws Exception {
		try {
			//�^�ǭȪ�l��
			String result = "";
			result = change.getValue(APIName).toString();
			return result;
		} catch(Exception e) {
			throw e;
		}
	}
	/**
	 * ���oItem LCP Audit Rule��AI LCP Criteria
	 * @param listLCPRule Item LCP Rule��C
	 * @return �ˬd�ͩR�g���άO�¥ͩR�g��/�X�k�ͩR�g���M��
	 * @throws Exception throw when exception is happened
	 */
	public String[] getLCPCriteriaNameValue(List<Row> listLCPRule) throws Exception {
		try {
			//��l�Ʀ^�ǭ�
			String[] result = new String[2];
			
			String criteriaName = "";
			String criteriaValue = "";
			//Loop LCP Rule List���ͦ^�ǭ�
			Iterator<Row> itListLCPRule = listLCPRule.iterator();
			while(itListLCPRule.hasNext()) {
				Row LCPRule = itListLCPRule.next();
				String criteriaNameValue = ExcelUtility.getSpecificCellValue(LCPRule, 2);
				//debug:2015/12/31:�u��PartsClass�~���ܰT��
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
			//�]�w�^�ǭ�
			result[0] = criteriaName;
			result[1] = criteriaValue;
			return result;
		} catch(Exception e) {
			throw e;
		}
	}
	
	/**
	 * ����Item�ͩR�g���]�w���ˮ�.
	 * @param changeAI Change���v�T���󪺶��X
	 * @param lcpRuleSheet �u�@�y�{�o��Item���ͩR�g���W�h
	 * @param eventType Event�ƥ�����
	 * @param change Change����
	 * @throws Exception throw when exception is happened
	 */
	public void executeItemLCPAudit(Iterator<IRow> changeAI, List<Row> lcpRuleSheet, int eventType, IChange change) throws Exception {
		try {
			/*
			 * Loop Affected Item Table
			 */
			while(changeAI.hasNext()) {
				/*
				 * ���oAI info.
				 */
				IRow ai = changeAI.next();
				//Bug fix 2016/1/6: ECR�S��Old Lifecycle Phase, �p�GCell.getValue()�|�X�{NullPointerException. 
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
				//��l��isPassed (default=false)
				boolean isPassed = false;
				/*
				 * ��惡�u�@�y�{���Ҧ�LCP Rule�A�ˬdAI LCP�O�_�ŦXLCP Rule
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
					 * AI-Class�ŦXLCP Rule
					 */
					if(aiClassAPIName.equals(lcpRuleAIClassAPIName)) {
						/*
						 * AI-OldLCP�ŦXLCP Rule
						 */
						if(matchAILCPCriteria(aiOldLCP, aiLCP, lcpRuleAILCPCriteria)) {
							/*
							 * Event Type=Update Table(Affected Item) or Extend Action Menu, �]�wITEM LCP
							 */
							if(eventType == EventConstants.EVENT_UPDATE_TABLE || 
									eventType == EventConstants.EVENT_EXTEND_ACTIONS_MENU) {
								//�M�wAI Released LCP
								String aiReleasedLCP = lcpRuleAIReleasedLCP.startsWith("$")? getChangeAttributeValue(change, lcpRuleAIReleasedLCP.substring(1)) : lcpRuleAIReleasedLCP;
								//���wAI Released LCP
								ICell changeAILCPCell = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE);
								changeAILCPCell.setValue(aiReleasedLCP);
								isPassed = true;
								////System.out.println("Update LCP as "+AIReleasedLCP);
								//������LCP Rule
								break;
							} 
							/*
							 * Event Type=Change Status for Workflow, �L�� 
							 */
							else if(eventType == EventConstants.EVENT_CHANGE_STATUS_FOR_WORKFLOW) {
								isPassed = true;
								//������LCP Rule
								break;
							}
						}	
					}	
				}
				/*
				 * �p�G�S���q�LLCP Rule�ˬd�A�O��ItemNo��ErrorLog
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
	