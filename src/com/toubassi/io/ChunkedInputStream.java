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
 * Created on Jul 2, 2004
 */
package com.toubassi.io;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PushbackInputStream;

/**
 * @author garrick
 */
public class ChunkedInputStream extends FilterInputStream
{
	private int chunkSize;
	private int numReadThisChunk;
	private long totalRead;
	private PushbackInputStream pushbackStream;

	public ChunkedInputStream(InputStream inputStream, int chunkSize)
	{
		super(new PushbackInputStream(inputStream, 1));
		pushbackStream = (PushbackInputStream)in;
		this.chunkSize = chunkSize;
	}
	
	public int read() throws IOException
	{
		if (numReadThisChunk >= chunkSize) {
			return -1;
		}
		int retVal = in.read();
		if (retVal > 0) {
			numReadThisChunk++;
			totalRead++;
		}
		return retVal;
	}
	
	public int read(byte[] buf, int off, int len) throws IOException
	{
		if (numReadThisChunk >= chunkSize) {
			return -1;
		}
		if (len > chunkSize - numReadThisChunk) {
			len = chunkSize - numReadThisChunk;
		}
		int numRead = in.read(buf, off, len);
		if (numRead > 0) {
			numReadThisChunk += numRead;
			totalRead += numRead;
		}
		return numRead;
	}
	
	public int available() throws IOException
	{
		if (numReadThisChunk >= chunkSize) {
			return 0;
		}
		int avail = in.available();
		if (avail > chunkSize - numReadThisChunk) {
			avail = chunkSize - numReadThisChunk;
		}		
		return avail;
	}
	
	public void close()
	{
	}
	
	public boolean hasMoreChunks() throws IOException
	{
		int b = pushbackStream.read();
		boolean hasMore = b >= 0;
		pushbackStream.unread(b);
		return hasMore;
	}
	
	public void nextChunk()
	{
		numReadThisChunk = 0;
	}
	
	public void reallyClose() throws IOException
	{
		in.close();
	}
	
	public long totalBytesRead()
	{
	    return totalRead;
	}
	
	public int bytesReadThisChunk()
	{
	    return numReadThisChunk;
	}
}
