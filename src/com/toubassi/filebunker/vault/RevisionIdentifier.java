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
 * Created on Jul 28, 2004
 */
package com.toubassi.filebunker.vault;

import com.toubassi.archive.Archivable;
import com.toubassi.archive.ArchiveInputStream;
import com.toubassi.archive.ArchiveOutputStream;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * @author garrick
 */
public class RevisionIdentifier implements XMLSerializable, Serializable, Archivable
{
    private GUID legacyGUID;
    
    private FileDigest digest;
    private String handlerName;
    private long size;
    private long backedupSize;
    private transient int refCount;
    
    public static final String guidCharacterClass()
    {
        return FileDigest.digestStringCharacterClass();
    }
    
    public RevisionIdentifier()
    {
        // Only for deserialization
    }
    
    public RevisionIdentifier(FileDigest digest, long size)
    {
        this.digest = digest;
        this.size = size;
        this.backedupSize = size;
    }
    
    public RevisionIdentifier(String handlerName, File file) throws IOException
    {
        this(handlerName, file, 0);
    }
    
    public RevisionIdentifier(String handlerName, File file, long backedupSize) throws IOException
    {
        this.handlerName = handlerName;
        this.digest = new FileDigest(file);
        size = file.length();
        this.backedupSize = backedupSize;
    }
    
    public RevisionIdentifier(String handlerName, FileDigest digest, long size, long backedupSize) throws IOException
    {
        this.handlerName = handlerName;
        this.digest = digest;
        this.size = size;
        this.backedupSize = backedupSize;
    }
    
    public void addReference()
    {
        refCount++;
    }
    
    public void removeReference()
    {
        if (refCount <= 0) {
            throw new IllegalStateException("Attempt to remove reference when count == " + refCount);
        }
        refCount--;
    }
    
    public int referenceCount()
    {
        return refCount;
    }
    
    public boolean hasReferences()
    {
        return refCount > 0;
    }
    
    public void setHandlerName(String name)
    {
        handlerName = name;
    }
    public String handlerName()
    {
        return handlerName;
    }
    
    public String guid()
    {
        if (legacyGUID != null) {
            return legacyGUID.guidString();
        }
        return digest.digestString();
    }
    
    /**
     * May return null.
     */
    public FileDigest digest()
    {
        return digest;
    }
    
    public synchronized void setSize(long size)
    {
        this.size = size;
    }

    public long size()
    {
        return size;
    }

    public synchronized void setBackedupSize(long size)
    {
        backedupSize = size;
    }

    public long backedupSize()
    {
        return backedupSize;
    }

    public long effectiveBackedupSize()
    {        
        return refCount == 0 ? backedupSize : (backedupSize / refCount);
    }

    public String toString()
    {
        return guid() + ":" + handlerName;
    }

	public void serializeXML(XMLSerializer writer)
	{
	    writer.push("identifier");
	    if (legacyGUID != null) {
	        writer.write("legacyGuid", legacyGUID.guidString());
	    }
	    else {
	        writer.write("guid", digest.digestString());
	    }
		writer.write("handler", handlerName);
	    writer.pop();
	}

    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        throw new RuntimeException("Can't read xml");
    }

    public void writeData(DataOutputStream out) throws IOException
    {
        throw new RuntimeException("Can't write legacy datastream");
    }
    
    public void readData(DataInputStream in) throws IOException
    {
        legacyGUID = new GUID(in.readUTF());
        handlerName = in.readUTF();
    }

    public void archive(ArchiveOutputStream output) throws IOException
    {
        output.writeClassVersion("com.toubassi.filebunker.vault.RevisionIdentifier", 1);
        output.writeBoolean(legacyGUID == null);
        if (legacyGUID != null) {
            output.writeObject(legacyGUID, Archivable.StrictlyTypedValue);
        }
        else {
            output.writeObject(digest, Archivable.StrictlyTypedValue);            
        }
        output.writeUniqueString(handlerName);
        output.writeCompactLong(size);
        output.writeCompactLong(backedupSize);
    }

    public void unarchive(ArchiveInputStream input) throws IOException
    {
        input.readClassVersion("com.toubassi.filebunker.vault.RevisionIdentifier");
        boolean hasDigest = input.readBoolean();
        if (hasDigest) {
            digest = (FileDigest)input.readObject(Archivable.StrictlyTypedValue, FileDigest.class);
        }
        else {
            legacyGUID = (GUID)input.readObject(Archivable.StrictlyTypedValue, GUID.class);
        }
        handlerName = input.readUniqueString();
        size = input.readCompactLong();
        backedupSize = input.readCompactLong();
    }    
}
