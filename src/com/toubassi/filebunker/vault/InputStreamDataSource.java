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
 * Created on Jul 3, 2004
 */
package com.toubassi.filebunker.vault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.activation.DataSource;

/**
 * @author garrick
 *
 * Note it is important that the message part that uses this DataSource
 * have its "Content-Transfer-Encoding" type set so that the getInputStream
 * method is called only once (since we are stream based we only have one
 * shot at it).  Otherwise the mime machinery will read the stream once just
 * to figure out an encoding.  For safety, set this header to "base64".
 */
public class InputStreamDataSource implements DataSource
{
	private InputStream inputStream;
	private boolean hasReturnedInputStream;
	private String name;
	
	public InputStreamDataSource(String name, InputStream inputStream)
	{
		this.inputStream = inputStream;
		this.name = name;
	}
	
	public InputStream getInputStream() throws IOException
	{
		if (hasReturnedInputStream) {
			throw new IllegalStateException("Can't call getInputStream more than once!");
		}
		hasReturnedInputStream = true;

		return inputStream;		
	}
	
	public OutputStream getOutputStream() throws IOException
	{
		throw new IOException("Cannot get an OutputStream for a InputStreamDataSource");
	}

	public String getContentType()
	{
		return "application/octet-stream";
	}
	
	public String getName()
	{
		return name;
	}
}
