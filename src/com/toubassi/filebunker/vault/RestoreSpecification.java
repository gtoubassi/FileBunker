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
 * Created on Aug 23, 2004
 */
package com.toubassi.filebunker.vault;

import com.toubassi.util.MultiIterator;
import com.toubassi.util.Platform;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 * @author garrick
 */
public class RestoreSpecification
{
    private ArrayList revisions = new ArrayList();
    private ArrayList dates = new ArrayList();
    
    public void add(File file, Date date)
    {
        if (!containsFile(file)) {
            revisions.add(file);
            dates.add(date);
        }
    }
    
    public void add(FileRevision revision)
    {
        if (!containsFile(revision.node().file())) {
            revisions.add(revision);
            dates.add(null);
        }
    }
    
    public void add(DirectoryRevision revision, Date date)
    {
        if (!containsFile(revision.node().file())) {
            revisions.add(revision);
            dates.add(date);
        }
    }
    
    public ArrayList revisions()
    {
        return revisions;
    }
    
    public void clear()
    {
        revisions.clear();
        dates.clear();
    }
    
    public Iterator fileRevisions(Vault vault)
    {
        MultiIterator iterator = new MultiIterator();
        
        for (int i = 0, count = revisions.size(); i < count; i++) {
            Date date = (Date)dates.get(i);
            Revision revision = (Revision)revisions.get(i);
            
            if (revision.isDirectory()) {
                DirectoryRevision directoryRevision = (DirectoryRevision)revision;
                iterator.add(directoryRevision.descendantFileRevisions(date));
            }
            else {
                iterator.add(revision);
            }
        }
        
        return iterator;
    }
    
    public File restorePathForRevision(FileRevision revision, File restoreRoot)
    {
        // commonPath will either be empty, or will not include the trailing slash,
        // which we depend on below.
        String commonPath = commonPath();
        int commonPathLength = commonPath.length();
        
        File file = revision.node().file();

        String partialPath = file.getPath().substring(commonPathLength);
        StringBuffer buffer = new StringBuffer();

        buffer.append(restoreRoot.getPath());
        if (!partialPath.startsWith(File.separator)) {
            buffer.append(File.separatorChar);
        }
        
        buffer.append(partialPath);
        
        // On windows if we end up with a drive letter in the name (which
        // can happen if we try to restore C:\ and D:\ simultaneously)
        // then we will have a colon in the name, which is not legal in windows
        if (Platform.isWindows()) {
            int firstSlash = buffer.indexOf(File.separator);
	        int colon = buffer.indexOf(":");
	        if (colon > -1 && firstSlash > -1 && colon > firstSlash) {
	            buffer.delete(colon, colon + 1);
	        }
        }
        
        return new File(buffer.toString());
    }
    
    public FileRevision findFileRevision(Vault vault, File file)
    {
        String path = file.getPath();
        for (int i = 0, count = revisions.size(); i < count; i++) {
            Revision revision = (Revision)revisions.get(i);
            
            if (revision.isDirectory()) {
                String dirPath = revision.node().file().getPath();
                
                if (path.startsWith(dirPath) && path.charAt(dirPath.length()) == File.separatorChar) {
                    Revision fileRevision = vault.findRevision(file, (Date)dates.get(i));

                    if (fileRevision.isDirectory()) {
                        // Shouldn't happen
                        return null;
                    }
                    return (FileRevision)fileRevision;
                }
            }
            else {
	            if (revision.node().file().equals(file)) {
	                return (FileRevision)revision;
	            }
            }
        }
        return null;
    }
    
    public String commonPath()
    {
        if (revisions.size() == 0) {
            return "";
        }
        
        String prefix = fileAt(0).getPath();

        for (int i = 1, count = revisions.size(); i < count; i++) {
            String path = fileAt(i).getPath();
            
            int prefixLen = prefix.length();
            int pathLen = path.length();
            int len = prefixLen > pathLen ? pathLen : prefixLen;
            
            int j;
            for (j = 0; j < len && prefix.charAt(i) == path.charAt(i); j++)
                ;
            
            // Find the last slash
            int lastSlash = prefix.lastIndexOf(File.separatorChar, j);
            if (lastSlash == -1) {
                return "";
            }
            prefix = prefix.substring(0, lastSlash);
        }
        
        return prefix;
    }
    
    private boolean containsFile(File file)
    {
        String filePath = file.getPath();

        for (int i = 0, count = revisions.size(); i < count; i++) {
            File targetFile = fileAt(i);
            
            String targetPath = targetFile.getPath();
            
            if (filePath.equals(targetPath)) {
                return true;
            }
            if (filePath.startsWith(targetPath) && filePath.charAt(targetPath.length()) == File.separatorChar) {
                return true;
            }
        }
        return false;
    }
    
    private File fileAt(int i)
    {
        Revision revision = (Revision)revisions.get(i);
        return revision.node().file();                
    }
}


