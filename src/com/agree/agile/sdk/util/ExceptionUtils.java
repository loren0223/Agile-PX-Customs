package com.agree.agile.sdk.util;

public class ExceptionUtils 
{
	public static Throwable getRootCauseException(Throwable th)
	{
		if(th.getCause() != null)
		{
			return getRootCauseException(th.getCause());
		}
			
		return th;
	}
}
