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
package com.toubassi.filebunker.vault;

import com.toubassi.archive.Archivable;
import com.toubassi.archive.ArchiveInputStream;
import com.toubassi.archive.ArchiveOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

/**
 * This class exists only for legacy purposes.  In FileBunker .95.1 and
 * earlier, GUIDs were generated using this class.  We now generate GUIDs
 * using the FileDigest class.  This class still exists for the purpose of
 * providing backward compatibility to files backed up using the older
 * versions.
 * @author garrick
 */
public class GUID implements Archivable
{
	private static int counter;
	private static Random random = new Random();

	private byte[] bytes;
    private String string;
    
	public static String guidStringCharacterClass()
	{
	    return "[A-Za-z0-9\\+/=]";
	}
	
	public static GUID nextGUID()
	{
	    try {
	        ByteArrayOutputStream guidByteStream = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(guidByteStream);
			dataOut.writeLong(System.currentTimeMillis());
			dataOut.writeInt(random.nextInt());
			dataOut.writeInt(counter++);
			dataOut.close();

	        return new GUID(guidByteStream.toByteArray());
	    }
	    catch (IOException e) {
	        throw new RuntimeException(e);
	    }
        
	}

    public GUID()
    {        
    }
    
    public GUID(byte[] bytes)
    {
        this.bytes = bytes;
        if (bytes.length != 16) {
            throw new RuntimeException("GUIDs must be 16 bytes long");
        }
    }
    
    public GUID(String guidString)
    {
        if (guidString.length() != 22) {
            throw new RuntimeException("GUID strings must be 22 characters long");
        }
        String paddedGuidString = guidString + "==";
        try {
            ByteArrayInputStream byteArrayInput = new ByteArrayInputStream(paddedGuidString.getBytes("US-ASCII"));
            InputStream base64Input = MimeUtility.decode(byteArrayInput, "base64");
            DataInputStream input = new DataInputStream(base64Input);
            bytes = new byte[16];
            input.readFully(bytes);
            if (!guidString().equals(guidString)) {
                throw new IOException("Error guid encoding is not symmetric");
            }
        }
        catch (MessagingException e) {        
            throw new RuntimeException(e);
        }
        catch (IOException e) {        
            throw new RuntimeException(e);
        }
    }
    
    public String guidString()
    {
        if (string == null) {
	        
            try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				OutputStream base64Out = MimeUtility.encode(out, "base64");
				base64Out.write(bytes);
				base64Out.close();
				
				String encoded = out.toString();
				if (encoded.length() != 24 || !encoded.endsWith("==")) {
				    throw new RuntimeException("Size of guid changed");
				}
				string = encoded.substring(0, 22);
            }
    	    catch (MessagingException e) {
    	        throw new RuntimeException(e);
    	    }
    	    catch (IOException e) {
    	        throw new RuntimeException(e);	        
    	    }	    
        }
        return string;
    }
    
    public void archive(ArchiveOutputStream output) throws IOException
    {
        output.writeClassVersion("com.toubassi.filebunker.vault.GUID", 1);
        output.write(bytes);
    }

    public void unarchive(ArchiveInputStream input) throws IOException
    {
        input.readClassVersion("com.toubassi.filebunker.vault.GUID");
        bytes = new byte[16];
        input.readFully(bytes);
    }    
}
