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
 * Created on Sep 2, 2004
 */
package com.toubassi.jface;

import java.text.Format;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * @author garrick
 */
public class ObjectChooserDialog extends ContextHintDialog
{
    private Object[] objects;
    private String title;
    private String okLabel;
    private ListViewer listViewer;
    private Object chosenObject;
    private Object selectedObject;
    private Format format;
    
    public ObjectChooserDialog(Shell shell, Object[] objects, String title, String infoTitle, String description)
    {
        this(shell, objects, title, infoTitle, description, IDialogConstants.OK_LABEL);
    }
    
    public ObjectChooserDialog(Shell shell, Object[] objects, String title, String infoTitle, String description, String okLabel)
    {
        super(shell);
        this.objects = objects;
        this.title = title;
        this.okLabel = okLabel;
        setInfoTitle(infoTitle);
        setDefaultHint(description);
    }
    
    public void setFormat(Format format)
    {
        this.format = format;
    }

    public void setSelection(Object object)
    {
        selectedObject = object;
    }
    
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, okLabel, false);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
    }
    
    public Object run()
    {
        chosenObject = null;
        open();        
        return chosenObject;
    }
    
    protected void okPressed()
    {
        IStructuredSelection selection = (IStructuredSelection)listViewer.getSelection();
        
        if (!selection.isEmpty()) {
            chosenObject = selection.getFirstElement();
            close();
        }        
    }

    public void create()
    {
        super.create();
        updateOkButton();
    }
    
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText(title);
    }
    
    protected void createCustomContents(Composite parent)
    {                
        parent.setLayout(new FormLayout());
        
        listViewer = new ListViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        FormData listViewerFormData = new FormData();
        listViewerFormData.top = new FormAttachment(0, 0);
        listViewerFormData.left = new FormAttachment(0, 0);
        listViewerFormData.right = new FormAttachment(100, 0);
        listViewerFormData.bottom = new FormAttachment(100, 0);
        listViewerFormData.height = 100;
        
        listViewer.getControl().setLayoutData(listViewerFormData);
        
        listViewer.setContentProvider(new ArrayContentProvider());
        listViewer.setLabelProvider(new FormatLabelProvider(format));
        listViewer.setInput(objects);        
        
        listViewer.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                updateOkButton();
            }
        });
        
        listViewer.addDoubleClickListener(new IDoubleClickListener()
        {
            public void doubleClick(DoubleClickEvent event)
            {
                okPressed();
            }    
        });

        StructuredSelection selection;
        if (selectedObject == null) {
            selection = new StructuredSelection(objects[0]);
        }
        else {
            selection = new StructuredSelection(selectedObject);
        }
        listViewer.setSelection(selection);
    }
    
    private void updateOkButton()
    {
        Button button = getButton(IDialogConstants.OK_ID);
        
        if (button != null) {
            button.setEnabled(!listViewer.getSelection().isEmpty());
        }
    }

}

class FormatLabelProvider implements ILabelProvider
{
    private Format format;
    
    public FormatLabelProvider(Format format)
    {
        this.format = format;
    }

    public Image getImage(Object element)
    {
        return null;
    }

    public String getText(Object element)
    {
        return format == null ? element.toString() : format.format(element);
    }

    public void addListener(ILabelProviderListener listener)
    {
    }

    public void dispose()
    {
    }

    public boolean isLabelProperty(Object element, String property)
    {
        return false;
    }

    public void removeListener(ILabelProviderListener listener)
    {
    }
}
