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

import com.toubassi.filebunker.ui.ImageCache;
import com.toubassi.filebunker.ui.LabelUtil;
import com.toubassi.filebunker.vault.FileRevision;
import com.toubassi.filebunker.vault.Revision;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * @author garrick
 */
public class RestoreTableLabelProvider implements ITableLabelProvider
{
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yyyy h:mm:ss a");
    
    public String getColumnText(Object element, int columnIndex)
    {
        Revision revision = (Revision)element;

        if (columnIndex == 0) {
            return revision.node().name();
        }
        
        if (columnIndex == 1) {
            
            if (revision.isDirectory()) {
                return "";
            }
            
            FileRevision fileRevision = (FileRevision)revision;
            
            return LabelUtil.formatMemorySize(fileRevision.size());
        }
        
        if (columnIndex == 2) {

            if (revision.isDirectory()) {
                return "";
            }
            
            Date date = revision.date();
            if (date == null) {
                return "";
            }
            return dateFormat.format(date);
        }
        throw new RuntimeException("Can't provide label for column: " + columnIndex);
    }
    
    public Image getColumnImage(Object element, int columnIndex)
    {
        if (columnIndex != 0) {
            return null;
        }

        Revision revision = (Revision)element;
        
        if (revision.isDirectory()) {
            return ImageCache.sharedCache().get(ImageCache.class, "icons/folder.gif");
        }
        else {
            return ImageCache.sharedCache().get(ImageCache.class, "icons/file.gif");
        }
    }

    public void addListener(ILabelProviderListener ilabelproviderlistener)
    {
    }
    
    public void dispose()
    {
    }
    
    public boolean isLabelProperty(Object obj, String s)
    {
        return false;
    }
    
    public void removeListener(ILabelProviderListener ilabelproviderlistener)
    {
    }
}
