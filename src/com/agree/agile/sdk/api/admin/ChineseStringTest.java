package com.agree.agile.sdk.api.admin;

import java.util.Iterator;

public class ChineseStringTest {

	public static void main(String[] args) {
		String aa = "123456789012345678901234567890";
		String bb = "1234567890低1234567890依1234567890";
		
		System.out.println("aa = "+aa);
		System.out.println("aa length = "+aa.length());
		System.out.println("bb = "+bb);
		System.out.println("bb length= "+bb.length());
		
		
		final int length = aa.length();
		int actualLength = 0;
		for (int offset = 0; offset < length; ) {
		   final int codepoint = aa.codePointAt(offset);
		   
		   if(Character.isIdeographic(codepoint))
			   actualLength += 2;
		   else
			   actualLength += 1;
		   
		   if(actualLength >= 30){
			   break;
		   }
		   
		   //System.out.println("Code point of aa("+offset+") = "+codepoint);
		   System.out.println("aa("+offset+") = "+aa.charAt(offset));
		   
		   offset += Character.charCount(codepoint);
		}
		
		System.out.println("aa actual length= "+actualLength);
		
		
		
	}
	
	private static Iterable<Integer> codePoints(final String string) {
	    return new Iterable<Integer>() {
	        public Iterator<Integer> iterator() {
	            return new Iterator<Integer>() {
	                int nextIndex = 0;

	                public boolean hasNext() {
	                    return nextIndex < string.length();
	                }

	                public Integer next() {
	                    int result = string.codePointAt(nextIndex);
	                    nextIndex += Character.charCount(result);
	                    return result;
	                }

	                public void remove() {
	                    throw new UnsupportedOperationException();
	                }
	            };
	        }
	    };
	}

}
