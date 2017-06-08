package com.agree.agile.sdk.api.admin;

import java.io.*;
import java.util.*;
import com.agile.api.*;
import com.agree.agile.sdk.util.*;

public class Test {
	
	public static IAgileSession session = null;
	public static AgileSessionFactory factory;
	//For outputting text file
	public static StringBuffer sb = new StringBuffer();
	
	public static void main(String[] args) {
		try {
			
			session = AgileSessionUtility.getAdminSession();
			
			/*
			IAdmin admin = session.getAdminInstance();
			
			//INode agileWorkflowsNode = admin.getNode(3641); //NODETABLE.ID=3641
			//System.out.println("***Get Workflows Node***");
			//printNodeProperties(agileWorkflows);
			//System.out.println();
		
			//System.out.println("***Get Workflow Node***");
			//String tab = "";
			//exploreNode(agileWorkflowsNode, tab);
			
			INode workflow = AgileNodeUtility.getSpecificWorkflowNode(admin, "ECR");
			System.out.println("workflow name is "+workflow.getProperty(PropertyConstants.PROP_NAME).getValue().toString());
			INode statusList = AgileNodeUtility.getWFStatusListNode(admin, "ECR");
			
			//Output text file
			//outputTextFile();
			*/
			
			IChange change = (IChange)session.getObject(IChange.OBJECT_TYPE, "PNR-00000039");
			ITable history = change.getTable(ChangeConstants.TABLE_WORKFLOW);
			Iterator it = history.iterator();
			boolean firstRow = true;
			while(it.hasNext()){
				IRow row = (IRow)it.next();
				ICell[] cells = row.getCells();
				
				if(firstRow){
					for(ICell cell : cells){
						System.out.print(cell.getName());
						System.out.print("\t");
					}
					System.out.println();
					firstRow = false;
				}
			
				for(ICell cell : cells){
					if(firstRow){
						System.out.print(cell.getName());
						System.out.print("\t");
					}
					
					System.out.print(cell.getValue());
					System.out.print("\t");
				}
				System.out.println();
			}
			
			
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static void printNodeProperties(INode node, String tab) throws Exception {
		try {
			IProperty[] props = node.getProperties();
			for(IProperty prop : props) {
				//System.out.println(tab+prop.getName()+"="+prop.getValue());
				if(prop.getName().equals("Name")) {
					sb.append(tab+"***"+prop.getName()+"="+prop.getValue());
				} else {
					sb.append(tab+prop.getName()+"="+prop.getValue());
				}
				sb.append(System.getProperty("line.separator")); //return
			}
		} catch(Exception ex) {
			throw ex;
		}
	}
	
	private static void exploreNode(INode node, String tab) throws Exception {
		try {
			printNodeProperties(node, tab);
			
			tab += "\t";
			
			Collection childNodes = node.getChildNodes();
			Iterator it1 = childNodes.iterator();
			while(it1.hasNext()) {
				INode childNode = (INode)it1.next();
				exploreNode(childNode, tab);
			}
		} catch(Exception ex) {
			throw ex;
		}
	}
	
	private static void outputTextFile() throws Exception {
		try {
			File file = new File("d:/test.txt");

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(sb.toString());
			bw.close();
		} catch(Exception ex) {
			throw ex;
		}
	}
	

	
}
