/*
 * Created on Apr 2, 2005
 */
package com.toubassi.filebunker.ui.backup;

import com.toubassi.filebunker.ui.LabelUtil;
import com.toubassi.filebunker.vault.BackupEstimate;
import com.toubassi.jface.ContextHintDialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;

/**
 * @author garrick
 */
public class PreviewDialog extends ContextHintDialog
{
    private BackupEstimate estimate;
    private TableViewer tableViewer;
    private boolean backupNow;
    
    public PreviewDialog(Shell shell, BackupEstimate estimate)
    {
        super(shell);
        this.estimate = estimate;
        setInfoTitle("Backup Preview");
        setDefaultHint("The following files are in need of backup.");
    }
    
    /**
     * @return true if the user wants to perform a backup.
     */
    public boolean run()
    {
        open();
        return backupNow;
    }
    
    protected void buttonPressed(int buttonId)
    {
        backupNow = buttonId == 1001;
        close();
    }
    
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText("Preview");
    }

    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, 1001, "Backup Now", false);
        createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
    }
    
    protected void createCustomContents(Composite parent)
    {
        parent.setLayout(new FormLayout());
        
        tableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        FormData tableViewerFormData = new FormData();
        tableViewerFormData.top = new FormAttachment(0, 0);
        tableViewerFormData.left = new FormAttachment(0, 0);
        tableViewerFormData.right = new FormAttachment(100, 0);
        tableViewerFormData.bottom = new FormAttachment(100, -30);
        tableViewerFormData.height = 400;
        
        tableViewer.getControl().setLayoutData(tableViewerFormData);

        TableColumn directoryColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT);
        directoryColumn.setText("Directory");
        directoryColumn.setWidth(300);
        directoryColumn.addSelectionListener(new SelectionAdapter() { 
              DirtyFileSorter sorter = new DirtyFileSorter(0);
              public void widgetSelected(SelectionEvent e) {
                  tableViewer.setSorter(null);
                  tableViewer.setSorter(sorter);
              }
        });
        
        TableColumn fileColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT);
        fileColumn.setText("File");
        fileColumn.setWidth(150);
        fileColumn.addSelectionListener(new SelectionAdapter() {           	
            DirtyFileSorter sorter = new DirtyFileSorter(1);
            public void widgetSelected(SelectionEvent e) {
                tableViewer.setSorter(null);
                tableViewer.setSorter(sorter);
            }
        });
        
        TableColumn sizeColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT);
        sizeColumn.setText("Size");
        sizeColumn.setWidth(80);
        sizeColumn.addSelectionListener(new SelectionAdapter() {           	
            DirtyFileSorter sorter = new DirtyFileSorter(2);
            public void widgetSelected(SelectionEvent e) {
                tableViewer.setSorter(null);
                tableViewer.setSorter(sorter);
            }
        });
        
        TableColumn backupSizeColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT);
        backupSizeColumn.setText("Est. Backup Size");
        backupSizeColumn.setWidth(100);
        backupSizeColumn.addSelectionListener(new SelectionAdapter() {           	
            DirtyFileSorter sorter = new DirtyFileSorter(3);
            public void widgetSelected(SelectionEvent e) {
                tableViewer.setSorter(null);
                tableViewer.setSorter(sorter);
            }
        });
        
        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.setContentProvider(new PreviewTableContentProvider());
        tableViewer.setLabelProvider(new DirtyFileLabelProvider());
        
        tableViewer.setInput(estimate);
        
        Label summaryLabel = new Label(parent, SWT.NONE);
        
        String summary;        
        summary = "" + estimate.numberOfDirtyFiles() + " files totalling " +
        	LabelUtil.formatMemorySize(estimate.totalSize()) +
        	" (estimated backup size of " +
        	LabelUtil.formatMemorySize(estimate.estimatedBackupSize()) + ")";
        summaryLabel.setText(summary);

        
        
        FontData fontData = summaryLabel.getFont().getFontData()[0];        
        Font boldFont = new Font(parent.getDisplay(), fontData.getName(), fontData.getHeight(), SWT.BOLD);
        summaryLabel.setFont(boldFont);

        FormData summaryLabelFormData = new FormData();
        summaryLabelFormData.right = new FormAttachment(100, 0);
        summaryLabelFormData.bottom = new FormAttachment(100, -5);

        summaryLabel.setLayoutData(summaryLabelFormData);
    }
}

class PreviewTableContentProvider implements IStructuredContentProvider
{
	public Object[] getElements(Object inputElement) {
	    BackupEstimate estimate = (BackupEstimate)inputElement;
	    
	    // Boy is this hokey
	    return estimate.dirtyFiles().toArray();
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
	}

	public void dispose()
	{
	}
}

class DirtyFileLabelProvider extends LabelProvider implements ITableLabelProvider
{    
    public Image getColumnImage(Object element, int columnIndex)
    {
        return null;
    }

    public String getColumnText(Object element, int columnIndex)
    {
        BackupEstimate.DirtyFile dirtyFile = (BackupEstimate.DirtyFile)element;
        
        if (columnIndex == 0) {
            return dirtyFile.file.getParent();
        }
        else if (columnIndex == 1) {
            return dirtyFile.file.getName();
        }
        else if (columnIndex == 2) {
            return LabelUtil.formatMemorySize(dirtyFile.size);
        }
        return LabelUtil.formatMemorySize(dirtyFile.estimatedBackupSize);
        
    }
}

class DirtyFileSorter extends ViewerSorter
{
    private int columnIndex;
    private boolean ascending;
    
    public DirtyFileSorter(int columnIndex)
    {
        this.columnIndex = columnIndex;
    }

    public int compare(Viewer viewer, Object o1, Object o2)
    {
        int result = compareAscending(viewer, o1, o2);
        return ascending ? result : -result;
    }

    private int compareAscending(Viewer viewer, Object o1, Object o2)
    {
        BackupEstimate.DirtyFile dirtyFile1 = (BackupEstimate.DirtyFile)o1;
        BackupEstimate.DirtyFile dirtyFile2 = (BackupEstimate.DirtyFile)o2;
        
        if (columnIndex == 0) {
            return dirtyFile1.file.getParent().compareTo(dirtyFile2.file.getParent());
        }
        else if (columnIndex == 1) {
            return dirtyFile1.file.getName().compareTo(dirtyFile2.file.getName());
        }
        else if (columnIndex == 2) {
            return (int)(dirtyFile1.size - dirtyFile2.size);
        }
        return (int)(dirtyFile1.estimatedBackupSize - dirtyFile2.estimatedBackupSize);
    }
    
    public void sort(final Viewer viewer, Object[] elements)
    {
        ascending = !ascending;
        super.sort(viewer, elements);
    }
}
