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
 * Created on Aug 12, 2004
 */
package com.toubassi.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

/**
 * @author garrick
 */
public class Password
{
    private static Random random;

    private static String encrypt(String plainText, byte[] randomBytes)
    {
        MessageDigest digest;
        
        try {            
            digest = MessageDigest.getInstance("SHA-1");
            
            if (randomBytes != null) {
                digest.update(randomBytes);
            }
            
            digest.update(plainText.getBytes("UTF-8"));            
            byte raw[] = digest.digest();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			OutputStream base64Out = MimeUtility.encode(out, "base64");
            if (randomBytes != null) {
                base64Out.write(randomBytes);
            }
			base64Out.write(raw);
			base64Out.close();
			return out.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        catch (MessagingException e) {
            throw new RuntimeException(e);
        }        
    }
    
    public static String encrypt(String plainText)
    {
        if (random == null) {
            random = new Random();
        }
        byte[] randomBytes = new byte[4];
        random.nextBytes(randomBytes);
        
        return encrypt(plainText, randomBytes);
    }
    
    public static boolean compare(String plainText, String encrypted)
    {
        try {
	        ByteArrayInputStream bytesIn = new ByteArrayInputStream(encrypted.getBytes("UTF-8"));
			InputStream base64In = MimeUtility.decode(bytesIn, "base64");
			byte randomBytes[] = new byte[4];
			base64In.read(randomBytes);
			
			if (encrypt(plainText, randomBytes).equals(encrypted)) {
			    return true;
			}
			
			// Temporarily for backward compatibility from before I added
			// the randomBytes.
	        return encrypt(plainText, null).equals(encrypted);
	    }
	    catch (UnsupportedEncodingException e) {
	        throw new RuntimeException(e);
	    }
	    catch (IOException e) {
	        throw new RuntimeException(e);
	    }
	    catch (MessagingException e) {
	        throw new RuntimeException(e);
	    }        
    }

	public static void main(String args[])
	{
	    if (args.length < 1) {
	        System.err.println("usage: Password clear1 clear2...");
	        System.exit(1);
	    }
	    
	    for (int i = 0; i < args.length; i++) {
	        String encrypted = encrypt(args[i]);
	        System.out.println(args[i] + ": " + encrypted);
	        System.out.println(compare(args[i], encrypted));
	    }
	}
}
