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

package com.toubassi.filebunker.ui.backup;


import com.toubassi.filebunker.ui.ImageCache;
import com.toubassi.filebunker.ui.LabelUtil;

import java.io.File;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.graphics.Image;



public class BackupTableLabelProvider implements ITableLabelProvider
{
    private ITreeContentProvider contentProvider;
    
    public BackupTableLabelProvider(ITreeContentProvider contentProvider)
    {
        this.contentProvider = contentProvider;
    }
    
    public String getColumnText(Object element, int columnIndex)
    {
        File file = (File)element;

        if (columnIndex == 0) {
            String name = file.getName();
            
            if (name == null || name.equals("")) {
                // To handle the root on windows which is "C:\"
                name = file.getPath();
                if (name.endsWith(File.separator) && name.length() > 1) {
                    name = name.substring(0, name.length() - 1);
                }
            }
            return name;
        }
        
        if (columnIndex == 1) {

            if (contentProvider.hasChildren(file)) {
                return "";
            }
            
            return LabelUtil.formatMemorySize(file.length());
        }

        throw new RuntimeException("Can't provide label for column: " + columnIndex);
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
    
    public Image getColumnImage(Object element, int columnIndex)
    {
        if (columnIndex != 0) {
            return null;
        }
        
        if (contentProvider.hasChildren(element)) {
            return ImageCache.sharedCache().get(ImageCache.class, "icons/folder.gif");
        }
        else {
            return ImageCache.sharedCache().get(ImageCache.class, "icons/file.gif");
        }
    }
}
