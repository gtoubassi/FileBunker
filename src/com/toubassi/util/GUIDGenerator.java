/*

Copyright (c) 2004, Garrick Toubassi

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

*/

/*
 * Created on Jul 3, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.toubassi.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

/**
 * @author garrick
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class GUIDGenerator
{
	private static GUIDGenerator shared;

	private int counter;
	private Random random = new Random();
	
	public static GUIDGenerator sharedInstance()
	{
		if (shared == null) {
			shared = new GUIDGenerator();
		}
		return shared;
	}
	
	
	public static String guidCharacterClass()
	{
	    return "[A-Za-z0-9\\+/=]";
	}
	
	public String nextGUID()
	{
	    try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			OutputStream base64Out = MimeUtility.encode(out, "base64");
			DataOutputStream dataOut = new DataOutputStream(base64Out);
			dataOut.writeLong(System.currentTimeMillis());
			dataOut.writeInt(random.nextInt());
			dataOut.writeInt(counter++);
			dataOut.close();
			
			String encoded = out.toString();
			if (encoded.length() != 24 || !encoded.endsWith("==")) {
			    throw new RuntimeException("Size of guid changed");
			}
			return encoded.substring(0, 22);
	    }
	    catch (MessagingException e) {
	        throw new RuntimeException(e);
	    }
	    catch (IOException e) {
	        throw new RuntimeException(e);	        
	    }	    
	}
	
	public static void main(String args[])
	{
	    System.out.println(GUIDGenerator.sharedInstance().nextGUID());
	    System.out.println(GUIDGenerator.sharedInstance().nextGUID());
	}
}
