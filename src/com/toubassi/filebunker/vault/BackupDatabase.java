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

import com.subx.common.NotificationCenter;
import com.toubassi.archive.Archivable;
import com.toubassi.archive.ArchiveInputStream;
import com.toubassi.archive.ArchiveOutputStream;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * The BackupDatabase class tracks all revisions of all files that have been
 * backed up (in any store). It manages a tree of Nodes, each Node representing
 * a node in the file system namespace. Each node has children (which are the
 * nodes under that node as in files in a directory), and revisions, which track
 * the history of that name. Revisions are either FileRevisions, which represent
 * a backup of that file, or DirectoryRevisions, which means that node was a
 * directory, and files inside of it were backed up at that date. So a given
 * node may have existed both as a file and a directory at different times. A
 * date can be used to navigate the tree, effectively giving you a consistent
 * slice at a particular time (or more accurately, at the time of the closest
 * earlier backup operation).
 * 
 * This class is thread safe.
 * 
 * @author garrick
 */
public class BackupDatabase implements XMLSerializable, Serializable, Archivable
{
    public static final String ContentsChangedNotification = "ContentsChangedNotification";
    
    private Node root;
    private File file;
    private transient HashMap fileRevisionDigests = new HashMap();
    private transient int changesSinceLastSaveCounter;

    public BackupDatabase() throws IOException
    {
        this(null);
    }
    
    public BackupDatabase(File configDirectory) throws IOException
    {
        clear();
        
        if (configDirectory != null) {
            file = new File(configDirectory, "database2");
            
            if (file.exists()) {
                long startMillis = 0, endMillis = 0;                
                long startMem = 0, endMem = 0;
                Runtime runtime = Runtime.getRuntime();
                boolean profile = false;
                
                if (profile) {
                    runtime.gc();
                    runtime.gc();
                    startMem = runtime.totalMemory() - runtime.freeMemory();                    
                    startMillis = System.currentTimeMillis();                    
                }

                load();
                
                if (profile) {
	                endMillis = System.currentTimeMillis();
	                runtime.gc();
	                runtime.gc();
	                endMem = runtime.totalMemory() - runtime.freeMemory();
	                
	                System.out.println(root.totalFileRevisions() + " revisions");
	                System.out.println((endMillis - startMillis) + " ms");
	                System.out.println((endMem - startMem) + " b");
                }                
            }
            else {
                File legacyFile = new File(configDirectory, "database");
                
                if (legacyFile.exists()) {
                    FileInputStream fileInput = new FileInputStream(legacyFile);
                    BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);
                    
                    try {
                        loadLegacy(bufferedInput);
                    }
                    finally {
                        bufferedInput.close();
                    }
                    
                    save();
                    legacyFile.delete();

                    /*
                    saveXML(new BufferedOutputStream(new FileOutputStream(new File(configDirectory, "legacy.xml"))));
                    load();
                    save();
                    load();
                    saveXML(new BufferedOutputStream(new FileOutputStream(new File(configDirectory, "new.xml"))));
                    */
                }
            }
        }
    }

    private void clear()
    {
        root = new Node(null);
    }

    public File file()
    {
        return file;
    }

    public boolean isEmpty()
    {
        return root.isEmpty();
    }
    
    public Node root()
    {
        return root;
    }

    public synchronized long backedupBytes()
    {
        return root.totalBackedupSize();
    }

    /**
     * Finds the nth (where n is maxNodesToReturn) largest nodes in the database
     * where largest is judged by Node.nodeBackedupSize().
     * 
     * @param maxNodesToReturn
     *            Specifies the maximum number of nodes to return. -1 means all.
     * @return A newly constructed Set sorted from largest (first) to smallest
     *         (last).
     */
    public synchronized SortedSet findLargestNodes(int maxNodesToReturn)
    {
        SortedSet set = new TreeSet(NodeBackedupSizeComparator.comparator);

        root.findLargestNodes(set, maxNodesToReturn);

        return set;
    }

    public synchronized boolean needsBackup(File file)
    {
        Revision latest = findLastRevision(file);

        if (latest == null || latest.isDirectory()) {
            return true;
        }
        long lastModified = file.lastModified();
        Date lastBackup = latest.date();
        // 1 second of slop
        if (lastModified + 1000 < lastBackup.getTime()) {
            return false;
        }
        
        FileRevision latestFileRevision = (FileRevision)latest;
        
        if (file.length() != latestFileRevision.size()) {
            return true;
        }
        
        FileDigest latestDigest = latestFileRevision.identifier().digest();
        if (latestDigest != null) {
            try {
                FileDigest digest = new FileDigest(file);
                if (digest.equals(latestDigest)) {
                    return false;
                }                
            }
            catch (IOException e) {                
            }
        }

        return true;
    }

    public synchronized Node findNode(File file)
    {
    	String path = file.getPath();
    	String absPath = file.getAbsolutePath();
        StringTokenizer tokenizer = new StringTokenizer(absPath,
                File.separator);

        Node node = root;
        while (tokenizer.hasMoreTokens()) {
            String pathComponent = tokenizer.nextToken();
            node = node.childWithName(pathComponent);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    public synchronized Revision findRevision(File file, Date date)
    {
        Node node = findNode(file);
        if (node != null) {
            return node.findRevision(date);
        }
        return null;
    }

    public synchronized Revision findLastRevision(File file)
    {
        Node node = findNode(file);
        if (node != null) {
            return node.lastRevision();
        }
        return null;
    }

    public synchronized FileRevision findLastFileRevision(File file)
    {
        Node node = findNode(file);
        if (node != null) {
            return node.lastFileRevision();
        }
        return null;
    }

    public FileRevision fileRevisionWithDigest(FileDigest digest)
    {
        return (FileRevision)fileRevisionDigests.get(digest);
    }
    
    private synchronized void addFileRevisionDigest(FileRevision revision)
    {
        FileDigest digest = revision.identifier().digest();
        if (digest != null) {
            fileRevisionDigests.put(digest, revision);
        }        
    }

    private synchronized void indexFileRevisionDigests(Node node)
    {
        List revisions = node.revisions();
        for (int i = 0, count = revisions.size(); i < count; i++) {
            Revision revision = (Revision)revisions.get(i);
            
            if (!revision.isDirectory()) {
                addFileRevisionDigest((FileRevision)revision);
            }
        }
        
        List children = node.children();
        for (int i = 0, count = children.size(); i < count; i++) {
            Node child = (Node)children.get(i);
            
            indexFileRevisionDigests(child);
        }
    }

    private synchronized void recordRevision(String absolutePath,
            FileRevision revision) throws IOException
    {
        StringTokenizer tokenizer = new StringTokenizer(absolutePath,
                File.separator);

        Node node = root;
        while (tokenizer.hasMoreTokens()) {
            String pathComponent = tokenizer.nextToken();

            // First make sure there is a node for the next component of the
            // path
            Node child = node.childWithName(pathComponent);
            if (child == null) {
                child = new Node(pathComponent);
                node.addChild(child);
            }

            // Create/update the DirectoryRevision if necessary to show that
            // 'node' is a directory contianing 'child'
            Revision nodeRevision = node.findRevision(revision.date());
            if (nodeRevision != node.lastRevision()) {
                throw new UnsupportedOperationException(
                        "Can't record a revision at " + absolutePath
                                + " because it is in the past.");
            }
            boolean addNewDirectoryRevision = true;

            if (nodeRevision != null && nodeRevision.isDirectory()) {
                DirectoryRevision directoryRevision = (DirectoryRevision) nodeRevision;
                if (directoryRevision.hasChild(child)) {
                    addNewDirectoryRevision = false;
                } else if (directoryRevision.date().equals(revision.date())) {
                    directoryRevision.addChild(child);
                    addNewDirectoryRevision = false;
                }
            }

            if (addNewDirectoryRevision) {
                DirectoryRevision newDirectoryRevision = new DirectoryRevision();
                newDirectoryRevision.setDate(revision.date());
                newDirectoryRevision.addChild(child);
                node.addRevision(newDirectoryRevision);
            }

            node = child;
        }

        node.addRevision(revision);
        addFileRevisionDigest(revision);

        databaseChanged();
    }

    public synchronized FileRevision recordRevision(File file, Date date,
            RevisionIdentifier identifier) throws IOException
    {
        FileRevision revision = new FileRevision();
        revision.setIdentifier(identifier);
        revision.setDate(date);

        recordRevision(file.getAbsolutePath(), revision);
        return revision;
    }

    public synchronized void updateDirectoryMembership(File directory,
            Date date, String children[]) throws IOException
    {
        Revision revision = findRevision(directory, date);

        if (revision == null || !revision.isDirectory()) {
            return;
        }

        // Make sure the revision we got is the last one, because we will be
        // updating the directory by
        // appending to the end, so we don't support updating directories that
        // existed "in the past"
        if (revision != findLastRevision(directory)) {
            throw new UnsupportedOperationException("Can't update directory "
                    + directory + " on " + date
                    + " because it is not the last revision");
        }

        DirectoryRevision directoryRevision = (DirectoryRevision) revision;
        DirectoryRevision updateDirectoryRevision = null;
        List childList = Arrays.asList(children);
        
        boolean didChange = false;

        Iterator i = directoryRevision.children();
        while (i.hasNext()) {
            Node child = (Node) i.next();
            if (!childList.contains(child.name())) {
                if (updateDirectoryRevision == null) {
                    if (directoryRevision.date().equals(date)) {
                        updateDirectoryRevision = directoryRevision;
                    } else {
                        updateDirectoryRevision = new DirectoryRevision();
                        updateDirectoryRevision.setDate(date);
                        revision.node().addRevision(updateDirectoryRevision);
                    }
                }
                updateDirectoryRevision.removeChild(child);
                didChange = true;
            }
        }

        if (didChange) {
            databaseChanged();
        }
    }

    public synchronized boolean removeRevision(FileRevision revision)
    {
        RevisionIdentifier identifier = revision.identifier();
        revision.node().removeRevision(revision);
        databaseChanged();
        if (identifier.hasReferences()) {
            return false;
        }
        if (identifier.digest() != null) {
            fileRevisionDigests.remove(identifier.digest());
        }
        return true;
    }
    
    public synchronized FileRevision findRevisionWithHandlerName(String name)
    {
        return root.findRevisionWithHandlerName(name);
    }
    
    public synchronized boolean removeRevisionsWithHandlerName(String name) throws IOException
    {
        boolean removed = root.removeRevisionsWithHandlerName(name);
        
        if (removed) {
            databaseChanged();
        }
        return removed;
    }

    public synchronized float averageBackedupSizeRatioForType(String extension)
    {
        if (!extension.startsWith(".")) {
            throw new IllegalArgumentException(
                    "Extension must start with a dot");
        }

        float ratio[] = new float[1];
        int numberOfRevisions[] = new int[1];

        root.accumulateBackedupSizeRatiosForType(extension, ratio,
                numberOfRevisions);
        if (ratio[0] == 0 || numberOfRevisions[0] == 0) {
            return 1.0f;
        }
        return ratio[0] / numberOfRevisions[0];
    }

    private synchronized void databaseChanged()
    {
        changesSinceLastSaveCounter++;
        NotificationCenter.sharedCenter().post(ContentsChangedNotification, this, null);
    }

    public synchronized boolean saveIfNecessary() throws IOException
    {
        if (changesSinceLastSaveCounter > 0) {
            save();
            return true;
        }
        return false;
    }

    public synchronized void save() throws IOException
    {
        File tempFile = File.createTempFile("database", null, file.getParentFile());
        FileOutputStream fileOutput = new FileOutputStream(tempFile);
        BufferedOutputStream output = new BufferedOutputStream(fileOutput);

        try {
            save(output);
            changesSinceLastSaveCounter = 0;
        } finally {
            output.close();
        }

        file.delete();
        if (!tempFile.renameTo(file)) {
            throw new IOException("Couldn't rename " + tempFile + " to " + file);
        }
    }

    public synchronized void save(OutputStream output) throws IOException
    {
        ArchiveOutputStream archiveOutput = new ArchiveOutputStream(output);

        archive(archiveOutput);
        archiveOutput.flush();
    }

    private synchronized void saveLegacy(OutputStream output) throws IOException
    {
        DataOutputStream dataOutput = new DataOutputStream(output);
        root.writeData(dataOutput);
        dataOutput.flush();
    }

    public synchronized void saveXML(OutputStream output) throws IOException
    {
        XMLSerializer serializer = new XMLSerializer(output);
        serializer.setIndentIncrement(2);
        serializeXML(serializer);
        serializer.flush();
    }

    public synchronized void load() throws IOException
    {
        FileInputStream fileInput = new FileInputStream(file);
        BufferedInputStream bufferedInput = new BufferedInputStream(fileInput, 4096);

        try {
            load(bufferedInput);
        } finally {
            bufferedInput.close();
        }
    }

    public synchronized void load(InputStream input) throws IOException
    {
        ArchiveInputStream archiveInput = new ArchiveInputStream(input);

        unarchive(archiveInput);
    }

    private synchronized void loadLegacy(InputStream input) throws IOException
    {
        clear();

        DataInputStream dataInput = new DataInputStream(input);
        root.readData(dataInput);
    }

    public void serializeXML(XMLSerializer serializer)
    {
        serializer.push("backupDatabase");
        List children = root.children();
        for (int i = 0, count = children.size(); i < count; i++) {
            Node node = (Node) children.get(i);
            node.serializeXML(serializer);
        }
        serializer.pop();
    }

    public XMLSerializable deserializeXML(XMLDeserializer deserializer,
            String container, String value)
    {        
        throw new RuntimeException("Can't read xml");
    }
    
    public void archive(ArchiveOutputStream output) throws IOException
    {
        output.writeClassVersion("com.toubassi.filebunker.vault.BackupDatabase", 1);
        output.writeObject(root, Archivable.StrictlyTypedValue);
    }
    
    public void unarchive(ArchiveInputStream input) throws IOException
    {
        input.readClassVersion("com.toubassi.filebunker.vault.BackupDatabase");
        root = (Node)input.readObject(Archivable.StrictlyTypedValue, Node.class);
        indexFileRevisionDigests(root);
    }
}

class NodeBackedupSizeComparator implements Comparator
{
    public static final NodeBackedupSizeComparator comparator = new NodeBackedupSizeComparator();

    public int compare(Object o1, Object o2)
    {
        Node node1 = (Node) o1;
        Node node2 = (Node) o2;

        long result = node2.nodeBackedupSize() - node1.nodeBackedupSize();

        if (result == 0) {
            if (node1 == node2) {
                return 0;
            }
            return node2.file().getPath().compareTo(node1.file().getPath());
        }
        return result < 0 ? -1 : 1;
    }
}