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
 * Created on Jul 4, 2004
 */
package com.toubassi.io.test;

import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializer;

import java.io.FileInputStream;

/**
 * @author garrick
 */
public class XMLSerializationTest implements XMLSerializable
{
	public void serializeXML(XMLSerializer serializer)
	{
	}

	public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
	{
		if (value != null) {
			System.out.println(container + " = " + value);
		}
		else {
			System.out.println(container);
		}		
		return null;
	}
	
	public static void main(String[] args) throws Exception
	{
		String file = "test.xml";
		
		FileInputStream in = new FileInputStream(file);
		XMLDeserializer deserializer = new XMLDeserializer(in);
		deserializer.parse(new XMLSerializationTest());
		deserializer.close();
	}
}
