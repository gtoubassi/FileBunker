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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;

import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;
import com.toubassi.util.Platform;

/**
 * @author garrick
 */
public class Node implements XMLSerializable, Serializable
{
    private Node parent;
    private String name;
    private File file;
    private transient long nodeBackedupSize;
    private transient long totalBackedupSize;
    private List revisions;
    private List children;
    
    public Node()
    {
        this(null);
    }
    
    public Node(String name)
    {
        this.name = name;
        revisions = Collections.EMPTY_LIST;
        children = Collections.EMPTY_LIST;
        invalidateSizes();
    }

    public void setParent(Node parent)
    {
        this.parent = parent;
    }
    
    public Node parent()
    {
        return parent;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public String name()
    {
        return name;
    }
    
    private synchronized void collectDescendantFileRevisionDates(Collection dates, boolean onlyCollectChildren)
    {
        if (!onlyCollectChildren) {
            collectFileRevisionDates(dates);
        }
        
        for (int i = 0; i < children.size() ; i++) {
            Node child = (Node)children.get(i);

            child.collectDescendantFileRevisionDates(dates, false);
        }        
    }

    public synchronized void collectDescendantFileRevisionDates(Collection dates)
    {
        collectDescendantFileRevisionDates(dates, true);
    }
    
    public synchronized void collectFileRevisionDates(Collection dates)
    {
	    for (int i = 0; i < revisions.size(); i++) {
	        Revision revision = (Revision)revisions.get(i);
	        
	        if (!revision.isDirectory()) {
	            FileRevision fileRevision = (FileRevision)revision;
	            dates.add(fileRevision.date());
	        }
	    }
    }
    
    public synchronized boolean isEmpty()
    {
        for (int i = 0; i < revisions.size(); i++) {
            Revision revision = (Revision)revisions.get(i);
            
            if (!revision.isDirectory()) {
                return false;
            }
        }
        
        for (int i = 0; i < children.size() ; i++) {
            Node child = (Node)children.get(i);

            if (!child.isEmpty()) {
                return false;
            }
        }
        
        return true;        
    }
    
    public synchronized long totalBackedupSize()
    {
        if (totalBackedupSize == -1) {
	        totalBackedupSize = nodeBackedupSize();
	        
	        for (int i = 0, count = children.size(); i < count; i++) {
	            Node child = (Node)children.get(i);
	            totalBackedupSize += child.totalBackedupSize();
	        }
        }
        
        return totalBackedupSize;
    }
    
    public synchronized long nodeBackedupSize()
    {
        if (nodeBackedupSize == -1) {
	        nodeBackedupSize = 0;
	        
	        for (int i = 0, count = revisions.size(); i < count; i++) {
	            Revision revision = (Revision)revisions.get(i);

	            if (!revision.isDirectory()) {
	                FileRevision fileRevision = (FileRevision)revision;
	                nodeBackedupSize += fileRevision.backedupSize();
	            }
	        }
        }
        
        return nodeBackedupSize;
    }

    public synchronized void findLargestNodes(SortedSet set, int maxNodesToReturn)
    {
        if (maxNodesToReturn == -1 || set.size() < maxNodesToReturn) {
            set.add(this);
        }
        else {
            long smallest = ((Node)set.last()).nodeBackedupSize();
            
            if (nodeBackedupSize() > smallest) {
                set.remove(set.last());
                set.add(this);
            }
        }
        
        for (int i = 0, count = children.size(); i < count; i++) {
            Node child = (Node)children.get(i);
            child.findLargestNodes(set, maxNodesToReturn);
        }
    }

    public synchronized void invalidateSizes()
    {
        nodeBackedupSize = -1;
        totalBackedupSize = -1;
        if (parent != null) {
            parent.invalidateSizes();
        }
    }
    
	public synchronized File file()
	{
	    if (file == null) {
	    	StringBuffer buffer = new StringBuffer();
	    	getFullPath(buffer);
	        file = new File(buffer.toString());
	    }
	    return file;
	}

	private synchronized void getFullPath(StringBuffer buffer)
	{
		// If we are the root node, we are done.
		if (parent == null) {
			return;
		}
				
		parent.getFullPath(buffer);			
		
		// On linux we always prepend with a slash since paths start
		// that way, while on windows slashes only SEPARATE components
		// (the first component is a drive).
		if (buffer.length() > 0 || !Platform.isWindows()) {
			buffer.append(File.separatorChar);							
		}
		buffer.append(name);
	}

	public List revisions()
    {
        return revisions;
    }
    
    public synchronized void addRevision(Revision revision)
    {
        Date date = revision.date();
        if (date != null) {
            Revision lastRevision = lastRevision();
            if (lastRevision != null) {
                if (!date.after(lastRevision.date())) {
                    throw new IllegalArgumentException("Can't add a revision with a date earlier than the last revision " + date + " " + lastRevision.date());
                }
            }
        }

        revision.setNode(this);
        if (revisions == Collections.EMPTY_LIST) {
            revisions = new ArrayList();
        }
        revisions.add(revision);
        invalidateSizes();
    }
    
    public synchronized FileRevision findRevisionWithHandlerName(String name)
    {
        for (int i = children.size() - 1 ; i >=0; i--) {
            Node child = (Node)children.get(i);
            
            FileRevision fileRevision = child.findRevisionWithHandlerName(name);
            if (fileRevision != null) {
                return fileRevision;
            }
        }
        
        for (int i = revisions.size() - 1 ; i >=0; i--) {
            Revision revision = (Revision)revisions.get(i);
            
            if (!revision.isDirectory()) {
                FileRevision fileRevision = (FileRevision)revision;
                
                if (name.equals(fileRevision.identifier().handlerName())) {
                    return fileRevision;
                }
            }
        }
        
        return null;
    }

    public synchronized boolean removeRevisionsWithHandlerName(String name)
    {
        boolean removed = false;
        
        for (int i = children.size() - 1 ; i >=0; i--) {
            Node child = (Node)children.get(i);
            
            removed |= child.removeRevisionsWithHandlerName(name);
        }
        
        for (int i = revisions.size() - 1 ; i >=0; i--) {
            Revision revision = (Revision)revisions.get(i);
            
            if (!revision.isDirectory()) {
                FileRevision fileRevision = (FileRevision)revision;
                
                if (name.equals(fileRevision.identifier().handlerName())) {
                    removed |= removeRevision(fileRevision);
                }
            }
        }
        
        return removed;
    }
    
    public synchronized boolean removeRevision(FileRevision revision)
    {
        boolean didRemove = revisions.remove(revision);

        if (didRemove) {
            invalidateSizes();

            // Strictly speaking we should never have children with no
            // revisions.  There should be one or more DirectoryRevisions
            // containing those children.
            if (revisions.isEmpty() && children.isEmpty()) {
                parent.removeChild(this);
            }
        }

        return didRemove;
    }
    
    public synchronized void removeChild(Node child)
    {
        if (children != Collections.EMPTY_LIST) {
            children.remove(child);
        }
        
        child.setParent(null);
        
        for (int i = revisions.size() - 1 ; i >=0; i--) {
            Revision revision = (Revision)revisions.get(i);
            
            if (revision.isDirectory()) {
                DirectoryRevision directoryRevision = (DirectoryRevision)revision;
                
                directoryRevision.deleteChild(child);
                if (directoryRevision.isEmpty()) {
                    revisions.remove(i);
                }
            }
        }
        
        if (revisions.isEmpty() && children.isEmpty() && parent != null) {
            parent.removeChild(this);
        }
    }
    
    public synchronized Revision findRevision(Date date)
    {
        if (date == null) {
            return lastRevision();
        }
        
        for (int i = revisions.size() - 1 ; i >=0; i--) {
            Revision revision = (Revision)revisions.get(i);
            Date revisionDate = revision.date();
            
            if (revisionDate.compareTo(date) < 1) {
                return revision;
            }
        }
        return null;
    }
    
    public synchronized Revision lastRevision()
    {
        if (revisions.isEmpty()) {
            return null;
        }
        return (Revision)revisions.get(revisions.size() - 1);
    }    
    
    public synchronized FileRevision lastFileRevision()
    {
        for (int i = revisions.size() - 1 ; i >=0; i--) {
            Revision revision = (Revision)revisions.get(i);
            
            if (!revision.isDirectory()) {
                return (FileRevision)revision;
            }
        }
        return null;
    }
    
    public synchronized DirectoryRevision lastDirectoryRevision()
    {
        for (int i = revisions.size() - 1 ; i >=0; i--) {
            Revision revision = (Revision)revisions.get(i);
            
            if (revision.isDirectory()) {
                return (DirectoryRevision)revision;
            }
        }
        return null;
    }
    
    public synchronized Revision previousRevision(Revision revision)
    {
        int index = revisions.indexOf(revision);
        
        if (index == -1) {
            throw new IllegalArgumentException("Attempt to find previous revision for a revision not associated with the target node");
        }
        
        if (index == 0) {
            return null;
        }
        
        return (Revision)revisions.get(index - 1);
    }
    
    public synchronized Revision nextRevision(Revision revision)
    {
        int index = revisions.indexOf(revision);
        
        if (index == -1) {
            throw new IllegalArgumentException("Attempt to find next revision for a revision not associated with the target node");
        }
        
        if (index == revisions.size() - 1) {
            return null;
        }
        
        return (Revision)revisions.get(index + 1);
    }
    
    public synchronized boolean hasBothFileAndDirectoryRevisions()
    {
        boolean isFile = false;
        boolean isDirectory = false;
        
        for (int i = 0, count = revisions.size(); i < count; i++) {
            Revision revision = (Revision)revisions.get(i);

            if (revision.isDirectory()) {
                isDirectory |= true;
            }
            else {
                isFile |= true;
            }
            
            if (isDirectory && isFile) {
                return true;
            }
        }        
        return false;
    }
    
    public List children()
    {
        return children;
    }
    
	public synchronized Node childWithName(String name)
	{
		for (int i = 0, count = children.size(); i < count; i++) {
			Node child = (Node)children.get(i);
			if (child.name().equals(name)) {
				return child;
			}
		}
		return null;
	}
	
    public synchronized void addChild(Node node)
    {
        if (children == Collections.EMPTY_LIST) {
            children = new ArrayList();
        }
        children.add(node);
        node.setParent(this);
        invalidateSizes();
    }
    
    public synchronized void accumulateBackedupSizeRatiosForType(String extension, float ratioOut[], int numberOfRevisions[])
    {
        if (name != null && name.endsWith(extension)) {

            for (int i = 0, count = revisions.size() ; i < count; i++) {
                Revision revision = (Revision)revisions.get(i);
                
                if (!revision.isDirectory()) {
                    FileRevision fileRevision = (FileRevision)revision;
                    numberOfRevisions[0]++;
                    ratioOut[0] += fileRevision.backedupSizeRatio();
                }
            }
        }

		for (int i = 0, count = children.size(); i < count; i++) {
			Node child = (Node)children.get(i);
			
			child.accumulateBackedupSizeRatiosForType(extension, ratioOut, numberOfRevisions);
		}
    }
    
	public void serializeXML(XMLSerializer writer)
	{
	    if (children.isEmpty() && revisions.isEmpty()) {
	        // Garbage collection for useless nodes.
	        return;
	    }
	    
		writer.push("node");

		if (name != null) {
	        writer.write("name", name);
	    }
	    
		for (int i = 0, count = children.size(); i < count; i++) {
			Node node = (Node)children.get(i);
			node.serializeXML(writer);
		}

		for (int i = 0, count = revisions.size(); i < count; i++) {
		    Revision revision = (Revision)revisions.get(i);
			revision.serializeXML(writer);
		}

		writer.pop();
	}

    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
		if ("name".equals(container)) {
			setName(value);
			return null;
		}
		else if ("node".equals(container)) {
			Node child = new Node();
			addChild(child);
			return child;
		}
		else if ("file".equals(container) || "directory".equals(container)) {
			Revision revision;
			if ("file".equals(container)) {
			    revision =  new FileRevision();
			}
			else {
			    revision = new DirectoryRevision();			    
			}
			addRevision(revision);
			return revision;
		}
		throw new RuntimeException("Unrecognized container: " + container);
    }
    
    public void writeData(DataOutputStream out) throws IOException
    {
        out.writeUTF(name == null ? "" : name);

        out.writeInt(children.size());
		for (int i = 0, count = children.size(); i < count; i++) {
			Node node = (Node)children.get(i);
			node.writeData(out);
		}

        out.writeInt(revisions.size());
		for (int i = 0, count = revisions.size(); i < count; i++) {
		    Revision revision = (Revision)revisions.get(i);
		    out.writeBoolean(revision.isDirectory());
			revision.writeData(out);
		}
    }
    
    public void readData(DataInputStream in) throws IOException
    {
        name = in.readUTF();
        if (name.length() == 0) {
            name = null;
        }
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
			Node child = new Node();
			addChild(child);
			child.readData(in);
        }
        
        count = in.readInt();
        for (int i = 0; i < count; i++) {
            boolean isDirectory = in.readBoolean();
            Revision revision;
            
            if (isDirectory) {
                revision = new DirectoryRevision();
            }
            else {
                revision = new FileRevision();
            }
			addRevision(revision);
			revision.readData(in);
        }
    }
}
