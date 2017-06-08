package com.agree.agile.sdk.util;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import com.sun.mail.smtp.SMTPAddressFailedException;
import com.sun.mail.smtp.SMTPAddressSucceededException;
import com.sun.mail.smtp.SMTPSendFailedException;


//@SuppressWarnings("unchecked")

public class MailUtils 
{
	public MailUtils() {}
	
	public static void postMail(String changeNumber, String axmlPath, String logPath, String errMsg, String originator) throws Exception
	{
		try 
		{
			String[] recipients = UtilConfigReader.readProperty("MAIL_RECIPIENTS").split(",");
			String from = UtilConfigReader.readProperty("MAIL_FROM");
			String smtp = UtilConfigReader.readProperty("MAIL_SMTP");
			String subject = UtilConfigReader.readProperty("MAIL_SUBJECT");
			String content = UtilConfigReader.readProperty("MAIL_CONTENT");
			
			// Edit mail info
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    Date systemDate = new Date();
		    String currentDatetime = dateFormat.format(systemDate);
		    // Subject
		    // Write mail subject and mail content
		    subject = subject.replaceAll("\\$[C][H][A][N][G][E]", changeNumber);
		    content = content.replaceAll("\\$[E][R][R][O][R]", errMsg);
		    content = content.replaceAll("\\$[C][H][A][N][G][E]", changeNumber);
		    content = content.replaceAll("\\$[O][R][I][G][I][N][A][T][O][R]", originator);

			//Set the host smtp address
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
		    for (int i = 0; i < recipients.length; i++)
		    {
		        addressTo[i] = new InternetAddress(recipients[i].trim());
		    }
		    msg.setRecipients(Message.RecipientType.TO, addressTo);
		   
		    // create and fill the content and log file
		    MimeBodyPart mbp1 = new MimeBodyPart();
		    MimeBodyPart mbp2 = new MimeBodyPart();
		    mbp1.setText(content);
		    //mbp2.attachFile(logPath);
		    // create the Multipart and add its parts to it
		    Multipart mp = new MimeMultipart();
		    mp.addBodyPart(mbp1);
		    //mp.addBodyPart(mbp2);
		    
		    //if AxmlPath not empty, fill the AXML zip file
		    if(!axmlPath.equals(""))
		    {
		    	MimeBodyPart mbp3 = new MimeBodyPart();
			    mbp3.attachFile(axmlPath);
			    mp.addBodyPart(mbp3);
		    }
		    
		    // Setting the Subject and Content Type
		    msg.setSubject(subject);
		    msg.setContent(mp);
		    msg.setSentDate(systemDate);
		    Transport.send(msg);
		} 
		catch (Exception e) 
		{
			/*
		     * Handle SMTP-specific exceptions.
		     */
		    if (e instanceof SendFailedException) 
		    {
		    	MessagingException sfe = (MessagingException)e;
		    	if (sfe instanceof SMTPSendFailedException) 
		    	{
		    		SMTPSendFailedException ssfe = (SMTPSendFailedException)sfe;
		    		System.out.println("SMTP SEND FAILED:");
		    		System.out.println(ssfe.toString());
		    		System.out.println("  Command: " + ssfe.getCommand());
		    		System.out.println("  RetCode: " + ssfe.getReturnCode());
		    		System.out.println("  Response: " + ssfe.getMessage());
		    	} 
		    	else 
		    	{
		    		System.out.println("Send failed: " + sfe.toString());
		    	}
		    	Exception ne;
		    	while ((ne = sfe.getNextException()) != null && ne instanceof MessagingException) 
		    	{
		    		sfe = (MessagingException)ne;
		    		if (sfe instanceof SMTPAddressFailedException) 
		    		{
		    			SMTPAddressFailedException ssfe = (SMTPAddressFailedException)sfe;
		    			System.out.println("ADDRESS FAILED:");
		    			System.out.println(ssfe.toString());
		    			System.out.println("  Address: " + ssfe.getAddress());
		    			System.out.println("  Command: " + ssfe.getCommand());
		    			System.out.println("  RetCode: " + ssfe.getReturnCode());
		    			System.out.println("  Response: " + ssfe.getMessage());
		    		} 
		    		else if (sfe instanceof SMTPAddressSucceededException) 
		    		{
		    			System.out.println("ADDRESS SUCCEEDED:");
		    			SMTPAddressSucceededException ssfe = (SMTPAddressSucceededException)sfe;
		    			System.out.println(ssfe.toString());
		    			System.out.println("  Address: " + ssfe.getAddress());
		    			System.out.println("  Command: " + ssfe.getCommand());
		    			System.out.println("  RetCode: " + ssfe.getReturnCode());
		    			System.out.println("  Response: " + ssfe.getMessage());
		    		}
		    	}
		    } 
		    else 
		    {
		    	System.out.println("Got Exception: " + e);
		    }
		    //Finally
		    throw e;
		}
	}

}
