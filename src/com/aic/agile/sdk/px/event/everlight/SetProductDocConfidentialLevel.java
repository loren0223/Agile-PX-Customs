//: SetProductDocConfidentialLevel.java
package com.aic.agile.sdk.px.event.everlight;
import java.util.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import com.agile.api.*;
import com.agile.px.*;
import com.aic.agile.sdk.util.*;
/**
 * �ھ�D01-Product Document�����~��������A�ܧ�PageThree.���K���šC
 * <p>EVL_PX_CONFIG:���y�z�զ��W�h�]�w���<br>
 * Everlight_Docs_Description_Element.xls<br>
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class SetProductDocConfidentialLevel implements IEventAction {
	final String EVL_PX_CONFIG = UtilConfigReader.readProperty("EVL_PX_CONFIG");
	final String SHEET_NAME = "Docs_Desc_Element";
	final String docDescElementSeparator = "_";
	ActionResult actionResult = null;
	String successLog = "SUCCESS - Confidential Level Configuration Succeed. ";
	Sheet sheet = null;
	IItem doc = null;
	int eventType = 0;
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
 	    	//�૬Item����
 	    	doc = (IItem)object.getDataObject();
 	    	//���oEvent Type
 	    	eventType = object.getEventType();
 	    	//���oDocument Type��T
 	    	String docType = doc.getCell(ItemConstants.ATT_TITLE_BLOCK_DOCUMENT_TYPE).getValue().toString(); 
			//debug output
			//System.out.println("docType is "+docType);
			/*
			 * �DD01-Product Document�����������B�z
			 */
			if(!docType.equals("D01-Product Document���~���")) {
				actionResult = new ActionResult(ActionResult.STRING, "Nothing to do. (Document Type:"+docType+")");
				//�Ǧ^Event Action Result
		 	    return new EventActionResult(request, actionResult);
			}
			/*
			 * Ū�����y�z�զ��W�h (EVL_PX_CONFIG.Docs_Decription_Element)
			 * Excel Column Header: �������(Subclass Name)|���Ӷ�����(Attribute Display Name)|�������ݩʭ�|Description Element|���K����
			 */
			sheet = ExcelUtility.getDataSheet(EVL_PX_CONFIG, SHEET_NAME);
			/*
			 * ���o�ŦXĲ�oEvent������Document Type��Elements
			 */
			List<Row> docDescElements = ExcelUtility.filterDataSheet(sheet, 0, docType);
			/*
			 * Loop�Ҧ�Elements: �ھڤ��Ӷ������ݩʱ���ȨM�w���K���šC
			 */
		    Iterator it = docDescElements.iterator();
		    while(it.hasNext()) {
		    	Row row = (Row)it.next();
		    	//���o���Ӷ������ݩʦW�١B����ȡB���K����
		    	String docCategoryAttrName = ExcelUtility.getSpecificCellValue(row, 1);
		    	String docCategoryAttrValue = ExcelUtility.getSpecificCellValue(row, 2);
		    	String confidentialLevel = ExcelUtility.getSpecificCellValue(row, 4);
		    	//debug
		    	//System.out.println("docCategoryAttrName is "+docCategoryAttrName);
		    	//System.out.println("docCategoryAttrValue is "+docCategoryAttrValue);
		    	//System.out.println("confidentialLevel is "+confidentialLevel);
		    	
		    	//�p�G���Ӷ������ݩʦW�١B����ȡA����@�ӬO�Ū��A�ߥX���~�T��
		    	if(!docCategoryAttrName.equals("") && docCategoryAttrValue.equals("")) {
		        	throw new Exception("WARNING - DOC_DESC_ELEMENT_CONFIG Lost!(���Ӷ���������ȿ�!) (Document Type:"+docType+")");
		        }else if(docCategoryAttrName.equals("") && !docCategoryAttrValue.equals("")) {
		        	throw new Exception("WARNING - DOC_DESC_ELEMENT_CONFIG Lost!(���Ӷ������ݩʦW�ٿ�!) (Document Type:"+docType+")");
		        }
		        //�p�G�Ҥ�����
		        else if(!docCategoryAttrName.equals("") && !docCategoryAttrValue.equals("")) {
		        	//���oĲ�oEvent�����󪺤��Ӷ������ݩ�
		    		ICell productDocumentType = doc.getCell(docCategoryAttrName);
		    		//�p�G��줣�s�b�A�ߥXĵ�i�T��
		    		if(productDocumentType==null) {
		    			throw new Exception("WARNING - Required Document Attribute does not exist!(���~��������ݩʤ��s�b!) (Attribute Name:"+docCategoryAttrName+")");
		    		}
		    		//���o�����ڪ����Ӷ������ݩʭ�
		    		String actualDocCategoryAttrValue = productDocumentType.getValue().toString();
		    		//�p�G��ڭ�=����ȡA�ܧ���K����
		    		if(actualDocCategoryAttrValue.equals(docCategoryAttrValue)) {
		    			ICell levelCell = doc.getCell(ItemConstants.ATT_PAGE_THREE_LIST05);
		    			IAgileList values = levelCell.getAvailableValues();
		    			values.setSelection(new Object[]{confidentialLevel});
		    			levelCell.setValue(values);
		    			//���ͦ��\��Action Result
		    			actionResult = new ActionResult(ActionResult.STRING, successLog);
		    			//�^��Event Action Result
		    			return new EventActionResult(request, actionResult);
		    		}
		        }
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
	
}

///:~
	