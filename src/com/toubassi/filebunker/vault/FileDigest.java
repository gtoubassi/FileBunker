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
import com.toubassi.util.FileFind;
import com.toubassi.util.FileFindDelegate;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;

import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

/**
 * @author garrick
 */
public class FileDigest implements Archivable
{
    private byte[] digestBytes;
    private String digestString;

	public static String digestStringCharacterClass()
	{
	    return "[A-Za-z0-9\\+/=]";
	}

	public FileDigest()
    {        
        // For unarchiving
    }

	public FileDigest(byte[] digestBytes)
    {        
	    // For testing
	    this.digestBytes = digestBytes;	    
	    if (digestBytes.length != 16) {
	        throw new RuntimeException();
	    }
    }

    public FileDigest(File file) throws IOException
    {
        this(file.getPath());
    }
    
    public FileDigest(String path) throws IOException
    {
        // No need to buffer because we will be reading in large chunks
        this(new FileInputStream(path));
    }
    
    public FileDigest(InputStream input) throws IOException
    {
        MessageDigest digest;

        try {
            //SHA-1 takes about 50% longer than MD5
            digest = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        
        byte[] buffer = new byte[1024];        
        int numRead = 0;
        while ((numRead = input.read(buffer)) >= 0) {
            if (numRead > 0) {
                digest.update(buffer, 0, numRead);
            }
        }
        
        input.close();
        
        digestBytes = digest.digest();
        if (digestBytes.length != 16) {
            throw new RuntimeException("Expected " + digest.getAlgorithm() + " to generate a 16 byte digest");
        }
    }
    
    public String digestString()
    {
        if (digestString == null) {
	        
            try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				OutputStream base64Out = MimeUtility.encode(out, "base64");
				base64Out.write(digestBytes);
				base64Out.close();

				String encoded = out.toString();
				if (encoded.length() != 24 || !encoded.endsWith("==")) {
				    throw new RuntimeException("Size of digest changed");
				}
				digestString = encoded.substring(0, 22);
            }
    	    catch (MessagingException e) {
    	        throw new RuntimeException(e);
    	    }
    	    catch (IOException e) {
    	        throw new RuntimeException(e);	        
    	    }	    
        }
        return digestString;
        
    }
    
    public boolean equals(Object other)
    {
        if (digestBytes == null || other == null || !(other instanceof FileDigest)) {
            return false;
        }
        FileDigest otherDigest = (FileDigest)other;
        return otherDigest.digestBytes != null &&
        	   Arrays.equals(digestBytes, otherDigest.digestBytes);
    }
    
    public int hashCode()
    {
        if (digestBytes == null) {
            // The only way we can get here is if the no arg constructor
            // invoked, which is only supposed to be for unarchiving.
            throw new RuntimeException("Can't compute the hashCode before its been unarchived");
        }
        int hash = digestBytes[0];
        hash = (hash << 8) | digestBytes[3];
        hash = (hash << 8) | digestBytes[7];
        hash = (hash << 8) | digestBytes[11];
        return hash;
    }
    
    public void archive(ArchiveOutputStream output) throws IOException
    {
        output.writeClassVersion("com.toubassi.filebunker.vault.FileDigest", 1);
        output.write(digestBytes);
    }

    public void unarchive(ArchiveInputStream input) throws IOException
    {
        input.readClassVersion("com.toubassi.filebunker.vault.FileDigest");
        digestBytes = new byte[16];
        input.readFully(digestBytes);
    }
    
    
    // Cheezy test
    public static void main(String args[]) throws IOException
    {
        class DigestFindDelegate implements FileFindDelegate
        {
            private HashMap digests = new HashMap();
            private int totalMatches = 0;
            private long totalBytes = 0;
            private long matchedBytes = 0;
            
            private boolean filesEquals(File file1, File file2) throws IOException
            {
                if (file1.length() != file2.length()) {
                    return false;
                }
                
                InputStream input1 = new BufferedInputStream(new FileInputStream(file1));
                InputStream input2 = new BufferedInputStream(new FileInputStream(file2));
                
                try {
	                while (true) {
	                    int b1 = input1.read();
	                    int b2 = input2.read();
	                    
	                    if (b1 != b2) {
	                        System.out.println("Not equal: " + file1 + " " + file2);
	                        return false;
	                    }
	                    if (b1 == -1) {
	                        break;
	                    }
	                }
                }
                finally {
                    input1.close();
                    input2.close();
                }
                return true;
            }
        	public boolean shouldRecurseIntoDirectory(File directory)
        	{
        	    return true;
        	}

        	public boolean processFile(File file)
        	{
        	    long length = file.length();
        	    
        	    if (length == 0) {
        	        return true;
        	    }
        	    
        	    totalBytes += length;
        	    
        	    try {
	        	    FileDigest digest = new FileDigest(file);
	        	    
	        	    File other = (File)digests.get(digest);
	        	    if (other != null) {
	        	        totalMatches++;
	        	        matchedBytes += length;
	        	        System.out.println("Checking " + file + " " + other);
	        	        assert filesEquals(file, other);
	        	    }
	        	    else {
	        	        digests.put(digest, file);
	        	    }
        	    }
        	    catch (IOException e) {
        	        throw new RuntimeException(e);
        	    }
        	    return true;
        	}

        	public void didProcessDirectoryContents(File directory, String children[])
        	{        	    
        	}
        }

        /*
        for (int i = 0; i < 5; i++) {
	        long start = System.currentTimeMillis();
	        FileDigest digest = new FileDigest("c:\\garrick\\Outlook\\archive.pst");
	        long end = System.currentTimeMillis();
	        System.out.println("Total time (ms): " + (end - start));
        }
        */

        FileFind find = new FileFind();
        DigestFindDelegate delegate = new DigestFindDelegate();
        find.setDelegate(delegate);
        long start = System.currentTimeMillis();
        find.find("c:\\garrick");
        long end = System.currentTimeMillis();
        System.out.println("Total files: " + delegate.digests.size());
        System.out.println("Total Bytes: " + delegate.totalBytes);
        System.out.println("Total matches: " + delegate.totalMatches);
        System.out.println("Matched Bytes: " + delegate.matchedBytes);
        System.out.println("Total time (ms): " + (end - start));
    }
}
