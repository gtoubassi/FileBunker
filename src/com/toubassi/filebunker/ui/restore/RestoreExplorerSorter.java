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
 * Created on Aug 20, 2004
 */
package com.toubassi.filebunker.ui.restore;

import com.toubassi.filebunker.vault.Revision;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * @author garrick
 */
public class RestoreExplorerSorter extends ViewerSorter
{
    public int category(Object element)
    {
        return ((Revision)element).isDirectory() ? 0 : 1;
    }
    
    public int compare(Viewer viewer, Object o1, Object o2)
    {
    	int cat1 = category(o1);
    	int cat2 = category(o2);

    	if (cat1 != cat2) {
    		return cat1 - cat2;    	    
    	}

        String name1 = ((Revision)o1).node().name();
        String name2 = ((Revision)o2).node().name();

    	if (name1 == null) {
    	    return -1;
    	}
    	if (name2 == null) {
    	    return 1;
    	}

    	return collator.compare(name1, name2);
    }
}
