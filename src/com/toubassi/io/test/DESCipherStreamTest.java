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

import com.toubassi.io.DESCipherInputStream;
import com.toubassi.io.DESCipherOutputStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * @author garrick
 */
public class DESCipherStreamTest
{
	public static void main(String[] args) throws Exception
	{
		if (args.length != 4 || !args[1].equals("-pw")) {
			System.err.println("usage: DESCipherStreamTest [-e|-d] -pw passPhrase file");
			System.exit(1);
		}
		
		boolean encode = args[0].equals("-e");
		String passPhrase = args[2];
		String file = args[3];
		String outFile;
		if (encode) {
			outFile = file + ".crypt";
		}
		else {
			if (file.endsWith(".crypt")) {
				outFile = file.substring(0, file.length() - 6);
			}
			else {
				outFile = file + ".decrypt";
			}
		}
		
		FileInputStream inStream = new FileInputStream(file);
		FileOutputStream outStream = new FileOutputStream(outFile);
		byte buf[] = new byte[1024];
		
		if (encode) {
			DESCipherOutputStream out = new DESCipherOutputStream(outStream, passPhrase);
			int numRead = 0;
			while ((numRead = inStream.read(buf)) != -1) {
				out.write(buf, 0, numRead);
			}
			out.close();
			inStream.close();
		}
		else {
			DESCipherInputStream in = new DESCipherInputStream(inStream, passPhrase);
			int numRead = 0;
			while ((numRead = in.read(buf)) != -1) {
				outStream.write(buf, 0, numRead);
			}
			outStream.close();
			in.close();
		}
	}
}
