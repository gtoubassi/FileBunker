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
package com.toubassi.archive.test;

import com.toubassi.archive.Archivable;
import com.toubassi.archive.ArchiveInputStream;
import com.toubassi.archive.ArchiveOutputStream;

import java.io.IOException;

/**
 * @author garrick
 */
public class Simple implements Archivable
{
    private static int counter = 1;
    
    private String s = Long.toString(System.currentTimeMillis());
    private int i = (int)System.currentTimeMillis();
    private int j = counter++;

    public boolean equals(Object other)
    {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof Simple)) {
            return false;
        }
        Simple otherSimple = (Simple)other;
        return i == otherSimple.i && j == otherSimple.j && s.equals(otherSimple.s);
    }
    
    public void archive(ArchiveOutputStream output) throws IOException
    {
        output.writeClassVersion("com.toubassi.archive.test.Simple", 57);
        output.writeInt(i);
        output.writeInt(j);
        output.writeUTF(s);
        output.writeUniqueString(s);
        output.writeUniqueString(s);
    }
    
    public void unarchive(ArchiveInputStream input) throws IOException
    {
        assert input.readClassVersion("com.toubassi.archive.test.Simple") == 57;
        i = input.readInt();
        j = input.readInt();
        s = input.readUTF();
        String s2 = input.readUniqueString();
        if (!s.equals(s2)) {
            throw new IOException("String mismatch: '" + s + "' '" + s2 + "'");
        }
        if (s2 != input.readUniqueString()) {
            throw new IOException("Unique strings not ==");
        }
    }    
}
