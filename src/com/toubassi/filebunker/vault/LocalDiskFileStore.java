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
 * Created on Aug 20, 2004
 */
package com.toubassi.filebunker.vault;

import com.subx.common.NotificationCenter;
import com.toubassi.io.ByteCountingInputStream;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;
import com.toubassi.util.ClassUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * LocalDiskFileStore is an implementation of FileStore that stores
 * all of its files on a local disk.  It is primarily used for testing.
 * @author garrick
 */
public class LocalDiskFileStore implements FileStore
{
    private long availableBytes = 1000*1024*1024; // 1000 MB
    private File rootDirectory;
    private VaultConfiguration vaultConfig;

    public String name()
    {
        return rootDirectory.getPath();
    }

    public boolean isConfigured()
    {
        return rootDirectory != null;
    }

    public void setConfiguration(VaultConfiguration configuration)
    {
        this.vaultConfig = configuration;
    }

    public FileStore editableCopy()
    {
        LocalDiskFileStore other = new LocalDiskFileStore();
        other.rootDirectory = rootDirectory;
        return other;
    }

    public boolean canHandleRevision(RevisionIdentifier identifier)
    {
        return identifier.handlerName().equals(name());
    }

    public boolean canBackupFile(File file) throws VaultException
    {
        return true;
    }

	public void backupFile(File file, String name, RevisionIdentifier identifier, FileOperationListener listener) throws VaultException
    {
		try {
            identifier.setHandlerName(name());
            
	        String password = vaultConfig.currentPassword();
	        ByteCountingInputStream countingStream = FileStoreUtil.backupInputStream(file, password, listener);
			
	        File target = fileForGuid(identifier.guid());
	        File parent = target.getParentFile();
	        if (!parent.isDirectory() && !parent.mkdirs()) {
	            throw new VaultException("Could not make directory " + target.getParentFile().getPath());
	        }
			FileOutputStream fileOutput = new FileOutputStream(target);
			BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput, 2048);
			
			byte buffer[] = new byte[2048];
			int numRead;
			
			while ((numRead = countingStream.read(buffer)) >= 0) {
			    bufferedOutput.write(buffer, 0, numRead);
			}

			countingStream.close();                
            bufferedOutput.close();                

		    long backedupSize = countingStream.byteCount();
		    adjustAvailableBytes(backedupSize);
		    identifier.setBackedupSize(backedupSize);
		}
		catch (OperationCanceledIOException e) {
		    throw new OperationCanceledVaultException(e);
		}
        catch (IOException e) {
            throw new VaultException(file, "Could not backup " + file.getPath() + " due to errors reading the file.", e);
        }
    }

    public InputStream restoreFile(RevisionIdentifier identifier, Date date)
            throws VaultException
    {
        try {
            FileInputStream fileInput = new FileInputStream(fileForGuid(identifier.guid()));
            BufferedInputStream input = new BufferedInputStream(fileInput, 2048);
			return FileStoreUtil.restoreInputStream(input, vaultConfig.passwordForDate(date));
        }
        catch (IOException e) {
            throw new VaultException("Error encountered.  The file could not be restored.", e);            
        }
    }

    public void deleteFile(RevisionIdentifier identifier, long backedupSize)
            throws VaultException
    {
        File file = fileForGuid(identifier.guid());

        if (!file.delete()) {
            throw new VaultException("Error encountered.  The file could not be deleted from the backup repository.");
        }        
        adjustAvailableBytes(backedupSize);
    }

    public long availableBytes() throws VaultException
    {
        return availableBytes;
    }

    public void performMaintenance() throws VaultException
    {        
    }

	public void prepareForShutdown() throws VaultException
	{
	}

    public void serializeXML(XMLSerializer serializer)
    {
	    serializer.push(ClassUtil.nameWithoutPackage(getClass()));

	    serializer.write("Directory", rootDirectory.getPath());
	    serializer.pop();
    }

    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        if ("Directory".equals(container)) {
            rootDirectory = new File(value);
            if (!rootDirectory.isDirectory() && !rootDirectory.mkdirs()) {
                throw new RuntimeException("Cannot make directory " + rootDirectory.getPath());
            }
        }
        return null;
    }

	protected synchronized void adjustAvailableBytes(long deltaBytes)
	{
	    availableBytes += deltaBytes;
	    NotificationCenter.sharedCenter().post(AvailableBytesChangedNotification, this, null);
	}
	
	private File fileForGuid(String guid)
	{
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(guid.getBytes("UTF-8"));
            byte raw[] = digest.digest();
            
            StringBuffer buffer = new StringBuffer(rootDirectory.getPath());
            buffer.append(File.separatorChar);
            for (int i = 0, count = raw.length; i < 3 && i < count; i++) {
                int b = raw[i] + 128;
                buffer.append(b);
                buffer.append(File.separatorChar);
            }
            
            for (int i = 0, count = guid.length(); i < count;  i++) {
                char ch = guid.charAt(i);
                
                if (ch == File.separatorChar) {
                    buffer.append("slash");
                }
                else {
                    buffer.append(guid);
                }
            }
            return new File(buffer.toString());
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
	}
}
