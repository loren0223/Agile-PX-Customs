//: DocsDescGenerator.java
package com.agree.agile.sdk.px.event.everlight;
import java.util.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;
/**
 * �ھ�EVL���y�z�զ��W�h�A�۰ʲ��ͤ��y�z.
 * <p>����Event Type�OCreate Object�BUpdate Title Block�BSave As�AĲ�oDocsDescGenerator���ͤ��y�z�C
 * ���ͤ��y�z��A�����P�_��L��󪺤��y�z�O�_���ơC�p�����ƭn����ĵ�i�T���i��user�Aĵ�i�T���|�����O���bDescription���C<br>
 * <p>EVL_PX_CONFIG:���y�z�զ��W�h�]�w���<br>
 * Everlight_Docs_Description_Element.xls<br>
 * 
 * @author Loren.Cheng
 * @version 1.0
 */
public class DocsDescGenerator implements IEventAction {
	final String EVL_PX_CONFIG = UtilConfigReader.readProperty("EVL_PX_CONFIG");
	final String SHEET_NAME = "Docs_Desc_Element";
	final String docDescElementSeparator = "_";
	ActionResult actionResult = null;
	String successLog = "SUCCESS - Document Description Generation Succeed. ";
	Sheet sheet = null;
	String docDesc = "";
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
 	    	//���oDocument Number
 	    	String docNumber = doc.getCell(ItemConstants.ATT_TITLE_BLOCK_NUMBER).getValue().toString();
 	    	//���oDocument Type��T
 	    	String docType = doc.getCell(ItemConstants.ATT_TITLE_BLOCK_DOCUMENT_TYPE).getValue().toString(); 
			//debug output
			//System.out.println("docType is "+docType);
			/*
			 * �p�GEvent Type�OSave As�A���o�t�s��New Object�A������򪺤��y�z�ܧ�@�~
			 */
			if(eventType==EventConstants.EVENT_SAVE_AS_OBJECT) {
				ISaveAsEventInfo saveAs = (ISaveAsEventInfo) request;
				//���oNew Object Number
				docNumber = saveAs.getNewNumber();
				//���oNew Object 
				doc = (IItem)session.getObject(ItemConstants.CLASS_DOCUMENTS_CLASS, docNumber);
				//����New Object Type
				docType = doc.getCell(ItemConstants.ATT_TITLE_BLOCK_DOCUMENT_TYPE).getValue().toString();
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
			 * Loop�Ҧ�Elements
			 * 		�P�_Ĳ�oEvent������O�_�ŦX���Ӷ���������
			 * 			�p�G���Ӷ������ݩʡB����ȳ��O�Ū��A�������Ӷ������ݩʭȧP�_�A�����M��Description Element�զ����y�z�ALoop����.
			 * 			�p�G���ŦX���Ӷ���������A�h����Description Element�զ����y�z�ALoop����.
			 * Loop����
			 * 		���G�S�����ͤ��y�z�A�ߥXĵ�i�T���i��user�Cĵ�i�T���|�����O���bDescription���C
			 */
		    Iterator it = docDescElements.iterator();
		    while(it.hasNext()) {
		    	Row row = (Row)it.next();
		    	//���o���Ӷ������ݩʡB����ȡBDescription Element
		    	String docCategoryAttrName = ExcelUtility.getSpecificCellValue(row, 1);
		    	String docCategoryAttrValue = ExcelUtility.getSpecificCellValue(row, 2);
		    	String docDescElement = ExcelUtility.getSpecificCellValue(row, 3);
		    	//debug
		    	//System.out.println("docCategoryAttrName is "+docCategoryAttrName);
		    	//System.out.println("docCategoryAttrValue is "+docCategoryAttrValue);
		    	//System.out.println("docDescElement is "+docDescElement);
		    	//�p�G���Ӷ������ݩʡB����ȳ��O�Ū�
		        if(docCategoryAttrName.equals("") && docCategoryAttrValue.equals("")) {
		    		docDesc = generateDocDesc(doc, docDescElement);
		    		break;
		    	}
		        //�p�G���Ӷ������ݩʡB����ȡA����@�ӬO�Ū��A�ߥXĵ�i�T���i��user�Cĵ�i�T���|�����O���bDescription���C
		        else if(!docCategoryAttrName.equals("") && docCategoryAttrValue.equals("")) {
		        	throw new Exception("WARNING - DOC_DESC_ELEMENT_CONFIG Lost!(���Ӷ���������ȿ�!) (Document Type:"+docType+")");
		        }else if(docCategoryAttrName.equals("") && !docCategoryAttrValue.equals("")) {
		        	throw new Exception("WARNING - DOC_DESC_ELEMENT_CONFIG Lost!(���Ӷ������ݩʦW�ٿ�!) (Document Type:"+docType+")");
		        }
		        //�p�G�Ҥ�����
		        else 
		        {
		    		//���oĲ�oEvent�����󪺤��Ӷ������ݩ�
		    		ICell cell = doc.getCell(docCategoryAttrName);
		    		//�p�G��줣�s�b�A�ߥXĵ�i�T���i��user�Cĵ�i�T���|�����O���bDescription���C
		    		if(cell==null) {
		    			throw new Exception("WARNING - Required Document Attribute does not exist!(���Ӷ������ݩʤ��s�b!) (Attribute Name:"+docCategoryAttrName+")");
		    		}
		    		//���o�����ڪ����Ӷ������ݩʭ�
		    		String actualDocCategoryAttrValue = cell.getValue().toString();
		    		//�p�G��ڭ�=����ȡA����Description Element�զ����y�z
		    		if(actualDocCategoryAttrValue.equals(docCategoryAttrValue)) {
		    			docDesc = generateDocDesc(doc, docDescElement);
		    			break;
		    		}
		    	}
		    }
		    
			/*
			 * �p�G�����ͤ��y�z
			 */
			if(!docDesc.equals("")) {
	    		//�P�_��L��󪺤��y�z�O�_����
				String existingDocName = checkExistingDocOfDuplicateDocDesc(session, docNumber, docDesc);
				//�p�G���A�ߥXĵ�i�T���i��user�Cĵ�i�T���|�����O���bDescription���C
				if(!existingDocName.equals("")) {
					throw new Exception("WARNING - Duplicate Document Description! (Document Type:"+docType+", Existing Document:"+existingDocName+")");
				}
				//�ܧ�Ĳ�oEvent�����󪺤��y�z
				doc.setValue(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION, docDesc);
				//���ͦ��\��Action Result
	    		actionResult = new ActionResult(ActionResult.STRING, successLog);
	    	} 
	    	/*
	    	 * �p�G�S�����ͤ��y�z...
	    	 */
			else 
	    	{
				//�ߥXĵ�i�T���i��user�Cĵ�i�T���|�����O���bDescription���C
    			throw new Exception("WARNING - There Is No Valid Document Description Element in EVL-PX-CONFIG!");
	    	}
	    } catch(Exception ex) {
	    	//���ͥ��Ѫ�Action Result
	    	actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
	    	//�Nĵ�i�T�������O���b����Description���C
	    	try {
	    		doc.setValue(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION, ex.getMessage());
	    	} catch(APIException apiEx) {
	    		apiEx.printStackTrace();
	    	}
	    } finally {
			sheet = null;
		}
		//�^�� Event Action Result
 	    return new EventActionResult(request, actionResult);
	}
	
	/**
	 * �ھ�Description Element�զ����y�z.
	 * @param doc Ĳ�oEvent��Document����
	 * @param docDescElement ���y�z�զ��W�h
	 * @return ���y�z
	 * @throws Exception throw when exception is happened
	 */
	public String generateDocDesc(IItem doc, String docDescElement) throws Exception {
		String result = "";
		try {
			//�Τ��y�z���j�r�����Ѥ��y�z�զ��W�h
			String[] elements = docDescElement.split(docDescElementSeparator);
			//���ӥ��ᶶ�ǲզ����y�z
			for(String element : elements) {
				//�p�G�^�ǵ��G���O�Ū��A�N�b�r��᭱�[�@�Ӥ��j�r��
				if(!result.equals("")) {
					result += docDescElementSeparator;
				}
				//�M��l�զ��W�h�r��O�_�����諸���A���Ÿ�([])
				int head = element.indexOf("[");
				int foot = element.indexOf("]");
				//�p�G���A���o[]�����r���@�ݩʦW�١A���oDocument�����ݩʭȡA�A���ݩʭȨ��N[]�����r��C
				if(head!=-1 && foot!=-1) {
					//���o�ݩʦW��
					String attrName = element.substring(head+1, foot);
					//debug
					//System.out.println("#### Attribute Name is "+attrName+" ####");
					//���o�ݩ����
					ICell cell = doc.getCell(attrName);
					//�p�G���y�z�զ����L�k���o�A�ߥXĵ�i�T���i��user�Cĵ�i�T���|�����O���bDescription���C
					if(cell==null) {
						throw new Exception("WARNING - Required Document Attribute does not exist!(Attribute Name:"+attrName+")");
					}
					//���o�ݩʭ�
					String attrValue = cell.getValue().toString();
					//debug
					//System.out.println("#### Attribute Value is "+attrValue+" ####");
					//�p�G���y�z�զ���쪺�ȬO�Ū��A�ߥXĵ�i�T���i��user�Cĵ�i�T���|�����O���bDescription���C
					if(attrValue.equals("")) {
						throw new Exception("WARNING - Required Document Attribute Value is empty!(Attribute Name:"+attrName+")");
					}
					//���p���y�z�r�� (���ݩʭȨ��N[]�����r��)
					result += element.substring(0,head) + attrValue + element.substring(foot+1,element.length());
				}
				//�p�G�S���A�����Τl�զ��W�h�r���@���y�z
				else {
					//���p���y�z�r��
					result += element;
				}
			}
			//�^�ǵ��G
			return result;
		} catch(Exception e) {
			throw e;
		}
	}
	
	/**
	 * 
	 * �P�_��L��󪺤��y�z�O�_���ơC�p�G���A�^�Ǥ��s���C
	 * @param session Agile User Session
	 * @param docNumber Ĳ�oEvent�����s��
	 * @param docDesc �զ������y�z
	 * @return �ۦP���y�z�����s��
	 * @throws Exception throw when exception is happened
	 */
	public String checkExistingDocOfDuplicateDocDesc(IAgileSession session, String docNumber, String docDesc) throws Exception {
		String result = "";
		try {
			//�]�w�d�߱���(�ۦP�y�z�����)
			String condition = 
					"SELECT * " +
	        		"FROM " +
	        		"	[DocumentsClass] " +
	        		"WHERE " +
	        		"	[Title Block.Description] equal to %0 ";
			//�إ߬d�ߪ���
	        IQuery query = (IQuery)session.createObject(IQuery.OBJECT_TYPE, condition);
	        //�]�w�d�߱����
	        query.setParams(new Object[]{docDesc});
	        //����d�߱o�쵲�G
	        ITable results = query.execute();
	        //�����d�ߨ쪺���s��
	        Iterator<IRow> it = results.iterator();
	        while(it.hasNext()) {
	        	//�d�ߨ쪺��󪫥�
	        	IItem item = (IItem)(it.next()).getReferent();
	        	//���o���s��
	        	String itemNumber = item.getName();
	        	//�u�O�����P��"Ĳ�oEvent����󪫥�"�����s��
	        	if(!itemNumber.equals(docNumber)) {
	        		if(!result.equals("")) {
		        		result += ",";
		        	}
	        		//�������s��(�r�����j)
		        	result += item.getName();
	        	}
	        }
	        return result;
		} catch(Exception ex) {
			throw ex;
		}
	}

}

///:~
	