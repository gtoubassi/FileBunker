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
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author garrick
 */
public class Revision implements XMLSerializable, Serializable, Archivable
{
    protected Node node;
    private Date date;

	private static final SimpleDateFormat dateFormat =
		new SimpleDateFormat("d MMM yyyy HH:mm:ss:S z");//"MM/dd/yyyy hh:mm:ss a, z");
    
	public Revision()
    {
    }
    
    public void setNode(Node node)
    {
        this.node = node;
    }

    public Node node()
    {
        return node;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }
    
    public Date date()
    {
        return date;
    }

    public boolean isDirectory()
    {
        return false;
    }

    public Revision previousRevision()
    {
        return node.previousRevision(this);
    }

    public Revision nextRevision()
    {
        return node.nextRevision(this);
    }    
    
	public void serializeXML(XMLSerializer writer)
	{
	    synchronized (dateFormat) {
	        writer.write("date", dateFormat.format(date));
	    }
	}

    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        throw new RuntimeException("Can't read xml");
    }

    public void writeData(DataOutputStream out) throws IOException
    {
        throw new RuntimeException("Can't write legacy datastream");
    }
    
    // This is used to uniqe dates that we read from old archives.
    // Since the readData code path is obsolete we don't need to
    // worry about cleaning up this Map.
    private static Map uniqueDates = new TreeMap();
    
    public void readData(DataInputStream in) throws IOException
    {
        Date tempDate = new Date(in.readLong());
        date = (Date)uniqueDates.get(tempDate);
        if (date == null) {
            date = tempDate;
            uniqueDates.put(date, date);
        }
    }

    public void archive(ArchiveOutputStream output) throws IOException
    {
        output.writeClassVersion("com.toubassi.filebunker.vault.Revision", 1);
        output.writeObject(date, Archivable.StrictlyTypedReference);
    }
    
    public void unarchive(ArchiveInputStream input) throws IOException
    {
        input.readClassVersion("com.toubassi.filebunker.vault.Revision");
        date = (Date)input.readObject(Archivable.StrictlyTypedReference, Date.class);
    }
}
