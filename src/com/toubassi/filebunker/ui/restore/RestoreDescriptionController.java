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
 * Created on Aug 20, 2004
 */
package com.toubassi.filebunker.ui.restore;

import com.toubassi.filebunker.vault.RestoreSpecification;
import com.toubassi.filebunker.vault.Revision;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;

/**
 * @author garrick
 */
public class RestoreDescriptionController
{
    private StyledText text;
    
    public RestoreDescriptionController(StyledText text)
    {
        this.text = text;
    }

    // Restore c:/foo/bar, c:/foo/bar, and 
    public void update(RestoreSpecification spec)
    {
        ArrayList revisions = spec.revisions();

        int numFiles = 0;
        int numDirectories = 0;
        
        for (int i = 0; i < revisions.size(); i++) {
            Revision revision = (Revision)revisions.get(i);
            
            if (revision.isDirectory()) {
                numDirectories++;
            }
            else {
                numFiles++;
            }            
        }
        
        if (numFiles == 0 && numDirectories == 0) {
            text.setText("No files or folders are selected to restore.");
            return;
        }

        Color blue = text.getDisplay().getSystemColor(SWT.COLOR_BLUE);

        StringBuffer buffer = new StringBuffer("Restore ");
        ArrayList styles = new ArrayList();

        if (numFiles > 0) {
            
            int numFilesEncountered = 0;
            for (int i = 0; i < revisions.size(); i++) {
                Revision revision = (Revision)revisions.get(i);
                
                if (!revision.isDirectory()) {
                    numFilesEncountered++;
                    
                    if (numFilesEncountered > 1) {
                        buffer.append(", ");
                        if (numFilesEncountered == numFiles) {
                            buffer.append("and ");
                        }
                    }
                    
                    File file = revision.node().file();
                    StyleRange style = new StyleRange(buffer.length(), file.getPath().length(), blue, null);
                    styles.add(style);
                    buffer.append(file.getPath());
                }
            }
            
            if (numDirectories > 0) {
                buffer.append(", and ");
            }
        }
                
        if (numDirectories > 0) {
            buffer.append("all files and folders under ");
            int numDirectoriesEncountered = 0;
            for (int i = 0; i < revisions.size(); i++) {
                Revision revision = (Revision)revisions.get(i);
                
                if (revision.isDirectory()) {
                    numDirectoriesEncountered++;
                    
                    if (numDirectoriesEncountered > 1) {
                        buffer.append(", ");
                        if (numDirectoriesEncountered == numDirectories) {
                            buffer.append("and ");
                        }
                    }
                    
                    File file = revision.node().file();
                    StyleRange style = new StyleRange(buffer.length(), file.getPath().length(), blue, null);
                    styles.add(style);
                    buffer.append(file.getPath());
                }
            }
        }
        
        buffer.append('.');
        
        text.setText(buffer.toString());
        text.setStyleRanges((StyleRange[])styles.toArray(new StyleRange[styles.size()]));        
    }

}
