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
 * Created on Aug 8, 2004
 */
package com.toubassi.filebunker.ui.backup;

import com.toubassi.filebunker.ui.FileProgressMonitorListener;
import com.toubassi.filebunker.vault.BackupEstimate;
import com.toubassi.filebunker.vault.BackupOperationListener;
import com.toubassi.filebunker.vault.BackupResult;
import com.toubassi.filebunker.vault.BackupSpecification;
import com.toubassi.filebunker.vault.FileOperationListener;
import com.toubassi.filebunker.vault.InsufficientSpaceException;
import com.toubassi.filebunker.vault.RestoreEstimate;
import com.toubassi.filebunker.vault.Vault;
import com.toubassi.filebunker.vault.VaultException;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * PerformBackup is a class responsible for performing the main backup
 * function.  It backs up the files as directed in a BackupSpecification
 * and has three main phases:
 * 
 *	1. Perform an estimate of the backup operation.
 *	2. If necessary, reclaim space from the store if there is not enough.
 *  3. Backup the files.
 * 
 * The class is designed to work in conjunction with an
 * ProgressMonitorDialog, and makes an effort to keep the progress monitor
 * up to date and respond to cancel requests.
 * 
 * @author garrick
 */
public class PerformBackup implements IRunnableWithProgress
{
    
    private Vault vault;
    private BackupSpecification spec;
    private BackupResult result;
    private BackupEstimate estimate;
    
    public PerformBackup(Vault vault, BackupSpecification spec, BackupResult result)
    {
        this.vault = vault;
        this.spec = spec;
        this.result = result;
    }

    public void run(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException
    {
        monitor.beginTask("Preparing for backup", 1000);

        try {
            estimate = vault.estimateBackup(spec, new BackupEstimateListener(monitor));
            
            // Lets be conservative since this is an estimate based on past
            // compression performance
            long estimatedSize = (long)(estimate.estimatedBackupSize() * 1.1);
            
            // Also take an upper bound by adding the uncompressed total size + room for
            // headers, etc per file.  This isn't totally accurate since some large files
            // may split into many messages and require much more for headers.  Really
            // we need to delegate this kind of calculation to the store since only it
            // knows how much overhead per byte or file is needed.
            long highEstimate = estimate.totalSize() + 2048 * estimate.numberOfFiles();
            
            if (estimatedSize > highEstimate) {
                estimatedSize = highEstimate;
            }
            
            long available = vault.availableBytes();
            
            if (estimatedSize > available) {
                long amountNeeded = estimatedSize - available;

                monitor.setTaskName("Reclaiming storage space");
                long amountRecovered = vault.recoverBytes(amountNeeded, new RecoverBytesListener(monitor));
                if (amountRecovered < amountNeeded) {
                    throw new InsufficientSpaceException();
                }
            }
            
            monitor.setTaskName("Backing up files");
            vault.backup(spec, new BackupListener(monitor, estimate.totalSize()), result);
            
            monitor.done();
        }
        catch (VaultException e) {
            throw new InvocationTargetException(e);
        }        
    }
    
    /**
     * May return null
     */
    public BackupEstimate estimate()
    {
        return estimate;
    }

}

class BackupEstimateListener implements FileOperationListener
{
    private IProgressMonitor monitor;
    private StringBuffer buffer = new StringBuffer();
    
    public BackupEstimateListener(IProgressMonitor monitor)
    {
        this.monitor = monitor;
    }
    
    public boolean fileProgress(File file, long bytesProcessed)
    {
        return !monitor.isCanceled();
    }

    public boolean willProcessFile(File file)
    {
        buffer.append("Scanning ");
        buffer.append(file.getName());        
        
        monitor.subTask(buffer.toString());
        
        buffer.delete(0, buffer.length());
        
        return !monitor.isCanceled();
    }

    public void operationDidEnd(Object result, boolean canceled)
    {
    }

    public void operationWillStart()
    {
    }
}

class RecoverBytesListener implements FileOperationListener
{
    private IProgressMonitor monitor;
    private StringBuffer buffer = new StringBuffer();
    
    public RecoverBytesListener(IProgressMonitor monitor)
    {
        this.monitor = monitor;
    }
    
    public boolean fileProgress(File file, long bytesProcessed)
    {
        return !monitor.isCanceled();
    }

    public boolean willProcessFile(File file)
    {
        buffer.append("Discarding redundant version of ");
        buffer.append(file.getName());
        
        monitor.subTask(buffer.toString());
        
        buffer.delete(0, buffer.length());

        return !monitor.isCanceled();
    }

    public void operationDidEnd(Object result, boolean cancelled)
    {
    }

    public void operationWillStart()
    {
    }
}


class BackupListener extends FileProgressMonitorListener implements BackupOperationListener
{
    private long estimatedSize;
    private Vault vault;
    private Date date;
    private RestoreEstimate estimate;
    private boolean finished;
        
    public BackupListener(IProgressMonitor monitor, long estimatedSize)
    {
        super(monitor);
        this.estimatedSize = estimatedSize;
    }
    
    protected long totalBytesToProcess()
    {
        return estimatedSize;
    }
    
    protected String operationMessagePrefix()
    {
        return "Backing up ";
    }

    protected void appendOperationMessageForFile(File file, StringBuffer buffer)
    {
        if (!finished) {
            super.appendOperationMessageForFile(file, buffer);
        }
        else {
            buffer.append("Finishing... ");
        }
    }
    
    protected long bytesToProcessForFile(File file)
    {
        return file.length();
    }
    
    public void finishedUserFiles()
    {
        finished = true;
    }
}
