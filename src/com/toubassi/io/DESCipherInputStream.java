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
 * Created on Jul 2, 2004
 */
package com.toubassi.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

/**
 * @author garrick
 */
public class DESCipherInputStream extends CipherInputStream
{
	public DESCipherInputStream(InputStream in, String passPhrase)
	{
		super(in, DESCipherOutputStream.createCipher(passPhrase, Cipher.DECRYPT_MODE));
	}

	public static String decrypt(String passPhrase, String cipherText)
	{
	    try {
	        ByteArrayInputStream byteArrayInput = new ByteArrayInputStream(cipherText.getBytes("UTF-8"));
	        InputStream base64Input = MimeUtility.decode(byteArrayInput, "base64");
	        DESCipherInputStream cipherInput = new DESCipherInputStream(base64Input, passPhrase);
	        
	        ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
	        byte buffer[] = new byte[1024];
	        int numRead;
	        while ((numRead = cipherInput.read(buffer)) != -1) {
	            byteArrayOutput.write(buffer, 0, numRead);
	        }
		    return byteArrayOutput.toString();	    
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
	    if (args.length < 2) {
	        System.err.println("usage: DESCipherInputStream password cipherText1 cipherText2...");
	        System.exit(1);
	    }
	    
	    String passPhrase = args[0];
	    for (int i = 1; i < args.length; i++) {
	        System.out.println(args[i] + ": " + decrypt(passPhrase, args[i]));
	    }
	}
}
