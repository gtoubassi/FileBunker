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
 * Created on Jul 1, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.toubassi.io;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * @author garrick
 */
public class OutputStreamFilteredInputStream extends FilterInputStream
{
	private OutputStream filter;
	private ReadableByteArrayOutputStream byteArrayOut;
	private byte chunkBuffer[] = new byte[1024];
	private byte singleByteBuffer[] = new byte[1];
	
	public OutputStreamFilteredInputStream(InputStream in)
	{
		super(in);
		byteArrayOut = new ReadableByteArrayOutputStream();
	}
	
	public OutputStream finalOutputStream()
	{
		return byteArrayOut;
	}
	
	/**
	 * We assume filter is wired directly or indirectly to finalOutputStream.
	 * @param filter
	 */
	public void setFilterStream(OutputStream filterStream)
	{
		filter = filterStream;
	}
	
	public int read() throws IOException
	{	
		int retVal = read(singleByteBuffer, 0, 1);
		if (retVal == 1) {
			int i = singleByteBuffer[0];
			int j = i < 0 ? 256 + i : i;
			return j;
		}
		return retVal;
	}
	
	public int read(byte[] b, int off, int len) throws IOException
	{
		int totalRead = 0;
		do {
			int numRead = byteArrayOut.read(b, off + totalRead, len - totalRead);
			
			if (numRead == -1) {
				if (totalRead == 0) {
					return -1;
				}
				return totalRead;
			}
			else if (numRead == 0) {
				processChunk();
			}
			else {
				totalRead += numRead;
			}
		}
		while (totalRead < len);
		return totalRead;
	}
	
	public void processChunk() throws IOException
	{
		int numRead = in.read(chunkBuffer);
		if (numRead == -1) {
			filter.close();
		}
		else if (numRead != 0) {
			filter.write(chunkBuffer, 0, numRead);
		}
	}
}
