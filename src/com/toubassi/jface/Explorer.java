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
 * Created on Aug 18, 2004
 */
package com.toubassi.jface;

import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;

import java.util.ArrayList;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;

/**
 * @author garrick
 */
public class Explorer extends Composite
{
    private boolean isChecked;
    
    private ITreeContentProvider contentProvider;
    private ICheckboxTreeContentProvider checkboxContentProvider;
    private ITableLabelProvider labelProvider;

    private SashForm sash;
    private TreeViewer treeViewer;
    private TableViewer tableViewer;
    private CheckboxTreeViewer checkboxTreeViewer;
    private CheckboxTableViewer checkboxTableViewer;
    private ExplorerCheckStateListener checkStateListener;
    private Viewer lastSelectedViewer;
    private HideLeavesFilter hideLeavesFilter;
    
    private ArrayList selectionListeners;
    private ArrayList tableDoubleClickListeners;
    private boolean ignoreSelectionChanges;
    
    public Explorer(Composite parent, String[] columnTitles, int[] columnWidths, int style)
    {
        super(parent, style & (~SWT.CHECK));
        isChecked = (style & SWT.CHECK) != 0;
        createContents(columnTitles, columnWidths);
        selectionListeners = new ArrayList();
        tableDoubleClickListeners = new ArrayList();
    }
    
    public void setWeights(int leftSide, int rightSide)
    {
        int[] weights = {leftSide, rightSide};
        
       sash.setWeights(weights);
    }
    
    public void setTableContextMenu(MenuManager menuManager)
    {
        Table table = tableViewer.getTable();
        table.setMenu(menuManager.createContextMenu(table));
    }
    
    public void setTreeContextMenu(MenuManager menuManager)
    {
        Tree tree = treeViewer.getTree();
        tree.setMenu(menuManager.createContextMenu(tree));
    }
    
    /**
     * Note this should actually be a ICheckboxTreeContentProvider if
     * the Exlorer was created with the CHECK style.
     * @param contentProvider
     */
    public void setContentProvider(ITreeContentProvider contentProvider)
    {
        this.contentProvider = contentProvider;
        if (isChecked) {
            checkboxContentProvider = (ICheckboxTreeContentProvider)contentProvider;
        }
        
        treeViewer.setContentProvider(contentProvider);
        tableViewer.setContentProvider(new ExplorerTableContentProvider(contentProvider));
    
        if (hideLeavesFilter != null) {
            hideLeavesFilter.setContentProvider(contentProvider);
        }
    }
    
    public ITreeContentProvider getContentProvider()
    {
        return contentProvider;
    }
    
    public void setLabelProvider(ITableLabelProvider labelProvider)
    {
        this.labelProvider = labelProvider;
        treeViewer.setLabelProvider(new ExplorerTreeLabelProvider(labelProvider));
        tableViewer.setLabelProvider(labelProvider);
    }
    
    public ITableLabelProvider getLabelProvider()
    {
        return labelProvider;
    }
    
    public void setHideLeavesInTree(boolean flag)
    {
        if (flag) {
            if (hideLeavesFilter == null) {
                hideLeavesFilter = new HideLeavesFilter(contentProvider);
            }
            addTreeFilter(hideLeavesFilter);
        }
        else if (hideLeavesFilter != null) {
            treeViewer.removeFilter(hideLeavesFilter);            
        }
    }
    
    public void addTreeFilter(ViewerFilter filter)
    {
        treeViewer.addFilter(filter);
    }
    
    public void setSorter(ViewerSorter sorter)
    {
        treeViewer.setSorter(sorter);
        tableViewer.setSorter(sorter);
    }
    
    public void addTableFilter(ViewerFilter filter)
    {
        tableViewer.addFilter(filter);
    }
        
    public Object[] getExpandedElements()
    {
        return treeViewer.getExpandedElements();
    }
    
    public void setExpandedState(Object element, boolean expanded)
    {
        treeViewer.setExpandedState(element, expanded);
        if (isChecked) {
            checkStateListener.updateTreeCheckboxesForAncestor(element);            
        }
    }
    
    public StructuredSelection getTreeSelection()
    {
	    return (StructuredSelection)treeViewer.getSelection();
    }
    
    public void setTreeSelection(StructuredSelection selection)
    {
        treeViewer.setSelection(selection);
    }
    
    public StructuredSelection getTableSelection()
    {
	    return (StructuredSelection)tableViewer.getSelection();        
    }
    
    public void setTableSelection(StructuredSelection selection)
    {
	    tableViewer.setSelection(selection);        
    }
    
    /**
     * Gets the selection from the last view that the user interacted
     * with that had a selection (either table or tree).
     * @return
     */
    public StructuredSelection getSelection()
    {
        StructuredSelection selection = StructuredSelection.EMPTY;
        
        if (lastSelectedViewer == tableViewer) {
            selection = getTableSelection();
            if (selection.isEmpty()) {
                selection = getTreeSelection();
            }
        }
        else {
            selection = getTreeSelection();
            if (selection.isEmpty()) {
                selection = getTableSelection();
            }            
        }
        return selection;
    }
    
    public void addTreeSelectionListener(ISelectionChangedListener listener)
    {
        treeViewer.addSelectionChangedListener(listener);
    }
    
    public void removeTreeSelectionListener(ISelectionChangedListener listener)
    {
        treeViewer.removeSelectionChangedListener(listener);
    }
    
    public void addTableSelectionListener(ISelectionChangedListener listener)
    {
        tableViewer.addSelectionChangedListener(listener);
    }
    
    public void removeTableSelectionListener(ISelectionChangedListener listener)
    {
        tableViewer.removeSelectionChangedListener(listener);
    }
    
    public void addSelectionListener(ISelectionChangedListener listener)
    {
        selectionListeners.add(listener);
    }
    
    public void removeSelectionListener(ISelectionChangedListener listener)
    {
        selectionListeners.remove(listener);
    }
    
    private void propagateSelectionChanged(SelectionChangedEvent event, Viewer viewer)
    {
        if (!ignoreSelectionChanges) {
	        lastSelectedViewer = viewer;
	        
	        for (int i = 0, count = selectionListeners.size(); i < count; i++) {
	            ISelectionChangedListener listener = (ISelectionChangedListener)selectionListeners.get(i);
	            
	            listener.selectionChanged(event);
	        }
        }
    }

    private void focusChanged(StructuredViewer viewer)
    {
        if (viewer == lastSelectedViewer) {
            return;
        }
        propagateSelectionChanged(new SelectionChangedEvent(viewer, viewer.getSelection()), viewer);
    }
    
    public void addTableDoubleClickListener(IDoubleClickListener listener)
    {
        tableDoubleClickListeners.add(listener);
    }
    
    public void removeTableDoubleClickListener(IDoubleClickListener listener)
    {
        tableDoubleClickListeners.remove(listener);
    }
    
    protected void propagateTableDoubleClick(DoubleClickEvent event)
    {
        for (int i = 0, count = tableDoubleClickListeners.size(); i < count; i++) {
            IDoubleClickListener listener = (IDoubleClickListener)tableDoubleClickListeners.get(i);
            
            listener.doubleClick(event);
        }
    }
    
    public void setInput(Object input)
    {
        treeViewer.setInput(input);
    }
    
    public void createContents(String[] columnTitles,  int[] columnWidths)
    {
        setLayout(new FillLayout());
        sash = new SashForm(this, SWT.HORIZONTAL);
        
        if (isChecked) {
            checkboxTreeViewer = new ExtendedCheckboxTreeViewer(sash);
            treeViewer = checkboxTreeViewer;
        }
        else {
            treeViewer = new ExtendedTreeViewer(sash);            
        }
        
        if (isChecked) {
            checkboxTableViewer = CheckboxTableViewer.newCheckList(sash, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
            tableViewer = checkboxTableViewer;
        }
        else {
            tableViewer = new TableViewer(sash, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        }

        for (int i = 0; i < columnTitles.length; i++) {
            TableColumn column = new TableColumn(tableViewer.getTable(), SWT.LEFT);

            column.setText(columnTitles[i]);
            if (columnWidths == null || columnWidths[i] == -1) {
                column.pack();
            }
            else {
                column.setWidth(columnWidths[i]);                                            
            }
        }

        tableViewer.getTable().setHeaderVisible(true);

        // Synchronize checkbox state within the UI and the model.
        if (isChecked) {
	        checkStateListener = new ExplorerCheckStateListener(checkboxTreeViewer, checkboxTableViewer);
	        checkboxTreeViewer.addCheckStateListener(checkStateListener);        
	        checkboxTableViewer.addCheckStateListener(checkStateListener);
        }
        
        // Update checked state when nodes are expanded
        treeViewer.addTreeListener(new ITreeViewerListener() {

            public void treeExpanded(TreeExpansionEvent event)
            {
                // For some unknown reason, the expanded state is not set yet?  Its
                // set after.  The checkStateListener expects the state to be updated/
                treeViewer.setExpandedState(event.getElement(), true);
                if (isChecked) {
                    checkStateListener.updateTreeCheckboxesForAncestor(event.getElement());
                }
            }
            
            public void treeCollapsed(TreeExpansionEvent event)
            {
            }
        });
        
        // Synchronize the TableViewer with the selection in the TreeViewer
        treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            
            public void selectionChanged(SelectionChangedEvent event)
            {
                IStructuredSelection selection = (IStructuredSelection)event.getSelection();
                
                Object selectedElement = selection.getFirstElement();
                tableViewer.setInput(selectedElement);
                if (isChecked) {
                    checkStateListener.updateTableCheckboxes();
                }
                
                propagateSelectionChanged(event, treeViewer);
            }
            
        });
        
        treeViewer.getTree().addFocusListener(new FocusAdapter()
        {
            public void focusGained(FocusEvent e)
            {
                focusChanged(treeViewer);
            }
        });

        // Synchronize the TreeViewer with double clicks in the TableViewer
        treeViewer.addDoubleClickListener(new IDoubleClickListener() {
            
            public void doubleClick(DoubleClickEvent event)
            {
                IStructuredSelection selection = (IStructuredSelection)event.getSelection();
                
                Object selectedElement = selection.getFirstElement();
                if (contentProvider.hasChildren(selectedElement)) {
                    treeViewer.setExpandedState(selectedElement, !treeViewer.getExpandedState(selectedElement));
                }
            }
            
        });
        
        // Pass on TableBiew selection changes
        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            
            public void selectionChanged(SelectionChangedEvent event)
            {
                propagateSelectionChanged(event, tableViewer);
            }
            
        });

        // Synchronize the TreeViewer with double clicks in the TableViewer
        tableViewer.addDoubleClickListener(new IDoubleClickListener() {
            
            public void doubleClick(DoubleClickEvent event)
            {
                IStructuredSelection selection = (IStructuredSelection)event.getSelection();
                
                Object selectedElement = selection.getFirstElement();
                Object children[] = contentProvider.getChildren(selectedElement);
                if (contentProvider.hasChildren(selectedElement)) {
                    treeViewer.setSelection(new StructuredSelection(selectedElement));
                }
                else {
                    propagateTableDoubleClick(event);
                }
            }
            
        });
                
        tableViewer.getTable().addFocusListener(new FocusAdapter()
        {
            public void focusGained(FocusEvent e)
            {
                focusChanged(tableViewer);
            }
        });

    }

    public void refresh()
    {
        try {
            ignoreSelectionChanges = true;
            treeViewer.refresh();
            tableViewer.refresh();

            if (isChecked) {
                checkStateListener.updateTableCheckboxes();
                checkStateListener.updateTreeCheckboxes();            
            }
            
            
            ignoreSelectionChanges = false;            
            if (lastSelectedViewer != null) {
	            SelectionChangedEvent event = new SelectionChangedEvent(lastSelectedViewer, lastSelectedViewer.getSelection());
	            propagateSelectionChanged(event, lastSelectedViewer);            
            }
        }
        finally {
            ignoreSelectionChanges = false;            
        }
    }

	public void serializeXML(XMLSerializer writer)
	{	
	    int[] weights = sash.getWeights();
	    int normalizedLeftSide = (int)(100.0 * ((float)weights[0]) / ((float)(weights[0] + weights[1])));
	    writer.write("explorerSash", Integer.toString(normalizedLeftSide));
	}

    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        if ("explorerSash".equals(container)) {
            int weights[] = new int[2];
            weights[0] = Integer.parseInt(value);
            weights[1] = 100 - weights[0];
            sash.setWeights(weights);
        }
        return null;
    }
}
