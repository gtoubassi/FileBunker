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
 * Created on Aug 4, 2004
 */
package com.toubassi.filebunker.vault;

import com.subx.common.NotificationCenter;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;
import com.toubassi.util.FileFind;
import com.toubassi.util.FileFindDelegate;
import com.toubassi.util.Glob;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

// Note we avoid using getParent or getParentFile because that creates new
// objects every time.  Maybe not a big deal for add/remove, but for contains,
// which the UI may hammer on, we want to avoid needless consing.

/**
 * @author garrick
 */
public class BackupSpecification implements XMLSerializable
{
    public static final String ChangedNotification = "ChangedNotification";
    
    private ArrayList includedFiles = new ArrayList();
    private ArrayList excludedFiles = new ArrayList();
    private ArrayList excludedFileGlobs = new ArrayList();
    private ArrayList excludedDirectoryGlobs = new ArrayList();
    private long excludedFileSize = 0;
    private boolean isIncremental = true;
    
    public ArrayList roots()
    {
        return includedFiles;
    }
    
    public boolean isEmpty()
    {
        return includedFiles.isEmpty();
    }
    
    public void setIsIncremental(boolean flag)
    {
        isIncremental = flag;
    }
    
    public boolean isIncremental()
    {
        return isIncremental;
    }
    
    public void addFile(File file)
    {
        removePathAndDescendants(file, excludedFiles);

        if (!containsFile(file, false)) {
            removePathAndDescendants(file, includedFiles);        
            includedFiles.add(file);                    
        }
        
        changed();
    }
    
    public void removeFile(File file)
    {
        removePathAndDescendants(file, includedFiles);
        removePathAndDescendants(file, excludedFiles);        

        if (containsFile(file, false)) {
            excludedFiles.add(file);                    
        }
        
        changed();
    }
    
    public boolean containsFile(File file)
    {
        return containsFile(file, true);
    }
    
    public boolean containsFile(File file, boolean applyExclusionFilters)
    {
        File nearestIncludedAncestor = nearestAncestor(file, includedFiles);
        File nearestExcludedAncestor = nearestAncestor(file, excludedFiles);
        
        if (nearestIncludedAncestor != null &&
           (nearestExcludedAncestor == null ||
            nearestIncludedAncestor.getPath().length() > nearestExcludedAncestor.getPath().length()))
        {
            if (!applyExclusionFilters || !isExcluded(file)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList includedFiles()
    {
        return includedFiles;
    }

    public ArrayList excludedFiles()
    {
        return excludedFiles;
    }
    
    public void removeAllExcludedFileGlobs()
    {
        excludedFileGlobs.clear();
    }
    
    public void addExcludedFileGlob(Glob glob)
    {
        excludedFileGlobs.add(glob);
    }
    
    public void removeAllExcludedDirectoryGlobs()
    {
        excludedDirectoryGlobs.clear();
    }
    
    public void addExcludedDirectoryGlob(Glob glob)
    {
        excludedDirectoryGlobs.add(glob);
    }
    
    public ArrayList excludedFileGlobs()
    {
        return excludedFileGlobs;
    }
    
    public ArrayList excludedDirectoryGlobs()
    {
        return excludedDirectoryGlobs;
    }
    
    /**
     * Files whose size is greater than the specified size are
     * skipped.  0 means no files are skipped based on size.
     */
    public void setExcludedFileSize(long size)
    {
        excludedFileSize = size;
    }
    
    public long excludedFileSize()
    {
        return excludedFileSize;
    }
    
    private boolean isExcluded(File file)
    {
        boolean isDirectory = file.isDirectory();
        ArrayList globs = isDirectory ? excludedDirectoryGlobs : excludedFileGlobs;
        String name = file.getName();
        
        for (int i = 0, count = globs.size(); i < count; i++) {
            Glob glob = (Glob)globs.get(i);
                        
            if (glob.matches(name)) {
                return true;
            }
        }
        
        if (!isDirectory && excludedFileSize != 0 && file.length() > excludedFileSize) {
            return true;
        }
        
        return false;
    }
    
    /**
     * The files included in this specification are validated against the actual
     * contents of the file system and pruned accordingly.  For instance if this
     * spec only has "c:/files" in it, and that file no longer exists on disk,
     * a call to validateContents will empty this spec.
     */
    public void validateContents()
    {
        boolean changed = false;

        for (int i = includedFiles.size() - 1; i >= 0; i--) {
            File file = (File)includedFiles.get(i);
            
            if (!file.exists()) {
                changed = true;
                removeFile(file);
            }
        }        

        for (int i = excludedFiles.size() - 1; i >= 0; i--) {
            File file = (File)excludedFiles.get(i);
            
            if (!file.exists()) {
                changed = true;
                excludedFiles.remove(i);
            }
        }        
        
        if (changed) {
            changed();
        }
    }

    public void find(BackupDatabase backupdb, FileFindDelegate delegate) throws OperationCanceledVaultException
    {
        find(backupdb, delegate, null);
    }
    
    public void find(BackupDatabase backupdb, FileFindDelegate delegate, FileOperationListener listener) throws OperationCanceledVaultException
    {
        FileFind find = new FileFind();
        
        find.setIgnoreHiddenFiles(true);
        find.setDelegate(new BackupSpecificationFileFindDelegate(this, backupdb, delegate, listener));
        
        for (int i = 0, count = includedFiles.size(); i < count; i++) {
            File file = (File)includedFiles.get(i);
            
            if (find.find(file)) {
                throw new OperationCanceledVaultException();
            }
        }
    }

    /**
     * Returns all files in the excludedFiles list which are explicitly used
     * to exclude files from the specified file, which is assumed to be a
     * member of includedFiles.
     */
    public void findFilesExplicitlyExcludedFrom(File file, ArrayList excluded)
    {
        excluded.clear();

        for (int i = 0, count = excludedFiles.size(); i < count; i++) {
            File excludedFile = (File)excludedFiles.get(i);
            
            if (nearestAncestor(excludedFile, includedFiles) == file) {
                excluded.add(excludedFile);
            }
        }
    }

    public void save(File file) throws VaultException
    {
        try {
	        XMLSerializer writer = new XMLSerializer(file);
	        serializeXML(writer);
	        writer.close();
	    } catch (Exception e) {
	        throw new VaultException(e);
	    }
    }
    
    public void serializeXML(XMLSerializer writer)
    {
        writer.push("BackupSpecification");
        writer.write("incremental", Boolean.toString(isIncremental));
        
        for (int i = 0; i < includedFiles.size(); i++) {
            File file = (File)includedFiles.get(i);
            writer.write("included", file.getPath());
        }
        for (int i = 0; i < excludedFiles.size(); i++) {
            File file = (File)excludedFiles.get(i);
            writer.write("excluded", file.getPath());            
        }
        
        for (int i = 0; i < excludedFileGlobs.size(); i++) {
            Glob glob = (Glob)excludedFileGlobs.get(i);
            writer.write("excludedFileGlob", glob.globExpression());            
        }

        for (int i = 0; i < excludedDirectoryGlobs.size(); i++) {
            Glob glob = (Glob)excludedDirectoryGlobs.get(i);
            writer.write("excludedDirectoryGlob", glob.globExpression());            
        }

        if (excludedFileSize > 0) {
            writer.write("excludedFileSize", Long.toString(excludedFileSize));
        }
        
        writer.pop();
    }
    
    public boolean load(File file) throws IOException
    {
        if (file.exists()) {
            XMLDeserializer.parse(file, this);
            return true;
        }
        return false;
    }

    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        if ("incremental".equals(container)) {
            isIncremental = Boolean.valueOf(value).booleanValue();
        }
        else if ("included".equals(container)) {
            includedFiles.add(new File(value));
        }
        else if ("excluded".equals(container)) {
            excludedFiles.add(new File(value));
        }
        else if ("excludedFileGlob".equals(container)) {
            excludedFileGlobs.add(new Glob(value));
        }
        else if ("excludedDirectoryGlob".equals(container)) {
            excludedDirectoryGlobs.add(new Glob(value));
        }
        else if ("excludedFileSize".equals(container)) {
            excludedFileSize = Long.parseLong(value);
        }
        return null;
    }

    private void changed()
    {
        NotificationCenter.sharedCenter().post(ChangedNotification, this, null);        
    }
    
    public String toString()
    {
        return "Included: " + includedFiles + "\nExcluded: " + excludedFiles;
    }

    /**
     * Returns the File from files that is either the same as the specified file,
     * or the nearest ancestor. /foo/bar is a nearer ancestor to /foo/bar/baz
     * than /foo.
     */
    public static File nearestAncestor(File file, ArrayList files)
    {
        String path = file.getPath();
        int length = path.length();
        File nearest = null;
        
        for (int i = 0, count = files.size(); i < count; i++) {
            File existingFile = (File)files.get(i);
            String existingPath = existingFile.getPath();
            
            // Now see if it actually is an ancestor
            if (path.startsWith(existingPath)) {
                int existingPathLength = existingPath.length();

                if (length == existingPathLength) {
                    // Exact match, no need to look further
                    return existingFile;
                }

                // Verify that this is a true descendant, not just a directory
                // with a common prefix.
                if (existingPath.endsWith(File.separator) || path.charAt(existingPathLength) == File.separatorChar) {
                    
                    // See if this is a nearer ancestor than the previous best (if we have one)
                    if (nearest == null || nearest.getPath().length() < existingPath.length()) {
                        nearest = existingFile;                        
                    }
                }
            }
        }
        return nearest;
    }

    /**
     * Removes all files in the collection that are the same as, or descendants
     * of the specified file.
     */
    private static void removePathAndDescendants(File file, ArrayList files)
    {
        String path = file.getPath();
        int length = path.length();
        Iterator iterator = files.iterator();
        
        while (iterator.hasNext()) {            
            File existingFile = (File)iterator.next();
            String existingPath = existingFile.getPath();
            
            if (existingPath.startsWith(path)) {
                int existingPathLength = existingPath.length();

                if (length == existingPathLength) {
                    iterator.remove();
                }
                else {
                
	                // Verify that this is a true descendant, not just a directory
	                // with a common prefix.
	                if (path.endsWith(File.separator) || existingPath.charAt(length) == File.separatorChar) {
	                    iterator.remove();
	                }
                }
            }
        }
    }
}

class BackupSpecificationFileFindDelegate implements FileFindDelegate
{
    private BackupSpecification spec;
    private BackupDatabase backupdb;
    private FileFindDelegate delegate;
    private FileOperationListener listener;
    
    public BackupSpecificationFileFindDelegate(BackupSpecification spec,
            BackupDatabase backupdb, FileFindDelegate delegate,
            FileOperationListener listener)
    {
        this.backupdb = backupdb;
        this.spec = spec;
        this.delegate = delegate;
        this.listener = listener;
    }
    
    public boolean processFile(File file)
    {
        if (spec.containsFile(file)) {
            
            if (listener != null && !listener.willProcessFile(file)) {
                return false;
            }
            
            if (!spec.isIncremental() || backupdb.needsBackup(file)) {
                return delegate.processFile(file);                
            }
        }
        return true;
    }

    public boolean shouldRecurseIntoDirectory(File directory)
    {
        return spec.containsFile(directory);
    }

	public void didProcessDirectoryContents(File directory, String children[])
	{
	    delegate.didProcessDirectoryContents(directory, children);
	}	
}