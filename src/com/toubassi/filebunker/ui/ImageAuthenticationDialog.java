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
 * Created on Sep 7, 2004
 */
package com.toubassi.filebunker.ui;

import com.toubassi.jface.ContextHintDialog;

import java.io.IOException;
import java.net.URL;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author garrick
 */
public class ImageAuthenticationDialog extends ContextHintDialog
{
    private Image image;
    private Text imageTextField;
    private String result;
    
	public ImageAuthenticationDialog(Shell parentShell, URL imageUrl) throws IOException
	{
	    super(parentShell);
	    image = new Image(parentShell.getDisplay(), imageUrl.openStream());
	    setInfoTitle("GMail User Authentication");
	    setDefaultHint("Please type in the characters you see in the picture " +
	    		"below.  This is required from time to time in order to " +
	    		"grant access to your GMail account.");
	}
	
	public String run()
	{
        if (open() == IDialogConstants.OK_ID) {
            return result;
        }
        return null;	    
	}
	
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText("Authentication");
    }    
	
    protected void createCustomContents(Composite parent)
    {
        parent.setLayout(new FormLayout());
        
        Label imageLabel = new Label(parent, SWT.NONE);
        imageLabel.setImage(image);
        
        FormData imageLabelFormData = new FormData();
        imageLabelFormData.top = new FormAttachment(0, 10);
        imageLabelFormData.left = new FormAttachment(parent, 0, SWT.CENTER);
        imageLabel.setLayoutData(imageLabelFormData);
                       
        Composite imageTextComposite = new Composite(parent, SWT.NONE);
        imageTextComposite.setLayout(new GridLayout(2, false));

        FormData imageTextCompositeFormData = new FormData();
        imageTextCompositeFormData.top = new FormAttachment(imageLabel, 15);
        imageTextCompositeFormData.left = new FormAttachment(imageLabel, 0, SWT.CENTER);
        imageTextComposite.setLayoutData(imageTextCompositeFormData);
        
        Label imageTextLabel = new Label(imageTextComposite, SWT.NONE);
        imageTextLabel.setText("Characters from the above picture:");

        GridData imageTextLabelGridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
        imageTextLabel.setLayoutData(imageTextLabelGridData);
        
        imageTextField = new Text(imageTextComposite, SWT.BORDER);
        GridData imageTextFieldGridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
        imageTextField.setLayoutData(imageTextFieldGridData);
    }
    
    protected void okPressed()
    {
        result = imageTextField.getText();
        super.okPressed();        
    }
}
