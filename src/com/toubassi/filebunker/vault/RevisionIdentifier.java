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

import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;
import com.toubassi.util.GUIDGenerator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * @author garrick
 */
public class RevisionIdentifier implements XMLSerializable, Serializable
{
    private String guid;
    private String handlerName;
    
    public static final String guidCharacterClass()
    {
        return GUIDGenerator.guidCharacterClass();
    }
    
    public RevisionIdentifier()
    {
        // Only for deserialization
    }
    
    public RevisionIdentifier(String handlerName)
    {
        guid = GUIDGenerator.sharedInstance().nextGUID();
        this.handlerName = handlerName;
    }
    
    public String handlerName()
    {
        return handlerName;
    }
    
    public String guid()
    {
        return guid;
    }
    
    public String toString()
    {
        return guid + ":" + handlerName;
    }

	public void serializeXML(XMLSerializer writer)
	{
	    writer.push("identifier");
		writer.write("guid", guid);
		writer.write("handler", handlerName);
	    writer.pop();
	}

    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        if ("guid".equals(container)) {
            guid = value;
        }
        else if ("handler".equals(container)) {
            handlerName = value;
        }
        return null;
    }

    public void writeData(DataOutputStream out) throws IOException
    {
        out.writeUTF(guid);
        out.writeUTF(handlerName);
    }
    
    public void readData(DataInputStream in) throws IOException
    {
        guid = in.readUTF();
        handlerName = in.readUTF();
    }
}
