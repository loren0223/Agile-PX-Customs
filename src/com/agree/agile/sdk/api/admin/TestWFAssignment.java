package com.agree.agile.sdk.api.admin;

import java.io.*;
import java.util.*;
import com.agile.api.*;
import com.agree.agile.sdk.util.*;

public class TestWFAssignment {
	
	public static IAgileSession session = null;
	public static AgileSessionFactory factory;
	//For outputting text file
	public static StringBuffer sb = new StringBuffer();
	
	public static void main(String[] args) {
		try {
			session = AgileSessionUtility.getAdminSessionCluster();
			//IChange change = (IChange)session.getObject(ChangeConstants.CLASS_CHANGE_ORDERS_CLASS, "DOC-081552");
			IChange change = (IChange)session.getObject(ChangeConstants.CLASS_CHANGE_ORDERS_CLASS, "C00002");
			
			String specificWF = "Default Change Orders";
			System.out.println("specificWF: "+specificWF);
			
			IWorkflow[] wfs = change.getWorkflows(); 
			IWorkflow workflow = null; 
			for (int i=0; i<wfs.length; i++) { 
				String wfName = wfs[i].getName();
				System.out.println("Change WF["+i+"] Name: "+wfName);
				if (wfName.equals(specificWF)) {
					workflow = wfs[i];
					break;
				}
			} 
			change.setWorkflow(workflow); 
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
