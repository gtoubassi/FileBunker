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
 * Created on Aug 10, 2004
 */
package com.toubassi.filebunker.ui;

import com.toubassi.filebunker.vault.Vault;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * AvailableBytesStatusItem displays the available space in the vault in the
 * status bar.  It listens for AvailableBytesChanged notifications from the
 * Vault, and updates the status bar appropriately.  Note that all updates
 * are performed in a background thread because the Vault.availableBytes()
 * may take several seconds to complete.  Usually it is slow only the first
 * time, but if a new store is added, then a subsequent operation may be
 * slow.
 * 
 * @author garrick
 */
public class AvailableBytesStatusItem extends ControlContribution
{
    private Object dirtyLock = new Object();
    private boolean isDirty = true;
    
    private Vault vault;
    private Label label;
    
    public AvailableBytesStatusItem(Vault vault, String id)
    {
        super(id);
        this.vault = vault;
    }
    
    protected synchronized Control createControl(Composite parent)
    {
        label = new Label(parent, SWT.LEFT);
        label.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));

        notifyAll();
        
        return label;
    }
    
    public void clearAvailableBytes()
    {
        if (label.isDisposed()) {
            return;
        }
        
        if (label.getDisplay().getThread() == Thread.currentThread()) {
            if (label.getText().length() > 0) {
                label.setText("");
            }
            return;
        }
        
        label.getDisplay().asyncExec( new Runnable() {
            
            public void run ()
            {
                // Recurse, but this time we will be in the UI thread.
                clearAvailableBytes();
            }            
        });                        
    }

    /**
     * Updates the availableBytes label with a new value.  Can be
     * called from a non-UI thread, in which case the update is done
     * asynchronously via Display.asyncExec.
     * @param availableBytes
     */
    public void updateAvailableBytes(final long availableBytes)
    {
        // There is a slight possibility of this if the Daemon fires
        // this message just as the user is quitting the app.
        if (label.isDisposed()) {
            return;
        }
        
        // If we are in the UI thread then we can update right now,
        // otherwise we do it asynchronously
        if (label.getDisplay().getThread() == Thread.currentThread()) {
            long availableMB = availableBytes / (1024*1024);        
            label.setText("" + availableMB + " MB free in the backup repository   ");                    
            label.getParent().layout(true);        
            return;            
        }
        
        label.getDisplay().asyncExec( new Runnable() {
            
            public void run ()
            {
                // Recurse, but this time we will be in the UI thread.
                updateAvailableBytes(availableBytes);
            }
            
        });                
    }
}

