//: AgileNodeUtility.java
package com.agree.agile.sdk.util;

import com.agile.api.IAdmin;
import com.agile.api.INode;
import com.agile.api.NodeConstants;

/**
 * Agile Administration Nodes 處理工具。 
 *  
 * @author loren
 *
 */
public class AgileNodeUtility {
	/**
	 * 
	 * @param admin Agile IAdmin Object
	 * @return Agile Workflows Node Object
	 * @throws Exception throw when exception is happened 
	 */
	public static INode getAgileWorkflowsNode(IAdmin admin) throws Exception {
		try {
			return admin.getNode(NodeConstants.NODE_AGILE_WORKFLOWS);
		} catch(Exception ex) {
			throw ex;
		}
	}
	/**
	 * 
	 * @param admin Agile IAdmin Object
	 * @param wfName Workflow API Name
	 * @return Specific Workflow Node Object
	 * @throws Exception throw when exception is happened 
	 */
	public static INode getSpecificWorkflowNode(IAdmin admin, String wfName) throws Exception {
		try {
			INode workflows = getAgileWorkflowsNode(admin);
			INode workflow = (INode)workflows.getChildNode(wfName); //API Name
			return workflow;
		} catch(Exception ex) {
			throw ex;
		}
	}
	/**
	 * 
	 * @param admin Agile IAdmin Object
	 * @param wfName Workflow API Name
	 * @return Status-List Node Object
	 * @throws Exception throw when exception is happened 
	 */
	public static INode getWFStatusListNode(IAdmin admin, String wfName) throws Exception {
		try {
			INode workflow = getSpecificWorkflowNode(admin, wfName);
			INode statusList = (INode)workflow.getChildNode("statusList"); //API Name
			return statusList;
		} catch(Exception ex) {
			throw ex;
		}
	}
	/**
	 * 
	 * @param admin Agile IAdmin Object
	 * @param wfName Workflow API Name
	 * @param stName Status API Name
	 * @return Specific Status Node Object
	 * @throws Exception throw when exception is happened 
	 */
	public static INode getWFSpecificStatusNode(IAdmin admin, String wfName, String stName) throws Exception {
		try {
			INode statusList = getWFStatusListNode(admin, wfName);
			INode status = (INode)statusList.getChildNode(stName); //API Name
			return status;
		} catch(Exception ex) {
			throw ex;
		}
	}
	/**
	 * 
	 * @param admin Agile IAdmin Object
	 * @param wfName Workflow API Name
	 * @param stName Status API Name
	 * @return Status-Property Node Object
	 * @throws Exception throw when exception is happened 
	 */
	public static INode getStatusPropertyNode(IAdmin admin, String wfName, String stName) throws Exception {
		try {
			INode status =  getWFSpecificStatusNode(admin, wfName, stName);
			INode statusProp = (INode)status.getChildNode("statusProperties"); //API Name
			return statusProp;
		} catch(Exception ex) {
			throw ex;
		}
	}
	/**
	 * 
	 * @param admin Agile IAdmin Object
	 * @param wfName Workflow API Name
	 * @param stName Workflow Status API Name
	 * @return Status-Criteria Node Object
	 * @throws Exception throw when exception is happened 
	 */
	public static INode getStatusCriteriaNode(IAdmin admin, String wfName, String stName) throws Exception {
		try {
			INode status =  getWFSpecificStatusNode(admin, wfName, stName);
			INode statusCriteria = (INode)status.getChildNode("criteriaSpecificProperties"); //API Name
			return statusCriteria;
		} catch(Exception ex) {
			throw ex;
		}
	}
	

}
///:~