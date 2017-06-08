//: BOMStructureValidator.java
package com.agree.agile.sdk.px.event.dfi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import com.agile.api.*;
import com.agile.px.*;

/**
 * DFI BOM結構與紅線註記檢核程式.
 * <p>BOM變更的結果不得違反下列規則:<br>
 * 1) 相同FN主料不能重複<br>
 * 2) 不同FN主料不能重複<br>
 * 3) 數量表示式必須是數值型態<br>
 * 4) 數量必須大於0<br>
 * 5) 相同FN替代料不能重複<br>
 * 6) 替代料(S)不能沒有主料(M)<br>
 * 7) 替代料數量必須等於主料<br>
 * 8) 替代料不能等於主料<br>
 * 9) 替代料不能有插件位置<br>
 * 10) 編輯BOM禁止編輯Find No,Main/Sub,Item No<br>
 * 11) 編輯BOM禁止刪除A料又新增A料(相同M/S)<br>
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
 	    	//取得Change Type資訊
			String changeType = change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString(); 
			//取得Change API Prefix(3 digits)
 	    	String changeAPIName = change.getAgileClass().getAPIName();
			String changeAPIPrefix = changeAPIName.substring(0, 3);
			
			//System.out.println("changeType is "+changeType);
			//System.out.println("changeAPIPrefix is "+changeAPIPrefix);
			
			//取得Affected Items(AI) Table
			ITable changeAITable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			//Loop所有AI，檢查BOM結構是否合法
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
				//取得Affected Item (AI)
				IRow ai = (IRow)changeAI.next();
				/*
				 * 有BOM Redline註記的才做BOM稽核
				 */
				if(ai.isFlagSet(ChangeConstants.FLAG_AI_ROW_HAS_REDLINE_BOM)) {
					//取得Pending Change Item Object
					IItem pendingChangeItem = (IItem)ai.getReferent();
					pendingChangeItem.setRevision(change);
					//取得Affected Item Number
					String itemNumber = pendingChangeItem.getName();
					/*
					 * 取得BOM結構字串陣列串列(Sorted by Find No,Main Sub,Item No ascending)
					 * BOM結構字串格式: FN|MS|ItemNo|Qty|RefDes
					 */
					ArrayList<String> fullPendingBOMString = getFullBOMString(pendingChangeItem);
					//BOM結構字串堆疊
					String fullPendingBOMStringStack = "";
					/*
					 * Loop BOM Structure
					 */
					Iterator<String> pendingBOM = fullPendingBOMString.iterator();
					while(pendingBOM.hasNext()) {
						//debug
						//System.out.println("fullPendingBOMString is "+fullPendingBOMStringStack);
						/*
						 * 依序取得BOM結構字串
						 */
						String pendingBOMString = pendingBOM.next();
						//debug
						//System.out.println("pendingBOMString is "+pendingBOMString);
						/*
						 * 從BOM結構字串取得Find No,Main Sub,Item No,Qty,Ref Des
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
						 * 與BOM結構字串堆疊比對:
						 * 如果Component是主料 (MainSub=M) {
						 * 		相同FN主料不能重複 
						 * 		不同FN主料不能重複
						 * 		數量表示式必須是數值型態
						 * 		數量必須大於0
						 * }
						 */
						if(mainSub.equals("M")) {
							//相同FN主料不能重複 
							if(fullPendingBOMStringStack.matches(".*("+findNo+"\\|"+mainSub+").*")) {
								duplicateMainItemSameFNExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
							}
							//不同FN主料不能重複
							else if(fullPendingBOMStringStack.matches(".*("+mainSub+"\\|"+itemNo+"\\|).*")) {
								duplicateMainItemDiffFNExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
							}
							//數量表示式必須是數值型態
							//數量必須大於0
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
						 * 與BOM結構字串堆疊比對:
						 * 如果Component是替代料 (MainSub=S) {
						 * 		相同FN替代料不可重複
						 * 		替代料(S)不能沒有主料(M)
						 * 		替代料數量必須等於主料 
						 * 		替代料不能等於主料
						 * 		替代料不能有插件位置
						 * }
						 */
						else if(mainSub.equals("S")) {
							//相同FN替代料不可重複
							if(fullPendingBOMStringStack.matches(".*("+findNo+"\\|"+mainSub+"\\|"+itemNo+"\\|).*")) {
								duplicateSubItemSameFNExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
							}
							//替代料(S)不能沒有主料(M)
							//替代料數量必須等於主料 
							if(!fullPendingBOMStringStack.matches(".*("+findNo+"\\|M).*")) {
								subItemWithoutMainItemExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
							} else {
								//debug:替代料數量不得為空
								if(qty.equals("")) {
									qty = "-";
								}
								if(!fullPendingBOMStringStack.matches(".*("+findNo+"\\|M\\|[^;]+\\|"+qty+").*")) {
									subItemQtyNotEqualMainItemQtyExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
								}
							}
							//替代料不能等於主料
							if(fullPendingBOMStringStack.matches(".*("+findNo+"\\|M\\|"+itemNo+"\\|).*")) { //Fixed 2016/02/16
								subItemEqualMainItemExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
							}
							//替代料不能有插件位置
							if(!refDes.equals("")) {
								subItemHasRefDesExpLog.append(tabSpace2+removePrefixZero(pendingBOMString)+"\n");
							}
						}
						//累積BOM結構字串堆疊，供下一個component當作判斷基準
						fullPendingBOMStringStack += pendingBOMString+";";
					}
					
					/*
					 * 取得Latest-Released-Revision的BOM結構字串陣列串列
					 */
					IItem item = (IItem)session.getObject(ItemConstants.CLASS_PARTS_CLASS, itemNumber);
					ArrayList<String> fullBOMString = getFullBOMString(item);
					/*
					 * 取得Latest-Released-Revision的BOM結構字串堆疊
					 */
					String fullBOMStringStack = "";
					Iterator<String> fullBOMStrings = fullBOMString.iterator();
					while(fullBOMStrings.hasNext()) {
						fullBOMStringStack += fullBOMStrings.next() + ";";
					}
					//debug
					//System.out.println("fullBOMString is "+fullBOMStringStack);
					
					//宣告Redline Add/Remove Component List字串
					//String addRedlineListString = ""; 
					//String removeRedlineListString = "";
					ArrayList<String> addRedlineRowList = new ArrayList<String>(); //Fixed 2016/02/16
					ArrayList<String> removeRedlineRowList = new ArrayList<String>(); //Fixed 2016/02/16
					//取得Redline BOM Table
					ITable redlineBOMTable = pendingChangeItem.getTable(ItemConstants.TABLE_REDLINEBOM);
					/*
					 * Loop Redline BOM Structure
					 */
					ITwoWayIterator redlineBOM = redlineBOMTable.getTableIterator();
					while(redlineBOM.hasNext()) {
						IRow redlineRow = (IRow)redlineBOM.next();
						//取得BOM Redline Component Info.
						String findNo = redlineRow.getCell(ItemConstants.ATT_BOM_FIND_NUM).getValue().toString();
						String mainSub = redlineRow.getCell(ItemConstants.ATT_BOM_BOM_LIST01).getValue().toString();
						String itemNo = redlineRow.getCell(ItemConstants.ATT_BOM_ITEM_NUMBER).getValue().toString();
						//組成Redline BOM Component Line結構字串
						String redlineItemString = findNo+"|"+mainSub+"|"+itemNo;
						String redlineItemString2 = mainSub+"|"+itemNo; //Fixed 2016/02/16
						//debug
						//System.out.println("redlineItemString is "+redlineItemString);
						
						/*
						 * 	編輯BOM禁止編輯Find No,Main/Sub,Item No
						 * 	Logic:	如果[BOM結構字串堆疊]沒有包含[Redline BOM Component Line結構字串]，
						 * 			表示Redline BOM Component Line 有變更 Find No/Main Sub/Item No.
						 */
						if(redlineRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_MODIFIED)) {
							if(!fullBOMStringStack.contains(redlineItemString)) {
								redlineModifyFindNoMainSubItemNoExpLog.append(tabSpace2+redlineItemString+"\n");
							}
						}
						/*
						 * 	編輯BOM禁止刪除A料又新增A料(相同M/S) //Fixed 2016/02/16
						 *  Logic: 	[Add/Remove Redline BOM Component Line結構字串] 比對 [Redline Add/Remove Component List字串]，
						 * 			如果存在於List，表示有Delete&Add操作!!
						 */
						//Redline Action = Add
						if(redlineRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_ADDED)) {
							//如果Redline Component 已經刪除了又新增
							//if(removeRedlineListString.contains(redlineItemString2)) { 
							if(removeRedlineRowList.contains(redlineItemString2)) { //Fixed 2016/02/16
								//拋出錯誤訊息
								redlineAddDeleteSameItemWithSameMainSubExpLog.append(tabSpace2+redlineItemString+"\n");
							} else {
								//Redline Component Line 加入 Redline Add Component List
								//addRedlineListString += redlineItemString2+";";
								addRedlineRowList.add(redlineItemString2); //Fixed 2016/02/16
							}
						}
						//Redline Action = Remove
						else if(redlineRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED)) {
							//因為 Redline Remove Component 無法從 Row 取得 Item No，[Redline BOM Component Line結構字串]需要特別處理
							redlineItemString += redlineRow.getReferent().getName();
							redlineItemString2 += redlineRow.getReferent().getName();
							//debug
							//System.out.println("redlineItemString(Remove) is "+redlineItemString);
							//如果Redline Component 已經新增了又刪除
							//if(addRedlineListString.contains(redlineItemString2)) {
							if(addRedlineRowList.contains(redlineItemString2)) { //Fixed 2016/02/16
								//拋出錯誤訊息
								redlineAddDeleteSameItemWithSameMainSubExpLog.append(tabSpace2+redlineItemString+"\n");
							} else {
								//Redline Component Line 加入 Redline Remove Component List
								//removeRedlineListString += redlineItemString2+";";
								removeRedlineRowList.add(redlineItemString2); //Fixed 2016/02/16
							}
						}
					}
					/*
					 * 組成單行Error Message <html tag>
					 */
					if(!duplicateMainItemSameFNExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("相同FN主料重複\n").append("</span>").append(duplicateMainItemSameFNExpLog.toString()).append("</td></tr>");
					if(!duplicateMainItemDiffFNExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("不同FN主料重複\n").append("</span>").append(duplicateMainItemDiffFNExpLog.toString()).append("</td></tr>");
					if(!mainItemQtyFormatExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("數量不是數值型態\n").append("</span>").append(mainItemQtyFormatExpLog.toString()).append("</td></tr>");	
					if(!mainItemQtyExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("數量沒有大於0\n").append("</span>").append(mainItemQtyExpLog.toString()).append("</td></tr>");
					if(!duplicateSubItemSameFNExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("相同FN替代料重複\n").append("</span>").append(duplicateSubItemSameFNExpLog.toString()).append("</td></tr>");
					if(!subItemWithoutMainItemExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("替代料沒有主料\n").append("</span>").append(subItemWithoutMainItemExpLog.toString()).append("</td></tr>");
					if(!subItemQtyNotEqualMainItemQtyExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("替代料數量不等於主料\n").append("</span>").append(subItemQtyNotEqualMainItemQtyExpLog.toString()).append("</td></tr>");
					if(!subItemEqualMainItemExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("替代料等於主料\n").append("</span>").append(subItemEqualMainItemExpLog.toString()).append("</td></tr>");
					if(!subItemHasRefDesExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("替代料不能有插件位置\n").append("</span>").append(subItemHasRefDesExpLog.toString()).append("</td></tr>");
					if(!redlineModifyFindNoMainSubItemNoExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("編輯BOM時，禁止編輯Find No|Main Sub|Item No\n").append("</span>").append(redlineModifyFindNoMainSubItemNoExpLog.toString()).append("</td></tr>");
					if(!redlineAddDeleteSameItemWithSameMainSubExpLog.toString().equals("")) 
						aiExpLog.append("<tr><td style='vertical-align: top;'>").append("<span style='color: red;'>").append("編輯BOM時，禁止刪除又新增有相同Main/Sub的Item\n").append("</span>").append(redlineAddDeleteSameItemWithSameMainSubExpLog.toString()).append("</td></tr>");
					//組成單行完整Message <html tag>
					if(!aiExpLog.toString().equals("")) 
						this.allAIExpLog.append("<tr>").append("<td>"+(++index)+"</td>").append("<td>"+itemNumber+"</td>").append("<td><table style='text-align: left; width: 100%;' border='0' cellpadding='2' cellspacing='2'><tbody>"+aiExpLog.toString()+"</tbody></table></td>").append("</tr>");
				}
			}
			 
			//產生成功/失敗訊息
			if(allAIExpLog.toString().equals(""))
				actionResult = new ActionResult(ActionResult.STRING, "BOM Structure Validation is OK.");
			else {
				/*
				 * Trigger: Actions Menu
				 * Output: The detail error messages.
				 */
				if(eventType == EventConstants.EVENT_EXTEND_ACTIONS_MENU) {
					//組成Error Message的完整HTML輸出
					allAIExpLog.insert(0, "<table style='text-align: left; width: 100%;' border='1' cellpadding='2' cellspacing='2'><tbody>"+"<tr><th>No</th><th>Affected Item</th><th>Error Message</th></tr>");
					allAIExpLog.append("</tbody></table>");
					throw new Exception("<h4>請修正下列<span style='color: red;'>BOM檢核錯誤</span><h4><hr>"+
							"<h5>BOM檢核項目：\n"+
							"1) 相同FN主料不能重複\n"+
							"2) 不同FN主料不能重複\n"+
							"3) 數量表示式必須是數值型態\n"+
							"4) 數量必須大於0\n"+
							"5) 相同FN替代料不能重複\n"+
							"6) 替代料(S)不能沒有主料(M)\n"+
							"7) 替代料數量必須等於主料\n"+
							"8) 替代料不能等於主料\n"+
							"9) 替代料不能有插件位置\n"+
							"10) 編輯BOM禁止編輯Find No,Main/Sub,Item No\n"+
							"11) 編輯BOM禁止刪除A料又新增A料(相同M/S)\n</h5>"+
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
		//傳回Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	
	/**
	 * 檢查字串是否為數值格式.
	 * @param str 要判斷的字串
	 * @return true表示是數值格式，false表示非數值格式
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
	 * 取得BOM結構字串陣列串列
	 * @param item 要取得BOM的Item物件
	 * @return BOM結構字串陣列串列
	 * @throws Exception throw when exception is happened
	 */
	public static ArrayList<String> getFullBOMString(IItem item) throws Exception {
		ArrayList<String> fullBOMString = new ArrayList<String>();
		try {
			//取得BOM Table
			ITable bomTable = item.getTable(ItemConstants.TABLE_BOM);
			Iterator bom = bomTable.iterator();
			//產生全BOM描述字串
			while(bom.hasNext()) {
				IRow component = (IRow)bom.next();
				String findNo = component.getCell(ItemConstants.ATT_BOM_FIND_NUM).getValue().toString();
				String mainSub = component.getCell(ItemConstants.ATT_BOM_BOM_LIST01).getValue().toString();
				String itemNo = component.getCell(ItemConstants.ATT_BOM_ITEM_NUMBER).getValue().toString();
				String qty = component.getCell(ItemConstants.ATT_BOM_QTY).getValue()==null? "" : component.getCell(ItemConstants.ATT_BOM_QTY).getValue().toString();
				String refDes = component.getCell(ItemConstants.ATT_BOM_REF_DES).getValue()==null? "" : component.getCell(ItemConstants.ATT_BOM_REF_DES).getValue().toString();
				//debug:2015/12/31 BOM檢查錯亂修正，避免Find No位數不同造成排序錯亂
				int x = 6 - findNo.length();
				for(int i=1; i<=x; i++) {
					findNo = "0"+findNo;
				}
				//BOM結構字串格式: FN|MS|ItemNO|Qty|RefDes
				String bomString = findNo+"|"+mainSub+"|"+itemNo+"|"+qty+"|"+refDes;
				fullBOMString.add(bomString);
			}
			//BOM結構字串排序
			Collections.sort(fullBOMString);
		} catch(Exception ex) {
			throw ex;
		}
		return fullBOMString;
	}
	/**
	 * 消除字串開頭所有的字元0
	 * @param bomString BOM結構字串
	 * @return 開頭非0的BOM結構字串
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
	