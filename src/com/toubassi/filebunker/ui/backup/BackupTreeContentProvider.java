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
 * Created on Aug 3, 2004
 */
package com.toubassi.filebunker.ui.backup;

import java.io.File;

import org.eclipse.jface.viewers.Viewer;

import com.toubassi.filebunker.vault.BackupSpecification;
import com.toubassi.jface.ICheckboxTreeContentProvider;
import com.toubassi.util.Platform;

/**
 * @author garrick
 */
public class BackupTreeContentProvider implements ICheckboxTreeContentProvider
{
    private static final Object[] emptyObjectArray = new Object[0];
    
    private File[] roots;
    private BackupSpecification backupSpec;
   
    public BackupTreeContentProvider(BackupSpecification backupSpec)
    {
        this.backupSpec = backupSpec;        
    }
    
    public void setRoots(File[] roots)
    {
        this.roots = roots;
    }
    
    private static File[] pruneHiddenFiles(File[] files)
    {
        int numHiddenFiles = 0;
        int numFiles = files.length;
        
        for (int i = 0; i < numFiles; i++) {
            if (files[i].isHidden()) {
                numHiddenFiles++;
            }
        }
        
        if (numHiddenFiles == 0) {
            return files;
        }
        
        File[] prunedFiles = new File[numFiles - numHiddenFiles];
        
        for (int i = 0, pruned = 0; i < numFiles; i++) {
            if (files[i].isHidden()) {
                continue;
            }
            prunedFiles[pruned++] = files[i];
        }
        
        return prunedFiles;
    }
    
    public Object[] getChildren(Object element)
    {
        File file = (File)element;
        File[] children = file.listFiles();        
        children = pruneHiddenFiles(children);
        return children == null ? emptyObjectArray : children;
    }
    
    public Object[] getElements(Object element)
    {
    	if (!Platform.isWindows()) {
    		// On linux if we have 1 root, we return its contents
    		// as the roots, so we don't end up with dummy / node
    		// at the top.
    		if (roots != null && roots.length == 1) {
    			return pruneHiddenFiles(roots[0].listFiles());
    		}
    	}
        return roots == null ? emptyObjectArray : roots;
    }
    
    public boolean hasChildren(Object element)
    {
        for (int i = 0, count = roots.length; i < count; i++) {
            if (roots[i].equals(element)) {
                return true;
            }
        }
        File file = (File)element;
        return file.isDirectory();
    }
    
    public Object getParent(Object element)
    {
        for (int i = 0, count = roots.length; i < count; i++) {
            if (roots[i].equals(element)) {
                return null;
            }
        }
        return ((File)element).getParentFile();
    }
    
    public boolean isChecked(Object element)
    {
        return backupSpec.containsFile((File)element);
    }

    public void setCheckedState(Object element, boolean checked)
    {
        File file = (File)element;
        
        if (checked) {
            backupSpec.addFile(file);
        }
        else {
            backupSpec.removeFile(file);            
        }
    }

    public void dispose()
    {
    }
    
    public void inputChanged(Viewer viewer, Object old_input, Object new_input)
    {
    }
}
