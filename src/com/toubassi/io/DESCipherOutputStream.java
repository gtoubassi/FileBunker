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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

/**
 * @author garrick
 */
public class DESCipherOutputStream extends CipherOutputStream
{
	// 8-byte Salt
	public static final byte[] salt = {
		(byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32,
		(byte)0x56, (byte)0x35, (byte)0xE3, (byte)0x03
	};
	
	public static Cipher createCipher(String passPhrase, int mode)
	{
		Cipher cipher;
		
		int iterationCount = 19;
		try {
			// Create the key
			KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount);
			SecretKey key = SecretKeyFactory.getInstance(
				"PBEWithMD5AndDES").generateSecret(keySpec);
			cipher = Cipher.getInstance(key.getAlgorithm());
    
			// Prepare the parameter to the ciphers
			AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);
    
			// Create the ciphers
			cipher.init(mode, key, paramSpec);
		} catch (java.security.GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
		return cipher;
	}

	public DESCipherOutputStream(OutputStream out, String passPhrase)
	{
		super(out, createCipher(passPhrase, Cipher.ENCRYPT_MODE));
	}
	
	public static String encrypt(String passPhrase, String clearText)
	{
	    try {
		    ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
			OutputStream base64Output = MimeUtility.encode(byteArrayOutput, "base64");
		    DESCipherOutputStream cipherOutput = new DESCipherOutputStream(base64Output, passPhrase);
		    cipherOutput.write(clearText.getBytes("UTF-8"));
		    cipherOutput.close();
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
	        System.err.println("usage: DESCipherOutputStream password clear1 clear2...");
	        System.exit(1);
	    }
	    
	    String passPhrase = args[0];
	    for (int i = 1; i < args.length; i++) {
	        System.out.println(args[i] + ": " + encrypt(passPhrase, args[i]));
	    }
	}
}
