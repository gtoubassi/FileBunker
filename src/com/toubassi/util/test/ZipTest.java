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
package com.toubassi.util.test;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.GZIPOutputStream;

/**
 * @author garrick
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ZipTest
{

	public static void main(String[] args)
	{
		if (args.length != 1) {
			System.err.println("usage: ZipTest file");
			System.exit(1);
		}
		
		String file = args[0];
		String zippedFile = file + ".gz";
		
		ZipEntry entry = new ZipEntry(file);
		try {
			FileInputStream fileIn = new FileInputStream(file);
			FileOutputStream fileOut = new FileOutputStream(zippedFile);
			GZIPOutputStream zipOut = new GZIPOutputStream(fileOut);
			
			//zipOut.putNextEntry(entry);
			
			byte buf[] = new byte[1024];
			int numRead;
			
			while ((numRead = fileIn.read(buf)) > -1) {
				zipOut.write(buf, 0, numRead);
			}

			zipOut.close();
			fileIn.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
