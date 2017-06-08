package com.agree.agile.sdk.px.event;
import java.sql.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;

/**
 * 
 * @author loren
 *
 */
public class AutoPromote implements IEventAction {
	/**
	 * 
	 */
	public EventActionResult doAction(IAgileSession session, INode actionNode, IEventInfo request) {
		
		PropertyConfigurator.configure("D:/Agile/Agile935/integration/sdk/extensions/log4j.properties"); //取得Log4j設定
		Logger logger = Logger.getLogger(AutoPromote.class.getName()); 	//建立Logger
		//logger.setAdditivity(false); 	//No appender accumulation
		
		ActionResult actionResult = null;
		String changeNumber = "";
		
		try{
			IDataObject object = ((IObjectEventInfo)request).getDataObject();
			if(object.getType() == IProgram.OBJECT_TYPE){
				logger.info("This is IProgram object!");
				IProgram program = (IProgram)object;
				
			}else if(object.getType() == IChange.OBJECT_TYPE){
				logger.info("This is IChange object!");
			}
			
			actionResult = new ActionResult(ActionResult.STRING, "TEST Successfully.");
		}catch(Exception ex){
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
			logger.error("Error Message is "+ex.getMessage());
		}
		return new EventActionResult(request, actionResult);
	}


}
