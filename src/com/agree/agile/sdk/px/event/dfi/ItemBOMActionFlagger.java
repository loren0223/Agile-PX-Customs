//: ItemBOMActionFlagger.java
package com.agree.agile.sdk.px.event.dfi;

import java.util.*;
import com.agile.api.*;
import com.agile.px.*;

/**
 * �Х�Item/BOM�O�s��(new)�άO�ܧ�(update)�A���ѫ��ERP��X�@�~�ѦҡC
 * <p>�Х�Item���y�{: PNR�n����Onew or update,��Lchange order��default flag���Oupdate.<br>
 * �Х�BOM���y�{: �w��DCN,ECO�n����Onew or update. �p�GAI�̫�o�檩�S��BOM�Aflag=new�F�Ϥ��Aflag=update<br>
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class ItemBOMActionFlagger implements IEventAction {
	ActionResult actionResult = null;
	String actionFlag = null;
	String successLog = "Nothing to do.";

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
			int eventType = object.getEventType();
			// ���oChange Type��T
			String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString();
			// ���oChange API Prefix(3 digits)
			String changeAPIName = change.getAgileClass().getAPIName();
			String changeAPIPrefix = changeAPIName.substring(0, 3);
			//debug
			//System.out.println("changeType is " + changeType);
			//System.out.println("changeAPIPrefix is " + changeAPIPrefix);

			/* 
			 * ���oAffected Items Table
			 */
			ITable changeAITable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			/*
			 *  �p�G�OUpdate Table Event�A���oUpdate Table Action
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
			 * (Disabled on 2015/12/31)��Event Type = (Row Update Action = Add-Row or Update-Row) or (Extend Actions Menu) �~�ʧ@
			 * ��Event Type = (Row Update Action = Add-Row) or (Extend Actions Menu) �~�ʧ@
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
					 * �p�GAI�̫�o�檩�S��BOM�Aflag=new�F�Ϥ��Aflag=update
					 */
					if (changeAPIPrefix.equals("DCN") || 
							changeAPIPrefix.equals("ECO") ||
							changeAPIPrefix.equals("PBR")) {
						// BOM-Action flag
						ICell aiBOMActionFlagCell = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_TEXT03);
						// Flag�O�Ū��~�B�z
						if (aiBOMActionFlagCell.getValue().toString().equals("")) {
							// Determine flag value
							// �p�GAI�̫�o�檩�S��BOM�Aflag=new�F�Ϥ��Aflag=update
							IItem item = (IItem)session.getObject(ItemConstants.CLASS_PARTS_CLASS, aiItemNumber);
							//System.out.println(item.getRevision());
							ITable bomTable = item.getTable(ItemConstants.TABLE_BOM);
							if (bomTable.isEmpty())
								actionFlag = "new";
							else
								actionFlag = "update";
							// Update BOM-Action flag
							aiBOMActionFlagCell.setValue(actionFlag);
							// ���ͦ��\�T��
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
					// Flag�O�Ū��~�B�z
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
						// ���ͦ��\�T��
						successLog = "Set Item/BOM Action Flag completely.";
					}
				}
			}
			//�]�wAction Result
			actionResult = new ActionResult(ActionResult.STRING, successLog);
		} catch (Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
		}
		// �Ǧ^Event Action Result
		return new EventActionResult(request, actionResult);
	}

}
// /:~
