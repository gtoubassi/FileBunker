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
 * Created on Aug 13, 2004
 */
package com.toubassi.filebunker.ui.configuration;

import com.toubassi.filebunker.ui.Log;
import com.toubassi.filebunker.vault.GMailFileStore;
import com.toubassi.filebunker.vault.Vault;
import com.toubassi.filebunker.vault.VaultConfiguration;
import com.toubassi.filebunker.vault.VaultException;
import com.toubassi.filebunker.vault.WebMailFileStore;
import com.toubassi.jface.ContextHintDialog;
import com.toubassi.util.Platform;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author garrick
 */
public class ConfigurationDialog extends ContextHintDialog
{
    private static final String dummyPasswordValue = "__dumy__";
    private static final Pattern hostPattern = Pattern.compile("^[a-zA-Z0-9\\-\\.\\_]+$");
    private static final Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9\\-\\.\\_]+@[a-zA-Z0-9\\-\\.\\_]+$");
    
    private static final String smtpFieldHint =
            "Please enter the SMTP server that you use for sending " +
            "email.  This can be found in the configuration of your " +
            "email application.";
    
    private static final String smtpAuthenticationHint =
        	"If your SMTP server requires authentication to send " +
        	"messages, check this box and then configure your " +
        	"credentials by clicking the 'Configure' button.";
    
    private static final String smtpAuthenticationConfigureHint =
        	"Click this button to configure your SMTP authentication " +
        	"credentials.";

    private static final String passwordFieldHint =
            "Please enter your FileBunker password.  This password will be " +
            "used to protect your data when it is backed up, and also " +
            "to protect your configuration information that is stored " +
            "locally.";                    

    private static final String confirmPasswordFieldHint =
        "Please confirm your FileBunker password.";                    
    
    private static final String visibleDrivesHint = 
        "Please select the drives that will be shown in the backup file " +
        "explorer.";        

    private static final String storeListViewerHint =
        "Double-click on an email repository to edit its properties.  ";

    private static final String newButtonHint =
        "Click the New button to define a new email account which can " +
        "be used as a storage repository for your backed up data.  You " +
        "need at least one, and can add as many as you like.";                    

    private static final String editButtonHint =
        "Click the Edit button to change the account details for an " +
        "already existing email repository.";                    

    private static final String deleteButtonHint =
        "Click the Delete button to remove an email account as a " +
        "storage repository.";                    

    private Vault vault;
    private VaultConfiguration vaultConfiguration;
    private ArrayList stores;
    
    private Text smtpField;
    private Button smtpAuthenticationCheckbox;
    private Button smtpAuthenticationConfigureButton;

    private Text passwordField;
    private Text confirmPasswordField;
    
    private ListViewer storeListViewer;
    private Button newButton;
    private Button editButton;
    private Button deleteButton;
    
    // Windows only
    private File[] allDrives;
    private File[] visibleDrives;
    private Button[] driveCheckboxes;
    
    
    public ConfigurationDialog(Shell shell, Vault vault, File[] visibleDrives)
    {
        super(shell);
        setInfoTitle("FileBunker Configuration");
        this.vault = vault;
        this.visibleDrives = visibleDrives;
    }
    
    public int open()
    {
        stores = new ArrayList();
        vaultConfiguration = vault.getEditableContents(stores);
        int returnCode = super.open();
        vaultConfiguration = null;
        stores = null;
        return returnCode;
    }
    
    public File[] visibleDrives()
    {
        return visibleDrives;
    }
    
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText("Configuration");
    }
    
    private void updateUIFromVaultConfiguration()
    {
        if (vaultConfiguration.currentPassword() != null) {
            passwordField.setText(dummyPasswordValue);
            confirmPasswordField.setText(dummyPasswordValue);
        }
        else {
            passwordField.setText("");
            confirmPasswordField.setText("");
        }
        
        String smtpUseAuthentication = vaultConfiguration.parameterForKey(WebMailFileStore.SmtpUseAuthenticationKey);
        String smtpAuthenticationUser = vaultConfiguration.parameterForKey(WebMailFileStore.SmtpUsernameKey);
        String smtpAuthenticationPassword = vaultConfiguration.parameterForKey(WebMailFileStore.SmtpPasswordKey);
        
        boolean useAuthentication = "true".equals(smtpUseAuthentication);
        smtpAuthenticationCheckbox.setSelection(useAuthentication);
        
        updateConfigureButton();
        
        String smtp = vaultConfiguration.parameterForKey(WebMailFileStore.SmtpServerKey);
        
        smtpField.setText(smtp == null ? "" : smtp);
    }

    private void updateModelFromUI()
    {
        String passwordValue = passwordField.getText();
        String confirmPasswordValue = confirmPasswordField.getText();
        
        if (!passwordValue.equals(dummyPasswordValue)) {
            vaultConfiguration.setPassword(passwordValue);
        }
        
        vaultConfiguration.setParameterForKey(WebMailFileStore.SmtpServerKey, smtpField.getText());
        
        vaultConfiguration.setParameterForKey(WebMailFileStore.SmtpUseAuthenticationKey,
                smtpAuthenticationCheckbox.getSelection() ? "true" : "false");
        
        try {
            vault.updateContents(vaultConfiguration, stores);
        }
        catch (VaultException e) {
            Log.log(e);
            MessageDialog.openError(getShell(), "Error", "An error occurred while " +
            		"trying to save your configuration changes.  The changes " +
            		"were not saved.");            
        }
        
        if (driveCheckboxes != null) {
	        int numChecked = 0;
	        for (int i = 0; i < driveCheckboxes.length; i++) {
	            if (driveCheckboxes[i].getSelection()) {
	                numChecked++;
	            }
	        }
	        
	        visibleDrives = new File[numChecked];
	
	        for (int i = 0, j = 0; i < driveCheckboxes.length; i++) {
	            if (driveCheckboxes[i].getSelection()) {
	                visibleDrives[j++] = allDrives[i];
	            }
	        }
        }
    }
    
    private boolean validateFields()
    {
        String passwordValue = passwordField.getText();
        String confirmPasswordValue = confirmPasswordField.getText();

        if (!passwordValue.equals(confirmPasswordValue)) {
            MessageDialog.openError(getShell(), "Error", "The password has not " +
            		"been correctly confirmed.  The 'Password' and 'Confirm " +
            		"Password' fields should match to ensure that your password " +
            		"is recorded correctly.");
            return false;
        }        
        
        if (passwordValue.length() < 6) {
            MessageDialog.openError(getShell(), "Error", "The password must be " +
            		"at least 6 characters long.");
            return false;            
        }
        
        String smtpServer = smtpField.getText();
        if (smtpServer.length() == 0) {
            MessageDialog.openError(getShell(), "Error", "Please specify a " +
            		"valid SMTP server name (e.g. host.company.com).");            
            return false;
        }
        else {
            Matcher matcher = hostPattern.matcher(smtpServer);
            if (!matcher.matches() || smtpServer.charAt(0) == '.') {
                MessageDialog.openError(getShell(), "Error", smtpServer + " is not " +
                		"a valid SMTP server name (e.g. host.company.com).");
                return false;
            }            
        }
        
        if (smtpAuthenticationCheckbox.getSelection()) {
            String username = vaultConfiguration.parameterForKey(WebMailFileStore.SmtpUsernameKey);
            String password = vaultConfiguration.parameterForKey(WebMailFileStore.SmtpPasswordKey);
            
            if (username == null || username.length() == 0 || password == null || password.length() == 0) {
                MessageDialog.openError(getShell(), "Error", "You have indicated " +
                		"that your SMTP server requires authentication, but you " +
                		"have not configured FileBunker for this authentication.  " +
                		"Click the Configure button next to the 'Requires " +
                		"Authentication' checkbox, and provide the requested " +
                		"information.");
                return false;
            }
        }
        
        if (driveCheckboxes != null) {
	        boolean anyDrivesSelected = false;
	        for (int i = 0; i < driveCheckboxes.length; i++) {
	            if (driveCheckboxes[i].getSelection()) {
	                anyDrivesSelected = true;
	                break;
	            }
	        }
	        
	        if (!anyDrivesSelected) {
	            MessageDialog.openError(getShell(), "Error", "At least one drive must be " +
	            		"selected to show in the backup explorer.");
	            return false;                        
	        }
        }
        
        return true;
    }
    
    protected void okPressed()
    {
        if (validateFields()) {
            updateModelFromUI();
            super.okPressed();
        }
    }
    
    protected void createCustomContents(Composite parent)
    {                
        Composite contents = parent;

        contents.setLayout(new FormLayout());
                
        Control formArea = createFormArea(contents);
        FormData formAreaFormData = new FormData();
        formAreaFormData.top = new FormAttachment(0, 0);
        formAreaFormData.left = new FormAttachment(0, 0);
        formAreaFormData.right = new FormAttachment(100, 0);
        formArea.setLayoutData(formAreaFormData);

        Label repositoriesLabel = new Label(contents, SWT.NONE);
        repositoriesLabel.setText("Email Repositories");
        FormData repositoriesLabelFormData = new FormData();
        repositoriesLabelFormData.top = new FormAttachment(formArea, 20, SWT.BOTTOM);
        repositoriesLabelFormData.left = new FormAttachment(0, 0);
        repositoriesLabel.setLayoutData(repositoriesLabelFormData);

        Label repositoriesSeperator = new Label(contents, SWT.SEPARATOR | SWT.HORIZONTAL);
        FormData repositoriesSeperatorFormData = new FormData();
        repositoriesSeperatorFormData.top = new FormAttachment(repositoriesLabel, 0, SWT.CENTER);
        repositoriesSeperatorFormData.left = new FormAttachment(repositoriesLabel, 5);
        repositoriesSeperatorFormData.right = new FormAttachment(100, 0);
        repositoriesSeperator.setLayoutData(repositoriesSeperatorFormData);
        
        Composite repositoriesComposite = new Composite(contents, SWT.NONE);
        repositoriesComposite.setLayout(new FormLayout());

        FormData repositoriesCompositeFormData = new FormData();
        repositoriesCompositeFormData.top = new FormAttachment(repositoriesLabel, 20, SWT.BOTTOM);
        repositoriesCompositeFormData.left = new FormAttachment(0, 0);
        repositoriesCompositeFormData.right = new FormAttachment(100, 0);
        repositoriesComposite.setLayoutData(repositoriesCompositeFormData);
        
        createRepositoriesArea(repositoriesComposite);
        
        updateUIFromVaultConfiguration();
    }
    
    private Control createFormArea(Composite parent)
    {
        Composite contents = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        contents.setLayout(layout);

        // Password field
        
        Label passwordLabel = new Label(contents, SWT.NONE);
        passwordLabel.setText("FileBunker Password:");
        
        GridData passwordLabelGridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
        passwordLabel.setLayoutData(passwordLabelGridData);

        passwordField = new Text(contents, SWT.BORDER);
        setControlHint(passwordField, passwordFieldHint);
        passwordField.setEchoChar('*');
        GridData passwordFieldGridData = new GridData(GridData.FILL_HORIZONTAL);
        passwordField.setLayoutData(passwordFieldGridData);
        
        
        // Confirm password field

        Label confirmPasswordLabel = new Label(contents, SWT.NONE);
        confirmPasswordLabel.setText("Confirm Password:");

        GridData confirmPasswordLabelGridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
        confirmPasswordLabel.setLayoutData(confirmPasswordLabelGridData);

        confirmPasswordField = new Text(contents, SWT.BORDER);
        setControlHint(confirmPasswordField, confirmPasswordFieldHint);
        confirmPasswordField.setEchoChar('*');
        GridData confirmPasswordFieldGridData = new GridData(GridData.FILL_HORIZONTAL);
        confirmPasswordField.setLayoutData(confirmPasswordFieldGridData);


        // SMTP Server field
        
        Label smtpLabel = new Label(contents, SWT.NONE);
        smtpLabel.setText("SMTP Server:");

        GridData smtpLabelGridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
        smtpLabel.setLayoutData(smtpLabelGridData);
        
        smtpField = new Text(contents, SWT.BORDER);
        setControlHint(smtpField, smtpFieldHint);
        GridData smtpFieldGridData = new GridData(GridData.FILL_HORIZONTAL);
        smtpField.setLayoutData(smtpFieldGridData);
        
        Label blank = new Label(contents, SWT.NONE);
        
        Composite smtpAuthenticationComposite = new Composite(contents, SWT.NONE);
        smtpAuthenticationComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout smtpAuthenticationCompositeGridLayout = new GridLayout(2, false);
        smtpAuthenticationCompositeGridLayout.marginHeight = 0;
        smtpAuthenticationCompositeGridLayout.marginWidth = 0;
        smtpAuthenticationComposite.setLayout(smtpAuthenticationCompositeGridLayout);
        
        smtpAuthenticationCheckbox = new Button(smtpAuthenticationComposite, SWT.CHECK);
        setControlHint(smtpAuthenticationCheckbox, smtpAuthenticationHint);
        smtpAuthenticationCheckbox.setText("Requires Authentication");
        smtpAuthenticationCheckbox.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                updateConfigureButton();
            }
        });
        
        smtpAuthenticationConfigureButton = new Button(smtpAuthenticationComposite, SWT.NONE);
        setControlHint(smtpAuthenticationConfigureButton, smtpAuthenticationConfigureHint);
        smtpAuthenticationConfigureButton.setText("Configure...");

        smtpAuthenticationConfigureButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                configureClicked();
            }
        });
        
        // Show drives field

        if (Platform.isWindows()) {

            allDrives = File.listRoots();
            if (allDrives.length > 1) {
            
	            Label spacer = new Label(contents, SWT.NONE);
	            spacer.setLayoutData(new GridData(4, 4));
	            Label spacer2 = new Label(contents, SWT.NONE);
	            spacer2.setLayoutData(new GridData(4, 4));
	
	            Label visibleDrivesLabel = new Label(contents, SWT.NONE);
	            visibleDrivesLabel.setText("Show Drives:");
	            
	            GridData visibleDrivesLabelGridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
	            visibleDrivesLabel.setLayoutData(visibleDrivesLabelGridData);
	
	            Composite drivesComposite = new Composite(contents, SWT.NONE);
	            GridData drivesCompositeGridData = new GridData(GridData.FILL_HORIZONTAL);
	            drivesComposite.setLayoutData(drivesCompositeGridData);
	            GridLayout gridLayout = new GridLayout();
	            gridLayout.numColumns = 5;
	            gridLayout.verticalSpacing = 3;
	            gridLayout.horizontalSpacing = 15;
	            gridLayout.marginWidth = 0;
	            gridLayout.marginHeight = 0;
	            drivesComposite.setLayout(gridLayout);
	            
	            driveCheckboxes = new Button[allDrives.length];
	            
	            for (int i = 0; i < allDrives.length; i++) {
	                driveCheckboxes[i] = new Button(drivesComposite, SWT.CHECK);
	                driveCheckboxes[i].setText(allDrives[i].getPath());
	                
	                setControlHint(driveCheckboxes[i], visibleDrivesHint);
	                
	                for (int j = 0; j < visibleDrives.length; j++) {
	                    if (allDrives[i].equals(visibleDrives[j])) {
	                        driveCheckboxes[i].setSelection(true);
	                        break;
	                    }
	                }
	            }
            }
        }
                
        return contents;
    }    

    private void createRepositoriesArea(Composite parent)
    {
        Composite buttonsComposite = new Composite(parent, SWT.NONE);
        GridLayout buttonsLayout = new GridLayout(1, true);
        buttonsLayout.marginHeight = 0;
        buttonsLayout.marginWidth = 0;
        buttonsLayout.verticalSpacing = 5;
        buttonsComposite.setLayout(buttonsLayout);
        
        FormData buttonsCompositeFormData = new FormData();
        buttonsCompositeFormData.top = new FormAttachment(0, 0);
        buttonsCompositeFormData.right = new FormAttachment(100, 0);
        buttonsComposite.setLayoutData(buttonsCompositeFormData);
        
        newButton = new Button(buttonsComposite, SWT.NONE);
        setControlHint(newButton, newButtonHint);
        newButton.setText("New");
        
        editButton = new Button(buttonsComposite, SWT.NONE);
        setControlHint(editButton, editButtonHint);
        editButton.setText("Edit");
        
        deleteButton = new Button(buttonsComposite, SWT.NONE);
        setControlHint(deleteButton, deleteButtonHint);
        deleteButton.setText("Delete");

        Point deleteSize = deleteButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        int buttonWidth = (int)(deleteSize.x * 1.5);
        
        GridData newButtonGridData = new GridData(GridData.FILL_HORIZONTAL);
        newButtonGridData.widthHint = buttonWidth;
        newButton.setLayoutData(newButtonGridData);
        
        GridData editButtonGridData = new GridData(GridData.FILL_HORIZONTAL);
        editButtonGridData.widthHint = buttonWidth;
        editButton.setLayoutData(editButtonGridData);
        
        GridData deleteButtonGridData = new GridData(GridData.FILL_HORIZONTAL);
        deleteButtonGridData.widthHint = buttonWidth;
        deleteButton.setLayoutData(deleteButtonGridData);
        
        newButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                newClicked();
            }
        });

        editButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                editClicked();
            }
        });

        deleteButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                deleteClicked();
            }
        });

        
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        
        storeListViewer = new ListViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        FormData storeListViewerFormData = new FormData();
        storeListViewerFormData.top = new FormAttachment(buttonsComposite, 0, SWT.TOP);
        storeListViewerFormData.left = new FormAttachment(0, 0);
        storeListViewerFormData.right = new FormAttachment(buttonsComposite, -15);
        storeListViewerFormData.bottom = new FormAttachment(100, -10);
        storeListViewerFormData.height = 100;

        setControlHint(storeListViewer.getList(), storeListViewerHint);
        
        storeListViewer.getControl().setLayoutData(storeListViewerFormData);
        
        storeListViewer.setContentProvider(new FileStoreListContentProvider(stores));
        storeListViewer.setLabelProvider(new FileStoreListLabelProvider());
        storeListViewer.setInput(new Object());        
        
        storeListViewer.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                IStructuredSelection selection = (IStructuredSelection)event.getSelection();
                
                editButton.setEnabled(!selection.isEmpty());
                deleteButton.setEnabled(!selection.isEmpty());
            }
        });
        
        storeListViewer.addDoubleClickListener(new IDoubleClickListener()
        {
            public void doubleClick(DoubleClickEvent event)
            {
                editClicked();
            }
        });

        Object first = storeListViewer.getElementAt(0);
        if (first != null) {
            storeListViewer.setSelection(new StructuredSelection(first));
        }
        
    }

    private void updateConfigureButton()
    {
        smtpAuthenticationConfigureButton.setEnabled(smtpAuthenticationCheckbox.getSelection());
    }

    private void configureClicked()
    {
        new ConfigureSmtpAuthenticationDialog(getShell(), vaultConfiguration).open();
    }

    private void newClicked()
    {
        GMailFileStore store = new GMailFileStore();

        store.setConfiguration(vaultConfiguration);
        int returnCode = new EditWebMailFileStoreDialog(getShell(), true, vault, store, stores).open();
        if (returnCode == IDialogConstants.OK_ID) {
            stores.add(store);
            storeListViewer.refresh();
        }
    }
    
    private void editClicked()
    {        
        GMailFileStore store = (GMailFileStore)((IStructuredSelection)storeListViewer.getSelection()).getFirstElement();        
        int returnCode = new EditWebMailFileStoreDialog(getShell(), true, vault, store, stores).open();
        if (returnCode == IDialogConstants.OK_ID) {
            storeListViewer.refresh();
        }
    }
    
    private void deleteClicked()
    {
        GMailFileStore store = (GMailFileStore)((IStructuredSelection)storeListViewer.getSelection()).getFirstElement();        
        boolean ok = false;
        
        if (vault.doesStoreContainBackupFiles(store)) {
            ok = MessageDialog.openQuestion(getShell(), "Warning",
                    "Files have previously been backed up to " +
                    store.email() + " and will not be accessible if you " +
                    "delete it.  Are you sure you want to delete " +
                    store.email() + "?");
        }
        else {
            ok = MessageDialog.openQuestion(getShell(), "Warning",
                    "Are you sure you want to delete " + store.email() + "?");
        }
        
        if (ok) {
            stores.remove(store);
            storeListViewer.refresh();            
        }
    }

}
