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
package com.toubassi.filebunker.ui;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * @author garrick
 */
public class AboutDialog extends Dialog
{
    private static String version;
    private static final int VIEW_LICENSE_ID = 100;
    
    private Font headerFont;
    private Font bodyFont;
    
    public AboutDialog(Shell shell)
    {
        super(shell);
        headerFont = new Font(shell.getDisplay(), "Times New Roman", 36, SWT.NONE);
        bodyFont = new Font(shell.getDisplay(), "Helvetica", 10, SWT.NONE);
        
        if (version == null) {
            File versionFile = ShowDocumentAction.findDocument("version.txt");
            
            if (versionFile != null) {
                try {
	                FileReader fileReader = new FileReader(versionFile);
	                BufferedReader bufferedReader = new BufferedReader(fileReader);
	                version = bufferedReader.readLine();                
	                bufferedReader.close();
                }
                catch (IOException e) {
                }
            }
            
            if (version == null) {
                version = "";
            }
        }
    }
    
    public boolean close()
    {
        boolean retVal = super.close();

        if (retVal) {
            headerFont.dispose();            
            bodyFont.dispose();            
        }        
        return retVal;
    }
        
    protected Control createDialogArea(Composite parent)
    {
        Label spacer;

        Composite contents = new Composite(parent, SWT.NONE);
        RowLayout layout = new RowLayout(SWT.VERTICAL);
        layout.fill = true;
        layout.marginHeight = 10;
        layout.marginWidth = 30;        
        layout.spacing = 0;
        contents.setLayout(layout);
        
        Label header = new Label(contents, SWT.CENTER);
        header.setFont(headerFont);
        header.setText("FileBunker");
        
        Label versionLabel = new Label(contents, SWT.CENTER);
        versionLabel.setText("Version " + version + "   Copyright 2004  Garrick Toubassi");

        /*
        //Spacer
        new Label(contents, SWT.NONE);
        new Label(contents, SWT.NONE);

        StyledText text = new ReadOnlyStyledText(contents, SWT.WRAP);
        text.setBackground(contents.getBackground());
        text.setFont(bodyFont);
        String prefix = "For more information, to file suggestions, or to\n" +
	    	"report bugs, visit ";
        String link = "filebunker.sourceforget.net";
        String suffix = ".";
        text.setText(prefix + link + suffix);

        StyleRange style = new StyleRange(prefix.length(), link.length(), contents.getDisplay().getSystemColor(SWT.COLOR_BLUE), null);
        style.fontStyle = SWT.BOLD;
        text.setStyleRange(style);
        */
        
        return contents;
    }

    protected void createButtonsForButtonBar(Composite parent)
    {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, VIEW_LICENSE_ID, "View License", false);
		
		getButton(IDialogConstants.OK_ID).setFocus();
    }
    
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == VIEW_LICENSE_ID) {
            File file = ShowDocumentAction.findDocument("License.html");
            if (file != null) {
                Program.launch(file.getPath());
            }
            return;
        }
        super.buttonPressed(buttonId);
    }
}
