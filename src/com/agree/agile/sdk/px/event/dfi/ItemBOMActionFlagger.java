//: ItemBOMActionFlagger.java
package com.agree.agile.sdk.px.event.dfi;

import java.util.*;
import com.agile.api.*;
import com.agile.px.*;

/**
 * 標示Item/BOM是新建(new)或是變更(update)，提供後端ERP整合作業參考。
 * <p>標示Item的流程: PNR要分辨是new or update,其他change order的default flag都是update.<br>
 * 標示BOM的流程: 針對DCN,ECO要分辨是new or update. 如果AI最後發行版沒有BOM，flag=new；反之，flag=update<br>
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class ItemBOMActionFlagger implements IEventAction {
	ActionResult actionResult = null;
	String actionFlag = null;
	String successLog = "Nothing to do.";

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
			int eventType = object.getEventType();
			// 取得Change Type資訊
			String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString();
			// 取得Change API Prefix(3 digits)
			String changeAPIName = change.getAgileClass().getAPIName();
			String changeAPIPrefix = changeAPIName.substring(0, 3);
			//debug
			//System.out.println("changeType is " + changeType);
			//System.out.println("changeAPIPrefix is " + changeAPIPrefix);

			/* 
			 * 取得Affected Items Table
			 */
			ITable changeAITable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			/*
			 *  如果是Update Table Event，取得Update Table Action
			 */
			int updateTableAction = -1;
			if(eventType == EventConstants.EVENT_UPDATE_TABLE) {
				IUpdateTableEventInfo updateTableEventInfo = (IUpdateTableEventInfo) request;
				IEventDirtyTable eventDirtyTable = updateTableEventInfo.getTable();
				Iterator<IEventDirtyRow> eventDirty = eventDirtyTable.iterator();
				IEventDirtyRow eventDirtyRow = eventDirty.next();
				updateTableAction = eventDirtyRow.getAction();
				//System.out.println("Update Table Action is " + updateTableAction);
			}
			
			/*
			 * (Disabled on 2015/12/31)當Event Type = (Row Update Action = Add-Row or Update-Row) or (Extend Actions Menu) 才動作
			 * 當Event Type = (Row Update Action = Add-Row) or (Extend Actions Menu) 才動作
			 */
			if (updateTableAction == EventConstants.DIRTY_ROW_ACTION_ADD || 
						eventType==EventConstants.EVENT_EXTEND_ACTIONS_MENU) {
				// Loop all affected items
				Iterator<IRow> changeAI = changeAITable.iterator();
				while (changeAI.hasNext()) {
					IRow ai = changeAI.next();
					String aiLCP = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE).toString();
					String aiOldLCP = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_LIFECYCLE_PHASE).toString();
					String aiItemNumber = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).toString();
					/*
					 * BOM-Action-Flag validation
					 * ----------------------------------------------
					 * 如果AI最後發行版沒有BOM，flag=new；反之，flag=update
					 */
					if (changeAPIPrefix.equals("DCN") || 
							changeAPIPrefix.equals("ECO") ||
							changeAPIPrefix.equals("PBR")) {
						// BOM-Action flag
						ICell aiBOMActionFlagCell = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_TEXT03);
						// Flag是空的才處理
						if (aiBOMActionFlagCell.getValue().toString().equals("")) {
							// Determine flag value
							// 如果AI最後發行版沒有BOM，flag=new；反之，flag=update
							IItem item = (IItem)session.getObject(ItemConstants.CLASS_PARTS_CLASS, aiItemNumber);
							//System.out.println(item.getRevision());
							ITable bomTable = item.getTable(ItemConstants.TABLE_BOM);
							if (bomTable.isEmpty())
								actionFlag = "new";
							else
								actionFlag = "update";
							// Update BOM-Action flag
							aiBOMActionFlagCell.setValue(actionFlag);
							// 產生成功訊息
							successLog = "Set Item/BOM Action Flag completely.";
						}
					}
					/*
					 * Item-Action-Flag validation
					 * -------------------------------------------------------
					 * PNR: OLD_LCP=null,flag=new; OLD_LCP!=null,flag=update.
					 * The default flag of other change orders is update
					 */
					// Item-Action flag
					ICell aiItemActionFlagCell = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_TEXT04);
					// Flag是空的才處理
					if (aiItemActionFlagCell.getValue().toString().equals("")) {
						// Determine flag value
						if (changeAPIPrefix.equals("PNR"))
							if (aiOldLCP.equals(""))
								actionFlag = "new";
							else
								actionFlag = "update";
						else
							actionFlag = "update";
						// Update Item-Action flag
						aiItemActionFlagCell.setValue(actionFlag);
						// 產生成功訊息
						successLog = "Set Item/BOM Action Flag completely.";
					}
				}
			}
			//設定Action Result
			actionResult = new ActionResult(ActionResult.STRING, successLog);
		} catch (Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
		}
		// 傳回Event Action Result
		return new EventActionResult(request, actionResult);
	}

}
// /:~
