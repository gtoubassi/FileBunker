/*
 * Created on Oct 22, 2004
 */
package com.toubassi.jface;

import org.eclipse.swt.widgets.TreeItem;

/**
 * @author garrick
 */
public interface TreeViewerAccess
{
    public TreeItem getNextItem(TreeItem item, boolean includeChildren);
    public TreeItem itemForElement(Object element);
}
