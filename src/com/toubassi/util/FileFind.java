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
package com.toubassi.util;

import java.io.File;

/**
 * @author garrick
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FileFind
{
	private FileFindDelegate delegate;
	private boolean ignoreHiddenFiles;
	
	public boolean ignoreHiddenFiles()
	{
	    return ignoreHiddenFiles;
	}
	
	public void setIgnoreHiddenFiles(boolean flag)
	{
	    ignoreHiddenFiles = flag;
	}

	public void setDelegate(FileFindDelegate findDelegate)
	{
		delegate = findDelegate;
	}
	
	public FileFindDelegate delegate()
	{
		return delegate;
	}
	
	public boolean find(String file)
	{
		return find(new File(file));
	}

	/**
	 * Returns true if the operation was aborted prematurely by the delegate
	 * (returning false from processFile)
	 */
	public boolean find(File file)
	{
		if (file.isFile()) {
		    
		    if (ignoreHiddenFiles && file.isHidden()) {
		        return false;
		    }
		    
			boolean continueFileScan = delegate.processFile(file);
			
			if (!continueFileScan) {
			    return true;
			}
		}
		else if (delegate.shouldRecurseIntoDirectory(file)){
			String children[] = file.list();
			
			if (children != null) {
				for (int i = 0; i < children.length; i++) {
					if (find(new File(file, children[i]))) {
					    return true;
					}
				}
				
				delegate.didProcessDirectoryContents(file, children);
			}
		}
		
		return false;
	}
}
