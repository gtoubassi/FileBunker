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
package com.toubassi.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Stack;

/**
 * @author garrick
 */
public class XMLSerializer extends PrintWriter
{
	private int indent;
	private int indentIncrement;
	private Stack containers = new Stack();
	private static String spaces = "                ";
	private static HashMap userData;
	
	public static void save(File file, XMLSerializable serializable) throws IOException
	{
	    XMLSerializer serializer = null;

	    try {
	        serializer = new XMLSerializer(file);
	        serializable.serializeXML(serializer);
	    }
	    finally {
	        if (serializer != null) {
	            serializer.close();
	        }
	    }
	}

	public XMLSerializer(File file) throws FileNotFoundException
	{
	    this(new BufferedOutputStream(new FileOutputStream(file)));
	}

	public XMLSerializer(OutputStream out)
	{
		super(out);
		indentIncrement = 2;
	}
	
	public Object getUserData(String key)
	{
	    return userData == null ? null : userData.get(key);
	}
	
	public void putUserData(String key, Object object)
	{
	    if (userData == null) {
	        userData = new HashMap();
	    }
	    userData.put(key, object);
	}

	public void setIndentIncrement(int indentIncrement)
	{
		this.indentIncrement = indentIncrement;
	}
	
	private void printIndenting()
	{
		int i = indent;
		int spaceLength = spaces.length();
		
		while (i > 0) {
			if (i > spaceLength) {
				write(spaces);
				i -= spaceLength;
			}
			else {
				write(spaces, 0, i);
				break;
			}
		}
	}
	
	public void push(String container)
	{
		printIndenting();
		indent += indentIncrement;
		write('<');
		write(container);
		write(">\n");
		containers.push(container);
	}
	
	public void pop()
	{
		String container = (String)containers.pop();
		indent -= indentIncrement;
		printIndenting();
		write("</");
		write(container);
		write(">\n");
	}
	
	public void write(String container, Object value)
	{
		printIndenting();
		write('<');
		write(container);
		write('>');
		
		write(value == null ? "" : value.toString());
		write("</");
		write(container);
		write(">\n");
	}
}
