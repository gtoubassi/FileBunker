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
 * Created on Aug 21, 2004
 */
package com.toubassi.filebunker.ui;

import com.toubassi.filebunker.vault.FileOperationListener;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author garrick
 */
public abstract class FileProgressMonitorListener implements FileOperationListener
{
    private IProgressMonitor monitor;
    
    private StringBuffer buffer = new StringBuffer();
    private long currentFileSize;
    private long bytesProcessedForFile;
    private long totalBytesProcessed;
    private long lastProgressMillis;
    private long lastBytesProcessedForFile;
    private float lastPercentage;
    private int numberOfFilesProcessed;
    private boolean hasShownPercentageForCurrentFile;
    
    public FileProgressMonitorListener(IProgressMonitor monitor)
    {
        this.monitor = monitor;
    }
    
    protected void totalPercentageWorked(float percentage)
    {
        int newWork = (int)((percentage - lastPercentage) * 1000.0);
        if (newWork > 0) {
            monitor.worked(newWork);
            lastPercentage += ((float)newWork) / 1000.0;
        }
    }
    
    protected abstract long totalBytesToProcess();
    
    /**
     * e.g. "Restoring " or "Backing up " as in "Restoring foo.doc" or
     * "Backing up foo.doc (75%)".
     * @return
     */
    protected abstract String operationMessagePrefix();
    
    protected abstract long bytesToProcessForFile(File file);
    
    protected void appendOperationMessageForFile(File file, StringBuffer buffer)
    {
        buffer.append(operationMessagePrefix());
        buffer.append(file.getName());
    }
    
    public boolean fileProgress(File file, long bytesProcessed)
    {
        bytesProcessedForFile += bytesProcessed;

        float fractionOfFile = ( ((float) bytesProcessedForFile) / ((float)currentFileSize) );
        if (fractionOfFile > 1.0f) {
            fractionOfFile = 1.0f;
        }
        int percentageOfFile = (int)(fractionOfFile * 100);
        totalPercentageWorked(((float)totalBytesProcessed + bytesProcessedForFile) / (float)totalBytesToProcess());
        
        lastBytesProcessedForFile = bytesProcessedForFile;

        long now = System.currentTimeMillis();
        // We don't want to cause seizures in the user by updating the
        // percentage too often.  We do it every half second, although
        // once we show a percentage, we always show it hitting the 100%
        // mark even if that happens quickly.
        if (!(hasShownPercentageForCurrentFile && percentageOfFile == 100)) {
            
            // Don't update display if we last updated < .5 sec ago.
	        if (now - lastProgressMillis < 500) {
	            return !monitor.isCanceled();
	        }
        }
        
        lastProgressMillis = now;
        
        appendOperationMessageForFile(file, buffer);

        if (percentageOfFile > 0) {
            
            hasShownPercentageForCurrentFile = true;

            buffer.append(" (");
	        buffer.append(percentageOfFile);
	        buffer.append("%)");
        }
        

        monitor.subTask(buffer.toString());

        buffer.delete(0, buffer.length());
        
        return !monitor.isCanceled();
    }

    public boolean willProcessFile(File file)
    {
        hasShownPercentageForCurrentFile = false;
        
        numberOfFilesProcessed++;
        // Current is really the last revision at this point
        totalBytesProcessed += currentFileSize;
        
        totalPercentageWorked(((float)totalBytesProcessed) / ((float)totalBytesToProcess()));
        
        currentFileSize = bytesToProcessForFile(file);
        bytesProcessedForFile = 0;
        lastBytesProcessedForFile = 0;
        
        appendOperationMessageForFile(file, buffer);
        
        monitor.subTask(buffer.toString());

        buffer.delete(0, buffer.length());
        
        lastProgressMillis = System.currentTimeMillis();
        
        return !monitor.isCanceled();
    }
    
    public long totalBytesProcessed()
    {
        return totalBytesProcessed;
    }
    
    public int numerOfFilesProcessed()
    {
        return numberOfFilesProcessed;
    }

    public void operationDidEnd(Object result, boolean canceled)
    {
    }

    public void operationWillStart()
    {
    }
}
