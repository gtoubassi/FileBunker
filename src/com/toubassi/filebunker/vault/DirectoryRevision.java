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
 * Created on Jul 26, 2004
 */
package com.toubassi.filebunker.vault;

import com.toubassi.archive.Archivable;
import com.toubassi.archive.ArchiveInputStream;
import com.toubassi.archive.ArchiveOutputStream;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Stack;

/**
 * @author garrick
 */
public class DirectoryRevision extends Revision
{
    private ArrayList added;
    private ArrayList removed;
    
    public DirectoryRevision()
    {
        added = new ArrayList();
        removed = new ArrayList();
    }
    
    public synchronized boolean isEmpty()
    {
        return added.isEmpty() && removed.isEmpty();
    }

    public ArrayList addedChildren()
    {
        return added;
    }
    
    public synchronized void addChild(Node child)
    {
        added.add(child);
        removed.remove(child);
    }

    public ArrayList removedChildren()
    {
        return removed;
    }
    
    public synchronized void removeChild(Node child)
    {
        removed.add(child);
        added.remove(child);
    }
    
    public synchronized void deleteChild(Node child)
    {
        removed.remove(child);
        added.remove(child);        
    }

    protected synchronized boolean wasChildRemovedLater(Node child, DirectoryRevision lastRevisionToCheck)
    {
        if (this == lastRevisionToCheck) {
            return false;
        }
        
        for (Revision current = nextRevision(); current != null && current.isDirectory(); current = current.nextRevision()) {
            DirectoryRevision directory = (DirectoryRevision)current;
            if (directory.removedChildren().contains(child)) {
                return true;
            }
            if (current == lastRevisionToCheck) {
                break;
            }
        }
        return false;
    }

    public boolean isDirectory()
    {
        return true;
    }
    
    public synchronized boolean hasChild(Node child)
    {
        if (removed.contains(child)) {
            return false;
        }
        if (added.contains(child)) {
            return true;
        }
        Revision previousRevision = previousRevision();
        if (previousRevision == null || !previousRevision.isDirectory()) {
            return false;
        }
        return ((DirectoryRevision)previousRevision).hasChild(child);
    }
    
    public Iterator children()
    {
        return new ChildrenIterator(this);
    }
    
    /**
     * Returns an iterator over all FileRevisions in this DirectoryRevision
     * (and sub-DirectoryRevisions) for the specified date.  Note that a null
     * date implies that you want the latest FileRevision of ALL nodes
     * under this DirectoryRevision's node.  That means if this directory
     * has a child foo which was both a file and a directory, then a null
     * date will return both foo as a file, AND all files that were in foo.
     */
    public Iterator descendantFileRevisions(Date date)
    {
        return new DescendantsIterator(this, date);
    }
    
	public void serializeXML(XMLSerializer writer)
	{
	    writer.push("directory");
	    super.serializeXML(writer);
	    for (int i = 0, count = added.size(); i < count; i++) {
	        Node child = (Node)added.get(i);
	        writer.write("added", child.name());
	    }
	    for (int i = 0, count = removed.size(); i < count; i++) {
	        Node child = (Node)removed.get(i);
	        writer.write("removed", child.name());
	    }
	    writer.pop();
	}


    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        throw new RuntimeException("Can't read xml");
    }

    public void writeData(DataOutputStream out) throws IOException
    {
        throw new RuntimeException("Can't write legacy datastream");
    }
    
    public void readData(DataInputStream in) throws IOException
    {
        super.readData(in);

        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            Node child = node.childWithName(in.readUTF());
            addChild(child);            
        }

        count = in.readInt();
        for (int i = 0; i < count; i++) {
            Node child = node.childWithName(in.readUTF());
            removeChild(child);            
        }
    }

    public void archive(ArchiveOutputStream output) throws IOException
    {
        super.archive(output);
        output.writeClassVersion("com.toubassi.filebunker.vault.DirectoryRevision", 1);
        output.writeList(added, Archivable.StrictlyTypedReference);
        output.writeList(removed, Archivable.StrictlyTypedReference);
    }
    
    public void unarchive(ArchiveInputStream input) throws IOException
    {
        super.unarchive(input);
        input.readClassVersion("com.toubassi.filebunker.vault.DirectoryRevision");
        added = input.readList(Archivable.StrictlyTypedReference, Node.class);
        removed = input.readList(Archivable.StrictlyTypedReference, Node.class);
    }
}

class ChildrenIterator implements Iterator
{
    DirectoryRevision revision;
    DirectoryRevision current;
    int index;
    Node currentChild;
    
    public ChildrenIterator(DirectoryRevision revision)
    {
        this.revision = revision;
        this.current = revision;
        index = -1;
        findNext();
    }
    
    public boolean hasNext()
    {
        return currentChild != null;
    }

    public Object next()
    {
        Node returnValue = currentChild;
        findNext();
        return returnValue;
    }
    
    private void findNext()
    {        
        currentChild = null;
        while (true) {
            ArrayList added = current.addedChildren();

            for (index++; index < added.size(); index++) {
	            Node child = (Node)added.get(index);
	            if (!current.wasChildRemovedLater(child, revision)) {
	                currentChild = child;
	                return;
	            }
	        }
	        Revision previousRevision = current.previousRevision();
	        if (previousRevision == null || !previousRevision.isDirectory()) {
	            break;
	        }
	        else {
		        current = (DirectoryRevision)previousRevision;
		        index = -1;	            
	        }
        }
    }

    public void remove()
    {
        throw new UnsupportedOperationException("Can't remove directory members");
    }
}

class DescendantsIterator implements Iterator
{
    private Object current;
    private Stack iteratorStack;
    private Date date;
    
    public DescendantsIterator(DirectoryRevision revision, Date date)
    {
        this.date = date;
        iteratorStack = new Stack();
        iteratorStack.push(revision.children());
        current = findNext();
    }
    
    private Iterator currentIterator()
    {
        while (true) {
	        if (iteratorStack.isEmpty()) {
	            return null;
	        }
	        Iterator iterator = (Iterator)iteratorStack.peek();
	        if (!iterator.hasNext()) {
	            iteratorStack.pop();
	        }
	        else {
	            return iterator;
	        }
        }
    }

    public boolean hasNext()
    {
        return current != null;
    }
    
    public Object next()
    {
        Object returnValue = current;
        current = findNext();
        return returnValue;
    }

    private Object findNext()
    {
        while (true) {
	        Iterator current = currentIterator();
	        if (current == null) {
	            return null;
	        }
	        Node node = (Node)current.next();

	        // a null date means that we want the latest rev from ALL nodes
	        if (date == null) {
	            FileRevision fileRevision = node.lastFileRevision();
	            DirectoryRevision directoryRevision = node.lastDirectoryRevision();
	            
	            if (directoryRevision != null) {
		            iteratorStack.push(directoryRevision.children());	                
	            }
	            
	            if (fileRevision != null) {
	                return fileRevision;
	            }
	        }
	        else {
		        Revision revision = node.findRevision(date);
		        if (revision != null) {
			        if (revision.isDirectory()) {
			            iteratorStack.push(((DirectoryRevision)revision).children());
			        }
			        else {
			            return revision;
			        }
		        }
	        }
        }
    }

    public void remove()
    {
        throw new UnsupportedOperationException("Can't remove descendants");
    }
}
