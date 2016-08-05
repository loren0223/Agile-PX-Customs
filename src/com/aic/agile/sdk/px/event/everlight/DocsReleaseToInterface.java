package com.aic.agile.sdk.px.event.everlight;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.DailyRollingFileAppender;

import java.util.ArrayList;
import java.util.Calendar;

import com.agile.api.APIException;
import com.agile.api.AgileSessionFactory;
import com.agile.api.ChangeConstants;
import com.agile.api.CommonConstants;
import com.agile.api.ExceptionConstants;
import com.agile.api.IAgileList;
import com.agile.api.IAgileSession;
import com.agile.api.IAttachmentFile;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.IStatus;
import com.agile.api.ITable;
import com.agile.api.ITwoWayIterator;
import com.agile.api.IUser;
import com.agile.api.ItemConstants;
import com.agile.api.UserConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.ICustomAction;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.IObjectEventInfo;
import com.aic.agile.sdk.util.UtilConfigReader;
import com.sun.mail.smtp.SMTPAddressFailedException;
import com.sun.mail.smtp.SMTPAddressSucceededException;
import com.sun.mail.smtp.SMTPSendFailedException;

/**
 * PLM to DMP 文件整合介面
 * 
 * 當特定表單發行特定文件時，在工作流程發行後，將表單主檔屬性、文件主檔屬性、文件附件檔案，拋轉到中介資料庫/檔案庫，提供DMP系統作為整合PLM的資料來源。
 * 特定表單的類別有:
 * Data Sheet Release Form
 * Manuf Spec Release
 * Product Document Release
 * Parts Approva
 * Document Obsolete
 * 特定文件的類別有:
 * D01-Data sheet
 * D01-Manufacturing specifications
 * D01-Product Document
 * D01-Approval Sheet
 * 
 * @author Vincent Liao, Loren Cheng
 *
 */
public class DocsReleaseToInterface implements ICustomAction, IEventAction {
	/*
	 * 取得log4j.properties路徑
	 */
	static final String log4jProperties = UtilConfigReader.readProperty("LOG4J_PROPERTIES");
	/*
	 * 宣告全域變數
	 */
	static String changeNo = "";
	static String originator = "";
	static String logFilePath = "";
	static String getFileRuntimeExceptionMsg = "";
	/**
	 * User手動執行程式進入點(Change Order.Action Menu)
	 */
	public ActionResult doAction(IAgileSession session, INode node, IDataObject obj) {
		//Gen Logger
		PropertyConfigurator.configure(log4jProperties);
		//設定日期格式，存成系統參數log4j.date，為了給log檔案名加入時間描述
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
		System.setProperty("log4j.date", dateFormat.format(new Date()));
		//Get Custom Logger
		Logger logger = Logger.getLogger(DocsReleaseToInterface.class.getName());
		logFilePath = ((DailyRollingFileAppender)logger.getAppender("fileAppender")).getFile();
		logger.debug("logFilePath: "+logFilePath);
		//變數宣告
		StringBuffer msgLog = new StringBuffer();
		logger.info("****************************************************");
		logger.info("*************** doAction - START *******************");
		try {
			/*
			 * 取得Change表單
			 */
			IChange change = (IChange) obj;
			changeNo = change.getName();
			originator = change.getCell(ChangeConstants.ATT_COVER_PAGE_ORIGINATOR).getValue().toString();
			logger.debug("Change Number: "+changeNo);
			logger.debug("Change Originator: "+originator);
			/*
			 * 判斷Change Status & Interface Status:
			 * 如果(Change Status == "Release") and (Interface Status == "" or "Fail")，才繼續執行；否則就不執行任何作業。
			 */
			String changeStatus = change.getCell(ChangeConstants.ATT_COVER_PAGE_STATUS).getValue().toString();
			String itfStatus = change.getCell(new Integer(1548))!=null? change.getCell(new Integer(1548)).getValue().toString() : "";
			logger.debug("Change Status: "+changeStatus);
			logger.debug("P3 Interface Status: "+itfStatus);
			if( !(changeStatus.equals("Release") && itfStatus.equals("Fail")) ) {
				throw new Exception("不允許執行文件轉DMP作業!!!請確認表單狀態與Flag狀態是否正確。");
			}
			/*
			 * 執行表單、文件、檔案拋轉動作
			 */
			msgLog = getMetaDataWithAttachments(change, logger);
			/*
			 * 產生Action Result
			 */
			logger.info("*************** doAction - END *********************");
			return new ActionResult(ActionResult.STRING, msgLog.toString());
		} catch(Exception ex) {
			/*
			 * 處理Exception: 針對Item.GetFile()的RuntimeException特別額外處理
			 */
			String exMsg = "";
			if(!this.getFileRuntimeExceptionMsg.equals("")) {
				exMsg = this.getFileRuntimeExceptionMsg;
			} else {
				exMsg = ex.toString();
			}
			logger.error("doAction Exception: "+exMsg);
			/*
			 * Mail通知IT，並夾帶log
			 */
			try {
				postMail(changeNo, originator, logFilePath, exMsg, logger);
				logger.error("Mail Notification Done!");
			} catch(Exception mailEx) {
				logger.error("Mail Notification Fail: "+mailEx.toString());
				mailEx.printStackTrace();
			}
			/*
			 * 產生Action Result
			 */
			return new ActionResult(ActionResult.EXCEPTION, new Exception(exMsg));
		}
	}
	
	/**
	 * 系統事件觸發程式進入點(Change Status for Workflow)
	 */
	public EventActionResult doAction(IAgileSession session, INode node, IEventInfo request) {
		//Gen Logger
		PropertyConfigurator.configure(log4jProperties);
		//設定日期格式，存成系統參數log4j.date，為了給log檔案名加入時間描述
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
		System.setProperty("log4j.date", dateFormat.format(new Date()));
		//Get Custom Logger
		Logger logger = Logger.getLogger(DocsReleaseToInterface.class.getName());
		logFilePath = ((DailyRollingFileAppender)logger.getAppender("fileAppender")).getFile();
		logger.debug("logFilePath: "+logFilePath);
		//宣告變數
		StringBuffer msgLog = new StringBuffer();
		ActionResult actionResult = null;
		logger.info("****************************************************");
		logger.info("*************** doAction - START *******************");
		try {
			/*
			 * 取得Change表單
			 */
			IObjectEventInfo object = (IObjectEventInfo) request;
			IChange change = (IChange) object.getDataObject();
			changeNo = change.getName();
			originator = change.getCell(ChangeConstants.ATT_COVER_PAGE_ORIGINATOR).getValue().toString();
			logger.debug("Change Number: "+changeNo);
			logger.debug("Change Originator: "+originator);
			
			/*
			 * 執行表單、文件、檔案拋轉動作
			 */
			msgLog = getMetaDataWithAttachments(change, logger);
			/*
			 * 產生Action Result
			 */
			actionResult = new ActionResult(ActionResult.STRING, msgLog.toString());
		} catch(Exception ex) {
			/*
			 * 處理Exception: 針對Item.GetFile()的RuntimeException特別額外處理
			 */
			String exMsg = "";
			if(!this.getFileRuntimeExceptionMsg.equals("")) {
				exMsg = this.getFileRuntimeExceptionMsg;
			} else {
				exMsg = ex.toString();
			}
			logger.error("doAction Exception: "+exMsg);
			/*
			 * Mail通知IT，並夾帶log
			 */
			try {
				postMail(changeNo, originator, logFilePath, exMsg, logger);
				logger.error("Mail Notification Done!");
			} catch(Exception mailEx) {
				logger.error("Mail Notification Fail: "+mailEx.toString());
				mailEx.printStackTrace();
			}
			/*
			 * 產生Action Result
			 */
			actionResult = new ActionResult(ActionResult.EXCEPTION, new Exception(exMsg));
		}
		logger.info("*************** doAction - END *********************");
		return new EventActionResult(request, actionResult);
	}
	
	/**
	 * 
	 * @param session
	 * @param change
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	public static StringBuffer getMetaDataWithAttachments(IChange change, Logger logger) throws Exception {	
		//logger.info("Method:getMetaDataWithAttachments - START");
		StringBuffer msgLog = new StringBuffer();
		ArrayList<String> batchSQLStatement = new ArrayList<String>();
		ArrayList<IItem> itemList = new ArrayList<IItem>();
		String localFileFolderPath = UtilConfigReader.readProperty("LOCAL_FILE_FOLDER_PATH");
		/*
		 * 取得中介資料庫連線(AutoCommit is false)
		 */
		Connection conn = getDBConnection(logger);
		conn.setAutoCommit(false);
		/*
		 * 宣告變數: History Table (Change申請單)
		 */
		String formNumber = "";
		String formDesc = "";
		String formType = "";
		String formCreationDate = "";
		String formReleaseDate = "";
		String currentTime = "";
		String flag = "R";
		String systemReply = "";
		String originator = "";
		String formPlant = "";
		/*
		 * 宣告變數: ExchangeDoc Table (所有文件共同欄位)
		 */
		String tbDocNo = "";
		String tbDocType = "";
		String tbLifecyclePhase = "";
		String tbDocDesc = "";
		String tbDocRev = "";
		String tbDocReleaseDate = "";
		String p2CreateUser = "";
		String p2Applicant = "";
		String p2OldDocNo = "";
		String p3ProdCategory = "";
		String p3DocLevel = "";
		String p3Factory = "";
		String p3ConfLevel = "";
		/*
		 * 宣告變數: ExchangeDoc Table (only for D01-Manufacturing specifications)
		 */
		String p3ProdType = "";
		/*
		 * 宣告變數: ExchangeDoc Table (only for D01-Product Document)
		 */
		String p3SOPDocNo = "";
		String p3prodTypeWtMaterialName = "";
		String p3Station = "";
		String p3ProdDocType = "";
		String p3Customer = "";
		/*
		 * 宣告變數: ExchangeDoc Table (only for D01-Approval Sheet)
		 */
		String p3MaterialOption = "";
		/*
		 * 宣告變數: ExchangeDoc Table (only for D01-PCN Document)
		 */
		String p3BU = "";
		String p3ProdDevBU = "";
		String p3LastBuyDate = "";
		String p3ProposedFirstShipDate = "";
		String p3QualificationSamplesDate = "";
		/*
		 * 宣告其他變數
		 */
		String filePath = "";
		String dmpFilePath = "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");	// modify by 2016/05/10
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));	// modify by 2016/05/24
		String sqlForHistory = "";
		String sqlForExchangeDoc = "";
		boolean hasValidAI = false;
		
		try {
			/*
			 * 取得申請表單屬性
			 */
			formNumber = change.getCell(new Integer(1047)).getValue().toString();
			formDesc = change.getCell(new Integer(1052)).getValue().toString();
			formType = change.getCell(new Integer(1069)).getValue().toString();
			formCreationDate = sdf.format((Date) change.getCell(new Integer(1061)).getValue());
			formReleaseDate = sdf.format((Date) change.getCell(new Integer(1051)).getValue());
			currentTime = sdf.format(Calendar.getInstance().getTime());
			originator = ((IUser)change.getCell(new Integer(1050)).getReferent()).getCell(UserConstants.ATT_GENERAL_INFO_USER_ID).getValue().toString();
			logger.info("Form Number: " + formNumber);
			logger.info("Form Description: "+ formDesc);
			logger.info("Form Type: " + formType);
			logger.info("Form Creation Date: " + formCreationDate);
			logger.info("Form Release Date: " + formReleaseDate);
			logger.info("Current Time: " + currentTime);
			logger.info("Originator: " + originator);
			//取得表單類別的API Name
			String changeAPIName = change.getAgileClass().getAPIName();
			/*
			 * Change Type : API Name
			 * --------------------------------------------
			 * Data Sheet Release Form : DataSheetReleaseFormDataSheet
			 * Manuf Spec Release : ManufSpecRelease
			 * Product Document Release : ProductDocumentRelease
			 * Parts Approval : PartsApproval
			 * Document Obsolete : DocumentObsolete
			 */
			/*
			 * 取得表單申請者廠區
			 */
			if(changeAPIName.equals("DataSheetReleaseFormDataSheet") || changeAPIName.equals("ManufSpecRelease") 
					|| changeAPIName.equals("ProductDocumentRelease")) {
				formPlant = change.getCell(new Integer(1542)).getValue().toString();
			} else if(changeAPIName.equals("PartsApproval")) {
				formPlant = change.getCell(new Integer(1545)).getValue().toString();
			} else if(changeAPIName.equals("DocumentObsolete")) {
				formPlant = change.getCell(new Integer(1540)).getValue().toString();
			}
			logger.info("Form Plant: " + formPlant);
			/*
			 * 宣告Insert into History的SQL
			 */
			sqlForHistory = " INSERT INTO HISTORY ( "+
							" 	form_number, form_description, form_type, form_create_date, "+
							" 	form_released_date, creation_date, doc_system_flag, doc_system_reply, "+ 
							" 	form_originator, form_plant "+
							" )VALUES( "+
							" 	'"+formNumber+"', N'"+formDesc+"', N'"+formType+"', '"+formCreationDate+"', "+
							" 	'"+formReleaseDate+"', '"+currentTime+"', '"+flag+"', '"+systemReply+"', "+
							" 	N'"+originator+"', N'"+formPlant+"')";
			batchSQLStatement.add(sqlForHistory);
			/************************************************************************/
			/************************************************************************/
			/*
			 * 處理表單所有Affected Item(AI)
			 */
			ITable table = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator it = table.iterator();
			int i = 0;
			//如果AI有需要處裡的合法類別就設為true
			while(it.hasNext()) {
				i++;
				IRow row = (IRow)it.next();
				/*
				 * 取得Item Released Revision
				 */
				String newRev = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV).getValue().toString();
				/*
				 * Doc Type : API Name
				 * --------------------------------------------
				 * D01-Data sheet : D01DataSheet
				 * D01-Manufacturing specifications : D01ManufacturingSpecifications
				 * D01-Product Document : D01ProductDocument
				 * D01-Approval Sheet : D01ApprovalSheet
				 * D01-PCN Document : ???
				 */
				IItem item = (IItem) row.getReferent();
				item.setRevision(newRev);
				/*
				 * 識別Item是否有附件
				 */
				String itemNo = item.getName();
				String itemType = item.getAgileClass().getName();	
				String itemAPIName = item.getAgileClass().getAPIName();
				logger.info("#"+i+" Affected Item: "+itemNo);
				logger.info("\t Item Type: "+itemType);
				logger.debug("\t Item Type API Name: "+itemAPIName);
				logger.info("\t New Rev: "+newRev);
				/*
				 * 只有Item Type是上述類別時，才處理AI
				 */
				if(itemAPIName.equals("D01DataSheet") || itemAPIName.equals("D01ManufacturingSpecifications") 
						|| itemAPIName.equals("D01ProductDocument") || itemAPIName.equals("D01ApprovalSheet")) {
					/*
					 * 紀錄要處理的Item
					 */
					hasValidAI = true;
					itemList.add(item);
					/*
					 * 取得AI屬性值
					 */
					if(itemAPIName.equals("D01DataSheet")) {
						tbDocNo = getItemAttrValue(item, new Integer(1001));
						tbDocType = getItemAttrValue(item, new Integer(1081));
						tbLifecyclePhase = getItemAttrValue(item, new Integer(1084));
						tbDocDesc = getItemAttrValue(item, new Integer(1002));
						tbDocRev = newRev;
						tbDocReleaseDate = sdf.format((Date) item.getCell(new Integer(1016)).getValue());
						p2CreateUser = ((IUser)item.getCell(new Integer(1420)).getReferent()).getCell(UserConstants.ATT_GENERAL_INFO_USER_ID).getValue().toString();
						p2Applicant = getItemAttrValue(item, new Integer(2010));
						p2OldDocNo = getItemAttrValue(item, new Integer(2008));
						p3ProdCategory = getItemAttrValue(item, new Integer(1540));
						p3DocLevel = getItemAttrValue(item, new Integer(1542));
						p3Factory = getItemAttrValue(item, new Integer(1545));	
						p3ConfLevel = getItemAttrValue(item, new Integer(1543));
						dmpFilePath = "技術規範(Technical Specifications)/規格書(Datasheet)";	
					} else if(itemAPIName.equals("D01ManufacturingSpecifications")) {
						tbDocNo = getItemAttrValue(item, new Integer(1001));
						tbDocType = getItemAttrValue(item, new Integer(1081));
						tbLifecyclePhase = getItemAttrValue(item, new Integer(1084));
						tbDocDesc = getItemAttrValue(item, new Integer(1002));
						tbDocRev = newRev;
						tbDocReleaseDate = sdf.format((Date) item.getCell(new Integer(1016)).getValue());
						p2CreateUser = ((IUser)item.getCell(new Integer(1420)).getReferent()).getCell(UserConstants.ATT_GENERAL_INFO_USER_ID).getValue().toString();
						p2Applicant = getItemAttrValue(item, new Integer(2010));
						p2OldDocNo = getItemAttrValue(item, new Integer(2008));
						p3ProdCategory = getItemAttrValue(item, new Integer(1540));
						p3ProdType = getItemAttrValue(item, new Integer(1541));
						p3DocLevel = getItemAttrValue(item, new Integer(1542));
						p3Factory = getItemAttrValue(item, new Integer(1545));
						p3ConfLevel = getItemAttrValue(item, new Integer(1543));
						dmpFilePath = "技術規範(Technical Specifications)/製造規格(Manufacturing specifications)";
					} else if(itemAPIName.equals("D01ProductDocument")) {
						tbDocNo = getItemAttrValue(item, new Integer(1001));
						tbDocType = getItemAttrValue(item, new Integer(1081));
						tbLifecyclePhase = getItemAttrValue(item, new Integer(1084));
						tbDocDesc = getItemAttrValue(item, new Integer(1002));
						tbDocRev = newRev;
						tbDocReleaseDate = sdf.format((Date) item.getCell(new Integer(1016)).getValue());
						p2CreateUser = ((IUser)item.getCell(new Integer(1420)).getReferent()).getCell(UserConstants.ATT_GENERAL_INFO_USER_ID).getValue().toString();
						p2Applicant = getItemAttrValue(item, new Integer(2010));
						p2OldDocNo = getItemAttrValue(item, new Integer(2008));
						p3SOPDocNo = getItemAttrValue(item, new Integer(1578));	
						p3ProdCategory = getItemAttrValue(item, new Integer(1540));
						p3DocLevel = getItemAttrValue(item, new Integer(1542));
						//P3.Plant
						p3Factory = getItemAttrValue(item, new Integer(1545));
						p3ConfLevel = getItemAttrValue(item, new Integer(1543));
						p3prodTypeWtMaterialName = getItemAttrValue(item, new Integer(1575));
						p3Station = getItemAttrValue(item, new Integer(1546));
						p3Customer = getItemAttrValue(item, new Integer(1576));
						dmpFilePath = "技術規範(Technical Specifications)/產品文件(Product Document)";
						/*
						 * Product Document.Product Document Type 需要的是Selected List Option's Description，
						 * 不是Selected List Option!
						 */
						//p3ProdDocType = getItemAttrValue(item, new Integer(1547));
						IAgileList listOption = (IAgileList)item.getCell(new Integer(1547)).getValue();
						IAgileList[] selected = listOption.getSelection();
						if(selected != null && selected.length > 0) {
							p3ProdDocType = selected[0].getDescription();
						} else {
							p3ProdDocType = "";
						}
						/*
						 * 
						 */
					} else if(itemAPIName.equals("D01ApprovalSheet")) {
						tbDocNo = getItemAttrValue(item, new Integer(1001));
						tbDocType = getItemAttrValue(item, new Integer(1081));
						tbLifecyclePhase = getItemAttrValue(item, new Integer(1084));
						tbDocDesc = getItemAttrValue(item, new Integer(1002));
						tbDocRev = newRev;
						tbDocReleaseDate = sdf.format((Date) item.getCell(new Integer(1016)).getValue());
						
						p2CreateUser = ((IUser)item.getCell(new Integer(1420)).getReferent()).getCell(UserConstants.ATT_GENERAL_INFO_USER_ID).getValue().toString();
						p2Applicant = getItemAttrValue(item, new Integer(2010));
						p2OldDocNo = getItemAttrValue(item, new Integer(2008));
						
						p3MaterialOption = getItemAttrValue(item, new Integer(1542));
						p3DocLevel = getItemAttrValue(item, new Integer(1544));
						p3Factory = getItemAttrValue(item, new Integer(1545)); 
						p3ConfLevel = getItemAttrValue(item, new Integer(1543));	
						dmpFilePath = "技術規範(Technical Specifications)/核准書(Approval Sheet)";
					}
					/*
					 * 特別處理:如果Create User是'admin'開頭, 覆寫成 'dccadmin'
					 */
					p2CreateUser = p2CreateUser.startsWith("admin")? "dccadmin" : p2CreateUser;
					/*
					 * 設定Item.Attachment的傳送路徑
					 */
					filePath = localFileFolderPath+itemNo+"_"+newRev;
					/*
					 * 宣告Insert into ExchangeDocs 的 SQL
					 */
					sqlForExchangeDoc = " INSERT INTO EXCHANGEDOCS ( "+
										"   form_number, tb_number, tb_document_type, tb_lifecycle_phase, "+
										"   tb_description, tb_rev, tb_rev_released_date, p2_create_user, "+
										"   p2_applicant, p2_old_doc_number, p3_product_category, p3_document_level, "+
										"   p3_factory, p3_confidential_level, p3_product_type, p3_sop_doc_number, "+
										"   p3_product_type_material_name, p3_station, p3_product_doc_type, p3_product_customer, "+
										"   p3_material_option, p3_bu, p3_product_dev_bu, p3_last_buy_date, "+
										"   p3_proposed_first_ship_date, p3_qualification_samples_date, attachment_files_path, dmp_files_path "+
										" )VALUES( "+
										"   '"+formNumber+"', '"+tbDocNo+"', '"+tbDocType+"', N'"+tbLifecyclePhase+"', "+ 
										"   N'"+tbDocDesc+"', '"+tbDocRev+"', '"+tbDocReleaseDate+"', N'"+p2CreateUser+"', "+ 
										"   N'"+p2Applicant+"', N'"+p2OldDocNo+"', N'"+p3ProdCategory+"', N'"+p3DocLevel+"', "+
										"   N'"+p3Factory+"', N'"+p3ConfLevel+"', N'"+p3ProdType+"', N'"+p3SOPDocNo+"', "+ 
										"   N'"+p3prodTypeWtMaterialName+"', N'"+p3Station+"', N'"+p3ProdDocType+"', N'"+p3Customer+"', "+
										"   N'"+p3MaterialOption+"', N'"+p3BU+"', N'"+p3ProdDevBU+"', N'"+p3LastBuyDate+"', "+
										"   N'"+p3ProposedFirstShipDate+"', N'"+p3QualificationSamplesDate+"', N'"+filePath+"', N'"+dmpFilePath+"' "+
										" )";
					batchSQLStatement.add(sqlForExchangeDoc);
					/*
					 * Log for ActionResult
					 */
					msgLog.append("表單編號: "+formNumber+", "+"Affected-Item: "+itemNo+", 文件發行至中介資料表格成功. \n" );
					/*
					 * Log AI 屬性值
					 */
					logger.info("\t Doc Number: " + tbDocNo);
					logger.info("\t Doc Type: " + tbDocType);
					logger.info("\t Lifecycle Phase: " + tbLifecyclePhase);
					logger.info("\t Description: " + tbDocDesc);
					logger.info("\t Revision: " + tbDocRev);
					logger.info("\t Release Date: " + tbDocReleaseDate);
					logger.info("\t Create User: " + p2CreateUser);
					logger.info("\t Applicant: " + p2Applicant);
					logger.info("\t Old Doc Number: " + p2OldDocNo);
					logger.info("\t Product Category: "+ p3ProdCategory);
					logger.info("\t Document Level: " + p3DocLevel);
					logger.info("\t Factory: " + p3Factory);
					logger.info("\t Confidential Level: " + p3ConfLevel);
					logger.info("\t ProductType: "+ p3ProdType);
					logger.info("\t SOP Doc Number: " + p3SOPDocNo);
					logger.info("\t Product Type/Material Name: " + p3prodTypeWtMaterialName);
					logger.info("\t Station: " + p3Station);
					logger.info("\t Product Doc Type: "+ p3ProdDocType);
					logger.info("\t Customer: " + p3Customer);
					logger.info("\t Material Option: " + p3MaterialOption);
					logger.info("\t BU: " + p3BU);
					logger.info("\t Product Dev BU: "+ p3ProdDevBU);
					logger.info("\t Last Buy Date: " + p3LastBuyDate);
					logger.info("\t Proposed First Ship Date: " + p3ProposedFirstShipDate);
					logger.info("\t Qualification Samples Date: " + p3QualificationSamplesDate);
					logger.info("\t File Path: "+ filePath);
					logger.info("\t DMP File Path: "+ dmpFilePath);
				} else {
					/*
					 * Log for ActionResult
					 */
					msgLog.append("表單編號: "+formNumber+", "+"Affected-Item: "+itemNo+", "+"Item-Type: "+itemType+", 非發行類型故不拋轉. \n" );
				}
			}	
			/*
			 * 確認有合法的AI才寫入中介表格
			 */
			if(hasValidAI) {
				/*
				 * 輸出Affected Item.Attachments到中介檔案庫
				 */
				logger.info("Method:copyAttachmentFilesToLocalFolder - START");
				for(IItem item : itemList) {
					//String itemNo = item.getName();
					copyAttachmentFilesToLocalFolder(item, logger);
				}
				logger.info("Method:copyAttachmentFilesToLocalFolder - END");
				/*
				 * 執行寫入History/ExchangeDocs Table(不commit)
				 */
				logger.info("Method:insertTableToInterface - START");
				for(String sql : batchSQLStatement) {
					insertInterfaceTable(sql, conn, logger);
				}
				/*
				 * 寫入無誤Commit
				 */
				conn.commit();
				logger.info("Method:insertTableToInterface - END");
				/*
				 * 紀錄交易成功flag
				 */
				change.getCell(new Integer(1548)).setValue("Success");
				logger.info("Transaction Result: Success");
			} else {
				/*
				 * 紀錄交易成功flag
				 */
				change.getCell(new Integer(1548)).setValue("No Result");
				logger.info("Transaction Result: No Result");
			}
		} catch(Exception ex) {
			/*
			 * Database Rollback
			 */
			conn.rollback();
			/*
			 * 紀錄交易失敗flag(P3.interfaceStatus)
			 */
			change.getCell(new Integer(1548)).setValue("Fail");
			logger.error("Transaction Result: Fail");
			throw ex;
		} finally {
			/*
			 * Close DB Connection
			 */
			if(conn!=null) {
				conn.close();
			}
			logger.info("Close DB Connection.");
		}
		//logger.info("Method:getMetaDataWithAttachments - END");
		return msgLog;
	}
	
	// getting an attachment for an item
	/**
	 * 
	 * @param item
	 * @param logger
	 * @return
	 * @throws Exception
	 */
	public static void copyAttachmentFilesToLocalFolder(IItem item, Logger logger) throws Exception {
		InputStream ins = null;
	    FileOutputStream fos = null;
	    String localFileFolderPath = UtilConfigReader.readProperty("LOCAL_FILE_FOLDER_PATH");
		String fileNameFilterPattern = UtilConfigReader.readProperty("FILE_NAME_FILTER_PATTERN");
		logger.debug("localFileFolderPath: "+localFileFolderPath);
		logger.debug("fileNameFilterPattern: "+fileNameFilterPattern);
		try {
			String itemNo = item.getName();
			String itemRev = item.getRevision();
			logger.info("Affected Item: "+itemNo+"("+itemRev+")");
			/*
			 * 取得Item.Attachments
			 */
			ITable table = item.getAttachments();
			ITwoWayIterator it = table.getTableIterator();
			int i = 0;
			while(it.hasNext()) {
				i++;
				IRow row = (IRow) it.next();
				/*
				 * 取得檔案名
				 */
				String fileName = row.getValue(ItemConstants.ATT_ATTACHMENTS_FILE_NAME).toString();
				logger.info("\t #"+i+" File Name: "+fileName);
				/*
				 * 如果檔案副檔名格式同下列規則的話就不複製到中介檔案庫:(不分大小寫)
				 * .DOC.PDF
				 * .DOCX.PDF
				 * .XLS.PDF 
				 * .XLSX.PDF
				 * .PPT.PDF 
				 * .PPTX.PDF
				 */
				boolean skipIt = fileName.toLowerCase().matches(fileNameFilterPattern);
				logger.debug("skipIt? "+skipIt);
				if(skipIt) {
					logger.info("\t --> 這是PLM自動轉檔產生的pdf，不拋到DMP.");
				} else {
				
					logger.debug("\t Pass File Name Filter.");
					/*
					 * 取得檔案(輸入串流)
					 */
					try {
						ins = ((IAttachmentFile)row).getFile();
					} catch(RuntimeException getFileEx) {
						/*
						 * 嚴重錯誤!!!! File could not be located!
						 */
						logger.fatal(getFileEx.toString());
						/*
						 * 記錄在全域變數
						 */
						getFileRuntimeExceptionMsg = getFileEx.toString();
						throw getFileEx;
					}
					logger.debug("\t Get File Input Stream.");
					
					/*
					 * 指定檔案輸出路徑: \\dmp_temp_folder\[ItemNo_ItemRev]
					 */
					File file = new File(localFileFolderPath+itemNo+"_"+itemRev);
					String filePath = file.getPath();
					logger.debug("\t Set File Path.");
					/*
					 * 如果輸出路徑不存在就建立目錄
					 */
					if(!file.exists()) {
						logger.info("\t Create file directory: "+filePath);
						file.mkdir();
						logger.info("\t Create file directory completely");
					}
					logger.debug("\t Pass File Folder Check.");
					/*
					 * 輸出檔案到中介檔案庫
					 */
					fos = new FileOutputStream(filePath+"\\"+fileName);
					logger.debug("\t File Output Stream Ready.");
					byte[] b = new byte[2048];
					int off = 0;
					int len = 0;
					while((len = ins.read(b)) != -1) {
						fos.write(b, off, len);
					}
					logger.debug("\t Copy File Done.");
				}
			}
		} catch(Exception ex) {
			throw ex;
		} finally {
			if(ins!=null) {
				ins.close();
			}
			if(fos!=null) {
				fos.close();
			}
		}
	}
	
	// insert table to interface database
	/**
	 * 
	 * @param sql
	 * @param conn
	 * @param logger
	 * @throws Exception
	 */
	private static void insertInterfaceTable(String sql, Connection conn, Logger logger) throws Exception {
		
		Statement state = null;
		try {
			state = conn.createStatement();
			logger.debug("SQL Statement: "+sql);
			state.executeUpdate(sql);
		} catch(Exception ex) {
			throw ex;
		} finally {
			//關閉statement
			if(state != null) {
				state.close();
			}
		}
	}
	
	// connect Microsoft SQL Server database
	/**
	 * 
	 * @return
	 */
	public static Connection getDBConnection(Logger logger) throws Exception {
		logger.info("Method:getDBConnection - START");
		String driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		String connURL = UtilConfigReader.readProperty("SQL_SERVER_URL");
		String user = UtilConfigReader.readProperty("SQL_SERVER_USER");
		String password = UtilConfigReader.readProperty("SQL_SERVER_PASSWORD");
		Connection conn = null;
		logger.info("\t SQL_SERVER_URL: "+connURL);
		logger.info("\t SQL_SERVER_USER: "+user);
		logger.info("\t SQL_SERVER_PASSWORD: "+password);
		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(connURL, user, password);
			logger.info("\t Connect to SQL Server with JDBC Driver successfully!");
		} catch(Exception ex) {
			throw ex;
		}
		logger.info("Method:getDBConnection - END");
		return conn;
	}
	
	
	/**
	 * 
	 * @param changeNumber
	 * @param originator
	 * @param logPath
	 * @param errMsg
	 * @throws Exception
	 */
	public static void postMail(String changeNumber, String originator, String logPath, String errMsg, Logger logger) throws Exception
	{
		try {
			/*
			 * 宣告變數
			 */
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date systemDate = new Date();
			String currentDatetime = dateFormat.format(systemDate);
			String apIndex = UtilConfigReader.readProperty("AP_SERVER_INDEX");
			/*
			 * 取得MAIL設定
			 */
			String[] recipients = UtilConfigReader.readProperty("MAIL_RECIPIENTS").split(",");
			String from = UtilConfigReader.readProperty("MAIL_FROM");
			String smtp = UtilConfigReader.readProperty("MAIL_SMTP");
			String subject = UtilConfigReader.readProperty("MAIL_SUBJECT");
			String content = UtilConfigReader.readProperty("MAIL_CONTENT");
			/*
			 * 產生MAIL主旨與內容
			 */
		    subject = subject.replaceAll("\\$[C][H][A][N][G][E]", changeNumber);
		    subject = subject.replaceAll("\\$[O][R][I][G][I][N][A][T][O][R]", originator);
		    subject = subject.replaceAll("\\$[E][R][R][O][R]", errMsg);
		    subject = subject.replaceAll("\\$[A][P]", apIndex);
		    
		    content = content.replaceAll("\\$[C][H][A][N][G][E]", changeNumber);
		    content = content.replaceAll("\\$[O][R][I][G][I][N][A][T][O][R]", originator);
		    content = content.replaceAll("\\$[E][R][R][O][R]", errMsg);
		    content = content.replaceAll("\\$[A][P]", apIndex);
		    
		    /*
		     * 建立郵件
		     */
			// Set the host smtp address
		    Properties props = new Properties();
		    props.put("mail.smtp.host", smtp);
		    // create some properties and get the default Session
		    boolean debug = false;
		    Session session = Session.getDefaultInstance(props, null);
		    session.setDebug(debug);
		    // create a message
		    Message msg = new MimeMessage(session);
		    // set the from and to address
		    InternetAddress addressFrom = new InternetAddress(from);
		    msg.setFrom(addressFrom);
		    InternetAddress[] addressTo = new InternetAddress[recipients.length]; 
		    for (int i = 0; i < recipients.length; i++) {
		        addressTo[i] = new InternetAddress(recipients[i].trim());
		    }
		    msg.setRecipients(Message.RecipientType.TO, addressTo);
		    // create and fill the content and log file
		    MimeBodyPart mbp1 = new MimeBodyPart();
		    MimeBodyPart mbp2 = new MimeBodyPart();
		    mbp1.setText(content);
		    mbp2.attachFile(logPath);
		    // create the Multipart and add its parts to it
		    Multipart mp = new MimeMultipart();
		    mp.addBodyPart(mbp1);
		    mp.addBodyPart(mbp2);
		    // Setting the Subject and Content Type
		    msg.setSubject(subject);
		    msg.setContent(mp);
		    msg.setSentDate(systemDate);
		    Transport.send(msg);
		} catch(Exception e) {
			/*
		     * Handle SMTP-specific exceptions.
		     */
		    if (e instanceof SendFailedException) {
		    	MessagingException sfe = (MessagingException)e;
		    	if (sfe instanceof SMTPSendFailedException) {
		    		SMTPSendFailedException ssfe = (SMTPSendFailedException)sfe;
		    		logger.error("SMTP SEND FAILED:");
		    		logger.error(ssfe.toString());
		    		logger.error("  Command: " + ssfe.getCommand());
		    		logger.error("  RetCode: " + ssfe.getReturnCode());
		    		logger.error("  Response: " + ssfe.getMessage());
		    	} else {
		    		logger.error("Send failed: " + sfe.toString());
		    	}
		    	Exception ne;
		    	while ((ne = sfe.getNextException()) != null && ne instanceof MessagingException) {
		    		sfe = (MessagingException)ne;
		    		if (sfe instanceof SMTPAddressFailedException) {
		    			SMTPAddressFailedException ssfe = (SMTPAddressFailedException)sfe;
		    			logger.error("ADDRESS FAILED:");
		    			logger.error(ssfe.toString());
		    			logger.error("  Address: " + ssfe.getAddress());
		    			logger.error("  Command: " + ssfe.getCommand());
		    			logger.error("  RetCode: " + ssfe.getReturnCode());
		    			logger.error("  Response: " + ssfe.getMessage());
		    		} else if (sfe instanceof SMTPAddressSucceededException) {
		    			logger.error("ADDRESS SUCCEEDED:");
		    			SMTPAddressSucceededException ssfe = (SMTPAddressSucceededException)sfe;
		    			logger.error(ssfe.toString());
		    			logger.error("  Address: " + ssfe.getAddress());
		    			logger.error("  Command: " + ssfe.getCommand());
		    			logger.error("  RetCode: " + ssfe.getReturnCode());
		    			logger.error("  Response: " + ssfe.getMessage());
		    		}
		    	}
		    } else {
		    	logger.error("Got Exception: " + e);
		    }
		    //Finally
		    throw e;
		}
	}
	
	/**
	 * 
	 * @param item
	 * @param baseID
	 * @return
	 * @throws Exception
	 */
	public static String getItemAttrValue(IItem item, int baseID) throws Exception {
		String attrValue = "";
		try {
			ICell cell = item.getCell(baseID);
			attrValue = cell.getValue()==null? "" : cell.getValue().toString();
		} catch(Exception ex) {
			throw ex;
		}
		return attrValue;
	}
	
	public static void main(String[] args) {
		//Gen Logger
		PropertyConfigurator.configure(log4jProperties);
		//Get Custom Logger
		Logger logger = Logger.getLogger(DocsReleaseToInterface.class.getName());
		logFilePath = ((RollingFileAppender)logger.getAppender("fileAppender")).getFile();
		logger.debug("logFilePath: "+logFilePath);
		//宣告變數
		StringBuffer msgLog = new StringBuffer();
		ActionResult actionResult = null;
		logger.info("****************************************************");
		logger.info("*************** doAction - START *******************");
		
		try {
			AgileSessionFactory factory = AgileSessionFactory.refreshInstance("http://plm-aptest.everlight.com:7001/Agile");
			HashMap params = new HashMap();
			params.put(AgileSessionFactory.USERNAME, "admin01");
			params.put(AgileSessionFactory.PASSWORD, "agile");
			IAgileSession session = factory.createSession(params);
			/*
			 * 取得Change表單
			 */
			IChange change = (IChange) session.getObject(ChangeConstants.CLASS_CHANGE_ORDERS_CLASS, "DOC000200002");
			changeNo = change.getName();
			originator = change.getCell(ChangeConstants.ATT_COVER_PAGE_ORIGINATOR).getValue().toString();
			logger.debug("Change Number: "+changeNo);
			logger.debug("Change Originator: "+originator);
			
			/*
			 * 執行表單、文件、檔案拋轉動作
			 */
			msgLog = getMetaDataWithAttachments(change, logger);
			/*
			 * 產生Action Result
			 */
			actionResult = new ActionResult(ActionResult.STRING, msgLog.toString());
		} catch(Exception ex) {
			/*
			 * 處理Exception: 針對Item.GetFile()的RuntimeException特別額外處理
			 */
			String exMsg = "";
			if(!getFileRuntimeExceptionMsg.equals("")) {
				exMsg = getFileRuntimeExceptionMsg;
			} else {
				exMsg = ex.toString();
			}
			logger.error("doAction Exception: "+exMsg);
			/*
			 * Mail通知IT，並夾帶log
			 */
			try {
				postMail(changeNo, originator, logFilePath, exMsg, logger);
				logger.error("Mail Notification Done!");
			} catch(Exception mailEx) {
				logger.error("Mail Notification Fail: "+mailEx.toString());
				mailEx.printStackTrace();
			}
			/*
			 * 產生Action Result
			 */
			actionResult = new ActionResult(ActionResult.EXCEPTION, new Exception(exMsg));
		}
		logger.info("*************** doAction - END *********************");
	
	}

}