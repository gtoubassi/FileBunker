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
package com.toubassi.filebunker.ui.restore;

import com.toubassi.filebunker.ui.LabelUtil;
import com.toubassi.filebunker.vault.DirectoryRevision;
import com.toubassi.filebunker.vault.FileRevision;
import com.toubassi.filebunker.vault.RestoreSpecification;
import com.toubassi.filebunker.vault.Revision;
import com.toubassi.filebunker.vault.Vault;
import com.toubassi.jface.Explorer;
import com.toubassi.jface.ObjectChooserDialog;

import java.util.Collections;
import java.util.Date;
import java.util.TreeSet;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * @author garrick
 */
public class ShowHistoryAction extends Action implements ISelectionChangedListener
{
    private Vault vault;
    private RestoreController controller;
    private Explorer explorer;
    
    public ShowHistoryAction(RestoreController controller)
    {
        super("Show History...");
        this.controller = controller;
        explorer = controller.explorer();
    }
    
    public void setVault(Vault vault)
    {
        this.vault = vault;
    }

    public void run()
    {
        StructuredSelection selection = explorer.getSelection();
        
        if (selection.isEmpty()) {
            return;
        }
        
        Revision revision = (Revision)selection.getFirstElement();
 
        /*
        if (revision.isDirectory()) {
            return;
        }
        */

        String infoTitle = "History of " + revision.node().name();
        TreeSet datesSet = new TreeSet(Collections.reverseOrder());
        String description;
        
        if (revision.isDirectory()) {
            description = "Below are all dates that files inside this folder " +
            		"were backed up.  You can select one and click the Restore " +
            		"button to retrieve the contents of the folder as of that " +
            		"date.";            
            
            revision.node().collectDescendantFileRevisionDates(datesSet);
        }
        else {
            description = "Below are all backed up versions of " + revision.node().name() + 
             	   ".  You can select one and click the Restore button " +
             	   "to retrieve that version.";            

            revision.node().collectFileRevisionDates(datesSet);
        }

        
        ObjectChooserDialog dialog = new ObjectChooserDialog(explorer.getShell(), datesSet.toArray(), "History", infoTitle, description, "Restore");
        dialog.setFormat(LabelUtil.shortDateTimeSecondsFormat);

        Date date = (Date)dialog.run();

        if (date != null) {
            RestoreSpecification spec = new RestoreSpecification();
            Revision restoreRevision = vault.findRevision(revision.node().file(), date);
            if (restoreRevision.isDirectory()) {
                spec.add((DirectoryRevision)restoreRevision, date);
            }
            else {
                spec.add((FileRevision)restoreRevision);
            }

            controller.restore(spec);
        }
    }
    
    public void selectionChanged(SelectionChangedEvent event)
    {
        StructuredSelection selection = (StructuredSelection)event.getSelection();

        boolean enabled = false;
        if (!selection.isEmpty()) {
            Revision revision = (Revision)selection.getFirstElement();
            
            enabled = !revision.isDirectory();
        }
        setEnabled(enabled);
    }
}
