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
 * Created on Aug 4, 2004
 */
package com.toubassi.jface;


import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;

/**
 * @author garrick
 */
public class ExplorerCheckStateListener implements ICheckStateListener
{
    private CheckboxTreeViewer treeViewer;
    private CheckboxTableViewer tableViewer;
    private boolean ignoreCheckStateChanges;

    public ExplorerCheckStateListener(CheckboxTreeViewer treeViewer, CheckboxTableViewer tableViewer)
    {
        this.treeViewer = treeViewer;
        this.tableViewer = tableViewer;
    }
    
    private ICheckboxTreeContentProvider contentProvider()
    {
        return (ICheckboxTreeContentProvider)treeViewer.getContentProvider();
    }

    public void updateTreeCheckboxes()
    {
        updateTreeCheckboxesForAncestor(null);
    }
    
    public void updateTreeCheckboxesForAncestor(Object element)
    {                
        ICheckboxTreeContentProvider contentProvider = contentProvider();
        
        if (element != null) {
            treeViewer.setChecked(element, contentProvider.isChecked(element));
        }
        
        if (element == null || treeViewer.getExpandedState(element)) {
            Object[] children;
            
            if (element != null) {
                children = contentProvider.getChildren(element);
            }
            else {
                children = contentProvider.getElements(treeViewer.getInput());
            }
            
            for (int i = 0; i < children.length; i++) {
                updateTreeCheckboxesForAncestor(children[i]);
            }
        }
    }
    
    public void updateTableCheckboxes()
    {
        Object input = tableViewer.getInput();

        if (input != null) {
            ICheckboxTreeContentProvider contentProvider = contentProvider();
	        Object[] children = (Object[])contentProvider.getChildren(input);
	        
	        for (int i = 0; i < children.length; i++) {
	            Object child = (Object)children[i];
	            
	            tableViewer.setChecked(child, contentProvider.isChecked(child));
	        }
        }
    }
    
    public void checkStateChanged(CheckStateChangedEvent event)
    {
        if (ignoreCheckStateChanges) {
            return;
        }
        
        ICheckboxTreeContentProvider contentProvider = contentProvider();
        Object element = event.getElement();
        
        // Update the model
        contentProvider.setCheckedState(element, event.getChecked());
        
        // Update the UI
        ignoreCheckStateChanges = true;
        try {
            updateTreeCheckboxesForAncestor(element);
            updateTableCheckboxes();
        }
        finally {
            ignoreCheckStateChanges = false;
        }
    }
}
