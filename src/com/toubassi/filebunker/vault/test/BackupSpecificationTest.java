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
 * Created on Aug 4, 2004
 */
package com.toubassi.filebunker.vault.test;

import com.toubassi.filebunker.vault.BackupSpecification;

import java.io.File;
import java.util.ArrayList;

/**
 * @author garrick
 */
public class BackupSpecificationTest
{

    public static void testAddRemoveAdd()
    {
        BackupSpecification spec = new BackupSpecification();
        
        spec.addFile(new File("/foo"));
        spec.removeFile(new File("/foo/bar"));
        spec.addFile(new File("/foo/bar/bazz"));
        
        assert spec.containsFile(new File("/foo"));
        assert spec.containsFile(new File("/foo/biff"));
        assert !spec.containsFile(new File("/foo/bar"));
        assert spec.containsFile(new File("/foo/bar/bazz"));
        assert spec.containsFile(new File("/foo/bar/bazz/biff.txt"));
    }
    
    public static void main(String[] args)
    {
        BackupSpecification spec = new BackupSpecification();
        
        spec.addFile(new File("/foo"));
        
        assert spec.containsFile(new File("/foo"));
        assert spec.containsFile(new File("/foo/bar"));
        assert spec.containsFile(new File("/foo/bar/bazz"));
        assert !spec.containsFile(new File("/foobar"));
        
        spec.removeFile(new File("/foo"));

        assert !spec.containsFile(new File("/foo"));
        assert !spec.containsFile(new File("/foo/bar"));
        assert !spec.containsFile(new File("/foo/bar/bazz"));
        assert !spec.containsFile(new File("/foobar"));

        spec.addFile(new File("/foo"));

        assert spec.containsFile(new File("/foo"));
        assert spec.containsFile(new File("/foo/bar"));
        assert spec.containsFile(new File("/foo/bar/bazz"));
        assert !spec.containsFile(new File("/foobar"));

        spec.removeFile(new File("/foo/bar/bazz"));

        assert spec.containsFile(new File("/foo"));
        assert spec.containsFile(new File("/foo/bar"));
        assert !spec.containsFile(new File("/foo/bar/bazz"));
        assert !spec.containsFile(new File("/foo/bar/bazz/blorg"));
        assert !spec.containsFile(new File("/foobar"));
        
        ArrayList roots = spec.roots();

        assert roots.size() == 1 && ((File)roots.get(0)).equals(new File("/foo"));

        spec.addFile(new File("/foo/bar/bazz"));
        
        assert spec.containsFile(new File("/foo"));
        assert spec.containsFile(new File("/foo/bar"));
        assert spec.containsFile(new File("/foo/bar/bazz"));
        assert spec.containsFile(new File("/foo/bar/bazz/blorg"));
        assert !spec.containsFile(new File("/foobar"));
        
        spec.removeFile(new File("/foo/bar/bazz"));

        assert spec.containsFile(new File("/foo"));
        assert spec.containsFile(new File("/foo/bar"));
        assert !spec.containsFile(new File("/foo/bar/bazz"));
        assert !spec.containsFile(new File("/foo/bar/bazz/blorg"));
        assert !spec.containsFile(new File("/foobar"));
        
        spec.removeFile(new File("/foo/bar"));

        assert spec.containsFile(new File("/foo"));
        assert !spec.containsFile(new File("/foo/bar"));
        assert !spec.containsFile(new File("/foo/bar/bazz"));
        assert !spec.containsFile(new File("/foo/bar/bazz/blorg"));
        assert !spec.containsFile(new File("/foobar"));

        spec.addFile(new File("/foo/bar"));
        
        assert spec.containsFile(new File("/foo"));
        assert spec.containsFile(new File("/foo/bar"));
        assert spec.containsFile(new File("/foo/bar/bazz"));
        assert spec.containsFile(new File("/foo/bar/bazz/blorg"));
        assert !spec.containsFile(new File("/foobar"));
        
        testAddRemoveAdd();
    }
}
