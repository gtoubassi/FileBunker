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
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.toubassi.io;

import java.io.OutputStream;

/**
 * @author garrick
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ReadableByteArrayOutputStream extends OutputStream
{
	byte buf[];
	int start;
	int end;
	boolean closed;
	
	public ReadableByteArrayOutputStream()
	{
		buf = new byte[1024];		
	}
	
	public int size()
	{
		if (end >= start) {
			return end - start;
		}
		return end + (buf.length - start);
	}
	
	public void copyBytes(byte dest[], int off, int len)
	{
		int size = size();
		if (len > size) {
			throw new IllegalArgumentException("Attempt to copyBytes too many bytes");
		}
		
		if (end >= start || len <= buf.length - start) {
			System.arraycopy(buf, start, dest, off, len);
		}
		else {
			System.arraycopy(buf, start, dest, off, buf.length - start);
			System.arraycopy(buf, 0, dest, off + buf.length - start, len - (buf.length - start));
		}
	}
	
	protected void grow()
	{
		byte newBuffer[] = new byte[buf.length * 2];
		int size = size();
		copyBytes(newBuffer, 0, size);
		buf = newBuffer;
		start = 0;
		end = size;		
	}
	
	protected void growIfNecessary(int size)
	{
		// Note we never allow the buffer to be filled exactly to its
		// capacity because we can't distinguish between a totally
		// full buffer and an empty one (in both cases start == end).
		// To avoid this we don't allow a totally full buffer. 
		while (size() + size >= buf.length) {
			grow();
		}
	}
	
	public void write(int b)
	{
		if (closed) {
			throw new IllegalStateException("Attempt to write to closed stream");
		}

		growIfNecessary(1);
		if (end < buf.length) {
			// No need to wrap
			buf[end] = (byte)b;
			end++;
		}
		else {
			// We are wrapping around
			if (start == 0) {
				// We should have capacity due to the call to growIfNecessary but
				// we are wrapping around and the start is at 0, so we don't!
				throw new IllegalStateException("Unexpected lack of capacity");
			}
			buf[0] = (byte)b;
			end = 1;
		}
	}

	public void write(byte b[], int off, int len)
	{
		if (closed) {
			throw new IllegalStateException("Attempt to write to closed stream");
		}
		
		growIfNecessary(len);
		if (end < start || buf.length - end >= len) {
			System.arraycopy(b, off, buf, end, len);
			end += len;
		}
		else {
			System.arraycopy(b, off, buf, end, buf.length - end);
			off += buf.length - end;
			len -= (buf.length - end);
			System.arraycopy(b, off, buf, 0, len);
			end = len;
		}
	}
	
	public void close()
	{
		closed = true;
	}
	
	public int read(byte b[], int off, int len)
	{
		int size = size();
		int numToRead = len > size ? size : len;
		
		if (numToRead > 0) {
			copyBytes(b, off, numToRead);
			if (end >= start || len <= buf.length - start) {
				start += numToRead;
			}
			else {
				start = numToRead - (buf.length - start);
			}
		}
		
		return numToRead == 0 && closed ? -1 : numToRead;
	}
}
