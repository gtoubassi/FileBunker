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
 * Created on Aug 17, 2004
 */
package com.toubassi.filebunker.ui.backup;

import com.subx.common.NotificationCenter;
import com.subx.common.NotificationListener;
import com.toubassi.filebunker.ui.LabelUtil;
import com.toubassi.filebunker.ui.Log;
import com.toubassi.filebunker.vault.BackupEstimate;
import com.toubassi.filebunker.vault.BackupResult;
import com.toubassi.filebunker.vault.BackupSpecification;
import com.toubassi.filebunker.vault.FailedLoginException;
import com.toubassi.filebunker.vault.InsufficientSpaceException;
import com.toubassi.filebunker.vault.OperationCanceledVaultException;
import com.toubassi.filebunker.vault.Vault;
import com.toubassi.filebunker.vault.VaultException;
import com.toubassi.filebunker.vault.WebMailFileStore;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;
import com.toubassi.jface.Explorer;
import com.toubassi.util.ExceptionUtil;
import com.toubassi.util.Platform;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.mail.AuthenticationFailedException;
import javax.mail.SendFailedException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * @author garrick
 */
public class BackupController implements NotificationListener, XMLSerializable
{
    private Vault vault;
    private File backupSpecFile;
    private BackupSpecification backupSpec;

    private File[] visibleRoots;
    private File[] allFileRoots;
    
    // Only used during UI state loading
    private ArrayList selectedFiles;
    private ArrayList expandedFiles;

    private Action configurationAction;
    private BackupTreeContentProvider contentProvider;
    private BackupTableLabelProvider labelProvider;
    private Explorer explorer;
    private SpecificationDescriptionController specificationController;
    private Button backupButton;
    private SashForm sash;
    
    public BackupController(File configDirectory, Action configurationAction)
    {
        this.configurationAction = configurationAction;
        
        allFileRoots = File.listRoots();

        backupSpecFile = new File(configDirectory, "spec.xml");

        backupSpec = new BackupSpecification();        
        try {
            backupSpec.load(backupSpecFile);
            backupSpec.validateContents();
        }
        catch (IOException e) {
            // If this file got corrupted, we can just ignore it.
            backupSpec = new BackupSpecification();
            Log.log(e);
        }

        backupSpec.setIsIncremental(true);
        
        NotificationCenter.sharedCenter().register(backupSpec, this);

        contentProvider = new BackupTreeContentProvider(backupSpec);
        labelProvider = new BackupTableLabelProvider(contentProvider);
    }
    
    public void setVault(Vault vault)
    {
        this.vault = vault;
    }
    
    public void setVisibleRoots(File[] roots)
    {
        visibleRoots = roots;
        contentProvider.setRoots(roots);
        if (explorer != null) {
            explorer.refresh();
        }
    }
    
    public File[] visibleRoots()
    {
        return visibleRoots;
    }
    
    private Shell getShell()
    {
        return backupButton.getShell();
    }

    public void createContents(Composite parent)
    {
        parent.setLayout(new FillLayout());

        sash = new SashForm(parent, SWT.VERTICAL);
        
        // Build the "File Explorer"
        explorer = new Explorer(sash, new String[] { "Name", "Size" }, new int[] {200, 100}, SWT.CHECK);
        explorer.setContentProvider(contentProvider);
        explorer.setLabelProvider(labelProvider);
        explorer.setHideLeavesInTree(true);
        explorer.setSorter(new BackupExplorerSorter());
        explorer.setInput(new Object());
        explorer.setWeights(35, 65);
        
        // Build the control panel below the explorer

        Composite controlPanelComposite = new Composite(sash, SWT.NONE);
        controlPanelComposite.setLayout(new FormLayout());
        
        // Build the BackupSpecification description field

        StyledText backupSpecificationText = new StyledText(controlPanelComposite, SWT.WRAP | SWT.READ_ONLY);
        backupSpecificationText.setForeground(sash.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
        backupSpecificationText.setBackground(sash.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        
        specificationController = new SpecificationDescriptionController(backupSpec, backupSpecificationText);
                
        FormData profileFormData = new FormData();
        profileFormData.top = new FormAttachment(0, 5);
        profileFormData.left = new FormAttachment(0, 5);
        profileFormData.right = new FormAttachment(100, -100);
        profileFormData.bottom = new FormAttachment(100, -5);
        
        backupSpecificationText.setLayoutData(profileFormData);

        // Backup button
        
        backupButton = new Button(controlPanelComposite, SWT.NONE);
        backupButton.setText("Backup");
        Point backupButtonSize = backupButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);

        FormData backupButtonFormData = new FormData();
        backupButtonFormData.top = new FormAttachment(0, 10);
        backupButtonFormData.left = new FormAttachment(100, -90);
        backupButtonFormData.right = new FormAttachment(100, -5);
        backupButtonFormData.bottom = new FormAttachment(100, -10);
        
        backupButton.setLayoutData(backupButtonFormData);
        
        backupButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event)
            {
                backupClicked();
            }

        });
        
        int[] topSashWeights = new int[] {85, 15};
        sash.setWeights(topSashWeights);

        updateBackupButton();        
    }

    private void updateBackupButton()
    {
        backupButton.setEnabled(!backupSpec.isEmpty());        
    }
    
    private void backupClicked()
    {
        Shell shell = getShell();
        
        if (!vault.isConfigured()) {
            
            String[] buttons = new String[] {"Configure", "Cancel"};
            
            MessageDialog dialog = new MessageDialog(shell, "Backup", null,
                    "You cannot perform a backup until FileBunker has been " +
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
        
        BackupResult result = new BackupResult();
        PerformBackup performBackup = new PerformBackup(vault, backupSpec, result);
        
        try {
            new ProgressMonitorDialog(shell).run(true, true, performBackup);

            String message;
            if (result.numberOfFiles() == 0) {
                message = "No files were backed up because none had been modified since the last backup.";
            }
            else {
                message = formatBackupResultMessage(result, performBackup.estimate(), false);
            }
            MessageDialog.openInformation(shell, "Backup Complete", message);

        }
        catch (InvocationTargetException e) {
            handleException(e, result, performBackup.estimate());
        }
        catch (InterruptedException e) {
            handleException(e, result, performBackup.estimate());
        }
    }
    
    private void handleException(Exception e, BackupResult result, BackupEstimate estimate)
    {
        Log.log(e);
        
        VaultException vaultException = VaultException.extract(e);
        UnknownHostException unknownHostException = (UnknownHostException)ExceptionUtil.extract(UnknownHostException.class, e);
        AuthenticationFailedException authenticationException = (AuthenticationFailedException)ExceptionUtil.extract(AuthenticationFailedException.class, e);
        SendFailedException sendFailedException = (SendFailedException)ExceptionUtil.extract(SendFailedException.class, e);
        
        if (authenticationException != null) {
            String smtp = vault.configuration().parameterForKey(WebMailFileStore.SmtpServerKey);
            String message = "FileBunker was unable to send backup files using the SMTP " +
            	"server " + smtp + " because your authentication credentials are " +
	    		"invalid.  Please supply the correct credentials in the " +
	    		"FileBunker configuration, by selecting the 'Configure' " +
	    		"button next to the 'Requires Authentication' checkbox.";
            MessageDialog.openInformation(getShell(), "SMTP Authentication Error", message);                                
        }
        else if (unknownHostException != null || sendFailedException != null) {
            String smtp = vault.configuration().parameterForKey(WebMailFileStore.SmtpServerKey);
            String message;
            
            if (unknownHostException != null) {
                message = "FileBunker was unable to send backup files using the SMTP " +
        		"server " + smtp + ".  Please correct this address in the FileBunker " +
   				"configuration.";                                
            }
            else if (sendFailedException.getMessage() != null &&
                     sendFailedException.getMessage().indexOf("550") >= 0)
            {
                // We need to use authentication and we aren't
                message = "The SMTP server " + smtp + " requires authentication.  " +
                		"Please provide these credentials in the FileBunker " +
                		"configuration.";                
            }
            else {
                // Misc sendFailedException.
                File file = vaultException == null ? null : vaultException.file();
                if (file == null) {
                    message = "An error occurred while trying to send a backup email.";
                }
                else {                    
                    message = "An error occurred while trying to send a backup email for " +
                    		file.getName() + ".";
                }
                message += formatBackupResultMessage(result, estimate, true);
            }

            MessageDialog.openInformation(getShell(), "Error", message);                                
        }
        else if (vaultException instanceof FailedLoginException) {
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
            message += formatBackupResultMessage(result, estimate, true);
            MessageDialog.openInformation(getShell(), "Failed Login", message);                
        }
        else if (vaultException instanceof InsufficientSpaceException) {
            String message = "The backup could not be performed due to a lack " +
            		"of space in the repository.  No files were backed up.";
            MessageDialog.openInformation(getShell(), "Backup Error", message);                
        }
        else if (vaultException instanceof OperationCanceledVaultException) {
            String message = "The backup was canceled.  ";
            message += formatBackupResultMessage(result, estimate, true);
            MessageDialog.openInformation(getShell(), "Backup Canceled", message);                
        }
        else {
            Log.log(e);

            File file = vaultException == null ? null : vaultException.file();
            String message;
            if (file == null) {
                message = "An error occurred while performing the backup.  ";
            }
            else {
                message = "An error occurred while backing up " + file.getPath() + ".  ";
            }
            message += formatBackupResultMessage(result, estimate, true);
            MessageDialog.openError(getShell(), "Backup Error", message);
        }
    }
    
    private String formatBackupResultMessage(BackupResult result, BackupEstimate estimate, boolean aborted)
    {
        StringBuffer buffer = new StringBuffer();
        
        if (result.numberOfFiles() == 0) {
            return "";
        }
        
        boolean singular = result.numberOfFiles() == 1;

        // 127 files totaling 82 MB were backed up in 12:14.
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
        buffer.append("backed up");
        
        // Report the time if it was 2 seconds or more
        if (result.backupDuration() >= 2000) {
	        buffer.append(" in ");
	        buffer.append(LabelUtil.formatHours(result.backupDuration()));
        }
        buffer.append('.');
        if (result.compressionRatio() < 1.0) {
            buffer.append("  The ");
            buffer.append(singular ? "file was " : "files were ");
            buffer.append("compressed to ");
            
            float ratio = result.compressionRatio() * 100;
            
            // A little trick so that we don't claim we compress
            // to "0%" which sounds awkward and not strictly true.
            // Instead we say "to less than 1%".
            if (ratio < 1.0f) {
                buffer.append("less than ");
                ratio = 1;
            }
            
            buffer.append(((int)ratio));
            buffer.append("% of ");
            buffer.append(singular ? "its " : "their ");
            buffer.append("original size.");
        }

        return buffer.toString();
    }
    
    public void handleNotification(String notification, Object sender, Object argument)
    {
        if (sender == backupSpec && BackupSpecification.ChangedNotification.equals(notification)) {
            updateBackupButton();
        }
    }
    
    public void save() throws VaultException
    {
        backupSpec.save(backupSpecFile);        
    }

    public void refresh()
    {
        explorer.refresh();
        backupSpec.validateContents();
    }
    
    public void willShow()
    {
        refresh();
    }
    
    public void didHide()
    {        
    }
    
    public void didLoadUIState(boolean didLoadFromFile)
    {
        if (visibleRoots == null) {
            // Can't use File.listRoots on Windows because it pops a dialog on A: (and any CD drives with no CDs)

            if (Platform.isWindows()) {
                visibleRoots = new File[] {new File("C:\\")};                                
            }
            else {
                visibleRoots = allFileRoots;
            }
        }
        setVisibleRoots(visibleRoots);
        
        if (expandedFiles != null) {
            for (int i = 0; i < expandedFiles.size(); i++) {
                explorer.setExpandedState(expandedFiles.get(i), true);            
            }
            expandedFiles = null;
        }
        else if (!didLoadFromFile) {
            // Expand the top level directories and select the first node.            
            StructuredSelection selection = new StructuredSelection(visibleRoots[0]);
            explorer.setTreeSelection(selection);
            explorer.setExpandedState(visibleRoots[0], true);
        }
        
        if (selectedFiles != null) {
            for (int i = 0; i < selectedFiles.size(); i++) {
                explorer.setTreeSelection(new StructuredSelection(selectedFiles.get(i)));
            }
            selectedFiles = null;
        }
    }
    
	public void serializeXML(XMLSerializer writer)
	{	
	    writer.push("Backup");

	    explorer.serializeXML(writer);

	    int[] weights = sash.getWeights();
	    int normalizedLeftSide = (int)(100.0 * ((float)weights[0]) / ((float)(weights[0] + weights[1])));
	    writer.write("verticalSash", Integer.toString(normalizedLeftSide));

	    Object[] expandedFiles = explorer.getExpandedElements();
	    for (int i = 0; i < expandedFiles.length; i++) {
	        writer.write("expandedFile", expandedFiles[i]);
	    }
	    StructuredSelection selection = explorer.getTreeSelection();
	    Iterator selectionIterator = selection.iterator();
	    while (selectionIterator.hasNext()) {
	        writer.write("selectedFile", selectionIterator.next());
	    }
	    
	    if (Platform.isWindows() && visibleRoots != null) {
	        for (int i = 0; i < visibleRoots.length; i++) {
	            writer.write("Drive", visibleRoots[i].getPath());
	        }
	    }
	    	    
	    writer.pop();
	}

    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        if ("verticalSash".equals(container)) {
            int weights[] = new int[2];
            weights[0] = Integer.parseInt(value);
            weights[1] = 100 - weights[0];
            sash.setWeights(weights);
        }
        else if ("expandedFile".equals(container)) {
            File file = new File(value);
            if (file.exists()) {
                if (expandedFiles == null) {
                    expandedFiles = new ArrayList();
                }
                expandedFiles.add(file);
            }
        }
        else if ("selectedFile".equals(container)) {
            File file = new File(value);
            if (file.exists()) {
                if (selectedFiles == null) {
                    selectedFiles = new ArrayList();
                }
                selectedFiles.add(file);
            }
        }
        else if ("Drive".equals(container)) {
            if (Platform.isWindows()) {
                
                // Make sure this drive is valid on this system.
                boolean isValid = false;
                for (int i = 0; i < allFileRoots.length; i++) {
                    if (allFileRoots[i].getPath().equals(value)) {
                        isValid = true;
                        break;
                    }
                }
                
                if (!isValid) {
                    return null;
                }
                
                // Grow the array
                File[] newRoots = new File[visibleRoots == null ? 1 : visibleRoots.length + 1];
                
                if (visibleRoots != null) {
                    System.arraycopy(visibleRoots, 0, newRoots, 0, visibleRoots.length);
                }                
                visibleRoots = newRoots;
                
                // Add it to the end.
                visibleRoots[visibleRoots.length - 1] = new File(value);
            }
        }
        else {
            return explorer.deserializeXML(deserializer, container, value);
        }
        return null;
    }
}
