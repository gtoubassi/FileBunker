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
package com.toubassi.jface;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @author garrick
 */
public class ExtendedCheckboxTreeViewer extends
        org.eclipse.jface.viewers.CheckboxTreeViewer implements
        TreeViewerAccess
{

    public ExtendedCheckboxTreeViewer(Composite parent)
    {
        super(parent);
    }

    public ExtendedCheckboxTreeViewer(Composite parent, int style)
    {
        super(parent, style);
    }

    public ExtendedCheckboxTreeViewer(Tree tree)
    {
        super(tree);
    }

    public TreeItem getNextItem(TreeItem item, boolean includeChildren)
    {
        return (TreeItem)super.getNextItem(item, includeChildren);
    }

    public TreeItem itemForElement(Object element)
    {
        return (TreeItem)findItem(element);
    }
}
