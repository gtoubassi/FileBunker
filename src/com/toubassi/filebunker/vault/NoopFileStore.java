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
 * Created on Aug 10, 2004
 */
package com.toubassi.filebunker.vault;

import com.subx.common.NotificationCenter;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Date;

/**
 * NoopFileStore is a debugging FileStore which actually does nothing
 * when asked to backup and restore files.  It claims to have 1Gb of
 * space, and will properly message OperationListeners.  This is used
 * for debugging and development when you don't want to actually wait
 * for a large number of files to be transferred to a file store.
 * 
 * @author garrick
 */
public class NoopFileStore implements FileStore
{
    private static String namePrefix = "noop";
    private static int nameSuffix = 1;
    private String name;
    private long available = 1000*1000*1024*1024;


    public NoopFileStore()
    {
        this(namePrefix + (nameSuffix++));
    }

    public NoopFileStore(String name)
    {
        this.name = name;
        
    }
    public String name()
    {
        return name;
    }

    public boolean isConfigured()
    {
        return true;
    }

    public FileStore editableCopy()
    {
        NoopFileStore copy = new NoopFileStore(name);
        copy.available = available;
        return copy;
    }

    public void setConfiguration(VaultConfiguration configuration)
    {
    }

    public boolean canHandleRevision(RevisionIdentifier identifier)
    {
        return name.startsWith(namePrefix);
    }

    public boolean canBackupFile(File file) throws VaultException
    {
        return true;
    }

    public RevisionIdentifier backupFile(File file, String backupName, long[] sizeOut, FileOperationListener listener) throws VaultException
    {
        long size = file.length();
        
        if (listener != null) {
            for (int i = 0; i < 100; i++) {
                if (!listener.fileProgress(file, size / 100 + 1)) {
                    return null;
                }
            }
        }
        
        sizeOut[0] = (long)(.7 * size);
        adjustAvailable(-sizeOut[0]);
        NotificationCenter.sharedCenter().post(MaintenanceNeededNotification, this, null);
        return new RevisionIdentifier(name);
    }

    public InputStream restoreFile(RevisionIdentifier identifier, Date date) throws VaultException
    {
        return new ByteArrayInputStream(new byte[0]);
    }

    public void deleteFile(RevisionIdentifier identifier, long backedupSize) throws VaultException
    {
        adjustAvailable(backedupSize);
    }

    public long availableBytes() throws VaultException
    {
        return available;
    }

    public void performMaintenance() throws VaultException
    {
    }

	public void prepareForShutdown() throws VaultException
	{
	}

	public void serializeXML(XMLSerializer serializer)
	{
	}

    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        return null;
    }
    
    private void adjustAvailable(long delta)
    {
        available += delta;
        NotificationCenter.sharedCenter().post(AvailableBytesChangedNotification, this, null);
    }

}
