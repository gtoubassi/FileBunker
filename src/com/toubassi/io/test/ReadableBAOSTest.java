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
package com.toubassi.io.test;

import com.toubassi.io.ReadableByteArrayOutputStream;

import java.util.Random;

/**
 * @author garrick
 */
public class ReadableBAOSTest
{
	public static void main(String[] args)
	{
		int total = 10*1024*1024;
		int maxBlock = 10*1023;
		
		Random random = new Random();
		byte buf[] = new byte[total];
		int bufStart = 0;
		byte input[] = new byte[total];
		int inputStart = 0;
		
		random.nextBytes(buf);
		
		ReadableByteArrayOutputStream rbaos = new ReadableByteArrayOutputStream();
		
		while (true) {
			boolean write = random.nextBoolean();
			int numBytes = (int)(random.nextDouble() * maxBlock);

			if (write) {
				if (numBytes > buf.length - bufStart) {
					numBytes = buf.length - bufStart;
				}
				if (numBytes > 0) {
					rbaos.write(buf, bufStart, numBytes);
					bufStart += numBytes;
				}
				else {
					rbaos.close();
				}
			}
			else {
				if (numBytes > input.length - inputStart) {
					numBytes = input.length - inputStart;
				}
				numBytes = rbaos.read(input, inputStart, numBytes);
				if (numBytes == -1) {
					break;
				}
				inputStart += numBytes;
			}
		}
		if (inputStart != bufStart || inputStart != buf.length) {
			System.err.println("Error in number of bytes read");
		}
		for (int i = 0; i < buf.length; i++) {
			if (input[i] != buf[i]) {				
				System.err.println("Error in data at position: " + i);
				break;
			}
		}
	}
}
