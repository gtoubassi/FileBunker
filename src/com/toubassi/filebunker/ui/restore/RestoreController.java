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
 * Created on Aug 19, 2004
 */
package com.toubassi.filebunker.ui.restore;

import com.toubassi.filebunker.ui.LabelUtil;
import com.toubassi.filebunker.ui.Log;
import com.toubassi.filebunker.vault.DirectoryRevision;
import com.toubassi.filebunker.vault.FailedLoginException;
import com.toubassi.filebunker.vault.FileNotFoundInStoreException;
import com.toubassi.filebunker.vault.FileRevision;
import com.toubassi.filebunker.vault.OperationCanceledVaultException;
import com.toubassi.filebunker.vault.RestoreEstimate;
import com.toubassi.filebunker.vault.RestoreResult;
import com.toubassi.filebunker.vault.RestoreSpecification;
import com.toubassi.filebunker.vault.Revision;
import com.toubassi.filebunker.vault.Vault;
import com.toubassi.filebunker.vault.VaultException;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;
import com.toubassi.jface.Explorer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * @author garrick
 */
public class RestoreController implements XMLSerializable
{    
    private Vault vault;
    private RestoreTreeContentProvider contentProvider;
    private Action configurationAction;
    private Explorer explorer;
    private StackLayout stackLayout;
    private Composite nothingToRestoreComposite;
    private Composite restoreContents;
    private Button restoreButton;
    private DateChooser dateChooser;
    private SashForm sash;
    private RestoreDescriptionController descriptionController;
    private ArrayList expandedFiles;
    private ArrayList selectedFiles;
    private RestoreSpecification spec;
    private ShowHistoryAction showRevisionsAction;
    
    public RestoreController(Action configurationAction)
    {
        this.configurationAction = configurationAction;
        expandedFiles = new ArrayList();
        selectedFiles = new ArrayList();
        spec = new RestoreSpecification();
    }
    
    public void setVault(Vault vault)
    {
        this.vault = vault;

        contentProvider = new RestoreTreeContentProvider(vault);        
        
        updateDateChooser();
        
        explorer.setContentProvider(contentProvider);
        explorer.setInput(new Object());
        
        showRevisionsAction.setVault(vault);
        
        for (int i = 0; i < expandedFiles.size(); i++) {
            File file = (File)expandedFiles.get(i);
            Revision revision = vault.findRevision(file, null);
            if (revision != null) {
                explorer.setExpandedState(revision, true);
            }
        }
        
        if (selectedFiles.size() > 0) {
            File file = (File)selectedFiles.get(0);
            Revision revision = vault.findRevision(file, null);
            if (revision != null) {
                explorer.setTreeSelection(new StructuredSelection(revision));
            }
        }
        
        updateStackLayout();
        explorerSelectionChanged();
    }
    
    private Shell getShell()
    {
        return restoreButton.getShell();
    }
    
    public Explorer explorer()
    {
        return explorer;
    }

    public void createContents(Composite parent)
    {
        stackLayout = new StackLayout();
        parent.setLayout(stackLayout);

        // The empty view which shows nothing but a message
        nothingToRestoreComposite = new Composite(parent, SWT.NONE);
        
        nothingToRestoreComposite.setLayout(new FormLayout());
        Label nothingToRestoreLabel = new Label(nothingToRestoreComposite, SWT.WRAP);
        nothingToRestoreLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
        nothingToRestoreLabel.setText("There are no backed up files residing " +
        		"in any of the currently configured email repositories.  Once " +
        		"a backup has been performed, those files will be visible on " +
        		"this tab.");
        FormData nothingToRestoreLabelFormData = new FormData();
        nothingToRestoreLabelFormData.left = new FormAttachment(25, 0);
        nothingToRestoreLabelFormData.right = new FormAttachment(75, 0);
        nothingToRestoreLabelFormData.top = new FormAttachment(35, 0);
        nothingToRestoreLabelFormData.bottom = new FormAttachment(100, 0);
        nothingToRestoreLabel.setLayoutData(nothingToRestoreLabelFormData);
        
        restoreContents = new Composite(parent, SWT.NONE);
        restoreContents.setLayout(new FillLayout());

        
        // The real restore view when there are actually files in the repository
        
        sash = new SashForm(restoreContents, SWT.VERTICAL);
        
        // Build the "File Explorer"
        explorer = new Explorer(sash, new String[] {"Name", "Size", "Versions", "Backup Date"}, new int[] {200, 100, 70, 140}, SWT.NONE);
        explorer.setLabelProvider(new RestoreTableLabelProvider());
        explorer.setHideLeavesInTree(true);
        explorer.setSorter(new RestoreExplorerSorter());
        explorer.setWeights(35, 65);

        showRevisionsAction = new ShowHistoryAction(this);
        
        explorer.addSelectionListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                explorerSelectionChanged();
            }
        });
        
        explorer.addTableDoubleClickListener(new IDoubleClickListener()
        {
            public void doubleClick(DoubleClickEvent event)
            {
                showRevisionsAction.run();
            }
        });

        
        // Build the control panel below the explorer

        Composite controlPanelComposite = new Composite(sash, SWT.NONE);
        controlPanelComposite.setLayout(new FormLayout());
        
        // Backup button
        
        restoreButton = new Button(controlPanelComposite, SWT.NONE);
        restoreButton.setText("Restore");

        FormData restoreButtonFormData = new FormData();
        restoreButtonFormData.top = new FormAttachment(0, 10);
        restoreButtonFormData.left = new FormAttachment(100, -90);
        restoreButtonFormData.right = new FormAttachment(100, -5);
        restoreButtonFormData.bottom = new FormAttachment(100, -10);
        
        restoreButton.setLayoutData(restoreButtonFormData);
        
        restoreButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event)
            {
                restoreClicked();
            }

        });
        
        // Build the restore description field
        StyledText restoreDescriptionText = new StyledText(controlPanelComposite, SWT.WRAP | SWT.READ_ONLY);
        restoreDescriptionText.setForeground(sash.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
        restoreDescriptionText.setBackground(sash.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        
        descriptionController = new RestoreDescriptionController(restoreDescriptionText);
                
        FormData restoreDescriptionTextFormData = new FormData();
        restoreDescriptionTextFormData.top = new FormAttachment(0, 5);
        restoreDescriptionTextFormData.left = new FormAttachment(0, 5);
        restoreDescriptionTextFormData.right = new FormAttachment(restoreButton, -15);
        restoreDescriptionTextFormData.bottom = new FormAttachment(100, -5);
        
        restoreDescriptionText.setLayoutData(restoreDescriptionTextFormData);
        
        int[] topSashWeights = new int[] {85, 15};
        sash.setWeights(topSashWeights);
        
        // Context menu for the tableView
        MenuManager menuManager = new MenuManager();
        explorer.setTableContextMenu(menuManager);
        explorer.setTreeContextMenu(menuManager);

        menuManager.add(showRevisionsAction);        
    }
    
    public void createTabControls(Composite parent)
    {
        dateChooser = new DateChooser(parent);
        dateChooser.setVisible(false);
        
        dateChooser.setSelectionListener(new DateChooser.Listener()
        {
            public void selectionChanged(DateChooser dateChooser)
            {
                contentProvider.setDate(dateChooser.selectedDate());
                explorer.refresh();
            }            
        });
    }

    private void updateDateChooser()
    {
        TreeSet datesSet = new TreeSet(Collections.reverseOrder());
        vault.root().collectDescendantFileRevisionDates(datesSet);
        dateChooser.setContents(datesSet);
        contentProvider.setDate(dateChooser.selectedDate());
    }
    
    private void updateStackLayout()
    {
        Display display = nothingToRestoreComposite.getDisplay();
        
        // If we are in the UI thread then we can update right now,
        // otherwise we do it asynchronously
        if (display.getThread() == Thread.currentThread()) {
	        boolean isEmpty = vault == null ? true : vault.isEmpty();
	
	        if (isEmpty && stackLayout.topControl != nothingToRestoreComposite) {
	            stackLayout.topControl = nothingToRestoreComposite;
	            nothingToRestoreComposite.getParent().layout();
	        }
	        else if (!isEmpty && stackLayout.topControl != restoreContents) {
	            stackLayout.topControl = restoreContents;
	            nothingToRestoreComposite.getParent().layout();                
	        }        
            return;            
        }
        
        display.asyncExec( new Runnable() {
            
            public void run ()
            {
                // Recurse, but this time we will be in the UI thread.
                updateStackLayout();
            }
            
        });                    
    }

    private void explorerSelectionChanged()
    {
        StructuredSelection selection = explorer.getSelection();

        spec.clear();
        Iterator i = selection.iterator();
        while (i.hasNext()) {
            Revision revision = (Revision)i.next();
            
            if (revision.isDirectory()) {
                spec.add((DirectoryRevision)revision, dateChooser.selectedDate());
            }
            else {
                spec.add((FileRevision)revision);
            }
        }
        
        restoreButton.setEnabled(!selection.isEmpty());
        descriptionController.update(spec);
    }

    private void restoreClicked()
    {
        restore(spec);
    }
    
    public void restore(RestoreSpecification restoreSpec)
    {
        Shell shell = getShell();
        
        if (!vault.isConfigured()) {
            
            String[] buttons = new String[] {"Configure", "Cancel"};
            
            MessageDialog dialog = new MessageDialog(shell, "Backup", null,
                    "You cannot restore files until FileBunker has been " +
                    "configured.  You can select the Configure button " +
                    "below or select Configuration from the File menu " +
                    "at any time.",
                    MessageDialog.INFORMATION, buttons, 0); 
            int selectedButton = dialog.open();
            
            if (selectedButton == 0) {
                configurationAction.run();
            }
            return;
        }

        String restorePath;
        ArrayList revisions = restoreSpec.revisions();
        boolean singleFileSelected = revisions.size() == 1 && !((Revision)revisions.get(0)).isDirectory();
        
        if (singleFileSelected) {
            FileRevision revision = (FileRevision)revisions.get(0);
            File proposedFile = proposedFileRestoreLocation(revision.node().file());
            
            FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
            dialog.setFilterPath(proposedFile.getParent());
            dialog.setFileName(proposedFile.getName());
            
            restorePath = dialog.open();
        }
        else {

            DirectoryDialog dialog = new DirectoryDialog(getShell());

            if (revisions.size() == 1) {
                dialog.setMessage("Choose the name that the selected folder " +
                		"will be restored as.  For files that already exist " +
       				    "on disk, unique names will be used so that no " +
        				"files are overwritten.");                
            }
            else {
                dialog.setMessage("Choose the folder into which the selected files " +
      				  "will be restored.  For files that already exist " +
      				  "on disk, unique names will be used so that no " +
      				  "files are overwritten.");                
            }
            dialog.setFilterPath(spec.commonPath());
            
            restorePath = dialog.open();
        }

        if (restorePath == null) {
            return;
        }
        
        File restoreRoot = new File(restorePath);
        
        if (singleFileSelected && restoreRoot.exists()) {
            MessageBox dialog = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
            
            dialog.setMessage(restoreRoot.getName() + " already exists.  Do you want to replace it?");
            
            if (dialog.open() != SWT.YES) {
                return;
            }
        }
        
        RestoreResult result = new RestoreResult();
        PerformRestore performRestore = new PerformRestore(vault, restoreSpec, restoreRoot, result);
        
        try {
            new ProgressMonitorDialog(shell).run(true, true, performRestore);

            String message;
            if (result.numberOfFiles() == 0) {
                message = "No files were restored.";
            }
            else {
                message = formatRestoreResultMessage(result, performRestore.estimate(), false);
            }
            MessageDialog.openInformation(shell, "Restore Complete", message);
        }
        catch (InvocationTargetException e) {
            handleException(e, restoreSpec, result, performRestore.estimate());
        }
        catch (InterruptedException e) {
            handleException(e, restoreSpec, result, performRestore.estimate());
        }
    }
    
    private void handleException(Exception e, RestoreSpecification restoreSpec, RestoreResult result, RestoreEstimate estimate)
    {
        VaultException vaultException = VaultException.extract(e);

        if (vaultException instanceof FailedLoginException) {
            FailedLoginException failedLogin = (FailedLoginException)vaultException;
            
            String name;
            if (failedLogin.store() != null) {
                name = failedLogin.store().name();
            }
            else {
                name = "your gmail account";
            }
            String message = "FileBunker was unable to login to " + name + ".  ";
            message += "Please check the email address and password you " +
            		"specified in the FileBunker configuration.  ";            
            message += formatRestoreResultMessage(result, estimate, true);
            MessageDialog.openInformation(getShell(), "Failed Login", message);                
        }
        else if (vaultException instanceof FileNotFoundInStoreException) {
            String message;
            File file = vaultException.file();
            
            if (file != null) {
                
                message = "FileBunker was unable to find " + file.getName() +
    			" in the backup repository.  ";                                    

                // See if it was recently backed up.
                FileRevision fileRevision = restoreSpec.findFileRevision(vault, file);

                if (fileRevision != null) {
                    long currentMillis = System.currentTimeMillis();
                    long backupMillis = fileRevision.date().getTime();
                
                    if (currentMillis - backupMillis < 4*60*60*1000) {
                        message += "The file was backed up very recently, and " +
                		"it may not appear in the backup repository " +
                		"for several hours.  Please try again later.  ";
                    }
                }
            }
            else {
                message = "FileBunker was unable to find a requested file" +
                		" in the backup repository.  ";                                
            }
            message += formatRestoreResultMessage(result, estimate, true);
            MessageDialog.openInformation(getShell(), "Restore Error", message);                
        }
        else if (vaultException instanceof OperationCanceledVaultException) {
            String message = "The restore was canceled.  ";
            message += formatRestoreResultMessage(result, estimate, true);
            MessageDialog.openInformation(getShell(), "Restore Canceled", message);                
        }
        else {
            Log.log(e);

            File file = vaultException == null ? null : vaultException.file();
            String message;
            if (file == null) {
                message = "An error occurred while trying to restore the files.  ";
            }
            else {
                message = "An error occurred while restoring " + file.getPath() + ".  ";
            }
            message += formatRestoreResultMessage(result, estimate, true);
            MessageDialog.openError(getShell(), "Error", message);
        }
    }
    
    private String formatRestoreResultMessage(RestoreResult result, RestoreEstimate estimate, boolean aborted)
    {
        StringBuffer buffer = new StringBuffer();
        
        if (result.numberOfFiles() == 0) {
            return "";
        }
        
        boolean singular = result.numberOfFiles() == 1;

        // 127 files totaling 82 MB were restored in 12:14.
        // 127 of 239 files totaling
        buffer.append(result.numberOfFiles());
        if (aborted && estimate != null) {
            buffer.append(" of ");
            buffer.append(estimate.numberOfFiles());
        }
        buffer.append(singular ? " file " : " files ");
        buffer.append("totaling ");
        buffer.append(LabelUtil.formatMemorySize(result.totalBytes()));
        buffer.append(' ');
        buffer.append(singular ? "was " : "were ");
        buffer.append("restored");
        
        // Report the time if it was 2 seconds or more
        if (result.restoreDuration() >= 2000) {
	        buffer.append(" in ");
	        buffer.append(LabelUtil.formatHours(result.restoreDuration()));
        }
        buffer.append('.');
        return buffer.toString();
    }

    private File proposedFileRestoreLocation(File file)
    {
        File parent = file.getParentFile();
        
        while (parent != null && !parent.exists()) {
            parent = parent.getParentFile();
        }
        
        if (parent == null) {
            file = new File(file.getName());
        }
        else {
            file = new File(parent, file.getName());
        }
        
        return Vault.uniquifyRestoreTarget(file);
    }
        
    public void refresh()
    {
        updateDateChooser();
        explorer.refresh();
        updateStackLayout();
    }
    
    public void willShow()
    {
        refresh();
        dateChooser.setVisible(dateChooser.numberOfDates() > 1);
    }
    
    public void didHide()
    {        
        dateChooser.setVisible(false);
    }

    public void serializeXML(XMLSerializer writer)
	{	
	    writer.push("Restore");

	    explorer.serializeXML(writer);

	    Object[] expandedRevisions = explorer.getExpandedElements();
	    for (int i = 0; i < expandedRevisions.length; i++) {
	        Revision revision = (Revision)expandedRevisions[i];
	        writer.write("expandedFile", revision.node().file());
	    }
	    StructuredSelection selection = explorer.getTreeSelection();
	    Iterator selectionIterator = selection.iterator();
	    while (selectionIterator.hasNext()) {
	        Revision revision = (Revision)selectionIterator.next();
	        writer.write("selectedFile", revision.node().file());
	    }
	    
	    writer.pop();
	}

    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        if ("expandedFile".equals(container)) {
            expandedFiles.add(new File(value));
        }
        else if ("selectedFile".equals(container)) {
            selectedFiles.add(new File(value));
        }
        else {
            return explorer.deserializeXML(deserializer, container, value);
        }
        return null;
    }
    
}
