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

*/package com.toubassi.filebunker.ui.backup;

import com.toubassi.filebunker.vault.BackupSpecification;
import com.toubassi.jface.ContextHintDialog;
import com.toubassi.util.Glob;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author garrick
 */
public class FiltersDialog extends ContextHintDialog
{
    private static final String ExcludedFilesHint =
        "Specify file names or patterns to exclude from the backup " +
        "such as '*.gif' or '*.avi'.  Separate multiple names with commas.";
    
    private static final String ExcludedDirectoriesHint =
        "Specify directory names or patterns to exclude from the backup " +
        "such as 'Attic' or '*Data'.  Separate multiple names with commas.";
    
    private static final String FileSizeConfigurationHint =
        "You can specify that files larger than a certain size will be " +
        "excluded from backup.";
    
    private static final String fileSizeChoiceLabels[] = {
            "Don't Exclude Based on Size",
            "Greater than 1 MB", "Greater than 2 MB", "Greater than 5 MB",
            "Greater than 10 MB", "Greater than 20 MB", "Greater than 50 MB",
            "Greater than 100 MB", "Greater than 200 MB",
            "Greater than 500 MB", "Greater than 1000 MB"}; 
    
    private static final int fileSizeChoices[] = {0, 1024*1024,
            2*1024*1024, 5*1024*1024, 10*1024*1024, 20*1024*1024, 50*1024*1024, 
            100*1024*1024, 200*1024*1024, 500*1024*1024, 1000*1024*1024};

    private BackupSpecification spec;
    private Text excludedFilesField;
    private Text excludedDirectoriesField;
    private Combo fileSizeCombo;
    
    public FiltersDialog(Shell shell, BackupSpecification spec)
    {
        super(shell);
        this.spec = spec;
        setInfoTitle("File Filters");
    }

    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText("Filters");
    }
    
    private String globString(ArrayList globs)
    {
        StringBuffer buffer = new StringBuffer();
        
        for (int i = 0, count = globs.size(); i < count; i++) {
            Glob glob = (Glob)globs.get(i);
            
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(glob.globExpression());
        }        
        return buffer.toString();
    }

    private void updateUIFromModel()
    {
        excludedFilesField.setText(globString(spec.excludedFileGlobs()));
        excludedDirectoriesField.setText(globString(spec.excludedDirectoryGlobs()));

        long excludedSize = spec.excludedFileSize();

        if (excludedSize == 0) {
            fileSizeCombo.select(0);
        }
        else {
            
            for (int i = 0; i < fileSizeChoices.length; i++) {
                
                // Select the nearest match that is less than or equal to the
                // configured value.
                if (excludedSize == fileSizeChoices[i] ||
                    (i < fileSizeChoices.length - 1 &&
                     excludedSize > fileSizeChoices[i] &&
                     excludedSize < fileSizeChoices[i + 1]))
                {
                    fileSizeCombo.select(i);
                    break;
                }
            }
        }
        
    }
    
    private void updateModelFromUI()
    {
        spec.removeAllExcludedFileGlobs();
        spec.removeAllExcludedDirectoryGlobs();
        
        StringTokenizer tokenizer = new StringTokenizer(excludedFilesField.getText(), ", ");        
        while (tokenizer.hasMoreTokens()) {
            String globExpression = tokenizer.nextToken();
            spec.addExcludedFileGlob(new Glob(globExpression));
        }
        
        tokenizer = new StringTokenizer(excludedDirectoriesField.getText(), ", ");        
        while (tokenizer.hasMoreTokens()) {
            String globExpression = tokenizer.nextToken();
            spec.addExcludedDirectoryGlob(new Glob(globExpression));
        }

        long excludedSize = fileSizeChoices[fileSizeCombo.getSelectionIndex()];
        spec.setExcludedFileSize(excludedSize);
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

        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        contents.setLayout(layout);

        // Excluded Files field
        
        Label excludedFilesLabel = new Label(contents, SWT.NONE);
        excludedFilesLabel.setText("Excluded Files:");
        
        GridData excludedFilesLabelGridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
        excludedFilesLabel.setLayoutData(excludedFilesLabelGridData);

        excludedFilesField = new Text(contents, SWT.BORDER);
        setControlHint(excludedFilesField, ExcludedFilesHint);
        GridData excludedFilesFieldGridData = new GridData(GridData.FILL_HORIZONTAL);
        excludedFilesField.setLayoutData(excludedFilesFieldGridData);
        
        // Excluded Directories field
        
        Label excludedDirectoriesLabel = new Label(contents, SWT.NONE);
        excludedDirectoriesLabel.setText("Excluded Directories:");
        
        GridData excludedDirectoriesLabelGridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
        excludedDirectoriesLabel.setLayoutData(excludedDirectoriesLabelGridData);

        excludedDirectoriesField = new Text(contents, SWT.BORDER);
        setControlHint(excludedDirectoriesField, ExcludedDirectoriesHint);
        GridData excludedDirectoriesFieldGridData = new GridData(GridData.FILL_HORIZONTAL);
        excludedDirectoriesField.setLayoutData(excludedDirectoriesFieldGridData);
        
        // Exclusion by Size
        
        Label excludedFileSizeLabel = new Label(contents, SWT.NONE);
        excludedFileSizeLabel.setText("Exclude by size:");
        
        GridData excludedFileSizeLabelGridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
        excludedFileSizeLabel.setLayoutData(excludedFileSizeLabelGridData);

        fileSizeCombo = new Combo(contents, SWT.DROP_DOWN | SWT.READ_ONLY);
        setControlHint(fileSizeCombo, FileSizeConfigurationHint);
        GridData fileSizeComboGridData = new GridData(GridData.FILL_HORIZONTAL);
        fileSizeCombo.setLayoutData(fileSizeComboGridData);
        
        fileSizeCombo.setVisibleItemCount(fileSizeChoiceLabels.length);
        for (int i = 0; i < fileSizeChoiceLabels.length; i++) {
            fileSizeCombo.add(fileSizeChoiceLabels[i]);
        }

        
        return contents;
    }

    protected void okPressed()
    {
        updateModelFromUI();
        super.okPressed();
    }
}
