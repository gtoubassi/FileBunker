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
 * Created on Jul 5, 2004
 */
package com.toubassi.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * @author garrick
 */
public class MultipleStreamInputStream extends InputStream
{
	private ArrayList streams = new ArrayList();
	private int streamIndex;
	
	public MultipleStreamInputStream()
	{
	}
	
	public MultipleStreamInputStream(InputStream stream1, InputStream stream2)
	{
		add(stream1);
		add(stream2);
	}
	public MultipleStreamInputStream(InputStream stream1, InputStream stream2, InputStream stream3)
	{
		add(stream1);
		add(stream2);
		add(stream3);
	}
	
	public void add(InputStream stream)
	{
		streams.add(stream);
	}
	
	private InputStream current()
	{
		if (streams.isEmpty()) {
			return null;
		}
		return (InputStream)streams.get(streamIndex);
	}
	
	private boolean nextStream()
	{
		if (streamIndex < streams.size() - 1) {
			streamIndex++;
			return true;
		}
		return false;
	}
	
	public void close() throws IOException
	{
		// Could close them as we use them.
		for (int i = 0, count = streams.size(); i < count; i++) {
			((InputStream)streams.get(i)).close();
		}
	}
	
	public int available() throws IOException
	{
		if (current() != null) {
			return current().available();
		}
		return 0;
	}

	public int read() throws IOException
	{
		if (current() == null) {
			return -1;
		}

		while (true) {
			int b = current().read();
			if (b == -1) {
				if (!nextStream()) {
					return -1;
				}
			}
			else {
				return b;
			}
		}
	}

	public int read(byte[] buf, int off, int len) throws IOException
	{
		if (current() == null) {
			return -1;
		}
		
		while (true) {
			int numRead = current().read(buf, off, len);
			if (numRead == -1) {
				if (!nextStream()) {
					return -1;
				}
			}
			else {
				return numRead;
			}
		}
	}
}
