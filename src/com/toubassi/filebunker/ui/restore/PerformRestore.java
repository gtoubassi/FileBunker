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

import com.toubassi.filebunker.ui.FileProgressMonitorListener;
import com.toubassi.filebunker.vault.FileRevision;
import com.toubassi.filebunker.vault.RestoreEstimate;
import com.toubassi.filebunker.vault.RestoreResult;
import com.toubassi.filebunker.vault.RestoreSpecification;
import com.toubassi.filebunker.vault.Revision;
import com.toubassi.filebunker.vault.Vault;
import com.toubassi.filebunker.vault.VaultException;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * @author garrick
 */
public class PerformRestore implements IRunnableWithProgress
{
    private Vault vault;
    private RestoreSpecification spec;
    private File restoreRoot;
    private RestoreResult result;
    private RestoreEstimate estimate;
    
    public PerformRestore(Vault vault, RestoreSpecification spec, File restoreRoot, RestoreResult result)
    {
        this.vault = vault;
        this.spec = spec;
        this.restoreRoot = restoreRoot;
        this.result = result;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException
    {
        ArrayList revisions = spec.revisions();
        boolean isSingleFile = revisions.size() == 1 && !((Revision)revisions.get(0)).isDirectory();

        if (isSingleFile) {
            FileRevision revision = (FileRevision)revisions.get(0);
            monitor.beginTask("Restoring " + revision.node().name(), 1000);
        }
        else {
            monitor.beginTask("Restoring files", 1000);
        }

        try {
            estimate = vault.estimateRestore(spec);
            RestoreListener listener = new RestoreListener(monitor, vault, spec, estimate);
            
            vault.restore(spec, restoreRoot, isSingleFile, listener, result);
        }
        catch (VaultException e) {
            throw new InvocationTargetException(e);
        }
        
        monitor.done();
        if (monitor.isCanceled()) {
            throw new InterruptedException();
        }
    }
    
    public RestoreEstimate estimate()
    {
        return estimate;
    }
}


class RestoreListener extends FileProgressMonitorListener
{
    private Vault vault;
    private RestoreEstimate estimate;
    private RestoreSpecification spec;
        
    public RestoreListener(IProgressMonitor monitor, Vault vault, RestoreSpecification spec, RestoreEstimate estimate)
    {
        super(monitor);
        this.estimate = estimate;
        this.vault = vault;
        this.spec = spec;
    }
    
    protected long totalBytesToProcess()
    {
        return estimate.totalSize();
    }
    
    protected String operationMessagePrefix()
    {
        return "Restoring ";
    }
    
    protected long bytesToProcessForFile(File file)
    {
        FileRevision revision = spec.findFileRevision(vault, file);
        
        if (revision == null || revision.isDirectory()) {
            return 0;
        }
        
        return ((FileRevision)revision).size();
    }
}

