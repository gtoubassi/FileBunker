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
 * Created on Aug 14, 2004
 */
package com.toubassi.filebunker.ui.configuration;

import com.toubassi.filebunker.ui.Log;
import com.toubassi.filebunker.vault.FileStore;
import com.toubassi.filebunker.vault.GMailFileStore;
import com.toubassi.filebunker.vault.Vault;
import com.toubassi.filebunker.vault.VaultException;
import com.toubassi.filebunker.vault.WebMailFileStore;
import com.toubassi.jface.ContextHintDialog;

import java.util.ArrayList;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author garrick
 */
public class EditWebMailFileStoreDialog extends ContextHintDialog
{
    private static final String dummyPasswordValue = "__dumy__";

    private static final String emailFieldHint =
        "Please enter the email address for a valid Gmail account.  This " +
        "account will be used as a repository for your backed up files.";
    
    private static final String passwordFieldHint =
        "Please enter the password used to login to your gmail account.";
    
    private static final String confirmPasswordFieldHint =
        "Please confirm the password used to login to your gmail account.";

    private boolean isNew;
    private Vault vault;
    private GMailFileStore store;
    private ArrayList stores;
    
    private Text emailField;
    private Text passwordField;
    private Text confirmPasswordField;
    
    
    public EditWebMailFileStoreDialog(Shell shell, boolean isNew, Vault vault, GMailFileStore store, ArrayList stores)
    {
        super(shell);
        this.vault = vault;
        this.store = store;
        this.stores = stores;
        this.isNew = isNew;
        if (isNew) {
            setInfoTitle("Create a new email repository");
        }
        else {
            setInfoTitle("Edit the " + store.email() + " repository");
        }
    }
    
    public GMailFileStore run()
    {
        if (open() == IDialogConstants.OK_ID) {
            return store;
        }
        return null;
    }

    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText(isNew ? "New Email Repository" : "Edit Email Repository");
    }

    private void updateStoreFromUI()
    {
        try {
            store.setEmailAddress(emailField.getText() + "@gmail.com");
            store.setAccountPassword(passwordField.getText());
        }
        catch (AddressException e) {
            // Should not get this because validateFields would have caught it.
            Log.log(e);
        }
        catch (VaultException e) {
            Log.log(e);
        }
    }
    
    private void updateUIFromStore()
    {
        String username = store.username();            
        emailField.setText(username == null ? "" : username);
        
        String password = null;
        
        try {
            password = store.accountPassword();
        }
        catch (VaultException e) {
            Log.log(e);
        }
        
        if (password == null || password.length() == 0) {
            passwordField.setText("");
        }
        else {
            passwordField.setText(dummyPasswordValue);
        }
        
        confirmPasswordField.setText(passwordField.getText());
    }
    
    private boolean validateFields()
    {
        if (emailField.getText().length() == 0) {
            MessageDialog.openError(getShell(), "Error", "Please specify a " +
            	"valid gmail address for this email repository.");
            return false;            
        }
        else {
            String address = emailField.getText() + "@gmail.com";
            
	        try {
	            new InternetAddress(address);
	        }
	        catch (AddressException e) {
	            MessageDialog.openError(getShell(), "Error", "Please specify a " +
	            	"valid gmail address for this email repository.");
	            return false;
	        }
	        
	        for (int i = 0; i < stores.size(); i++) {
	            FileStore otherStore = (FileStore)stores.get(i);
	            
	            if (otherStore instanceof WebMailFileStore) {
	                WebMailFileStore webMailStore = (WebMailFileStore)otherStore;

	                if (webMailStore != store && webMailStore.email().equals(address)) {
	    	            MessageDialog.openError(getShell(), "Error", "You have already " +
	    	            		"defined an email repository for " + address + ".  Please " +
   	            				"specify a unique email address.");
	    	            return false;
		            }
	            }
	        }
	        
	        // If the store's email was changed, and the store does contain backup files,
	        // then warn the user because changing the name is tantamount to deleting the
	        // old store (and the files backed up to that store cannot be restored).
	        if (store.email() != null && !address.equals(store.email()) && vault.doesStoreContainBackupFiles(store)) {
	            boolean ok = MessageDialog.openQuestion(getShell(), "Warning",
	                    "You have changed the email address to " + address +
	                    ".  Files have previously been backed up to " +
	                    store.email() + " and will not be accessible if you " +
	                    "proceed with the change of email address.  Do you want " +
	                    "to proceed?");
	            if (!ok) {
	                return false;
	            }
	        }
        }

        String passwordValue = passwordField.getText();
        String confirmPasswordValue = confirmPasswordField.getText();
        
        if (!passwordValue.equals(confirmPasswordValue)) {
            MessageDialog.openError(getShell(), "Error", "The password has not " +
            		"been correctly confirmed.  The 'Password' and 'Confirm " +
            		"Password' fields should match to ensure that your password " +
            		"is recorded correctly.");
            return false;
        }        
        
        if (passwordValue.length() == 0) {
            MessageDialog.openError(getShell(), "Error", "Please specify the password " +
            		"that you use to login to your gmail account.");
            return false;            
        }
        
        return true;
    }

    protected void okPressed()
    {
        if (validateFields()) {
            updateStoreFromUI();
            super.okPressed();
        }
    }

    protected void createCustomContents(Composite parent)
    {
        FormLayout layout = new FormLayout();
        parent.setLayout(layout);
        
        Control formArea = createFormArea(parent);
        FormData formAreaFormData = new FormData();
        formAreaFormData.top = new FormAttachment(0, 0);
        formAreaFormData.left = new FormAttachment(0, 0);
        formAreaFormData.bottom = new FormAttachment(100, 0);
        formAreaFormData.right = new FormAttachment(100, 0);
        formArea.setLayoutData(formAreaFormData);
        
        updateUIFromStore();
    }
    
    private Control createFormArea(Composite parent)
    {
        Composite contents = new Composite(parent, SWT.NONE);

        FormData contentsFormData = new FormData();
        contentsFormData.top = new FormAttachment(0, 0);
        contentsFormData.left = new FormAttachment(0, 0);
        contentsFormData.right = new FormAttachment(100, 0);
        contents.setLayoutData(contentsFormData);
        
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        contents.setLayout(layout);

        Label emailLabel = new Label(contents, SWT.NONE);
        emailLabel.setText("Email:");

        GridData emailLabelGridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
        emailLabel.setLayoutData(emailLabelGridData);

        Composite emailFieldComposite = new Composite(contents, SWT.NONE);
        emailFieldComposite.setLayout(new FormLayout());
        
        Label atGmailLabel = new Label(emailFieldComposite, SWT.NONE);
        atGmailLabel.setText("@gmail.com");

        emailField = new Text(emailFieldComposite, SWT.BORDER | SWT.RIGHT);
        setControlHint(emailField, emailFieldHint);
        FormData emailFieldFormData = new FormData();
        emailFieldFormData.left = new FormAttachment(0, 0);        
        emailFieldFormData.right = new FormAttachment(atGmailLabel, -3);        
        emailField.setLayoutData(emailFieldFormData);

        FormData atGmailLabelFormData = new FormData();
        atGmailLabelFormData.top = new FormAttachment(emailField, 0, SWT.CENTER);        
        atGmailLabelFormData.right = new FormAttachment(100, 0);        
        atGmailLabel.setLayoutData(atGmailLabelFormData);
        
        GridData emailFieldCompositeGridData = new GridData(GridData.FILL_HORIZONTAL);
        emailFieldComposite.setLayoutData(emailFieldCompositeGridData);
        
        
        Label passwordLabel = new Label(contents, SWT.NONE);
        passwordLabel.setText("Password:");
        
        GridData passwordLabelGridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
        passwordLabel.setLayoutData(passwordLabelGridData);

        passwordField = new Text(contents, SWT.BORDER);
        setControlHint(passwordField, passwordFieldHint);
        passwordField.setEchoChar('*');
        GridData passwordFieldGridData = new GridData(GridData.FILL_HORIZONTAL);
        passwordField.setLayoutData(passwordFieldGridData);
        
        
        Label confirmPasswordLabel = new Label(contents, SWT.NONE);
        confirmPasswordLabel.setText("Confirm Password:");

        GridData confirmPasswordLabelGridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
        confirmPasswordLabel.setLayoutData(confirmPasswordLabelGridData);

        confirmPasswordField = new Text(contents, SWT.BORDER);
        setControlHint(confirmPasswordField, confirmPasswordFieldHint);
        confirmPasswordField.setEchoChar('*');
        GridData confirmPasswordFieldGridData = new GridData(GridData.FILL_HORIZONTAL);
        confirmPasswordField.setLayoutData(confirmPasswordFieldGridData);
        
        return contents;
    }

}
