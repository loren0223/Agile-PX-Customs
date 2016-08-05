//: BOMBulkChange.java
package com.aic.agile.sdk.px.event.dfi;

import java.util.*;

import com.agile.api.*;
import com.agile.px.*;
import com.aic.agile.sdk.util.*;

/**
 * DFI BOM �j�q�ܧ�.
 * <p>�����All Model Change�@�~�ɡA�ھ�Item to Remove(�R����)�BReplacement Item(������)�A
 * �妸�۰��ܧ�Ҧ���Affected Items(���v�T����)��BOM�A���ͬ��u���O�C<br>
 * ���u���O�����޿�O�A���Item to Remove�çR���A�s�WReplacement Item�åB
 * Find No,Main Sub,Qty,Ref Des���PItem to Remove�A�̫�A�NItem to Remove�s�W�����N�ơC<br>
 * �p�GItem to Remove �P Replacement Item �����Y�O�D���N�Ƥ������ܡA
 * �����NItem Number=Replacement Item�����N�ƧR���A�קK�H��BOM�ˮֳW�h�C
 *  
 * @author Loren.Cheng
 * @version 1.0
 */
public class BOMBulkChange implements IEventAction {
	ActionResult actionResult = null;
	String actionFlag = null;
	String successLog = "BOM Bulk Change is done!";

	/** 
	 * Agile PLM Event PX �i�J�I.
	 * @param session �ϥΪ�Session
	 * @param actionNode Ĳ�o�{������m
	 * @param request Event����
	 */
	public EventActionResult doAction(IAgileSession session, INode actionNode,
			IEventInfo request) {
		try {
			// ���oĲ�oEvent������
			IObjectEventInfo object = (IObjectEventInfo) request;
			// �૬Change����
			IChange change = (IChange) object.getDataObject();
			// �ϧOEvent Type
			// int eventType = object.getEventType();
			// ���oChange Type��T
			String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString();
			// ���oChange API Prefix(3 digits)
			String changeAPIName = change.getAgileClass().getAPIName();
			String changeAPIPrefix = changeAPIName.substring(0, 3);
			//debug
			//System.out.println("changeType is " + changeType);
			//System.out.println("changeAPIPrefix is " + changeAPIPrefix);
			//����Removed Item��Find No
			String itemRemovedFN = "";
			
			/*
			 * ���oAll Model Change (Yes/No)
			 */
			String allModelChange = change.getCell(ChangeConstants.ATT_PAGE_THREE_LIST24).getValue().toString();
			//�p�G���OAll Model Change�A�����{���A����ܴ��ܰT��
			if(allModelChange.equals("No")) {
				actionResult = new ActionResult(ActionResult.STRING, "Nothing to do. DFI-BOM-Bulk-Change executes when [Page Three.All Model Change] is Yes.");
				return new EventActionResult(request, actionResult);
			}
			/*
			 * ���oItem to Remove, Replacement Item����P�Ƹ�
			 */
			IItem itemToRemoveObj = (IItem)change.getCell(ChangeConstants.ATT_PAGE_THREE_LIST23).getReferent();
			IItem replacementItemObj = (IItem)change.getCell(ChangeConstants.ATT_PAGE_THREE_LIST22).getReferent();
			String itemToRemove = itemToRemoveObj==null? "" : itemToRemoveObj.getName();
			String replacementItem = replacementItemObj==null? "" : replacementItemObj.getName();
			/*
			 * �p�G���ŭȡA�ߥX���~�T��
			 */
			if(itemToRemoveObj==null || replacementItemObj==null) {
				actionResult = new ActionResult(ActionResult.EXCEPTION, new Exception("[Page Three.Item to Remove/Replacement Item] is required for DFI-BOM-Bulk-Change!"));
				return new EventActionResult(request, actionResult);
			}
			/*
			 * �p�GReplacement Item��LCP�����\�[�JBOM�A�ߥX���~�T��
			 */
			IItem item = (IItem)session.getObject(ItemConstants.CLASS_PARTS_CLASS, replacementItem);
			String lcp = item.getCell(ItemConstants.ATT_TITLE_BLOCK_LIFECYCLE_PHASE).getValue().toString();
			//System.out.println(lcp);
			//���oParts.LifecyclePhases �� Enabled=Yes & Add on BOM Rule=Disallow
			ArrayList<String> badLCPs = this.getBadComponentLCPs(session);
			if(badLCPs.contains(lcp)) {
				actionResult = new ActionResult(ActionResult.EXCEPTION, new Exception("Replacement Item is not allowed to add on BOM! Lifecycle Phase["+lcp+"] is illegal!"));
				return new EventActionResult(request, actionResult);
			}
			/*
			 * ���oAffected Items Table
			 */
			ITable changeAITable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator<IRow> changeAI = changeAITable.iterator();
			/*
			 * Loop all affected items
			 */
			while (changeAI.hasNext()) {
				IRow ai = changeAI.next();
				//���oPending Change Item
				IItem pendingChangeItem = (IItem)ai.getReferent();
				/*
				 * �p�GBOM Frozen=Yes�Ϊŭ�(������)�A�h�������B�z
				 */
				/*
				ICell bomFrozenCell = pendingChangeItem.getCell(ItemConstants.ATT_PAGE_TWO_LIST02);
				String bomFrozen = bomFrozenCell==null? "" : bomFrozenCell.getValue().toString();
				if(bomFrozen.equals("Yes") || bomFrozen.equals("")) {
					continue;
				}
				*/
				/*
				 * �p�GERP Item EOL Status=LSS��LSX�A�h�������B�z
				 */
				ICell eolStatusCell = pendingChangeItem.getCell(ItemConstants.ATT_PAGE_TWO_LIST05);
				String eolStatus = eolStatusCell==null? "" : eolStatusCell.getValue().toString();
				if(eolStatus.equals("LSS") || eolStatus.equals("LSX")) {
					continue;
				}
				//���oRedline BOM Table
				ITable redlineBOMTable = pendingChangeItem.getTable(ItemConstants.TABLE_REDLINEBOM);
				/*
				 * Loop�Ҧ�Redline Component Line
				 */
				ITwoWayIterator redlineBOM = redlineBOMTable.getTableIterator();
				while(redlineBOM.hasNext()) {
					IRow redlineRow = (IRow)redlineBOM.next();
					//���oComponent Line Info.
					String findNo = redlineRow.getCell(ItemConstants.ATT_BOM_FIND_NUM).getValue().toString();
					String mainSub = redlineRow.getCell(ItemConstants.ATT_BOM_BOM_LIST01).getValue().toString();
					String itemNo = redlineRow.getCell(ItemConstants.ATT_BOM_ITEM_NUMBER).getValue().toString();
					String qty = redlineRow.getCell(ItemConstants.ATT_BOM_QTY).getValue().toString();
					//�`�N���o�����m���y�k�A�קKRuntime Error
					IReferenceDesignatorCell refDesCell = (IReferenceDesignatorCell)redlineRow.getCell(ItemConstants.ATT_BOM_REF_DES);
					String refDes = refDesCell==null? "" : refDesCell.getExpandedValue();
					
					/* 
					 * �p�GComponent Item Number = Item to Remove
					 * �B �O�D��(M) 
					 * �B �S������redline action
					 */
					if(itemNo.equals(itemToRemove) && mainSub.equals("M") && 
							(!redlineRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_ADDED) &&
							!redlineRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED) &&
							!redlineRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED)) ) {
						//�R�� Item to Remove & ����itemRemovedFN
						redlineBOMTable.removeRow(redlineRow);
						itemRemovedFN = findNo;
						//�s�W Replacement Item (Find No/Main Sub/Qty/Ref Des �P Item to Remove)
						IRow newMainItem = redlineBOMTable.createRow(replacementItemObj);
						newMainItem.setValue(ItemConstants.ATT_BOM_FIND_NUM, findNo);
						newMainItem.setValue(ItemConstants.ATT_BOM_BOM_LIST01, mainSub);
						newMainItem.setValue(ItemConstants.ATT_BOM_QTY, qty);
						newMainItem.setValue(ItemConstants.ATT_BOM_REF_DES, refDes);
						//�s�WItem to Remove ��@���N�� (Find No/Qty �P Item to Remove, Main Sub=S, �LRef Des)
						IRow newSubItem = redlineBOMTable.createRow(itemToRemoveObj);
						newSubItem.setValue(ItemConstants.ATT_BOM_FIND_NUM, findNo);
						newSubItem.setValue(ItemConstants.ATT_BOM_BOM_LIST01, "S");
						newSubItem.setValue(ItemConstants.ATT_BOM_QTY, qty);
						//����Loop�Ҧ�Redline Component Line
						break;
					}
				}
				/*
				 * �ALoop�Ҧ�Redline Component Line.
				 * 
				 * �p�GItem to Remove/Replacement Item �����Y�O�D���N�����A
				 * ��itemRemovedFN + S + Replacement Item�A
				 * �R����D�ƪ����N�ơC
				 */
				redlineBOM = redlineBOMTable.getTableIterator();
				while(redlineBOM.hasNext()) {
					IRow redlineRow = (IRow)redlineBOM.next();
					String findNo = redlineRow.getCell(ItemConstants.ATT_BOM_FIND_NUM).getValue().toString();
					String mainSub = redlineRow.getCell(ItemConstants.ATT_BOM_BOM_LIST01).getValue().toString();
					String itemNo = redlineRow.getCell(ItemConstants.ATT_BOM_ITEM_NUMBER).getValue().toString();
					if(findNo.equals(itemRemovedFN) && mainSub.equals("S") && itemNo.equals(replacementItem)) {
						redlineBOMTable.remove(redlineRow);
						break;
					}
				}
			}
			
			/*
			 * �]�wAction Result
			 */
			actionResult = new ActionResult(ActionResult.STRING, successLog);
		} catch (Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
		}
		// �Ǧ^Event Action Result
		return new EventActionResult(request, actionResult);
	}
	
	/**
	 * ���oPLM��x�]�w�A�Ҧ������\�[�JBOM��Item Lifecycle Phase
	 * @param session User session
	 * @return badLCPs �Ҧ������\�[�JBOM��Item Lifecycle Phase
	 * @throws Exception throw when exception is happened
	 */
	public ArrayList<String> getBadComponentLCPs(IAgileSession session) throws Exception {
		try {
			ArrayList<String> badLCPs = new ArrayList<String>();
        	//���oAdmin instance
		    IAdmin admin = session.getAdminInstance();
			//���oParts LifecyclePhases �`�I
		    INode partsLCPNode = admin.getNode("PartsClass.LifeCyclePhases");
		    //���o�Ҧ�Lifecycle Phases
		    Collection lcpNodes = partsLCPNode.getChildNodes();
		    Iterator it = lcpNodes.iterator();
		    while(it.hasNext()) {
		    	INode node = (INode)it.next();
		    	//���oLifecycle Phase�Բ��ݩ�		    	
		    	String name = node.getProperty("Name").getValue().toString();
		    	String enabled = node.getProperty("Enabled").getValue().toString();
		    	String addOnBOMRule = node.getProperty("Add LifeCyclePhase On Bom Rule").getValue().toString();
		    	//�NEnabled=Yes & Add on BOM Rule=Disallow��LCP�^��
		    	if(enabled.equals("Yes") && addOnBOMRule.equals("Disallow")) {
		    		badLCPs.add(name);
		    	}
		    }
		    //�^��
		    return badLCPs;
		}catch(Exception e) {
			throw e;
		}
	}
}
// /:~
