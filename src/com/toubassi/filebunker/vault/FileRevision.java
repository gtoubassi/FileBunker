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
    private long size;
    private long backedupSize;

    public FileRevision()
    {
    }

    public void setIdentifier(RevisionIdentifier identifier)
    {
        this.identifier = identifier;
    }

    public RevisionIdentifier identifier()
    {
        return identifier;
    }
    
    public void setSize(long size)
    {
        this.size = size;
        if (node != null) {
            node.invalidateSizes();
        }
    }

    public long size()
    {
        return size;
    }

    public synchronized void setBackedupSize(long size)
    {
        backedupSize = size;
        if (node != null) {
            node.invalidateSizes();
        }
    }

    public long backedupSize()
    {
        return backedupSize;
    }
    
    /**
     * The ratio of backupSize to size.  
     * @return
     */
    public float backedupSizeRatio()
    {
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
		writer.write("size", Long.toString(size));
		writer.write("backedupSize", Long.toString(backedupSize));
	    writer.pop();
	}

    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        if ("identifier".equals(container)) {
            setIdentifier(new RevisionIdentifier());
            return identifier;
        }
        else if ("size".equals(container)) {
            setSize(Long.parseLong(value));
        }
        else if ("backedupSize".equals(container)) {
            setBackedupSize(Long.parseLong(value));
        }
        else {
            super.deserializeXML(deserializer, container, value);            
        }
        return null;
    }

    public void writeData(DataOutputStream out) throws IOException
    {
        super.writeData(out);
        out.writeLong(size);
        out.writeLong(backedupSize);
        identifier.writeData(out);
    }
    
    public void readData(DataInputStream in) throws IOException
    {
        super.readData(in);
        size = in.readLong();
        backedupSize = in.readLong();
        RevisionIdentifier identifier = new RevisionIdentifier();
        identifier.readData(in);
        setIdentifier(identifier);
    }
}

