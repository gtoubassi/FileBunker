/*
 * Created on Sep 11, 2004
 */
package com.toubassi.filebunker.ui.configuration;

import com.toubassi.filebunker.vault.VaultConfiguration;
import com.toubassi.filebunker.vault.WebMailFileStore;
import com.toubassi.jface.ContextHintDialog;

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
public class ConfigureSmtpAuthenticationDialog extends ContextHintDialog
{
    private static final String dummyPasswordValue = "__dumy__";

    private static final String usernameFieldHint =
        "Please enter the user name used to authenticate yourself to the SMTP server.";
    
    private static final String passwordFieldHint =
        "Please enter the password used to authenticate yourself to the SMTP server.";
    
    private static final String confirmPasswordFieldHint =
        "Please confirm the password used to authenticate yourself to the SMTP server.";

    private VaultConfiguration config;
    private Text usernameField;
    private Text passwordField;
    private Text confirmPasswordField;
    
    public ConfigureSmtpAuthenticationDialog(Shell shell, VaultConfiguration config)
    {
        super(shell);
        this.config = config;
        setInfoTitle("Configure SMTP Authentication");
    }
    
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText("SMTP Authentication");
    }
    
    public void updateModelFromUI()
    {
        String username = usernameField.getText();
        String password = passwordField.getText();

        config.setParameterForKey(WebMailFileStore.SmtpUsernameKey, username);
        config.setParameterForKey(WebMailFileStore.SmtpPasswordKey, password, true);
    }
    
    public void updateUIFromModel()
    {
        String username = config.parameterForKey(WebMailFileStore.SmtpUsernameKey);
        String password = config.parameterForKey(WebMailFileStore.SmtpPasswordKey);
        
        usernameField.setText(username == null ? "" : username);
        
        if (password == null || password.length() == 0) {
            passwordField.setText("");
        }
        else {
            passwordField.setText(dummyPasswordValue);
        }
        confirmPasswordField.setText(passwordField.getText());        
    }
    
    public boolean validateFields()
    {
        if (usernameField.getText().length() == 0) {
            MessageDialog.openError(getShell(), "Error", "Please specify a " +
            	"user name.");
            return false;                        
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
            MessageDialog.openError(getShell(), "Error", "Please specify a password.");
            return false;            
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
        FormLayout layout = new FormLayout();
        parent.setLayout(layout);
        
        Control formArea = createFormArea(parent);
        FormData formAreaFormData = new FormData();
        formAreaFormData.top = new FormAttachment(0, 0);
        formAreaFormData.left = new FormAttachment(0, 0);
        formAreaFormData.bottom = new FormAttachment(100, 0);
        formAreaFormData.right = new FormAttachment(100, 0);
        formArea.setLayoutData(formAreaFormData);
        
        updateUIFromModel();
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
        
        
        Label usernameLabel = new Label(contents, SWT.NONE);
        usernameLabel.setText("User name:");
        
        GridData usernameLabelGridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
        usernameLabel.setLayoutData(usernameLabelGridData);

        usernameField = new Text(contents, SWT.BORDER);
        setControlHint(usernameField, usernameFieldHint);
        GridData usernameFieldGridData = new GridData(GridData.FILL_HORIZONTAL);
        usernameField.setLayoutData(usernameFieldGridData);
        
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
