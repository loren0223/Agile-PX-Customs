//: BOMBulkChange.java
package com.aic.agile.sdk.px.event.dfi;

import java.util.*;

import com.agile.api.*;
import com.agile.px.*;
import com.aic.agile.sdk.util.*;

/**
 * DFI BOM 大量變更.
 * <p>當執行All Model Change作業時，根據Item to Remove(刪除料)、Replacement Item(替換料)，
 * 批次自動變更所有的Affected Items(受影響物件)的BOM，產生紅線註記。<br>
 * 紅線註記產生邏輯是，找到Item to Remove並刪除，新增Replacement Item並且
 * Find No,Main Sub,Qty,Ref Des等同Item to Remove，最後再將Item to Remove新增為替代料。<br>
 * 如果Item to Remove 與 Replacement Item 的關係是主替代料互換的話，
 * 必須將Item Number=Replacement Item的替代料刪除，避免違反BOM檢核規則。
 *  
 * @author Loren.Cheng
 * @version 1.0
 */
public class BOMBulkChange implements IEventAction {
	ActionResult actionResult = null;
	String actionFlag = null;
	String successLog = "BOM Bulk Change is done!";

	/** 
	 * Agile PLM Event PX 進入點.
	 * @param session 使用者Session
	 * @param actionNode 觸發程式的位置
	 * @param request Event物件
	 */
	public EventActionResult doAction(IAgileSession session, INode actionNode,
			IEventInfo request) {
		try {
			// 取得觸發Event的物件
			IObjectEventInfo object = (IObjectEventInfo) request;
			// 轉型Change物件
			IChange change = (IChange) object.getDataObject();
			// 區別Event Type
			// int eventType = object.getEventType();
			// 取得Change Type資訊
			String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString();
			// 取得Change API Prefix(3 digits)
			String changeAPIName = change.getAgileClass().getAPIName();
			String changeAPIPrefix = changeAPIName.substring(0, 3);
			//debug
			//System.out.println("changeType is " + changeType);
			//System.out.println("changeAPIPrefix is " + changeAPIPrefix);
			//紀錄Removed Item的Find No
			String itemRemovedFN = "";
			
			/*
			 * 取得All Model Change (Yes/No)
			 */
			String allModelChange = change.getCell(ChangeConstants.ATT_PAGE_THREE_LIST24).getValue().toString();
			//如果不是All Model Change，結束程式，並顯示提示訊息
			if(allModelChange.equals("No")) {
				actionResult = new ActionResult(ActionResult.STRING, "Nothing to do. DFI-BOM-Bulk-Change executes when [Page Three.All Model Change] is Yes.");
				return new EventActionResult(request, actionResult);
			}
			/*
			 * 取得Item to Remove, Replacement Item物件與料號
			 */
			IItem itemToRemoveObj = (IItem)change.getCell(ChangeConstants.ATT_PAGE_THREE_LIST23).getReferent();
			IItem replacementItemObj = (IItem)change.getCell(ChangeConstants.ATT_PAGE_THREE_LIST22).getReferent();
			String itemToRemove = itemToRemoveObj==null? "" : itemToRemoveObj.getName();
			String replacementItem = replacementItemObj==null? "" : replacementItemObj.getName();
			/*
			 * 如果為空值，拋出錯誤訊息
			 */
			if(itemToRemoveObj==null || replacementItemObj==null) {
				actionResult = new ActionResult(ActionResult.EXCEPTION, new Exception("[Page Three.Item to Remove/Replacement Item] is required for DFI-BOM-Bulk-Change!"));
				return new EventActionResult(request, actionResult);
			}
			/*
			 * 如果Replacement Item的LCP不允許加入BOM，拋出錯誤訊息
			 */
			IItem item = (IItem)session.getObject(ItemConstants.CLASS_PARTS_CLASS, replacementItem);
			String lcp = item.getCell(ItemConstants.ATT_TITLE_BLOCK_LIFECYCLE_PHASE).getValue().toString();
			//System.out.println(lcp);
			//取得Parts.LifecyclePhases 中 Enabled=Yes & Add on BOM Rule=Disallow
			ArrayList<String> badLCPs = this.getBadComponentLCPs(session);
			if(badLCPs.contains(lcp)) {
				actionResult = new ActionResult(ActionResult.EXCEPTION, new Exception("Replacement Item is not allowed to add on BOM! Lifecycle Phase["+lcp+"] is illegal!"));
				return new EventActionResult(request, actionResult);
			}
			/*
			 * 取得Affected Items Table
			 */
			ITable changeAITable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator<IRow> changeAI = changeAITable.iterator();
			/*
			 * Loop all affected items
			 */
			while (changeAI.hasNext()) {
				IRow ai = changeAI.next();
				//取得Pending Change Item
				IItem pendingChangeItem = (IItem)ai.getReferent();
				/*
				 * 如果BOM Frozen=Yes或空值(元件類)，則忽略不處理
				 */
				/*
				ICell bomFrozenCell = pendingChangeItem.getCell(ItemConstants.ATT_PAGE_TWO_LIST02);
				String bomFrozen = bomFrozenCell==null? "" : bomFrozenCell.getValue().toString();
				if(bomFrozen.equals("Yes") || bomFrozen.equals("")) {
					continue;
				}
				*/
				/*
				 * 如果ERP Item EOL Status=LSS或LSX，則忽略不處理
				 */
				ICell eolStatusCell = pendingChangeItem.getCell(ItemConstants.ATT_PAGE_TWO_LIST05);
				String eolStatus = eolStatusCell==null? "" : eolStatusCell.getValue().toString();
				if(eolStatus.equals("LSS") || eolStatus.equals("LSX")) {
					continue;
				}
				//取得Redline BOM Table
				ITable redlineBOMTable = pendingChangeItem.getTable(ItemConstants.TABLE_REDLINEBOM);
				/*
				 * Loop所有Redline Component Line
				 */
				ITwoWayIterator redlineBOM = redlineBOMTable.getTableIterator();
				while(redlineBOM.hasNext()) {
					IRow redlineRow = (IRow)redlineBOM.next();
					//取得Component Line Info.
					String findNo = redlineRow.getCell(ItemConstants.ATT_BOM_FIND_NUM).getValue().toString();
					String mainSub = redlineRow.getCell(ItemConstants.ATT_BOM_BOM_LIST01).getValue().toString();
					String itemNo = redlineRow.getCell(ItemConstants.ATT_BOM_ITEM_NUMBER).getValue().toString();
					String qty = redlineRow.getCell(ItemConstants.ATT_BOM_QTY).getValue().toString();
					//注意取得插件位置的語法，避免Runtime Error
					IReferenceDesignatorCell refDesCell = (IReferenceDesignatorCell)redlineRow.getCell(ItemConstants.ATT_BOM_REF_DES);
					String refDes = refDesCell==null? "" : refDesCell.getExpandedValue();
					
					/* 
					 * 如果Component Item Number = Item to Remove
					 * 且 是主料(M) 
					 * 且 沒有任何redline action
					 */
					if(itemNo.equals(itemToRemove) && mainSub.equals("M") && 
							(!redlineRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_ADDED) &&
							!redlineRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED) &&
							!redlineRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED)) ) {
						//刪除 Item to Remove & 紀錄itemRemovedFN
						redlineBOMTable.removeRow(redlineRow);
						itemRemovedFN = findNo;
						//新增 Replacement Item (Find No/Main Sub/Qty/Ref Des 同 Item to Remove)
						IRow newMainItem = redlineBOMTable.createRow(replacementItemObj);
						newMainItem.setValue(ItemConstants.ATT_BOM_FIND_NUM, findNo);
						newMainItem.setValue(ItemConstants.ATT_BOM_BOM_LIST01, mainSub);
						newMainItem.setValue(ItemConstants.ATT_BOM_QTY, qty);
						newMainItem.setValue(ItemConstants.ATT_BOM_REF_DES, refDes);
						//新增Item to Remove 當作替代料 (Find No/Qty 同 Item to Remove, Main Sub=S, 無Ref Des)
						IRow newSubItem = redlineBOMTable.createRow(itemToRemoveObj);
						newSubItem.setValue(ItemConstants.ATT_BOM_FIND_NUM, findNo);
						newSubItem.setValue(ItemConstants.ATT_BOM_BOM_LIST01, "S");
						newSubItem.setValue(ItemConstants.ATT_BOM_QTY, qty);
						//結束Loop所有Redline Component Line
						break;
					}
				}
				/*
				 * 再Loop所有Redline Component Line.
				 * 
				 * 如果Item to Remove/Replacement Item 的關係是主替代互換，
				 * 用itemRemovedFN + S + Replacement Item，
				 * 刪除原主料的替代料。
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
			 * 設定Action Result
			 */
			actionResult = new ActionResult(ActionResult.STRING, successLog);
		} catch (Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
		}
		// 傳回Event Action Result
		return new EventActionResult(request, actionResult);
	}
	
	/**
	 * 取得PLM後台設定，所有不允許加入BOM的Item Lifecycle Phase
	 * @param session User session
	 * @return badLCPs 所有不允許加入BOM的Item Lifecycle Phase
	 * @throws Exception throw when exception is happened
	 */
	public ArrayList<String> getBadComponentLCPs(IAgileSession session) throws Exception {
		try {
			ArrayList<String> badLCPs = new ArrayList<String>();
        	//取得Admin instance
		    IAdmin admin = session.getAdminInstance();
			//取得Parts LifecyclePhases 節點
		    INode partsLCPNode = admin.getNode("PartsClass.LifeCyclePhases");
		    //取得所有Lifecycle Phases
		    Collection lcpNodes = partsLCPNode.getChildNodes();
		    Iterator it = lcpNodes.iterator();
		    while(it.hasNext()) {
		    	INode node = (INode)it.next();
		    	//取得Lifecycle Phase詳細屬性		    	
		    	String name = node.getProperty("Name").getValue().toString();
		    	String enabled = node.getProperty("Enabled").getValue().toString();
		    	String addOnBOMRule = node.getProperty("Add LifeCyclePhase On Bom Rule").getValue().toString();
		    	//將Enabled=Yes & Add on BOM Rule=Disallow的LCP回傳
		    	if(enabled.equals("Yes") && addOnBOMRule.equals("Disallow")) {
		    		badLCPs.add(name);
		    	}
		    }
		    //回傳
		    return badLCPs;
		}catch(Exception e) {
			throw e;
		}
	}
}
// /:~
