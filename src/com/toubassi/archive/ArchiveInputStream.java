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
package com.toubassi.archive;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Reads an archive created using ArchiveOutputStream.
 * See ArchiveOutputStream for more information.
 * @author garrick
 */
public class ArchiveInputStream extends DataInputStream
{
    private int version;
    private ArrayList strings = new ArrayList();
    private ArrayList instances = new ArrayList();
    private ArrayList classes = new ArrayList();
    private HashMap classVersions = new HashMap();
    
    public static Object unarchive(String path) throws IOException
    {
        ArchiveInputStream input = new ArchiveInputStream(path);
        Object object = input.readObject(Archivable.PolymorphicReference, null);
        input.close();
        return object;
    }
    
    public static Object unarchive(File file) throws IOException
    {
        return unarchive(file.getPath());
    }
    
    public ArchiveInputStream(InputStream input) throws IOException
    {
        super(input instanceof BufferedInputStream ? input : new BufferedInputStream(input));
        if (readInt() != ArchiveOutputStream.Magic) {
            throw new IOException("Bad magic number");
        }
        version = readCompactInt();
    }

    public ArchiveInputStream(File file) throws IOException
    {
        this(file.getPath());
    }

    public ArchiveInputStream(String path) throws IOException
    {
        this(new FileInputStream(path));
    }
    
    private Object instantiateUnarchivedObject(Class unarchivedObjectClass) throws IOException
    {
        try {
            return unarchivedObjectClass.newInstance();
        }
        catch (Exception e) {
            IOException wrapper = new IOException("Could not instantiate " + unarchivedObjectClass);
            wrapper.initCause(e);
            throw wrapper;
        }
    }
    
    private Class classForName(String classname) throws IOException
    {
        try {
            return Class.forName(classname);
        }
        catch (ClassNotFoundException e) {            
            IOException wrapper = new IOException("Could not find class " + classname);
            wrapper.initCause(e);
            throw wrapper;
        }        
    }
    
    private void unarchiveObject(Object object) throws IOException
    {
        if (object instanceof Archivable) {
            ((Archivable)object).unarchive(this);
        }
        else {
            ExternalArchiver archiver = ArchiveOutputStream.externalArchiverForClassname(object.getClass().getName());
            
            if (archiver == null) {
                throw new IOException("Don't know how to unarchive " + object.getClass());
            }
            archiver.unarchive(object, this);            
        }
    }
    
    private Object instantiateArchivedPolymorphicObject(int style) throws IOException
    {
        Class archivableClass;        
        
        int id = readCompactInt();
        
        if (id == 0) {
            archivableClass = classForName(readUTF());
            classes.add(archivableClass);
        }
        else {
            archivableClass = (Class)classes.get(id - 1);
        }

        Object unarchivedObject = instantiateUnarchivedObject(archivableClass);
        if (style == Archivable.PolymorphicReference) {
            instances.add(unarchivedObject);
        }
        unarchiveObject(unarchivedObject);
        return unarchivedObject;
    }
    
    /**
     * Reads an object that was archived with the specified style.  If the
     * style was either StrictlyTypedReference or StrictlyTypedValue, then
     * the Class of that object must be specified.
     */
    public Object readObject(int style, Class archivableClass) throws IOException
    {
        Object unarchivedObject = null;

        if (style == Archivable.StrictlyTypedValue) {
            unarchivedObject = instantiateUnarchivedObject(archivableClass);
            unarchiveObject(unarchivedObject);
        }
        else if (style == Archivable.StrictlyTypedReference) {
            int id = readCompactInt();
            if (id > 0) {
                unarchivedObject = instances.get(id - 1);                
            }
            else if (id == 0){
                unarchivedObject = instantiateUnarchivedObject(archivableClass);
                instances.add(unarchivedObject);
                unarchiveObject(unarchivedObject);
            }
        }
        else if (style == Archivable.PolymorphicValue) {
            unarchivedObject = instantiateArchivedPolymorphicObject(style);
        }
        else if (style == Archivable.PolymorphicReference) {
            int id = readCompactInt();
            if (id > 0) {
                unarchivedObject = instances.get(id - 1);                
            }
            else if (id == 0){
                unarchivedObject = instantiateArchivedPolymorphicObject(style);
            }            
        }
        return unarchivedObject;
    }

    /**
     * A convenience for reading an object that was written as a
     * PolymorphicReference (or with the single arg writeObject method)
     */
    public Object readObject() throws IOException
    {
        return readObject(Archivable.PolymorphicReference, null);
    }

    /**
     * Reads an ArrayList, whose elements were written with the specified
     * style.  If the elements were written as StrictlyTypedReference or
     * StrictlyTypedValue, then the element class must be specified.
     */
    public ArrayList readList(int style, Class elementClass) throws IOException
    {
        int size = readCompactInt();
        if (size == -1) {
            return null;
        }
        ArrayList list = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(readObject(style, elementClass));
        }
        return list;
    }

    /**
     * If you want something other than an ArrayList you can specify the
     * list.  If you are reading into an ArrayList, use the 2 arg readList
     * because it will allocate the ArrayList to the correct size initially.
     */
    public void readList(List list, int style, Class elementClass) throws IOException
    {
        int size = readCompactInt();
        if (size == -1) {
            return;
        }
        for (int i = 0; i < size; i++) {
            list.add(readObject(style, elementClass));
        }
    }
    
    public String readUniqueString() throws IOException
    {
        int id = readCompactInt();
        
        if (id == -1) {
            return null;
        }
        if (id > 0) {
            return (String)strings.get(id - 1);
        }
        String string = readUTF();
        strings.add(string);
        return string;
    }
    
    public int readClassVersion(String classname) throws IOException
    {
        Integer id = (Integer)classVersions.get(classname);
        
        if (id == null) {
            id = new Integer(readCompactInt());
            classVersions.put(classname, id);
        }
        return id.intValue();
    }    

    public int readCompactInt() throws IOException
    {
        int c, value;
        boolean negative;

        c = in.read();
        if (c < 0) {
            throw new EOFException();
        } else if (c == 0x040) {
            return Integer.MIN_VALUE;
        }

        negative = (c & 0x40) != 0;
        value = c & 0x3f;

        int shift = 6;
        
        while ((c & 0x80) != 0) {
            c = in.read();
            if (c < 0) {
                throw new EOFException();
            }
            value = value | ((c & 0x7f) << shift);
            shift += 7;
        }
        
        if (negative)
            value = -value;

        return value;
    }

    public long readCompactLong() throws IOException
    {
        long c, value;
        boolean negative;

        c = in.read();
        if (c < 0) {
            throw new EOFException();
        }
        else if (c == 0x040) {
            return Long.MIN_VALUE;
        }

        negative = (c & 0x40) != 0;
        value = c & 0x3f;

        int shift = 6;
        
        while ((c & 0x80) != 0) {
            c = in.read();
            if (c < 0) {
                throw new EOFException();
            }
            value = value | ((c & 0x7f) << shift);
            shift += 7;
        }

        if (negative)
            value = -value;

        return value;
    }
}
