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
 * Created on Jul 27, 2004
 */
package com.toubassi.filebunker.vault.test;

import com.toubassi.filebunker.vault.BackupDatabase;
import com.toubassi.filebunker.vault.DirectoryRevision;
import com.toubassi.filebunker.vault.FileRevision;
import com.toubassi.filebunker.vault.Node;
import com.toubassi.filebunker.vault.RevisionIdentifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.SortedSet;


/**
 * @author garrick
 */
public class BackupDatabaseTest
{

    public static void testFindLargest() throws Exception
    {
        BackupDatabase db = new BackupDatabase();
        
        File[] files = new File[1000];
        int[] sizes = new int[files.length + 1];
        
        sizes[sizes.length - 1] = 1000000000;
        
        Random random = new Random();
        
        Date date = new Date();
        for (int i = 0; i < files.length; i++) {
            files[i] = new File("c:/file" + (i + 1));
            sizes[i] = random.nextInt(10000000) + 1;
            db.recordRevision(files[i], date, new RevisionIdentifier("test"), sizes[i]);
        }

        Date date2 = new Date(date.getTime() + 1000);
        for (int i = 0; i < files.length; i++) {
            int size = random.nextInt(10000) + 1;
            db.recordRevision(files[i], date2, new RevisionIdentifier("test"), size);
            sizes[i] += size;
        }

        SortedSet all = db.findLargestNodes(-1);
        Iterator i = all.iterator();

        // The root and c: node also show up.
        assert all.size() == files.length + 2;

        Arrays.sort(sizes);
        int currentSize = sizes.length - 2;
        while (currentSize >= 0) {
            Node node = (Node)i.next();
            assert node.nodeBackedupSize() == sizes[currentSize];
            assert node.nodeBackedupSize() <= sizes[currentSize + 1];
            currentSize--;
        }
        
        assert ((Node)i.next()).nodeBackedupSize() == 0;
        assert ((Node)i.next()).nodeBackedupSize() == 0;

        // Just get the top ten
        SortedSet top10 = db.findLargestNodes(10);
        i = top10.iterator();

        assert top10.size() == 10;

        Arrays.sort(sizes);
        currentSize = sizes.length - 2;
        while (i.hasNext()) {
            Node node = (Node)i.next();
            assert node.nodeBackedupSize() == sizes[currentSize];
            assert node.nodeBackedupSize() <= sizes[currentSize + 1];
            currentSize--;
        }

        /**/
    }

    public static void main(String args[]) throws Exception
    {
        BackupDatabase db = new BackupDatabase();
        
        File file1 = new File("c:/dir1/dir2/file1");
        File file2 = new File("c:/dir1/dir2/file2");
        File file3 = new File("c:/dir1/dir2/fileordir3");
        File dir = new File("c:/dir1/dir2");
        
        // Make sure the empty db doesn't crash when asking for nonexistent nodes/revisions
        assert db.findNode(file1) == null;
        assert db.findRevision(file1, new Date()) == null;
        assert db.findLastRevision(file1) == null;
        assert db.findLastRevision(dir) == null;
        
        Date date1 = new Date();
        
        // Test new revisions
        db.recordRevision(file1, date1, new RevisionIdentifier("test"), 1024);
        db.recordRevision(file2, date1, new RevisionIdentifier("test"), 1024);
        db.recordRevision(file3, date1, new RevisionIdentifier("test"), 1024);
        
        Date date2 = new Date(date1.getTime() + 1000);

        assert db.findNode(file1) != null;
        assert db.findRevision(file1, date1) != null;
        assert db.findRevision(file1, date2) != null;
        assert db.findLastRevision(file1) == db.findRevision(file1, date2);
        assert db.findLastRevision(dir).isDirectory();
        
        // Check sizes
        assert db.root().totalBackedupSize() == 1024*3;
        assert db.root().nodeBackedupSize() == 0;
        assert db.findNode(file1).totalBackedupSize() == 1024;
        assert db.findNode(file1).nodeBackedupSize() == 1024;
        
        // Test follow on revisions
        db.recordRevision(file1, date2, new RevisionIdentifier("test"), 1024);
        db.recordRevision(file2, date2, new RevisionIdentifier("test"), 1024);
        db.recordRevision(file3, date2, new RevisionIdentifier("test"), 1024);
        
        assert db.findRevision(file1, date2) != null;
        assert db.findRevision(file1, date1) != db.findRevision(file1, date2);

        // Check sizes
        assert db.root().totalBackedupSize() == 1024*6;
        assert db.root().nodeBackedupSize() == 0;
        assert db.findNode(file1).totalBackedupSize() == 1024*2;
        assert db.findNode(file1).nodeBackedupSize() == 1024*2;
        
        // Test changing a file to a directory
        Date date3 = new Date(date2.getTime() + 1000);
        
        File file4 = new File("c:/dir1/dir2/fileordir3/file1");
        assert db.findNode(file4) == null;

        db.recordRevision(file4, date3, new RevisionIdentifier("test"), 1024);

        assert db.findRevision(file4, date3) != null;
        assert !db.findRevision(file3, date2).isDirectory();
        assert db.findRevision(file3, date3).isDirectory();
        assert !db.findRevision(file4, date3).isDirectory();

        // Check sizes
        assert db.root().totalBackedupSize() == 1024*7;
        assert db.root().nodeBackedupSize() == 0;
        assert db.findNode(file3).totalBackedupSize() == 1024*3;
        assert db.findNode(file3).nodeBackedupSize() == 1024*2;
        
        // Test a new revision on the new subfile
        Date date4 = new Date(date3.getTime() + 1000);
        db.recordRevision(file4, date4, new RevisionIdentifier("test"), 1024);
        assert db.findRevision(file4, date4) != null;
        assert db.findRevision(file4, date3) != null;
        assert db.findRevision(file4, date2) == null;
        assert db.findRevision(file4, date4) != db.findRevision(file4, date3);

        // Change it back into a file
        // Test changing a file to a directory
        Date date5 = new Date(date4.getTime() + 1000);
        
        db.recordRevision(file3, date5, new RevisionIdentifier("test"), 1024);
        assert !db.findRevision(file3, date1).isDirectory();
        assert !db.findRevision(file3, date2).isDirectory();
        assert db.findRevision(file3, date3).isDirectory();
        assert db.findRevision(file3, date4).isDirectory();
        assert !db.findRevision(file3, date5).isDirectory();
        
        // Directory updating
        Date date6 = new Date(date5.getTime() + 1000);
        String[] dirContents = {"file1", "file2"};
    	
    	Iterator i = ((DirectoryRevision)db.findRevision(dir, date5)).children();
    	assert ((Node)i.next()).name().equals("file1");
    	assert ((Node)i.next()).name().equals("file2");
    	assert ((Node)i.next()).name().equals("fileordir3");
    	assert i.next() == null;

    	db.updateDirectoryMembership(dir, date6, dirContents);
    	i = ((DirectoryRevision)db.findRevision(dir, date5)).children();
    	assert ((Node)i.next()).name().equals("file1");
    	assert ((Node)i.next()).name().equals("file2");
    	assert ((Node)i.next()).name().equals("fileordir3");
    	assert i.next() == null;
        
    	i = ((DirectoryRevision)db.findRevision(dir, date6)).children();
    	assert ((Node)i.next()).name().equals("file1");
    	assert ((Node)i.next()).name().equals("file2");
    	assert i.next() == null;

    	// Add back a different third file to the directory
        Date date7 = new Date(date6.getTime() + 1000);
        File file5 = new File("c:/dir1/dir2/file5");
        
        db.recordRevision(file5, date7, new RevisionIdentifier("test"), 1024);
        assert db.findRevision(file5, date6) == null;
        assert db.findRevision(file5, date7) != null;

    	i = ((DirectoryRevision)db.findRevision(dir, date5)).children();
    	assert ((Node)i.next()).name().equals("file1");
    	assert ((Node)i.next()).name().equals("file2");
    	assert ((Node)i.next()).name().equals("fileordir3");
    	assert i.next() == null;
        
    	i = ((DirectoryRevision)db.findRevision(dir, date6)).children();
    	assert ((Node)i.next()).name().equals("file1");
    	assert ((Node)i.next()).name().equals("file2");
    	assert i.next() == null;
        
    	i = ((DirectoryRevision)db.findRevision(dir, date7)).children();
    	assert ((Node)i.next()).name().equals("file5");
    	assert ((Node)i.next()).name().equals("file1");
    	assert ((Node)i.next()).name().equals("file2");
    	assert i.next() == null;

    	// Another delete
        Date date8 = new Date(date7.getTime() + 1000);
        String[] dirContents2 = {"file2", "file5"};
    	
    	db.updateDirectoryMembership(dir, date8, dirContents2);
    	i = ((DirectoryRevision)db.findRevision(dir, date7)).children();
    	assert ((Node)i.next()).name().equals("file5");
    	assert ((Node)i.next()).name().equals("file1");
    	assert ((Node)i.next()).name().equals("file2");
    	assert i.next() == null;
        
    	i = ((DirectoryRevision)db.findRevision(dir, date8)).children();
    	assert ((Node)i.next()).name().equals("file5");
    	assert ((Node)i.next()).name().equals("file2");
    	assert i.next() == null;

    	/**/
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
        db.save(out);
        
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        BackupDatabase db2 = new BackupDatabase();
        db2.load(in);
        
    	ByteArrayOutputStream out2 = new ByteArrayOutputStream();
    	db2.save(out2);

    	i = ((DirectoryRevision)db2.findRevision(dir, date7)).children();
    	assert ((Node)i.next()).name().equals("file5");
    	assert ((Node)i.next()).name().equals("file1");
    	assert ((Node)i.next()).name().equals("file2");
    	assert i.next() == null;

    	i = ((DirectoryRevision)db2.findRevision(dir, date8)).children();
    	assert ((Node)i.next()).name().equals("file5");
    	assert ((Node)i.next()).name().equals("file2");
    	assert i.next() == null;
    	
        assert Arrays.equals(out.toByteArray(), out2.toByteArray());
        
        // Test removing revisions
        assert db.findRevision(file1, date1) != null;
        assert db.findRevision(file1, date2) != null;
        db.removeRevision((FileRevision)db.findRevision(file1, date1));
        assert db.findRevision(file1, date1) == null;
        assert db.findRevision(file1, date2) != null;

        // Check sizes
        assert db.root().totalBackedupSize() + 1024 == db2.root().totalBackedupSize();
        
    	out = new ByteArrayOutputStream();
        db.save(out);
        
        in = new ByteArrayInputStream(out.toByteArray());
        BackupDatabase db3 = new BackupDatabase();
        db3.load(in);

        assert db3.findRevision(file1, date1) == null;
        assert db3.findRevision(file1, date2) != null;        

        // Check sizes
        assert db.root().totalBackedupSize() == db3.root().totalBackedupSize();
        
        testFindLargest();
    }
}
