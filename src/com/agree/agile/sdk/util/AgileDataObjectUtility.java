//: AgileDataObjectUtility.java
package com.agree.agile.sdk.util;
import java.math.BigDecimal;

import com.agile.api.*;
/**
 * Copyright 2015 AdvancedTEK International Corporation, 8F, No.303, Sec. 1, 
 * Fusing S. Rd., Da-an District, Taipei City 106, Taiwan(R.O.C.); Telephone
 * +886-2-2708-5108, Facsimile +886-2-2754-4126, or <http://www.advtek.com.tw/>
 * All rights reserved.
 * @author Loren.Cheng
 * @version 0.1
 */
public class AgileDataObjectUtility {
	/**
	 * 
	 * @param session
	 * @param obj
	 * @param attObj
	 * @return
	 * @throws Exception
	 */
	public static String getAttributeValue(IAgileSession session, IDataObject obj, String attObj) throws Exception { 
		String attValue = "";
		//Get attribute type
		Integer attType = AgileDataObjectUtility.getAttributeType(session, obj, attObj);
		//Get attribute base id
		Integer attId = AgileDataObjectUtility.getP3AttributeId(obj, attObj);
		try {
			//Get attribute value
			//Attribute type = Text, MultiText, Date, Money
			if(attType.equals(DataTypeConstants.TYPE_STRING) || 
					attType.equals(DataTypeConstants.TYPE_DATE) || 
					attType.equals(DataTypeConstants.TYPE_MONEY)) {
				Object numObj = obj.getValue(attId);
				attValue = numObj==null? "" : numObj.toString();
			}
			//Attribute type = Numeric
			if(attType.equals(DataTypeConstants.TYPE_DOUBLE) || 
					attType.equals(DataTypeConstants.TYPE_INTEGER)) {
				Object numObj = obj.getValue(attId);
				attValue = numObj==null? "" : numObj.toString();
				//Trim ".0" string if return value format likes 1.0, 10.0, 100.0...
				if(!attValue.equals("") && attValue.endsWith(".0")) {
					attValue = attValue.substring(0, attValue.length()-2);
				}
			}
			//Attribute type = List
			if(attType.equals(DataTypeConstants.TYPE_SINGLELIST)) {
				ICell cell = obj.getCell(attId);
				IAgileList cellList = (IAgileList)cell.getValue();
				IAgileList[] cellSelected = cellList.getSelection();
				if(cellSelected != null && cellSelected.length > 0)
					attValue = cellSelected[0].getValue().toString();
				attValue = attValue==null? "" : attValue;
			}
			//Attribute type = MultiList
			if(attType.equals(DataTypeConstants.TYPE_MULTILIST)) {
				ICell cell = obj.getCell(attId);
				IAgileList cellList = (IAgileList)cell.getValue();
				attValue = cellList.toString();
				attValue = attValue==null? "" : attValue;
			}
		} catch(APIException ex) {
			throw ex;
		} catch(Exception ex) {
			throw ex;
		}
		return attValue;
	}
	/**
	 * 
	 * @param obj
	 * @param attObj
	 * @return
	 * @throws Exception
	 */
	public static Integer getP3AttributeId (IDataObject obj, String attObj) throws Exception {
		Integer attId = null;
		//To lower case
		attObj = attObj.toLowerCase();
		//if attObj is not attribute object, return it as Base ID. 
		if(!attObj.startsWith("text") && 
				!attObj.startsWith("multitext") && 
				!attObj.startsWith("date") && 
				!attObj.startsWith("list") && 
				!attObj.startsWith("multilist") && 
				!attObj.startsWith("money") && 
				!attObj.startsWith("numeric")) {
			attId = Integer.parseInt(attObj);
			return attId;
		} else {
			//Get the Base ID of attribute object.
			try {
				//Verify object type is IChange, IItem or IProgram
				if(obj.getType() == IChange.OBJECT_TYPE) {
					//Text 01~25
					if(attObj.equals("text01")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT01;
					if(attObj.equals("text02")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT02;
					if(attObj.equals("text03")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT03;
					if(attObj.equals("text04")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT04;
					if(attObj.equals("text05")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT05;
					if(attObj.equals("text06")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT06;
					if(attObj.equals("text07")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT07;
					if(attObj.equals("text08")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT08;
					if(attObj.equals("text09")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT09;
					if(attObj.equals("text10")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT10;
					if(attObj.equals("text11")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT11;
					if(attObj.equals("text12")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT12;
					if(attObj.equals("text13")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT13;
					if(attObj.equals("text14")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT14;
					if(attObj.equals("text15")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT15;
					if(attObj.equals("text16")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT16;
					if(attObj.equals("text17")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT17;
					if(attObj.equals("text18")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT18;
					if(attObj.equals("text19")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT19;
					if(attObj.equals("text20")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT20;
					if(attObj.equals("text21")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT21;
					if(attObj.equals("text22")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT22;
					if(attObj.equals("text23")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT23;
					if(attObj.equals("text24")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT24;
					if(attObj.equals("text25")) attId=ChangeConstants.ATT_PAGE_THREE_TEXT25;
					//Date 01~15
					if(attObj.equals("date01")) attId=ChangeConstants.ATT_PAGE_THREE_DATE01;
					if(attObj.equals("date02")) attId=ChangeConstants.ATT_PAGE_THREE_DATE02;
					if(attObj.equals("date03")) attId=ChangeConstants.ATT_PAGE_THREE_DATE03;
					if(attObj.equals("date04")) attId=ChangeConstants.ATT_PAGE_THREE_DATE04;
					if(attObj.equals("date05")) attId=ChangeConstants.ATT_PAGE_THREE_DATE05;
					if(attObj.equals("date06")) attId=ChangeConstants.ATT_PAGE_THREE_DATE06;
					if(attObj.equals("date07")) attId=ChangeConstants.ATT_PAGE_THREE_DATE07;
					if(attObj.equals("date08")) attId=ChangeConstants.ATT_PAGE_THREE_DATE08;
					if(attObj.equals("date09")) attId=ChangeConstants.ATT_PAGE_THREE_DATE09;
					if(attObj.equals("date10")) attId=ChangeConstants.ATT_PAGE_THREE_DATE10;
					if(attObj.equals("date11")) attId=ChangeConstants.ATT_PAGE_THREE_DATE11;
					if(attObj.equals("date12")) attId=ChangeConstants.ATT_PAGE_THREE_DATE12;
					if(attObj.equals("date13")) attId=ChangeConstants.ATT_PAGE_THREE_DATE13;
					if(attObj.equals("date14")) attId=ChangeConstants.ATT_PAGE_THREE_DATE14;
					if(attObj.equals("date15")) attId=ChangeConstants.ATT_PAGE_THREE_DATE15;
					//List 01~25
					if(attObj.equals("list01")) attId=ChangeConstants.ATT_PAGE_THREE_LIST01;
					if(attObj.equals("list02")) attId=ChangeConstants.ATT_PAGE_THREE_LIST02;
					if(attObj.equals("list03")) attId=ChangeConstants.ATT_PAGE_THREE_LIST03;
					if(attObj.equals("list04")) attId=ChangeConstants.ATT_PAGE_THREE_LIST04;
					if(attObj.equals("list05")) attId=ChangeConstants.ATT_PAGE_THREE_LIST05;
					if(attObj.equals("list06")) attId=ChangeConstants.ATT_PAGE_THREE_LIST06;
					if(attObj.equals("list07")) attId=ChangeConstants.ATT_PAGE_THREE_LIST07;
					if(attObj.equals("list08")) attId=ChangeConstants.ATT_PAGE_THREE_LIST08;
					if(attObj.equals("list09")) attId=ChangeConstants.ATT_PAGE_THREE_LIST09;
					if(attObj.equals("list10")) attId=ChangeConstants.ATT_PAGE_THREE_LIST10;
					if(attObj.equals("list11")) attId=ChangeConstants.ATT_PAGE_THREE_LIST11;
					if(attObj.equals("list12")) attId=ChangeConstants.ATT_PAGE_THREE_LIST12;
					if(attObj.equals("list13")) attId=ChangeConstants.ATT_PAGE_THREE_LIST13;
					if(attObj.equals("list14")) attId=ChangeConstants.ATT_PAGE_THREE_LIST14;
					if(attObj.equals("list15")) attId=ChangeConstants.ATT_PAGE_THREE_LIST15;
					if(attObj.equals("list16")) attId=ChangeConstants.ATT_PAGE_THREE_LIST16;
					if(attObj.equals("list17")) attId=ChangeConstants.ATT_PAGE_THREE_LIST17;
					if(attObj.equals("list18")) attId=ChangeConstants.ATT_PAGE_THREE_LIST18;
					if(attObj.equals("list19")) attId=ChangeConstants.ATT_PAGE_THREE_LIST19;
					if(attObj.equals("list20")) attId=ChangeConstants.ATT_PAGE_THREE_LIST20;
					if(attObj.equals("list21")) attId=ChangeConstants.ATT_PAGE_THREE_LIST21;
					if(attObj.equals("list22")) attId=ChangeConstants.ATT_PAGE_THREE_LIST22;
					if(attObj.equals("list23")) attId=ChangeConstants.ATT_PAGE_THREE_LIST23;
					if(attObj.equals("list24")) attId=ChangeConstants.ATT_PAGE_THREE_LIST24;
					if(attObj.equals("list25")) attId=ChangeConstants.ATT_PAGE_THREE_LIST25;
					//Money 01~10
					if(attObj.equals("money01")) attId=ChangeConstants.ATT_PAGE_THREE_MONEY01;
					if(attObj.equals("money02")) attId=ChangeConstants.ATT_PAGE_THREE_MONEY02;
					if(attObj.equals("money03")) attId=ChangeConstants.ATT_PAGE_THREE_MONEY03;
					if(attObj.equals("money04")) attId=ChangeConstants.ATT_PAGE_THREE_MONEY04;
					if(attObj.equals("money05")) attId=ChangeConstants.ATT_PAGE_THREE_MONEY05;
					if(attObj.equals("money06")) attId=ChangeConstants.ATT_PAGE_THREE_MONEY06;
					if(attObj.equals("money07")) attId=ChangeConstants.ATT_PAGE_THREE_MONEY07;
					if(attObj.equals("money08")) attId=ChangeConstants.ATT_PAGE_THREE_MONEY08;
					if(attObj.equals("money09")) attId=ChangeConstants.ATT_PAGE_THREE_MONEY09;
					if(attObj.equals("money10")) attId=ChangeConstants.ATT_PAGE_THREE_MONEY10;
					//MultiList 01~03
					if(attObj.equals("multilist01")) attId=ChangeConstants.ATT_PAGE_THREE_MULTILIST01;
					if(attObj.equals("multilist02")) attId=ChangeConstants.ATT_PAGE_THREE_MULTILIST02;
					if(attObj.equals("multilist03")) attId=ChangeConstants.ATT_PAGE_THREE_MULTILIST03;
					//MultiText 10,20,30,31~45
					if(attObj.equals("multitext10")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT10;
					if(attObj.equals("multitext20")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT20;
					if(attObj.equals("multitext30")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT30;
					if(attObj.equals("multitext31")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT31;
					if(attObj.equals("multitext32")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT32;
					if(attObj.equals("multitext33")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT33;
					if(attObj.equals("multitext34")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT34;
					if(attObj.equals("multitext35")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT35;
					if(attObj.equals("multitext36")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT36;
					if(attObj.equals("multitext37")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT37;
					if(attObj.equals("multitext38")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT38;
					if(attObj.equals("multitext39")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT39;
					if(attObj.equals("multitext40")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT40;
					if(attObj.equals("multitext41")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT41;
					if(attObj.equals("multitext42")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT42;
					if(attObj.equals("multitext43")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT43;
					if(attObj.equals("multitext44")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT44;
					if(attObj.equals("multitext45")) attId=ChangeConstants.ATT_PAGE_THREE_MULTITEXT45;
					//Numeric 01~10
					if(attObj.equals("numeric01")) attId=ChangeConstants.ATT_PAGE_THREE_NUMERIC01;
					if(attObj.equals("numeric02")) attId=ChangeConstants.ATT_PAGE_THREE_NUMERIC02;
					if(attObj.equals("numeric03")) attId=ChangeConstants.ATT_PAGE_THREE_NUMERIC03;
					if(attObj.equals("numeric04")) attId=ChangeConstants.ATT_PAGE_THREE_NUMERIC04;
					if(attObj.equals("numeric05")) attId=ChangeConstants.ATT_PAGE_THREE_NUMERIC05;
					if(attObj.equals("numeric06")) attId=ChangeConstants.ATT_PAGE_THREE_NUMERIC06;
					if(attObj.equals("numeric07")) attId=ChangeConstants.ATT_PAGE_THREE_NUMERIC07;
					if(attObj.equals("numeric08")) attId=ChangeConstants.ATT_PAGE_THREE_NUMERIC08;
					if(attObj.equals("numeric09")) attId=ChangeConstants.ATT_PAGE_THREE_NUMERIC09;
					if(attObj.equals("numeric10")) attId=ChangeConstants.ATT_PAGE_THREE_NUMERIC10;
				}
				
				if(obj.getType() == IItem.OBJECT_TYPE) {
					//Text 01~25
					if(attObj.equals("text01")) attId=ItemConstants.ATT_PAGE_THREE_TEXT01;
					if(attObj.equals("text02")) attId=ItemConstants.ATT_PAGE_THREE_TEXT02;
					if(attObj.equals("text03")) attId=ItemConstants.ATT_PAGE_THREE_TEXT03;
					if(attObj.equals("text04")) attId=ItemConstants.ATT_PAGE_THREE_TEXT04;
					if(attObj.equals("text05")) attId=ItemConstants.ATT_PAGE_THREE_TEXT05;
					if(attObj.equals("text06")) attId=ItemConstants.ATT_PAGE_THREE_TEXT06;
					if(attObj.equals("text07")) attId=ItemConstants.ATT_PAGE_THREE_TEXT07;
					if(attObj.equals("text08")) attId=ItemConstants.ATT_PAGE_THREE_TEXT08;
					if(attObj.equals("text09")) attId=ItemConstants.ATT_PAGE_THREE_TEXT09;
					if(attObj.equals("text10")) attId=ItemConstants.ATT_PAGE_THREE_TEXT10;
					if(attObj.equals("text11")) attId=ItemConstants.ATT_PAGE_THREE_TEXT11;
					if(attObj.equals("text12")) attId=ItemConstants.ATT_PAGE_THREE_TEXT12;
					if(attObj.equals("text13")) attId=ItemConstants.ATT_PAGE_THREE_TEXT13;
					if(attObj.equals("text14")) attId=ItemConstants.ATT_PAGE_THREE_TEXT14;
					if(attObj.equals("text15")) attId=ItemConstants.ATT_PAGE_THREE_TEXT15;
					if(attObj.equals("text16")) attId=ItemConstants.ATT_PAGE_THREE_TEXT16;
					if(attObj.equals("text17")) attId=ItemConstants.ATT_PAGE_THREE_TEXT17;
					if(attObj.equals("text18")) attId=ItemConstants.ATT_PAGE_THREE_TEXT18;
					if(attObj.equals("text19")) attId=ItemConstants.ATT_PAGE_THREE_TEXT19;
					if(attObj.equals("text20")) attId=ItemConstants.ATT_PAGE_THREE_TEXT20;
					if(attObj.equals("text21")) attId=ItemConstants.ATT_PAGE_THREE_TEXT21;
					if(attObj.equals("text22")) attId=ItemConstants.ATT_PAGE_THREE_TEXT22;
					if(attObj.equals("text23")) attId=ItemConstants.ATT_PAGE_THREE_TEXT23;
					if(attObj.equals("text24")) attId=ItemConstants.ATT_PAGE_THREE_TEXT24;
					if(attObj.equals("text25")) attId=ItemConstants.ATT_PAGE_THREE_TEXT25;
					//Date 01~15
					if(attObj.equals("date01")) attId=ItemConstants.ATT_PAGE_THREE_DATE01;
					if(attObj.equals("date02")) attId=ItemConstants.ATT_PAGE_THREE_DATE02;
					if(attObj.equals("date03")) attId=ItemConstants.ATT_PAGE_THREE_DATE03;
					if(attObj.equals("date04")) attId=ItemConstants.ATT_PAGE_THREE_DATE04;
					if(attObj.equals("date05")) attId=ItemConstants.ATT_PAGE_THREE_DATE05;
					if(attObj.equals("date06")) attId=ItemConstants.ATT_PAGE_THREE_DATE06;
					if(attObj.equals("date07")) attId=ItemConstants.ATT_PAGE_THREE_DATE07;
					if(attObj.equals("date08")) attId=ItemConstants.ATT_PAGE_THREE_DATE08;
					if(attObj.equals("date09")) attId=ItemConstants.ATT_PAGE_THREE_DATE09;
					if(attObj.equals("date10")) attId=ItemConstants.ATT_PAGE_THREE_DATE10;
					if(attObj.equals("date11")) attId=ItemConstants.ATT_PAGE_THREE_DATE11;
					if(attObj.equals("date12")) attId=ItemConstants.ATT_PAGE_THREE_DATE12;
					if(attObj.equals("date13")) attId=ItemConstants.ATT_PAGE_THREE_DATE13;
					if(attObj.equals("date14")) attId=ItemConstants.ATT_PAGE_THREE_DATE14;
					if(attObj.equals("date15")) attId=ItemConstants.ATT_PAGE_THREE_DATE15;
					//List 01~25
					if(attObj.equals("list01")) attId=ItemConstants.ATT_PAGE_THREE_LIST01;
					if(attObj.equals("list02")) attId=ItemConstants.ATT_PAGE_THREE_LIST02;
					if(attObj.equals("list03")) attId=ItemConstants.ATT_PAGE_THREE_LIST03;
					if(attObj.equals("list04")) attId=ItemConstants.ATT_PAGE_THREE_LIST04;
					if(attObj.equals("list05")) attId=ItemConstants.ATT_PAGE_THREE_LIST05;
					if(attObj.equals("list06")) attId=ItemConstants.ATT_PAGE_THREE_LIST06;
					if(attObj.equals("list07")) attId=ItemConstants.ATT_PAGE_THREE_LIST07;
					if(attObj.equals("list08")) attId=ItemConstants.ATT_PAGE_THREE_LIST08;
					if(attObj.equals("list09")) attId=ItemConstants.ATT_PAGE_THREE_LIST09;
					if(attObj.equals("list10")) attId=ItemConstants.ATT_PAGE_THREE_LIST10;
					if(attObj.equals("list11")) attId=ItemConstants.ATT_PAGE_THREE_LIST11;
					if(attObj.equals("list12")) attId=ItemConstants.ATT_PAGE_THREE_LIST12;
					if(attObj.equals("list13")) attId=ItemConstants.ATT_PAGE_THREE_LIST13;
					if(attObj.equals("list14")) attId=ItemConstants.ATT_PAGE_THREE_LIST14;
					if(attObj.equals("list15")) attId=ItemConstants.ATT_PAGE_THREE_LIST15;
					if(attObj.equals("list16")) attId=ItemConstants.ATT_PAGE_THREE_LIST16;
					if(attObj.equals("list17")) attId=ItemConstants.ATT_PAGE_THREE_LIST17;
					if(attObj.equals("list18")) attId=ItemConstants.ATT_PAGE_THREE_LIST18;
					if(attObj.equals("list19")) attId=ItemConstants.ATT_PAGE_THREE_LIST19;
					if(attObj.equals("list20")) attId=ItemConstants.ATT_PAGE_THREE_LIST20;
					if(attObj.equals("list21")) attId=ItemConstants.ATT_PAGE_THREE_LIST21;
					if(attObj.equals("list22")) attId=ItemConstants.ATT_PAGE_THREE_LIST22;
					if(attObj.equals("list23")) attId=ItemConstants.ATT_PAGE_THREE_LIST23;
					if(attObj.equals("list24")) attId=ItemConstants.ATT_PAGE_THREE_LIST24;
					if(attObj.equals("list25")) attId=ItemConstants.ATT_PAGE_THREE_LIST25;
					//Money 01~10
					if(attObj.equals("money01")) attId=ItemConstants.ATT_PAGE_THREE_MONEY01;
					if(attObj.equals("money02")) attId=ItemConstants.ATT_PAGE_THREE_MONEY02;
					if(attObj.equals("money03")) attId=ItemConstants.ATT_PAGE_THREE_MONEY03;
					if(attObj.equals("money04")) attId=ItemConstants.ATT_PAGE_THREE_MONEY04;
					if(attObj.equals("money05")) attId=ItemConstants.ATT_PAGE_THREE_MONEY05;
					if(attObj.equals("money06")) attId=ItemConstants.ATT_PAGE_THREE_MONEY06;
					if(attObj.equals("money07")) attId=ItemConstants.ATT_PAGE_THREE_MONEY07;
					if(attObj.equals("money08")) attId=ItemConstants.ATT_PAGE_THREE_MONEY08;
					if(attObj.equals("money09")) attId=ItemConstants.ATT_PAGE_THREE_MONEY09;
					if(attObj.equals("money10")) attId=ItemConstants.ATT_PAGE_THREE_MONEY10;
					//MultiList 01~03
					if(attObj.equals("multilist01")) attId=ItemConstants.ATT_PAGE_THREE_MULTILIST01;
					if(attObj.equals("multilist02")) attId=ItemConstants.ATT_PAGE_THREE_MULTILIST02;
					if(attObj.equals("multilist03")) attId=ItemConstants.ATT_PAGE_THREE_MULTILIST03;
					//MultiText 10,20,30,31~45
					if(attObj.equals("multitext10")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT10;
					if(attObj.equals("multitext20")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT20;
					if(attObj.equals("multitext30")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT30;
					if(attObj.equals("multitext31")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT31;
					if(attObj.equals("multitext32")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT32;
					if(attObj.equals("multitext33")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT33;
					if(attObj.equals("multitext34")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT34;
					if(attObj.equals("multitext35")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT35;
					if(attObj.equals("multitext36")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT36;
					if(attObj.equals("multitext37")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT37;
					if(attObj.equals("multitext38")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT38;
					if(attObj.equals("multitext39")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT39;
					if(attObj.equals("multitext40")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT40;
					if(attObj.equals("multitext41")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT41;
					if(attObj.equals("multitext42")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT42;
					if(attObj.equals("multitext43")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT43;
					if(attObj.equals("multitext44")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT44;
					if(attObj.equals("multitext45")) attId=ItemConstants.ATT_PAGE_THREE_MULTITEXT45;
					//Numeric 01~10
					if(attObj.equals("numeric01")) attId=ItemConstants.ATT_PAGE_THREE_NUMERIC01;
					if(attObj.equals("numeric02")) attId=ItemConstants.ATT_PAGE_THREE_NUMERIC02;
					if(attObj.equals("numeric03")) attId=ItemConstants.ATT_PAGE_THREE_NUMERIC03;
					if(attObj.equals("numeric04")) attId=ItemConstants.ATT_PAGE_THREE_NUMERIC04;
					if(attObj.equals("numeric05")) attId=ItemConstants.ATT_PAGE_THREE_NUMERIC05;
					if(attObj.equals("numeric06")) attId=ItemConstants.ATT_PAGE_THREE_NUMERIC06;
					if(attObj.equals("numeric07")) attId=ItemConstants.ATT_PAGE_THREE_NUMERIC07;
					if(attObj.equals("numeric08")) attId=ItemConstants.ATT_PAGE_THREE_NUMERIC08;
					if(attObj.equals("numeric09")) attId=ItemConstants.ATT_PAGE_THREE_NUMERIC09;
					if(attObj.equals("numeric10")) attId=ItemConstants.ATT_PAGE_THREE_NUMERIC10;
				}
				
				if(obj.getType() == IProgram.OBJECT_TYPE) {
					//Text 01~25
					if(attObj.equals("text01")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT01;
					if(attObj.equals("text02")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT02;
					if(attObj.equals("text03")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT03;
					if(attObj.equals("text04")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT04;
					if(attObj.equals("text05")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT05;
					if(attObj.equals("text06")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT06;
					if(attObj.equals("text07")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT07;
					if(attObj.equals("text08")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT08;
					if(attObj.equals("text09")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT09;
					if(attObj.equals("text10")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT10;
					if(attObj.equals("text11")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT11;
					if(attObj.equals("text12")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT12;
					if(attObj.equals("text13")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT13;
					if(attObj.equals("text14")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT14;
					if(attObj.equals("text15")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT15;
					if(attObj.equals("text16")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT16;
					if(attObj.equals("text17")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT17;
					if(attObj.equals("text18")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT18;
					if(attObj.equals("text19")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT19;
					if(attObj.equals("text20")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT20;
					if(attObj.equals("text21")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT21;
					if(attObj.equals("text22")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT22;
					if(attObj.equals("text23")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT23;
					if(attObj.equals("text24")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT24;
					if(attObj.equals("text25")) attId=ProgramConstants.ATT_PAGE_THREE_TEXT25;
					//Date 01~15
					if(attObj.equals("date01")) attId=ProgramConstants.ATT_PAGE_THREE_DATE01;
					if(attObj.equals("date02")) attId=ProgramConstants.ATT_PAGE_THREE_DATE02;
					if(attObj.equals("date03")) attId=ProgramConstants.ATT_PAGE_THREE_DATE03;
					if(attObj.equals("date04")) attId=ProgramConstants.ATT_PAGE_THREE_DATE04;
					if(attObj.equals("date05")) attId=ProgramConstants.ATT_PAGE_THREE_DATE05;
					if(attObj.equals("date06")) attId=ProgramConstants.ATT_PAGE_THREE_DATE06;
					if(attObj.equals("date07")) attId=ProgramConstants.ATT_PAGE_THREE_DATE07;
					if(attObj.equals("date08")) attId=ProgramConstants.ATT_PAGE_THREE_DATE08;
					if(attObj.equals("date09")) attId=ProgramConstants.ATT_PAGE_THREE_DATE09;
					if(attObj.equals("date10")) attId=ProgramConstants.ATT_PAGE_THREE_DATE10;
					if(attObj.equals("date11")) attId=ProgramConstants.ATT_PAGE_THREE_DATE11;
					if(attObj.equals("date12")) attId=ProgramConstants.ATT_PAGE_THREE_DATE12;
					if(attObj.equals("date13")) attId=ProgramConstants.ATT_PAGE_THREE_DATE13;
					if(attObj.equals("date14")) attId=ProgramConstants.ATT_PAGE_THREE_DATE14;
					if(attObj.equals("date15")) attId=ProgramConstants.ATT_PAGE_THREE_DATE15;
					//List 01~25
					if(attObj.equals("list01")) attId=ProgramConstants.ATT_PAGE_THREE_LIST01;
					if(attObj.equals("list02")) attId=ProgramConstants.ATT_PAGE_THREE_LIST02;
					if(attObj.equals("list03")) attId=ProgramConstants.ATT_PAGE_THREE_LIST03;
					if(attObj.equals("list04")) attId=ProgramConstants.ATT_PAGE_THREE_LIST04;
					if(attObj.equals("list05")) attId=ProgramConstants.ATT_PAGE_THREE_LIST05;
					if(attObj.equals("list06")) attId=ProgramConstants.ATT_PAGE_THREE_LIST06;
					if(attObj.equals("list07")) attId=ProgramConstants.ATT_PAGE_THREE_LIST07;
					if(attObj.equals("list08")) attId=ProgramConstants.ATT_PAGE_THREE_LIST08;
					if(attObj.equals("list09")) attId=ProgramConstants.ATT_PAGE_THREE_LIST09;
					if(attObj.equals("list10")) attId=ProgramConstants.ATT_PAGE_THREE_LIST10;
					if(attObj.equals("list11")) attId=ProgramConstants.ATT_PAGE_THREE_LIST11;
					if(attObj.equals("list12")) attId=ProgramConstants.ATT_PAGE_THREE_LIST12;
					if(attObj.equals("list13")) attId=ProgramConstants.ATT_PAGE_THREE_LIST13;
					if(attObj.equals("list14")) attId=ProgramConstants.ATT_PAGE_THREE_LIST14;
					if(attObj.equals("list15")) attId=ProgramConstants.ATT_PAGE_THREE_LIST15;
					if(attObj.equals("list16")) attId=ProgramConstants.ATT_PAGE_THREE_LIST16;
					if(attObj.equals("list17")) attId=ProgramConstants.ATT_PAGE_THREE_LIST17;
					if(attObj.equals("list18")) attId=ProgramConstants.ATT_PAGE_THREE_LIST18;
					if(attObj.equals("list19")) attId=ProgramConstants.ATT_PAGE_THREE_LIST19;
					if(attObj.equals("list20")) attId=ProgramConstants.ATT_PAGE_THREE_LIST20;
					if(attObj.equals("list21")) attId=ProgramConstants.ATT_PAGE_THREE_LIST21;
					if(attObj.equals("list22")) attId=ProgramConstants.ATT_PAGE_THREE_LIST22;
					if(attObj.equals("list23")) attId=ProgramConstants.ATT_PAGE_THREE_LIST23;
					if(attObj.equals("list24")) attId=ProgramConstants.ATT_PAGE_THREE_LIST24;
					if(attObj.equals("list25")) attId=ProgramConstants.ATT_PAGE_THREE_LIST25;
					//Money 01~10
					if(attObj.equals("money01")) attId=ProgramConstants.ATT_PAGE_THREE_MONEY01;
					if(attObj.equals("money02")) attId=ProgramConstants.ATT_PAGE_THREE_MONEY02;
					if(attObj.equals("money03")) attId=ProgramConstants.ATT_PAGE_THREE_MONEY03;
					if(attObj.equals("money04")) attId=ProgramConstants.ATT_PAGE_THREE_MONEY04;
					if(attObj.equals("money05")) attId=ProgramConstants.ATT_PAGE_THREE_MONEY05;
					if(attObj.equals("money06")) attId=ProgramConstants.ATT_PAGE_THREE_MONEY06;
					if(attObj.equals("money07")) attId=ProgramConstants.ATT_PAGE_THREE_MONEY07;
					if(attObj.equals("money08")) attId=ProgramConstants.ATT_PAGE_THREE_MONEY08;
					if(attObj.equals("money09")) attId=ProgramConstants.ATT_PAGE_THREE_MONEY09;
					if(attObj.equals("money10")) attId=ProgramConstants.ATT_PAGE_THREE_MONEY10;
					//MultiList 01~03
					if(attObj.equals("multilist01")) attId=ProgramConstants.ATT_PAGE_THREE_MULTILIST01;
					if(attObj.equals("multilist02")) attId=ProgramConstants.ATT_PAGE_THREE_MULTILIST02;
					if(attObj.equals("multilist03")) attId=ProgramConstants.ATT_PAGE_THREE_MULTILIST03;
					//MultiText 10,20,30,31~45
					if(attObj.equals("multitext10")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT10;
					if(attObj.equals("multitext20")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT20;
					if(attObj.equals("multitext30")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT30;
					if(attObj.equals("multitext31")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT31;
					if(attObj.equals("multitext32")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT32;
					if(attObj.equals("multitext33")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT33;
					if(attObj.equals("multitext34")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT34;
					if(attObj.equals("multitext35")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT35;
					if(attObj.equals("multitext36")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT36;
					if(attObj.equals("multitext37")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT37;
					if(attObj.equals("multitext38")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT38;
					if(attObj.equals("multitext39")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT39;
					if(attObj.equals("multitext40")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT40;
					if(attObj.equals("multitext41")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT41;
					if(attObj.equals("multitext42")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT42;
					if(attObj.equals("multitext43")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT43;
					if(attObj.equals("multitext44")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT44;
					if(attObj.equals("multitext45")) attId=ProgramConstants.ATT_PAGE_THREE_MULTITEXT45;
					//Numeric 01~10
					if(attObj.equals("numeric01")) attId=ProgramConstants.ATT_PAGE_THREE_NUMERIC01;
					if(attObj.equals("numeric02")) attId=ProgramConstants.ATT_PAGE_THREE_NUMERIC02;
					if(attObj.equals("numeric03")) attId=ProgramConstants.ATT_PAGE_THREE_NUMERIC03;
					if(attObj.equals("numeric04")) attId=ProgramConstants.ATT_PAGE_THREE_NUMERIC04;
					if(attObj.equals("numeric05")) attId=ProgramConstants.ATT_PAGE_THREE_NUMERIC05;
					if(attObj.equals("numeric06")) attId=ProgramConstants.ATT_PAGE_THREE_NUMERIC06;
					if(attObj.equals("numeric07")) attId=ProgramConstants.ATT_PAGE_THREE_NUMERIC07;
					if(attObj.equals("numeric08")) attId=ProgramConstants.ATT_PAGE_THREE_NUMERIC08;
					if(attObj.equals("numeric09")) attId=ProgramConstants.ATT_PAGE_THREE_NUMERIC09;
					if(attObj.equals("numeric10")) attId=ProgramConstants.ATT_PAGE_THREE_NUMERIC10;
				}
				
				//Error Handle
				if(attId == null)
					throw new Exception("ERROR: Invalid Attribute ID ["+attObj+"]");
				//return
					return attId;
			} catch(Exception ex) {
				throw ex;
			}
		}
	}
	/**
	 * 
	 * @param session
	 * @param obj
	 * @param attObj
	 * @return
	 * @throws Exception
	 */
	public static IAttribute getAttributeObject(IAgileSession session, IDataObject obj, String attObj) throws Exception {
		IAttribute iAttObj = null;
		try {
			//Get dataobject attribute base id
			int attId = AgileDataObjectUtility.getP3AttributeId(obj, attObj); 
			//Get Admin instance
			IAdmin admin = session.getAdminInstance();
			//Get IAttribute obj
			if(obj.getType() == IChange.OBJECT_TYPE) {
				iAttObj = admin.getAgileClass(ChangeConstants.CLASS_CHANGE_BASE_CLASS).getAttribute(attId);
			}
			if(obj.getType() == IItem.OBJECT_TYPE) {
				iAttObj = admin.getAgileClass(ItemConstants.CLASS_ITEM_BASE_CLASS).getAttribute(attId);
			}
			if(obj.getType() == IProgram.OBJECT_TYPE) {
				iAttObj = admin.getAgileClass(ProgramConstants.CLASS_PROGRAM_BASE_CLASS).getAttribute(attId);
			}			
		} catch(APIException ae) {
			throw ae;
		} catch(Exception ex) {
			throw ex;
		} 
		return iAttObj;	
	}
	/**
	 * 
	 * @param session
	 * @param obj
	 * @param attObj
	 * @return
	 * @throws Exception
	 */
	public static Integer getAttributeType(IAgileSession session, IDataObject obj, String attObj) throws Exception {
		Integer attType = null;
		try {
			//Get IAttibute object
			IAttribute iAttObj = AgileDataObjectUtility.getAttributeObject(session, obj, attObj);
			//Get attribute type
			attType = iAttObj.getDataType();
		} catch(APIException ae) {
			throw ae;
		} catch(Exception ex) {
			throw ex;
		}
		return attType;	
	}
	/**
	 * 
	 * @param listOption
	 * @param separator
	 * @return
	 * @throws Exception
	 */
	public static String getListOptionCode(String listOption, String separator) throws Exception {
		String listOpnCode = "";
		try {
			listOption = listOption.trim();
			int index = listOption.indexOf(separator);
			//If List Option format is Code_Desc
			if(index > 0) {
				listOpnCode = listOption.substring(0, index);
			} else {
				//If List Option format is not Code_Desc
				listOpnCode = listOption;
			}
			return listOpnCode;
		} catch(Exception ex) {
			throw ex;
		}
	}
	/**
	 * 
	 * @param listOption
	 * @param separator
	 * @return
	 * @throws Exception
	 */
	public static String getListOptionDesc(String listOption, String separator) throws Exception {
		String listOpnDesc = "";
		try {
			listOption = listOption.trim();
			int index = listOption.indexOf(separator);
			//If List Option format is Code_Desc
			if(index > 0) {
				listOpnDesc = listOption.substring(index+1);
			} else {
				//If List Option format is not Code_Desc
				listOpnDesc = listOption;
			}
			return listOpnDesc;
		} catch(Exception ex) {
			throw ex;
		}
	}
	
	/**
	* 提供精確的小數位四舍五入處理。
	* @param v 需要四舍五入的數字
	* @param scale 小數點后保留幾位
	* @return 四舍五入后的結果
	*/
	public static double round(String v, int scale) {
		if(scale<0) {
			throw new IllegalArgumentException("The scale must be a positive integer or zero");
		}
		BigDecimal b = new BigDecimal(v);
		BigDecimal one = new BigDecimal("1");
		return b.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	/**
	 * 
	 * @param session
	 * @param userId
	 * @return
	 * @throws Exception
	 */
	public static IUser getUserObj(IAgileSession session,String userId) throws Exception {
		IUser userObj = null;
		try {
			userObj = (IUser)session.getObject(IUser.OBJECT_TYPE, userId);
			if(userObj==null) {
				throw new Exception("No such user ID: ["+userId+"]");
			}
		} catch(APIException e) {
			throw e;
		} catch(Exception e) {
			throw e;
		}
		return userObj;
	}
	/**
	 * 
	 * @param session
	 * @param groupName
	 * @return
	 * @throws Exception
	 */
	public static IUserGroup getUserGroupObj(IAgileSession session,String groupName) throws Exception {
		IUserGroup userGroupObj = null;
		try {
			userGroupObj = (IUserGroup)session.getObject(IUserGroup.OBJECT_TYPE, groupName);
			if(userGroupObj==null) {
				throw new Exception("No such user group: ["+groupName+"]");
			}
		} catch(APIException e) {
			throw e;
		} catch(Exception e) {
			throw e;
		}
		return userGroupObj;
	}
}
///:~