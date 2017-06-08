package com.agree.agile.sdk.px.custom;

import java.sql.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.agile.api.*;
import com.agile.px.*;
import com.agree.agile.sdk.util.*;

public class test implements ICustomAction 
{
	public ActionResult doAction(IAgileSession session, INode actionNode, IDataObject request) 
	{
		//Variables declaration
		ActionResult actResult = null;
		String changeNumber = "";
		
		try {
			IChange change = (IChange)(request);
			changeNumber = change.getName();
			return actResult = new ActionResult(ActionResult.STRING, changeNumber);
			
		} catch(Exception ex) {
			return actResult = new ActionResult(ActionResult.EXCEPTION, ex);
		}
	}

}
