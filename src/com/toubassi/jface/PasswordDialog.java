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
package com.toubassi.jface;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author garrick
 */
public class PasswordDialog extends ContextHintDialog
{
    public interface Authenticator
    {
        public boolean authenticate(String password);
    }
    
    private Authenticator authenticator;
    private Text passwordField;
    private Label errorLabel;
    
	public PasswordDialog(Shell parentShell, String infoTitle, Authenticator authenticator)
	{
	    super(parentShell);
	    this.authenticator = authenticator;
	    setInfoTitle(infoTitle);
	}
	
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText("Password");
    }    
	
    protected void createCustomContents(Composite parent)
    {
        parent.setLayout(new FormLayout());
        
        Label passwordLabel = new Label(parent, SWT.NONE);
        passwordLabel.setText("Password:");
        
        passwordField = new Text(parent, SWT.BORDER);
        passwordField.setEchoChar('*');
        FormData passwordFieldFormData = new FormData();
        passwordFieldFormData.left = new FormAttachment(passwordLabel, 10);
        passwordFieldFormData.right = new FormAttachment(100, 0);
        passwordField.setLayoutData(passwordFieldFormData);

        FormData passwordLabelFormData = new FormData();
        passwordLabelFormData.top = new FormAttachment(passwordField, 0, SWT.CENTER);
        passwordLabelFormData.left = new FormAttachment(0, 0);
        passwordLabel.setLayoutData(passwordLabelFormData);
        
        errorLabel = new Label(parent, SWT.NONE);
        errorLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
        FormData errorLabelFormData = new FormData();
        errorLabelFormData.top = new FormAttachment(passwordField, 5);
        errorLabelFormData.left = new FormAttachment(passwordField, 0, SWT.LEFT);
        errorLabelFormData.right = new FormAttachment(passwordField, 0, SWT.RIGHT);
        errorLabel.setLayoutData(errorLabelFormData);
    }
	
	protected void okPressed()
	{
	    if (authenticator.authenticate(passwordField.getText())) {
	        super.okPressed();
	    }
	    else {
		    errorLabel.setText("The password is invalid.");
		    errorLabel.getParent().update();
	        getShell().getDisplay().beep();
		    passwordField.setFocus();
		    passwordField.selectAll();
	    }
	}
}
