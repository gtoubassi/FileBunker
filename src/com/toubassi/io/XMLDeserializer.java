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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author garrick
 */
public class XMLDeserializer extends DefaultHandler
{
	private InputStream input;
	private String currentElement;
	private StringBuffer characters;
	private Stack handlers;
	private Stack handlerContainers;
	private HashMap userData;

	public static void parse(File file, XMLSerializable handler) throws IOException
	{
	    XMLDeserializer deserializer = new XMLDeserializer(file);
	    try {
	        deserializer.parse(handler);
	    }
	    finally {
	        deserializer.close();
	    }
	}
	
	public XMLDeserializer(InputStream input)
	{
		this.input = input;
		handlers = new Stack();
		handlerContainers = new Stack();
		characters = new StringBuffer();
	}
	
	public XMLDeserializer(String path) throws FileNotFoundException
	{
	    this(new File(path));
	}
	
	public XMLDeserializer(File file) throws FileNotFoundException
	{
	    this(new BufferedInputStream(new FileInputStream(file)));
	}
	
	public void parse(XMLSerializable handler) throws IOException
	{
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

			handlers.push(handler);
			handlerContainers.push("");
			
			parser.parse(input, this);
		}
		catch (SAXException e) {
			IOException ioException = new IOException();
			ioException.initCause(e);
			throw ioException;
		}
		catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected void finalize()
	{
		try {
			close();
		}
		catch (IOException e) {
		    //swallow
		}
	}
	
	public void close() throws IOException
	{
		if (input != null) {
			input.close();
			input = null;		
		}
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
	
	protected XMLSerializable currentHandler()
	{
		return (XMLSerializable)handlers.peek();
	}
	
	private void handleXML(String container, String value)
	{
	    XMLSerializable currentHandler = currentHandler();
		XMLSerializable newHandler = currentHandler.deserializeXML(this, container, value);
		if (newHandler != null) {
			handlers.push(newHandler);
			handlerContainers.push(container);
		}
	}
	
	public void startElement(String uri,
							 String localName,
							 String qName,
							 Attributes attributes)
							 throws SAXException
	{
		if (currentElement != null) {
			handleXML(currentElement, null);
		}
		currentElement = qName;		
		characters.delete(0, characters.length());
	}
	
	public void endElement(String uri,
						   String localName,
						   String qName)
						   throws SAXException
	{		
		if (currentElement != null) {
			if (!currentElement.equals(qName)) {
				throw new IllegalStateException("Expected tag " + currentElement + " but got " + qName);
			}
			String value = null;
			
			if (characters.length() > 0) {
				value = characters.toString();
			}
			handleXML(qName, value);
			currentElement = null;
		}
		else {
			if (handlerContainers.peek().equals(qName)) {
				handlers.pop();
				handlerContainers.pop();
			}
		}
		characters.delete(0, characters.length());
	}

	public void characters(char[] ch,
						   int start,
						   int length)
						   throws SAXException
	{
		characters.append(ch, start, length);
	}
}
