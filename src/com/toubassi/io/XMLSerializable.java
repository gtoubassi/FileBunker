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


/**
 * Interface implemented by those that want to be serializable as
 * XML using the XMLSerializer/XMLDeserializer scheme.
 * 
 * @author garrick
 */
public interface XMLSerializable
{
	public void serializeXML(XMLSerializer serializer);
    
	/**
	 * value may be null indicating that this container is a "proper" container
	 * with subcontainers in it.  For example:
	 * 
	 * 		<container>
	 * 			<value>Hello</value>
	 * 		</container>
	 * 
	 * The call sequence would be handleXML("container", null), followed by
	 * handleXML("value", "Hello").  handleXML should return the
	 * XMLSerializable that will handle the rest of the elements in this
	 * current proper container.  When that proper container's closing
	 * element is encountered than the previous XMLSerializable will be used
	 * to continue procesing tags.  Returning null implies that the current
	 * XMLSerializable should continue to handle tags.
	 */
	public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value);

}
