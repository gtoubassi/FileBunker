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
package com.toubassi.filebunker.ui.backup;

import com.subx.common.NotificationCenter;
import com.subx.common.NotificationListener;
import com.toubassi.filebunker.vault.BackupSpecification;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;

/**
 * @author garrick
 */
public class SpecificationDescriptionController implements NotificationListener
{
    private BackupSpecification backupSpec;
    private StyledText text;
    private ArrayList excluded = new ArrayList();
    private ArrayList directoriesWithExclusions = new ArrayList();
    
    public SpecificationDescriptionController(BackupSpecification backupSpec, StyledText text)
    {
        this.backupSpec = backupSpec;
        this.text = text;
        
        NotificationCenter.sharedCenter().register(BackupSpecification.ChangedNotification, backupSpec, this);

        updateText();
    }
    
    // This method formats an easily readable string describing the
    // backup specification.  An example message would be:
    //
    // "Backup c:/foo.txt, c:/bar.xls, and all files under c:/tom.  Also backup
    // c:/garrick except for c:/garrick/bigfiles."
    public void updateText()
    {
        ArrayList includedFiles = backupSpec.includedFiles();
        ArrayList excludedFiles = backupSpec.excludedFiles();
        
        if (includedFiles.isEmpty()) {
            text.setText("No files are selected for backup.  Select the check box to the left of a file to mark it for backup.");            
            return;
        }

        Color red = text.getDisplay().getSystemColor(SWT.COLOR_RED);
        Color blue = text.getDisplay().getSystemColor(SWT.COLOR_BLUE);
        
        ArrayList styles = new ArrayList();
        StringBuffer buffer = new StringBuffer();

               
        buffer.append("Backup ");

        // The first sentence includes all included files (not directories).  This is
        // the "Backup c:/foo.txt. c:/bar.xls..." portion of the example message.
        int numFiles = 0;
        for (int i = 0, count = includedFiles.size(); i < count; i++) {

            File file = (File)includedFiles.get(i);
            
            if (file.isFile()) {
                numFiles++;
                
                if (numFiles > 1) {
                    if (numFiles == includedFiles.size()) {
                        buffer.append(", and ");
                    }
                    else {
                        buffer.append(", ");                        
                    }
                }

                appendFile(file, buffer, styles, blue);
            }            
        }
        
        if (numFiles == includedFiles.size()) {
            // We only had individual files, so put a period on it, we are done!
            // Otherwise we leave the sentence hanging so that directories can
            // be added on.
            buffer.append(".");
        }
        else {            

            // We've got directories.
            if (numFiles > 0) {
                // If we had some files, then we need to append the
                // directories to an already existing sentence.
                buffer.append(", and ");
            }

            // Find all top level directories that have no exclusions under
            // them.  A "top level" directory is one which was not included
            // in response to an exclusion.  For instance if
            //		includedFiles = [c:/tom, c:/garrick, c:/garrick/Documents/Work]
            //  and excludedFiles = [c:/garrick/Documents]
            //
            // Then c:/tom and c:/garrick are "top level" directories, although
            // since c:/garrick has exclusions then only c:/tom will end up in
            // the list.
            
            ArrayList topLevelDirectoriesWithoutExclusions = new ArrayList();
            for (int i = 0, count = includedFiles.size(); i < count; i++) {
                File file = (File)includedFiles.get(i);
                
                if (file.isFile()) {
                    continue;
                }
                
                backupSpec.findFilesExplicitlyExcludedFrom(file, excluded);
                File parent = file.getParentFile();
                if (excluded.isEmpty() && (parent == null || BackupSpecification.nearestAncestor(parent, includedFiles) == null)) {
                    topLevelDirectoriesWithoutExclusions.add(file);
                }
            }
            
            // If we have any topLevelDirectoriesWithoutExclusions, then pack
            // them into one sentence (potentially along with the lone files).
            boolean appendedDirectory = false;
            if (topLevelDirectoriesWithoutExclusions.size() > 0) {
                buffer.append("all files under ");

	            for (int i = 0, count = topLevelDirectoriesWithoutExclusions.size(); i < count; i++) {
	                File file = (File)topLevelDirectoriesWithoutExclusions.get(i);
	
	                if (i > 0 && i == topLevelDirectoriesWithoutExclusions.size() - 1) {
	                    buffer.append(", and ");
	                }
	                else if (i > 0) {
	                    buffer.append(", ");
	                }

	                appendFile(file, buffer, styles, blue);
	            }
	            
	            appendedDirectory = true;
                buffer.append(".");
            }
            

            // Now do all other directories, one sentence each.
            for (int i = 0, count = includedFiles.size(); i < count; i++) {
                File file = (File)includedFiles.get(i);

                if (file.isFile() || topLevelDirectoriesWithoutExclusions.contains(file)) {
                    continue;
                }
                
                if (appendedDirectory == true) {
                    // We are starting a new sentence, rather than joining
                    // the initial sentence containing files.
                    buffer.append("  Also backup ");
                }
	            appendedDirectory = true;
                
                buffer.append("all files under ");
                
                appendFile(file, buffer, styles, blue);
                
                backupSpec.findFilesExplicitlyExcludedFrom(file, excluded);
                
                if (excluded.size() > 0) {
                    buffer.append(" except for ");
                    
                    for (int j = 0, jcount = excluded.size(); j < jcount; j++) {
                        File excludedFile = (File)excluded.get(j);
                        
                        if (j > 0 && j == excluded.size() - 1) {
                            buffer.append(", and ");
                        }
                        else if (j > 0) {
                            buffer.append(", ");
                        }

                        appendFile(excludedFile, buffer, styles, red);
                    }
                }
                buffer.append(".");                
            }            
        }
        
        text.setText(buffer.toString());
        text.setStyleRanges((StyleRange[])styles.toArray(new StyleRange[styles.size()]));
    }
    
    private void appendFile(File file, StringBuffer buffer, ArrayList styles, Color color)
    {
        String path = file.getPath();
        
        StyleRange style = new StyleRange(buffer.length(), path.length(), color, null);
        styles.add(style);

        buffer.append(path);            
    }
        
    public void handleNotification(String notification, Object sender, Object argument)
    {
        updateText();
        //System.out.println("-----");
        //System.out.println(sender);
    }
}
