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
import java.io.Serializable;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author garrick
 */
public class Revision implements XMLSerializable, Serializable
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
        if ("date".equals(container)) {
    	    synchronized (dateFormat) {
    			Date date = (Date)dateFormat.parseObject(value, new ParsePosition(0));
    			setDate(date);            
    	    }
        }
        return null;
    }

    public void writeData(DataOutputStream out) throws IOException
    {
        out.writeLong(date.getTime());
    }
    
    public void readData(DataInputStream in) throws IOException
    {
        date = new Date(in.readLong());
    }
}
