//: BOMStructureValidator.java
package com.agree.agile.sdk.px.event.dfi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import com.agile.api.*;
import com.agile.px.*;

/**
 * DFI BOM���c�P���u���O�ˮֵ{��.
 * <p>BOM�ܧ󪺵��G���o�H�ϤU�C�W�h:<br>
 * 1) �ۦPFN�D�Ƥ��୫��<br>
 * 2) ���PFN�D�Ƥ��୫��<br>
 * 3) �ƶq��ܦ������O�ƭȫ��A<br>
 * 4) �ƶq�����j��0<br>
 * 5) �ۦPFN���N�Ƥ��୫��<br>
 * 6) ���N��(S)����S���D��(M)<br>
 * 7) ���N�Ƽƶq��������D��<br>
 * 8) ���N�Ƥ��൥��D��<br>
 * 9) ���N�Ƥ��঳�����m<br>
 * 10) �s��BOM�T��s��Find No,Main/Sub,Item No<br>
 * 11) �s��BOM�T��R��A�ƤS�s�WA��(�ۦPM/S)<br>
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class BOMStructureValidator implements IEventAction {
	ActionResult actionResult = null;
	String actionFlag = null;
	StringBuffer allAIExpLog = new StringBuffer();
	String tabSpace = "    ";
	String tabSpace2 = "        ";
	int index = 0;
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
 	    	//���oChange Type��T
			String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString(); 
			//���oChange API Prefix(3 digits)
 	    	String changeAPIName = change.getAgileClass().getAPIName();
			String changeAPIPrefix = changeAPIName.substring(0, 3);
			
			//System.out.println("changeType is "+changeType);
			//System.out.println("changeAPIPrefix is "+changeAPIPrefix);
			
			//���oAffected Items(AI) Table
			ITable changeAITable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			//Loop�Ҧ�AI�A�ˬdBOM���c�O�_�X�k
			Iterator changeAI = changeAITable.iterator();
			while(changeAI.hasNext()) {
				//Error Log Buffer
				StringBuffer aiExpLog = new StringBuffer();
				StringBuffer duplicateMainItemSameFNExpLog = new StringBuffer();
				StringBuffer duplicateMainItemDiffFNExpLog = new StringBuffer();
				StringBuffer mainItemQtyFormatExpLog = new StringBuffer();
				StringBuffer mainItemQtyExpLog = new StringBuffer();
				StringBuffer duplicateSubItemSameFNExpLog = new StringBuffer();
				StringBuffer subItemWithoutMainItemExpLog = new StringBuffer();
				StringBuffer subItemQtyNotEqualMainItemQtyExpLog = new StringBuffer();
				StringBuffer subItemEqualMainItemExpLog = new StringBuffer();
				StringBuffer subItemHasRefDesExpLog = new StringBuffer();
				StringBuffer redlineModifyFindNoMainSubItemNoExpLog = new StringBuffer();
				StringBuffer redlineAddDeleteSameItemWithSameMainSubExpLog = new StringBuffer();
				//���oAffected Item (AI)
				IRow ai = (IRow)changeAI.next();
				/*
				 * ��BOM Redline���O���~��BOM�]��
				 */
				if(ai.isFlagSet(ChangeConstants.FLAG_AI_ROW_HAS_REDLINE_BOM)) {
					//���oPending Change Item Object
					IItem pendingChangeItem = (IItem)ai.getReferent();
					pendingChangeItem.setRevision(change);
					//���oAffected Item Number
					String itemNumber = pendingChangeItem.getName();
					/*
					 * ���oBOM���c�r��}�C��C(Sorted by Find No,Main Sub,Item No ascending)
					 * BOM���c�r��榡: FN|MS|ItemNo|Qty|RefDes
					 */
					ArrayList<String> fullPendingBOMString = getFullBOMString(pendingChangeItem);
					//BOM���c�r����|
					String fullPendingBOMStringStack = "";
					/*
					 * Loop BOM Structure
					 */
					Iterator<String> pendingBOM = fullPendingBOMString.iterator();
					while(pendingBOM.hasNext()) {
						//debug
						//System.out.println("fullPendingBOMString is "+fullPendingBOMStringStack);
						/*
						 * �̧Ǩ��oBOM���c�r��
						 */
						String pendingBOMString = pendingBOM.next();
						//debug
						//System.out.println("pendingBOMString is "+pendingBOMString);
						/*
						 * �qBOM���c�r����oFind No,Main Sub,Item No,Qty,Ref Des
						 */
						String[] bomAttributes = pendingBOMString.split("\\|");
						//debug
						/*
						for(String attr : bomAttributes) {
							System.out.println(attr);
						}*/
						String findNo = bomAttributes[0];
						String mainSub = bomAttributes[1];
						String itemNo = bomAttributes[2];
						String qty = bomAttributes.length>=4? bomAttributes[3] : "";
						String refDes = bomAttributes.length==5? bomAttributes[4] : "";
						//debug
						//System.out.println("findNo is "+findNo);
						//System.out.println("mainSub is "+mainSub);
						//System.out.println("itemNo is "+itemNo);
						//System.out.println("qty is "+qty);
						//System.out.println("refDes is "+refDes);
						/*
						 * �PBOM���c�r����|���:
						 * �p�GComponent�O�D�� (MainSub=M) {
						 * 		�ۦPFN�D�Ƥ��୫�� 
						 * 		���PFN�D�Ƥ��୫��
						 * 		�ƶq��ܦ������O�ƭȫ��A
						 * 		�ƶq�����j��0
						 * }
						 */
						if(mainSub.equals("M")) {
							//�ۦPFN�D�Ƥ��୫�� 
							if(fullPendingBOMStringStack.matches(".*("+findNo+"\\|"+mainSub+").*")) {
								duplicateMainItemSameFNExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
							}
							//���PFN�D�Ƥ��୫��
							else if(fullPendingBOMStringStack.matches(".*("+mainSub+"\\|"+itemNo+"\\|).*")) {
								duplicateMainItemDiffFNExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
							}
							//�ƶq��ܦ������O�ƭȫ��A
							//�ƶq�����j��0
							if(isNumeric(qty)) {
								double d = Double.parseDouble(qty);
								if(d <= (double)0) {
									mainItemQtyExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
								}
							} else {
								mainItemQtyFormatExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
							}
						} 
						/*
						 * �PBOM���c�r����|���:
						 * �p�GComponent�O���N�� (MainSub=S) {
						 * 		�ۦPFN���N�Ƥ��i����
						 * 		���N��(S)����S���D��(M)
						 * 		���N�Ƽƶq��������D�� 
						 * 		���N�Ƥ��൥��D��
						 * 		���N�Ƥ��঳�����m
						 * }
						 */
						else if(mainSub.equals("S")) {
							//�ۦPFN���N�Ƥ��i����
							if(fullPendingBOMStringStack.matches(".*("+findNo+"\\|"+mainSub+"\\|"+itemNo+"\\|).*")) {
								duplicateSubItemSameFNExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
							}
							//���N��(S)����S���D��(M)
							//���N�Ƽƶq��������D�� 
							if(!fullPendingBOMStringStack.matches(".*("+findNo+"\\|M).*")) {
								subItemWithoutMainItemExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
							} else {
								//debug:���N�Ƽƶq���o����
								if(qty.equals("")) {
									qty = "-";
								}
								if(!fullPendingBOMStringStack.matches(".*("+findNo+"\\|M\\|[^;]+\\|"+qty+").*")) {
									subItemQtyNotEqualMainItemQtyExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
								}
							}
							//���N�Ƥ��൥��D��
							if(fullPendingBOMStringStack.matches(".*("+findNo+"\\|M\\|"+itemNo+"\\|).*")) { //Fixed 2016/02/16
								subItemEqualMainItemExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
							}
							//���N�Ƥ��঳�����m
							if(!refDes.equals("")) {
								subItemHasRefDesExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
							}
						}
						//�ֿnBOM���c�r����|�A�ѤU�@��component��@�P�_���
						fullPendingBOMStringStack += pendingBOMString+";";
					}
					
					/*
					 * ���oLatest-Released-Revision��BOM���c�r��}�C��C
					 */
					IItem item = (IItem)session.getObject(ItemConstants.CLASS_PARTS_CLASS, itemNumber);
					ArrayList<String> fullBOMString = getFullBOMString(item);
					/*
					 * ���oLatest-Released-Revision��BOM���c�r����|
					 */
					String fullBOMStringStack = "";
					Iterator<String> fullBOMStrings = fullBOMString.iterator();
					while(fullBOMStrings.hasNext()) {
						fullBOMStringStack += fullBOMStrings.next() + ";";
					}
					//debug
					//System.out.println("fullBOMString is "+fullBOMStringStack);
					
					//�ŧiRedline Add/Remove Component List�r��
					//String addRedlineListString = ""; 
					//String removeRedlineListString = "";
					ArrayList<String> addRedlineRowList = new ArrayList<String>(); //Fixed 2016/02/16
					ArrayList<String> removeRedlineRowList = new ArrayList<String>(); //Fixed 2016/02/16
					//���oRedline BOM Table
					ITable redlineBOMTable = pendingChangeItem.getTable(ItemConstants.TABLE_REDLINEBOM);
					/*
					 * Loop Redline BOM Structure
					 */
					ITwoWayIterator redlineBOM = redlineBOMTable.getTableIterator();
					while(redlineBOM.hasNext()) {
						IRow redlineRow = (IRow)redlineBOM.next();
						//���oBOM Redline Component Info.
						String findNo = redlineRow.getCell(ItemConstants.ATT_BOM_FIND_NUM).getValue().toString();
						String mainSub = redlineRow.getCell(ItemConstants.ATT_BOM_BOM_LIST01).getValue().toString();
						String itemNo = redlineRow.getCell(ItemConstants.ATT_BOM_ITEM_NUMBER).getValue().toString();
						//�զ�Redline BOM Component Line���c�r��
						String redlineItemString = findNo+"|"+mainSub+"|"+itemNo;
						String redlineItemString2 = mainSub+"|"+itemNo; //Fixed 2016/02/16
						//debug
						//System.out.println("redlineItemString is "+redlineItemString);
						
						/*
						 * 	�s��BOM�T��s��Find No,Main/Sub,Item No
						 * 	Logic:	�p�G[BOM���c�r����|]�S���]�t[Redline BOM Component Line���c�r��]�A
						 * 			���Redline BOM Component Line ���ܧ� Find No/Main Sub/Item No.
						 */
						if(redlineRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_MODIFIED)) {
							if(!fullBOMStringStack.contains(redlineItemString)) {
								redlineModifyFindNoMainSubItemNoExpLog.append(tabSpace2+redlineItemString+"\n");
							}
						}
						/*
						 * 	�s��BOM�T��R��A�ƤS�s�WA��(�ۦPM/S) //Fixed 2016/02/16
						 *  Logic: 	[Add/Remove Redline BOM Component Line���c�r��] ��� [Redline Add/Remove Component List�r��]�A
						 * 			�p�G�s�b��List�A��ܦ�Delete&Add�ާ@!!
						 */
						//Redline Action = Add
						if(redlineRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_ADDED)) {
							//�p�GRedline Component �w�g�R���F�S�s�W
							//if(removeRedlineListString.contains(redlineItemString2)) { 
							if(removeRedlineRowList.contains(redlineItemString2)) { //Fixed 2016/02/16
								//�ߥX���~�T��
								redlineAddDeleteSameItemWithSameMainSubExpLog.append(tabSpace2+redlineItemString+"\n");
							} else {
								//Redline Component Line �[�J Redline Add Component List
								//addRedlineListString += redlineItemString2+";";
								addRedlineRowList.add(redlineItemString2); //Fixed 2016/02/16
							}
						}
						//Redline Action = Remove
						else if(redlineRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED)) {
							//�]�� Redline Remove Component �L�k�q Row ���o Item No�A[Redline BOM Component Line���c�r��]�ݭn�S�O�B�z
							redlineItemString += redlineRow.getReferent().getName();
							redlineItemString2 += redlineRow.getReferent().getName();
							//debug
							//System.out.println("redlineItemString(Remove) is "+redlineItemString);
							//�p�GRedline Component �w�g�s�W�F�S�R��
							//if(addRedlineListString.contains(redlineItemString2)) {
							if(addRedlineRowList.contains(redlineItemString2)) { //Fixed 2016/02/16
								//�ߥX���~�T��
								redlineAddDeleteSameItemWithSameMainSubExpLog.append(tabSpace2+redlineItemString+"\n");
							} else {
								//Redline Component Line �[�J Redline Remove Component List
								//removeRedlineListString += redlineItemString2+";";
								removeRedlineRowList.add(redlineItemString2); //Fixed 2016/02/16
							}
						}
					}
					/*
					 * �զ����Error Message <html tag>
					 */
					if(!duplicateMainItemSameFNExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("�ۦPFN�D�ƭ���\n").append("</span>").append(duplicateMainItemSameFNExpLog.toString()).append("</td></tr>");
					if(!duplicateMainItemDiffFNExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("���PFN�D�ƭ���\n").append("</span>").append(duplicateMainItemDiffFNExpLog.toString()).append("</td></tr>");
					if(!mainItemQtyFormatExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("�ƶq���O�ƭȫ��A\n").append("</span>").append(mainItemQtyFormatExpLog.toString()).append("</td></tr>");	
					if(!mainItemQtyExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("�ƶq�S���j��0\n").append("</span>").append(mainItemQtyExpLog.toString()).append("</td></tr>");
					if(!duplicateSubItemSameFNExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("�ۦPFN���N�ƭ���\n").append("</span>").append(duplicateSubItemSameFNExpLog.toString()).append("</td></tr>");
					if(!subItemWithoutMainItemExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("���N�ƨS���D��\n").append("</span>").append(subItemWithoutMainItemExpLog.toString()).append("</td></tr>");
					if(!subItemQtyNotEqualMainItemQtyExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("���N�Ƽƶq������D��\n").append("</span>").append(subItemQtyNotEqualMainItemQtyExpLog.toString()).append("</td></tr>");
					if(!subItemEqualMainItemExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("���N�Ƶ���D��\n").append("</span>").append(subItemEqualMainItemExpLog.toString()).append("</td></tr>");
					if(!subItemHasRefDesExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("���N�Ƥ��঳�����m\n").append("</span>").append(subItemHasRefDesExpLog.toString()).append("</td></tr>");
					if(!redlineModifyFindNoMainSubItemNoExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("�s��BOM�ɡA�T��s��Find No|Main Sub|Item No\n").append("</span>").append(redlineModifyFindNoMainSubItemNoExpLog.toString()).append("</td></tr>");
					if(!redlineAddDeleteSameItemWithSameMainSubExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("�s��BOM�ɡA�T��R���S�s�W���ۦPMain/Sub��Item\n").append("</span>").append(redlineAddDeleteSameItemWithSameMainSubExpLog.toString()).append("</td></tr>");
					//�զ���槹��Message <html tag>
					if(!aiExpLog.toString().equals("")) 
						this.allAIExpLog.append("<tr>").append("<td>"+(++index)+"</td>").append("<td>"+itemNumber+"</td>").append("<td><table style='text-align: left; width: 100%;' border='0' cellpadding='2' cellspacing='2'><tbody>"+aiExpLog.toString()+"</tbody></table></td>").append("</tr>");
				}
			}
			 
			//���ͦ��\/���ѰT��
			if(allAIExpLog.toString().equals(""))
				actionResult = new ActionResult(ActionResult.STRING, "BOM Structure Validation is OK.");
			else {
				/*
				 * Trigger: Actions Menu
				 * Output: The detail error messages.
				 */
				if(eventType == EventConstants.EVENT_EXTEND_ACTIONS_MENU) {
					//�զ�Error Message������HTML��X
					allAIExpLog.insert(0, "<table style='text-align: left; width: 100%;' border='1' cellpadding='2' cellspacing='2'><tbody>"+"<tr><th>No</th><th>Affected Item</th><th>Error Message</th></tr>");
					allAIExpLog.append("</tbody></table>");
					throw new Exception("<h4>�Эץ��U�C<span style='color: red;'>BOM�ˮֿ��~</span><h4><hr>"+
							"<h5>BOM�ˮֶ��ءG\n"+
							"1) �ۦPFN�D�Ƥ��୫��\n"+
							"2) ���PFN�D�Ƥ��୫��\n"+
							"3) �ƶq��ܦ������O�ƭȫ��A\n"+
							"4) �ƶq�����j��0\n"+
							"5) �ۦPFN���N�Ƥ��୫��\n"+
							"6) ���N��(S)����S���D��(M)\n"+
							"7) ���N�Ƽƶq��������D��\n"+
							"8) ���N�Ƥ��൥��D��\n"+
							"9) ���N�Ƥ��঳�����m\n"+
							"10) �s��BOM�T��s��Find No,Main/Sub,Item No\n"+
							"11) �s��BOM�T��R��A�ƤS�s�WA��(�ۦPM/S)\n</h5>"+
							"<hr>"+
							allAIExpLog.toString());
				} 
				/*
				 * Trigger: Change Status for Workflow
				 * Output: Error notice.
				 */
				else if(eventType == EventConstants.EVENT_CHANGE_STATUS_FOR_WORKFLOW) {
					throw new Exception("WARNING! - BOM Structure Validation is FAIL. Please validate and fix BOM Errors. Click [Actions]->[BOM Structure Validation] for the details.");
				}
			}
		} catch(Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
		}
		//�Ǧ^Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	
	/**
	 * �ˬd�r��O�_���ƭȮ榡.
	 * @param str �n�P�_���r��
	 * @return true��ܬO�ƭȮ榡�Afalse��ܫD�ƭȮ榡
	 */
	public static boolean isNumeric(String str) {  
		try {   
			double d = Double.parseDouble(str);  
		} catch(NumberFormatException nfe) {  
			return false;  
		}  
		return true;  
	}
	/**
	 * ���oBOM���c�r��}�C��C
	 * @param item �n���oBOM��Item����
	 * @return BOM���c�r��}�C��C
	 * @throws Exception throw when exception is happened
	 */
	public static ArrayList<String> getFullBOMString(IItem item) throws Exception {
		ArrayList<String> fullBOMString = new ArrayList<String>();
		try {
			//���oBOM Table
			ITable bomTable = item.getTable(ItemConstants.TABLE_BOM);
			Iterator bom = bomTable.iterator();
			//���ͥ�BOM�y�z�r��
			while(bom.hasNext()) {
				IRow component = (IRow)bom.next();
				String findNo = component.getCell(ItemConstants.ATT_BOM_FIND_NUM).getValue().toString();
				String mainSub = component.getCell(ItemConstants.ATT_BOM_BOM_LIST01).getValue().toString();
				String itemNo = component.getCell(ItemConstants.ATT_BOM_ITEM_NUMBER).getValue().toString();
				String qty = component.getCell(ItemConstants.ATT_BOM_QTY).getValue()==null? "" : component.getCell(ItemConstants.ATT_BOM_QTY).getValue().toString();
				String refDes = component.getCell(ItemConstants.ATT_BOM_REF_DES).getValue()==null? "" : component.getCell(ItemConstants.ATT_BOM_REF_DES).getValue().toString();
				//debug:2015/12/31 BOM�ˬd���íץ��A�קKFind No��Ƥ��P�y���Ƨǿ���
				int x = 6 - findNo.length();
				for(int i=1; i<=x; i++) {
					findNo = "0"+findNo;
				}
				//BOM���c�r��榡: FN|MS|ItemNO|Qty|RefDes
				String bomString = findNo+"|"+mainSub+"|"+itemNo+"|"+qty+"|"+refDes;
				fullBOMString.add(bomString);
			}
			//BOM���c�r��Ƨ�
			Collections.sort(fullBOMString);
		} catch(Exception ex) {
			throw ex;
		}
		return fullBOMString;
	}
	/**
	 * �����r��}�Y�Ҧ����r��0
	 * @param bomString BOM���c�r��
	 * @return �}�Y�D0��BOM���c�r��
 	 * @throws Exception throw when exception is happened
	 */
	public String removePrefixZero(String bomString) throws Exception {
		try {
			while(bomString.startsWith("0")) {
				bomString = bomString.substring(1, bomString.length());
			}
			return bomString;
		} catch(Exception ex) {
			throw ex;
		}
	}
}
///:~
	