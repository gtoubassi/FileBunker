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
 * Created on Jul 26, 2004
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
import java.io.IOException;

/**
 * @author garrick
 */
public class FileRevision extends Revision
{
    private RevisionIdentifier identifier;
    
    public FileRevision()
    {
    }

    public void revisionWasRemoved()
    {
        if (identifier != null) {
            identifier.removeReference();
        }
    }
    
    public void setIdentifier(RevisionIdentifier identifier)
    {
        if (this.identifier != null) {
            this.identifier.removeReference();
        }
        this.identifier = identifier;
        identifier.addReference();
    }

    public RevisionIdentifier identifier()
    {
        return identifier;
    }
    
    public long size()
    {
        return identifier != null ? identifier.size() : 0;
    }

    public long effectiveBackedupSize()
    {
        return identifier != null ? identifier.effectiveBackedupSize() : 0;
    }
    
    public long backedupSize()
    {
        return identifier != null ? identifier.backedupSize() : 0;
    }
    
    /**
     * The ratio of backupSize to size.  
     * @return
     */
    public float backedupSizeRatio()
    {
        long backedupSize = backedupSize();
        long size = size();
        
        if (backedupSize == 0 || size == 0) {
            return 1;
        }
        return ((float)backedupSize) / ((float)size);
    }

	public void serializeXML(XMLSerializer writer)
	{
	    writer.push("file");
	    super.serializeXML(writer);
	    
	    identifier.serializeXML(writer);
	    writer.pop();
	}

    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        throw new RuntimeException("Cannot read xml");
    }

    public void writeData(DataOutputStream out) throws IOException
    {
        throw new IOException("Cannot write old datastream");
    }
    
    public void readData(DataInputStream in) throws IOException
    {
        // reading the old format.
        super.readData(in);
        RevisionIdentifier identifier = new RevisionIdentifier();
        identifier.setSize(in.readLong());
        identifier.setBackedupSize(in.readLong());
        identifier.readData(in);
        setIdentifier(identifier);
    }

    public void archive(ArchiveOutputStream output) throws IOException
    {
        super.archive(output);
        output.writeClassVersion("com.toubassi.filebunker.vault.FileRevision", 1);
        output.writeObject(identifier, Archivable.StrictlyTypedReference);
    }
    
    public void unarchive(ArchiveInputStream input) throws IOException
    {
        super.unarchive(input);
        input.readClassVersion("com.toubassi.filebunker.vault.FileRevision");
        setIdentifier((RevisionIdentifier)input.readObject(Archivable.StrictlyTypedReference, RevisionIdentifier.class));
    }
}

