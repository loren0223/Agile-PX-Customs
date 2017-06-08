//: ItemRevFormatValidator.java
package com.agree.agile.sdk.px.event.dfi;
import java.util.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;
/**
 * Item�����榡���Ҿ�.
 * <p>Item�o��e�ˬdNew Revision�榡�O�_���T�A���T���ܤ~���\�y�{�o��C<br>
 * <p>DFI_PX_CONFIG�����]�w:<br>
 * Item Rev Format Validator
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class ItemRevFormatValidator implements IEventAction {
	final String DFI_PX_CONFIG = UtilConfigReader.readProperty("DFI_PX_CONFIG");
	final String SHEET_NAME = "Item Rev Format Validator";
	ActionResult actionResult = null;
	String successLog = "Nothing to do.";
	StringBuffer revFormatErrorLog = new StringBuffer();
	StringBuffer revSeqErrorLog = new StringBuffer();
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
 	    	
			//System.out.println("changeType is "+changeType);
			//System.out.println("changeAPIPrefix is "+changeAPIPrefix);
			
			/*
			 * Ū DFI_PX_CONFIG - Item Rev Format Validator
			 */
			sheet = ExcelUtility.getDataSheet(DFI_PX_CONFIG, SHEET_NAME);
			/*
			 * ���o�ŦXChange API Prefix��Item Revision Format RegEx
			 */
			//Excel Column Header: |Change API Prefix|AI Class API Name|AI Rev Format RegEx|
			List<Row> revFormatRegExSheet = ExcelUtility.filterDataSheet(sheet, 0, changeAPIPrefix);
			
		    /*
		     * ���oAffected Items Table
		     */
		    ITable changeAITable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator<IRow> changeAI = changeAITable.iterator();
		    /*
		     * �ˬdItem Rev Format & Rev Order
		     */
			validateItemRevFormat(changeAI, revFormatRegExSheet);
			/*
			 * ���� Action Result Message
			 */
			//System.out.println("ErrorLog is "+failLog.toString());
			//�S�����~
			if(revFormatErrorLog.toString().equals("") && revSeqErrorLog.toString().equals("")) {
	    		//���ͦ��\�T��
				successLog = "Item Revison Validation is OK.";
	    		//Action result is successful.
	    		actionResult = new ActionResult(ActionResult.STRING, successLog);
	    	} 
	    	//�p�G�����~�o��
	    	else {
	    		//Combine���~�T��
	    		String revErrorLog = "";
	    		//New Rev Format�����~
	    		if(!revFormatErrorLog.toString().equals("")) {
		    		//���~�T���ɤW�e������
		    		revFormatErrorLog.insert(0,"WARNING: Wrong New Revision Format of Affected Items. New Revision Format should be "+ getRevFormatRegEx(revFormatRegExSheet) +"."+
		    				"Affected Items List: [" );
		    		//���~�T���ɤW�᭱�A��
		    		revFormatErrorLog.append("]");
		    		//
		    		revErrorLog += revFormatErrorLog.toString();
    			}
	    		//New Rev Seq�����~
	    		if(!revSeqErrorLog.toString().equals("")) {
		    		//���~�T���ɤW�e������
	    			revSeqErrorLog.insert(0,"WARNING: New Revision should be greater than Old Revision. "+
		    				"Affected Items List: [" );
		    		//���~�T���ɤW�᭱�A��
	    			revSeqErrorLog.append("]");
	    			//
	    			if(!revErrorLog.equals("")) {
	    				revErrorLog += " �F ";
	    			}
	    			revErrorLog += revSeqErrorLog.toString();
	    		}
	    		//�ߥX���~Exception
	    		throw new Exception(revErrorLog);
	    	}
	    } catch(Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} finally {
			sheet = null;
		}
		//�Ǧ^Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	
	/**
	 * ���oChange�����ݩʭ�.
	 * @param change Change����
	 * @param APIName Change�ݩ�API Name
	 * @return Change�����ݩʭ�      
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
	 * ����Item New Rev�O�_�X�k.
	 * @param changeAI Change����Affected Item Table��Iterator 
	 * @param revFormatRegExSheet DFI_PX_CONFIG��Item Rev Format RegEx�C��
	 * @throws Exception throw when exception is happened
	 */
	private void validateItemRevFormat(Iterator<IRow> changeAI, List<Row> revFormatRegExSheet) throws Exception {
		try {
			/*
			 * Loop Affected Item Table
			 */
			while(changeAI.hasNext()) {
				//���oAI info
				IRow ai = changeAI.next();
				String aiNewRev = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_REVISION).getValue()==null? "" : ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_REVISION).getValue().toString();
				String aiOldRev = ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_REV).getValue()==null? "" : ai.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_REV).getValue().toString();
				String aiClassAPIName = ai.getReferent().getAgileClass().getSuperClass().getAPIName();
				String itemNumber = ai.getReferent().getName();
				//debug:�p�G�}�b�����u���j�����A�ɨ��p����00
				if(aiOldRev.length()==3) {
					aiOldRev += "00";
				}
				//debug
				//System.out.println("aiNewRev is "+aiNewRev);
				//System.out.println("aiOldRev is "+aiOldRev);
				//��l��Check-Flag (default=false)
				boolean isRegExPassed = false;
				boolean isRevSeqPassed = false;
				/*
				 * ��惡Change Type���Ҧ�RegEx�A�ˬdNew Rev Format�O�_�ŦX
				 */
				Iterator<Row> revFormatRegExs = revFormatRegExSheet.iterator();
				//System.out.println("revFormatRegExs size is "+revFormatRegExs.size());
				while(revFormatRegExs.hasNext()) {
					Row revFormatRegEx = revFormatRegExs.next();
					//�qDFI_PX_CONFIG Excel ���o AI Class API Name & Rev Format RegEx
					String valAIClassAPIName = ExcelUtility.getSpecificCellValue(revFormatRegEx, 1);
					String valAIRevFormatRegEx = ExcelUtility.getSpecificCellValue(revFormatRegEx, 2);
					//System.out.println("valAIClassAPIName is "+valAIClassAPIName);
					//System.out.println("valAIRevFormatRegEx is "+valAIRevFormatRegEx);
					
					/*
					 * AI-Class = Parts
					 */
					if(aiClassAPIName.equals(valAIClassAPIName)) {
						/*
						 * �p�GItem New Rev���ŦXDFI�����榡�A�ˮ֤��q�L�A�[�J���~�M��
						 */
						if(aiNewRev.matches(valAIRevFormatRegEx)) {
							isRegExPassed = true;
							//������RegEx
							break;
						}
					}
					/*
					 * AI-Class = Documents
					 */
					else {
						//Document����������榡
						isRegExPassed = true;
						//������RegEx
						break;
					}
				}
				/*
				 * �p�GOld Rev�����šA�BNew Rev <= Old Rev�A�ˮ֤��q�L�A�[�J���~�M��
				 */
				if(!aiOldRev.equals("")) {
					//debug
					//System.out.print("aiNewRev.compareTo(aiOldRev) is "+aiNewRev.compareTo(aiOldRev));
					if(aiNewRev.compareTo(aiOldRev) > 0) {
						isRevSeqPassed = true;
					}
				} else {
					isRevSeqPassed = true;
				}
				
				//�p�G�S���q�LRev Format >> �O��ItemNo��revFormatErrorLog
				if(!isRegExPassed) {
					revFormatErrorLog.append(itemNumber+"  ");
				}
				
				//�p�G�S���q�LRev Seq�ˬd  >> �O��ItemNo��revSeqErrorLog
				if(!isRevSeqPassed) {
					revSeqErrorLog.append(itemNumber+"  ");
				}
			}
		} catch(Exception e) {
			throw e;
		}
	}
	/**
	 * �^�ǦX�k��New Rev���`�W��ܦ�.
	 * @param revFormatRegExSheet �A�γo��Change��New Rev Format��DFI_PX_CONFIG��Excel Row
	 * @return �X�k��New Rev���`�W��ܦ�
	 * @throws Exception throw when exception is happened
	 */
	private String getRevFormatRegEx(List<Row> revFormatRegExSheet) throws Exception {
		try {
			String revFormatRegEx = "";
			Iterator<Row> it = revFormatRegExSheet.iterator();
			while(it.hasNext()) {
				Row row = it.next();
				String valAIRevFormatRegEx = ExcelUtility.getSpecificCellValue(row, 2);
				revFormatRegEx += valAIRevFormatRegEx;
				if(it.hasNext()){
					revFormatRegEx += " | ";
				}
			}
			return revFormatRegEx;
		} catch(Exception e) {
			throw e;
		}
	}
}

///:~
	