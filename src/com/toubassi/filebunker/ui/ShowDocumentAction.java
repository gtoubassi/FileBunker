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

import java.io.File;
import java.util.StringTokenizer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.program.Program;

/**
 * @author garrick
 */
public class ShowDocumentAction extends Action
{
    private String document;

    public ShowDocumentAction(String title, String document)
    {
        setText(title);
        this.document = document;
    }
    
    public void run()
    {
        showDocument(document);
    }
    
    /**
     * To find FileBunker documentation we use the classpath as a hint.  We
     * assume our filesystem is organized in one of two ways.  "release" or
     * "developer".  In release form, the documents are in
     * FileBunker/Resources/doc, whereas most members of the classpath are 
     * FileBunker/Resources/lib/SomeJar.jar.  So taking a member 'm' of the
     * classpath and testing m/../../doc/SomeDoc.html is how we find the document.
     * In developer form, the documents are in FileBunker/doc, while the
     * classes are assummed to be in FileBunker/classes or FileBunker/lib.  So
     * we test m/../doc/SomeDocument.html.
     * 
     * @return The file representing the document or null if not found.
     */
    public static File findDocument(String document)
    {
        String classpath = System.getProperty("java.class.path");
        StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);
        
        while (tokenizer.hasMoreTokens()) {
            File file = new File(tokenizer.nextToken());
            
            File parent = file.getParentFile();
            File grandParent = null;
            if (parent != null) {
                grandParent = parent.getParentFile();
            }
            
            // In release form
            if (grandParent != null) {
                File docFile = new File(grandParent, "doc" + File.separator + document);
                
                if (docFile.exists()) {
                    return docFile;
                }
            }
            
            // In developer form
            if (parent != null) {
                File docFile = new File(parent, "doc" + File.separator + document);
                
                if (docFile.exists()) {
                    return docFile;
                }	                
            }
        }
        
        return null;
    }
    
    public static void showDocument(String document)
    {
        File file = findDocument(document);
        
        if (file != null) {
            Program.launch(file.getPath());
        }
        else {
            MessageDialog.openError(null, "Error", "Could not find " + document);
        }
    }

}
