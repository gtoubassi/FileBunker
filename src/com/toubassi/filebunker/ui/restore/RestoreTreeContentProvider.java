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

import com.toubassi.filebunker.vault.DirectoryRevision;
import com.toubassi.filebunker.vault.Node;
import com.toubassi.filebunker.vault.Revision;
import com.toubassi.filebunker.vault.Vault;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * @author garrick
 */
public class RestoreTreeContentProvider implements ITreeContentProvider
{
    private static Object[] emptyArray = new Object[0];
    private Vault vault;
    private Date date;
    
    
    public RestoreTreeContentProvider(Vault vault)
    {
        this.vault = vault;
    }
    
    public void setDate(Date date)
    {
        this.date = date;
    }
    
    private Object[] visibleChildren(Revision revision)
    {
        if (revision == null || !revision.isDirectory()) {
            return emptyArray;
        }

        ArrayList children = new ArrayList();

        // If date == null, then show all children, which means children
        // that have both file and directory revisions appear twice.
        if (date == null) {
            List childNodes = revision.node().children();
            
            for (int i = 0, count = childNodes.size(); i < count; i++) {
                Node child = (Node)childNodes.get(i);
                
                if (child.hasBothFileAndDirectoryRevisions()) {
                    children.add(child.lastDirectoryRevision());
                    children.add(child.lastFileRevision());
                }
                else {
                    children.add(child.lastRevision());
                }            
            }

        }
        else {
            Iterator i = ((DirectoryRevision)revision).children();
            
            while (i.hasNext()) {
                Node child = (Node)i.next();
                children.add(child.findRevision(date));
            }
        }
        return children.toArray(new Revision[children.size()]);                    
    }
    
    public Object[] getChildren(Object parentElement)
    {
        Revision revision = (Revision)parentElement;
        if (revision.isDirectory()) {
            return visibleChildren(revision);
        }
        return emptyArray;
    }

    public Object getParent(Object element)
    {
        Revision revision = (Revision)element;
        Node node = revision.node();
        Node parentNode = node.parent();
        
        if (parentNode == null) {
            return null;
        }
        
        return date == null ? parentNode.lastDirectoryRevision() : parentNode.findRevision(date);
    }

    public boolean hasChildren(Object element)
    {
        Revision revision = (Revision)element;
        return revision.isDirectory();
    }

    public Object[] getElements(Object inputElement)
    {
        Revision revision;
        
        if (date == null) {
            revision = vault.root().lastDirectoryRevision();            
        }
        else {
            revision = vault.root().findRevision(date);   
        }
        
        return visibleChildren(revision);
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }
}
