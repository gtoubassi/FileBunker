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

import com.toubassi.io.ChunkedInputStream;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Random;

/**
 * @author garrick
 */
public class ChunkedInputStreamTest
{
	public static void main(String[] args) throws Exception
	{
		byte data[] = new byte[10*1024];
		byte input[] = new byte[data.length];
		Random random = new Random();
		random.nextBytes(data);
		
		ChunkedInputStream chunkIn = new ChunkedInputStream(new ByteArrayInputStream(data), 123);
		
		int totalRead = 0;
		int numReadThisChunk = 0;
		while (totalRead < data.length) {
			int numToRead = input.length - totalRead < 17 ? input.length - totalRead : 17;
			int numRead = chunkIn.read(input, totalRead, numToRead);
			if (numRead == -1) {
				if (numReadThisChunk != 123) {
					throw new RuntimeException("Should have more chunks");
				}
				if (!chunkIn.hasMoreChunks()) {
					throw new RuntimeException("Should have more chunks");
				}
				System.out.println("Successfully read a chunk");
				chunkIn.nextChunk();
				numReadThisChunk = 0;
			}
			else {
				numReadThisChunk += numRead;
				totalRead += numRead;
			}
		}
		
		if (!Arrays.equals(data, input)) {
			throw new RuntimeException("Input data doesn't match!");
		}
	}
}
