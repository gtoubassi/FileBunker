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
package com.toubassi.io.test;

import com.toubassi.io.OutputStreamFilteredInputStream;

import java.io.ByteArrayInputStream;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author garrick
 */
public class OutputStreamFilteredInputStreamTest
{
	public static void main(String[] args) throws Exception
	{
		byte data[] = new byte[10*1024];
		byte input[] = new byte[10*1024];
		Random random = new Random();
		random.nextBytes(data);
		
		for (int i = 10; i < data.length; i++) {
			if (i % 250 == 0) {
				System.out.println("" + i);
			}

			ByteArrayInputStream in = new ByteArrayInputStream(data, 0, i);
			OutputStreamFilteredInputStream stream = new OutputStreamFilteredInputStream(in);
			GZIPOutputStream gzipOut = new GZIPOutputStream(stream.finalOutputStream());
			stream.setFilterStream(gzipOut);
			GZIPInputStream gzipIn = new GZIPInputStream(stream);

			int totalRead = 0;
			while (totalRead < i) {
				int numToRead = (int)(random.nextDouble() * 128);
				if (numToRead > i - totalRead) {
					numToRead = i - totalRead;
				}
				
				int numRead = gzipIn.read(input, totalRead, i - totalRead);
				if (numRead == -1) {
				}
				
				totalRead += numRead;
			}
			for (int j = 0; j < i; j++) {
				if (data[j] != input[j]) {
					System.err.println("Failure: Bad value at index: " + j);
				}
			}
		}		
	}
}
  