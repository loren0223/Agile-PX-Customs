package com.agree.agile.sdk.px.event;

import java.sql.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;

public class test implements IEventAction 
{
	public EventActionResult doAction(IAgileSession session, INode actionNode, IEventInfo request) 
	{
		//Gen Logger
		PropertyConfigurator.configure("D:/Agile/Agile935/integration/sdk/extensions/log4j.properties");
		//Get Root Logger
		//Logger logger = Logger.getRootLogger();
		
		//Get Custom Logger
		Logger logger = Logger.getLogger(test.class.getName());
		//No appender accumulation
		//logger.setAdditivity(false);
		
		//Variables declaration
		ActionResult actResult = null;
		String changeNumber = "";
		
		try {
			IChange change = (IChange)((IObjectEventInfo)request).getDataObject();
			changeNumber = change.getName();
			logger.info("Change No. is "+changeNumber);
			logger.warn("Hello!");
			if(true) {
				throw new Exception("TEST");
			}
			
			actResult = new ActionResult(ActionResult.STRING, "TEST Successfully.");
			
		} catch(Exception ex) {
			actResult = new ActionResult(ActionResult.EXCEPTION, ex);
			//Send Error Notification
			try {
				logger.error("Error Message is "+ex.getMessage());
				
				//MailUtils.postMail(changeNumber, "", "", ex.getMessage(), "Loren Cheng");
			} catch(Exception mailEx) {
				mailEx.printStackTrace();
			}
		}
		return new EventActionResult(request, actResult);
	}


}
