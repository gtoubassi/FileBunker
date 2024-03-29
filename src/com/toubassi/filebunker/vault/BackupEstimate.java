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
 * Created on Aug 7, 2004
 */
package com.toubassi.filebunker.vault;

import com.toubassi.util.FileFindDelegate;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author garrick
 */
public class BackupEstimate implements FileFindDelegate
{
    private HashMap estimatesByType;
    private BackupDatabase backupdb;
    private FileOperationListener listener;
    private long totalSize;
    private long estimatedBackupSize;
    private ArrayList dirtyFiles;
    
    public class DirtyFile
    {
        public File file;
        public long size;
        public long estimatedBackupSize;
        
        public DirtyFile(File file, long size, long estimatedBackupSize)
        {
            this.file = file;
            this.size = size;
            this.estimatedBackupSize = estimatedBackupSize;
        }
    }
    
    public BackupEstimate(FileOperationListener listener, BackupDatabase backupdb)
    {
        this.backupdb = backupdb;
        this.listener = listener;
        estimatesByType = new HashMap();
        dirtyFiles = new ArrayList();
    }
    
    void addFile(File file)
    {
        String path = file.getPath();
        
        long size = file.length();

        totalSize += size;

        FileRevision revision = backupdb.findLastFileRevision(file);
        float ratio = 1.0f;

        if (revision != null) {
            ratio = revision.backedupSizeRatio();
        }
        else {
            // Estimate by the extension
            int lastDot = path.lastIndexOf('.');
            if (path.indexOf(File.separatorChar, lastDot) == -1) {
                String extension = path.substring(lastDot);
                
                Float result = (Float)estimatesByType.get(extension);
                
                if (result == null) {
                    result = new Float(backupdb.averageBackedupSizeRatioForType(extension));
                    estimatesByType.put(extension, result);
                }
                ratio = result.floatValue();                
            }
        }
        
        long backupSize = (long)(size * ratio);

        dirtyFiles.add(new DirtyFile(file, size, backupSize));
        
        estimatedBackupSize += backupSize;
    }
    
    public int numberOfDirtyFiles()
    {
        return dirtyFiles.size();
    }
    
    public ArrayList dirtyFiles()
    {
        return dirtyFiles;
    }
    
    public long totalSize()
    {
        return totalSize;
    }
    
    public long estimatedBackupSize()
    {
        return estimatedBackupSize;
    }
    
    public boolean processFile(File file)
    {
        boolean continueFileScan = true;

        if (listener != null) {
            continueFileScan = listener.willProcessFile(file);
        }
        
        addFile(file);
        
        return continueFileScan;
    }

    public boolean shouldRecurseIntoDirectory(File directory)
    {
        return true;
    }

	public void didProcessDirectoryContents(File directory, String children[])
	{
	}
	
    public String toString()
    {
		return "          Number of Files: " + dirtyFiles.size() + "\n" +
		       "              Total Bytes: " + totalSize() + "\n" +
		       "Estimated Backed Up Bytes: " + estimatedBackupSize() + "\n";
    }
}
