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

import com.toubassi.util.FileFind;
import com.toubassi.util.FileFindDelegate;

import java.io.File;

/**
 * @author garrick
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FileFindTest implements FileFindDelegate
{
	private long totalSize;
	
	public boolean shouldRecurseIntoDirectory(File directory)
	{
		return true;
	}
	
	public boolean processFile(File file)
	{
		long length = file.length();
		totalSize += length;
		System.out.println(file.getPath() + " " + length);
		return true;
	}

	public void didProcessDirectoryContents(File directory, String children[])
	{
	}	

	public static void main(String[] args)
	{
		if (args.length != 1) {
			System.err.println("usage: FileFindTest <path>");
			System.exit(1);
		}
		
		FileFindTest test = new FileFindTest();
		FileFind find = new FileFind();
		
		find.setDelegate(test);
		find.find(args[0]);
		System.out.println("Total: " + test.totalSize);
	}
}
