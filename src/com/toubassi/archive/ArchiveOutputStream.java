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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This is an archiving mechanism designed to generate small and fast
 * serialized versions of object graphs.  ints and longs can be written
 * in a compact fashion, and object references can be written in a
 * variety of ways to avoid per object overhead.  Objects can be
 * archived if they either implement Archivable, or if there is
 * an ExternalArchiver registered for them.
 * 
 * @author garrick
 */
public class ArchiveOutputStream extends DataOutputStream
{
    private static final MutableIdentityHashKey probeIdKey = new MutableIdentityHashKey();
    static final HashMap externalArchivers = new HashMap();

    /** All archives start with this magic number. */    
    static int Magic = 0x55fa66de;
    
    /** A version number to allow for changing the internal archiving format. */
    static int ArchiveVersion = 1;
    
    /**
     * Keeps track of class version information set with writeClassVersion.
     * Maps class name to version Integer.
     */
    private HashMap classVersions = new HashMap();
    
    /** 
     * Keeps track of strings written with writeCompactString.
     * Maps strings to an Integer id.
     */
    private HashMap strings = new HashMap();
    
    /**
     * Keeps track of instances of objects written as either
     * StrictlyTypedReferences or PolymorphicReferences.  Maps
     * objects to an Integer id.
     */
    private HashMap instances = new HashMap();
    
    /**
     * Keeps track of classes of objects that have been archived
     * as either PolymorphicReference or PolymorphicValue.  Maps
     * classes to an Integer id.
     */
    private HashMap classes = new HashMap();
    
    /**
     * Tracks statistics # of bytes per class, per instance, both
     * with and without children included.  Only enabled if non-null
     * (by calling setKeepStatistics).
     */
    private ArchiveStatistics statistics;
    
    
    static {
        // Static initializer to initialize built in external archivers.
        registerExternalArchiver(new DateArchiver(), Date.class.getName());
    }
    
    static ExternalArchiver externalArchiverForClassname(String classname)
    {
        return (ExternalArchiver)externalArchivers.get(classname);
    }
    
    public static void registerExternalArchiver(ExternalArchiver archiver, String classname)
    {
        externalArchivers.put(classname, archiver);
    }

    /**
     * Convenience for archiving an object to a file.
     */
    public static void archive(String path, Object object) throws IOException
    {
        ArchiveOutputStream output = new ArchiveOutputStream(path);
        output.writeObject(object, Archivable.PolymorphicReference);
        output.close();
    }
    
    /**
     * Convenience for archiving an object to a file.
     */
    public static void archive(File file, Archivable object) throws IOException
    {
        archive(file.getPath(), object);
    }
    
    /**
     * Creates an ArchiveOutputStream which will archive data/objects to
     * the specified underlying OutputStream.
     */
    public ArchiveOutputStream(OutputStream output) throws IOException
    {
        super(output instanceof BufferedOutputStream ? output : new BufferedOutputStream(output));
        writeInt(Magic);
        writeCompactInt(ArchiveVersion);
    }

    /**
     * Convenience constructor that creates an ArchiveOutputStream which
     * will archive data/objects to the specified file.
     */
    public ArchiveOutputStream(File file) throws IOException
    {
        this(file.getPath());
    }

    /**
     * Convenience constructor that creates an ArchiveOutputStream which
     * will archive data/objects to the specified file.
     */
    public ArchiveOutputStream(String path) throws IOException
    {
        this(new FileOutputStream(path));
    }
    
    /**
     * If true, then statistics are kept on the archive process.  See
     * dumpStatistics.  By default statistics are not kept.
     */
    public void setKeepStatistics(boolean flag)
    {
        statistics = (flag ? new ArchiveStatistics(this) : null);
    }

    /**
     * When objects are written as a reference for the first time, a 0
     * is written, followed by the object itself.  If it has already
     * been written, the objects id is written (which is an incrementing
     * int starting at 1 representing the order it was written).
     */
    private boolean writeObjectIdentifier(Object object) throws IOException
    {
        if (object == null) {
            writeCompactInt(-1);
            return false;
        }
        
        Integer id;
        synchronized (probeIdKey) {
            probeIdKey.setObject(object);
            id = (Integer)instances.get(probeIdKey);            
            probeIdKey.clearObject();
        }
        
        if (id == null) {
            id = new Integer(instances.size() + 1);
            instances.put(new IdentityHashKey(object), id);

            writeCompactInt(0);
            return true;
        }
        else {
            writeCompactInt(id.intValue());
            return false;
        }                
    }

    /**
     * When polymorphic references or values are written, class info is
     * included, which is essentially a uniqued string (0, followed by
     * UTF, the first time, a non zero positive int following).  We don't
     * use UniqueString because that is slower because it would require
     * many calls to Class.forName() when unarchiving.
     */
    private void writeClassInformation(Object object) throws IOException
    {
        Integer id = (Integer)classes.get(object.getClass());
        
        if (id == null) {
            id = new Integer(classes.size() + 1);
            classes.put(object.getClass(), id);
            writeCompactInt(0);
            writeUTF(object.getClass().getName());
        }
        else {
            writeCompactInt(id.intValue());
        }
    }

    /**
     * Write an object.  See Archivable for the archiving styles.  The
     * different styles allow you to control the per object overhead.
     */
    public void writeObject(Object object, int style) throws IOException
    {
        if (statistics != null && object != null) {
            statistics.begin(object.getClass().getName());
        }

        try {
	        if (style == Archivable.StrictlyTypedValue) {
	        }
	        else if (style == Archivable.StrictlyTypedReference) {
	            if (!writeObjectIdentifier(object)) {
	                return;
	            }
	        }
	        else if (style == Archivable.PolymorphicValue) {
	            writeClassInformation(object);
	        }
	        else if (style == Archivable.PolymorphicReference) {
	            if (!writeObjectIdentifier(object)) {
	                return;
	            }
	            writeClassInformation(object);
	        }
	        else {
	            throw new IOException("Unknown archive style " + style);
	        }
	        
	        if (object instanceof Archivable) {
		        ((Archivable)object).archive(this);	            
	        }
	        else {
	            ExternalArchiver archiver = externalArchiverForClassname(object.getClass().getName());
	            
	            if (archiver == null) {
	                throw new IOException("Don't know how to archive " + object.getClass());
	            }
	            archiver.archive(object, this);
	        }
        }
        finally {
            if (statistics != null && object != null) {
                statistics.end();
            }
        }        
    }

    /**
     * Writes the object in the most robust (and expensive) format, 
     * PolymorphicReference.
     */
    public void writeObject(Object object) throws IOException
    {
        writeObject(object, Archivable.PolymorphicReference);
    }

    /**
     * Writes the elements in a list using the specified style.
     * Note the list itself is essentially written as a value, so
     * two references to the same list will result in two lists
     * on unarchiving.
     */
    public void writeList(List list, int elementStyle) throws IOException
    {
        if (list instanceof ArrayList) {
            writeList((ArrayList)list, elementStyle);
            return;
        }
        
        if (list == null) {
            writeCompactInt(-1);
            return;
        }
        
        writeCompactInt(list.size());
        Iterator i = list.iterator();        
        while (i.hasNext()) {
            writeObject(i.next(), elementStyle);
        }
    }

    /**
     * We have this for performance since we don't need to create
     * an Iterator to walk an ArrayList.
     */
    public void writeList(ArrayList list, int elementStyle) throws IOException
    {
        if (list == null) {
            writeCompactInt(-1);
            return;
        }
        writeCompactInt(list.size());
        for (int i = 0, count = list.size(); i < count; i++) {
            writeObject(list.get(i), elementStyle);            
        }
    }

    /**
     * Writes a list with elements in the most robust/expensive
     * PolymorphicReference style.
     */
    public void writeList(List list) throws IOException
    {
        writeList(list, Archivable.PolymorphicReference);
    }

    /**
     * Archivable classes should call this at the beginning of their
     * archive method (and call readClassVersion at the beginning
     * of their unarchive method).  This only adds the overhead of roughly
     * 1 byte per class per archive.  It is not a per instance cost.
     */
    public void writeClassVersion(String classname, int version) throws IOException
    {
        if (classVersions.get(classname) == null) {
            classVersions.put(classname, new Integer(version));
            writeCompactInt(version);
        }
    }

    /**
     * Writes a String that is expected to be written many times.
     * Subsequent attempts to write will only write out an id.
     */
    public void writeUniqueString(String string) throws IOException
    {
        if (string == null) {
            writeCompactInt(-1);
        }
        
        Integer id = (Integer)strings.get(string);
        
        if (id != null) {
            writeCompactInt(id.intValue());
        }
        else {
            id = new Integer(strings.size() + 1);
            strings.put(string, id);
            writeCompactInt(0);
            writeUTF(string);
        }
    }

    /**
     * Writes a "packed" int using 1, 2, 3, 4, or 5 bytes depending on
     * its size.  For ints that are likely to be less than about 2^27
     * (about 134 million), this will be no worse than calling writeInt.
     */
    public void writeCompactInt(int value) throws IOException
    {
        if (value < 0) {
            if (value == Integer.MIN_VALUE) {
                write(0x40);
                return;
            }

            value = -value;
            if (value < 0x40) {
                write(value | 0x40);
                return;
            } else {
                write((value & 0xff) | 0xc0);
            }
        } else {
            if (value < 0x40) {
                write(value);
                return;
            } else {
                write((value & 0x3f) | 0x80);
            }
        }

        value = value >>> 6;
        while (true) {
            if (value < 0x80) {
                write((int)value);
                break;
            }
            write((int)((value & 0xff) | 0x80));
            value = value >>> 7;            
        }
    }
    
    /**
     * Writes a "packed" long using from 1 to 10 bytes depending on
     * its size.  For longs that are likely to be less than about 2^55
     * (about 3.6e16), this will be no worse than calling writeLong.
     */
    public void writeCompactLong(long value) throws IOException
    {
        if (value < 0) {
            if (value == Long.MIN_VALUE) {
                write(0x40);
                return;
            }

            value = -value;
            if (value < 0x40) {
                write((int)(value | 0x40));
                return;
            } else {
                write((int)((value & 0xff) | 0xc0));
            }
        } else {
            if (value < 0x40) {
                write((int)value);
                return;
            } else {
                write((int)((value & 0x3f) | 0x80));
            }
        }

        value = value >>> 6;
        while (true) {
            if (value < 0x80) {
                write((int)value);
                break;
            }
            write((int)((value & 0xff) | 0x80));
            value = value >>> 7;            
        }
    }

    /**
     * Print the statistics to System.out.println if they are
     * being kept.  See setKeepStatistics.
     */
    public void dumpStatistics()
    {
        if (statistics != null) {
            statistics.dumpStatistics();
        }
    }
}

/**
 * Used to keep to implement an == HashMap rather than a
 * .equals HashMap.
 */
class IdentityHashKey
{
    protected Object object;
    
    public IdentityHashKey(Object object)
    {
        this.object = object;
    }
    
    public int hashCode()
    {
        return System.identityHashCode(object);
    }
    
    public boolean equals(Object other)
    {
        if (!(other instanceof IdentityHashKey)) {
            return false;
        }
        return object == ((IdentityHashKey)other).object;
    }
}

/**
 * Used to implement a reusable probe to an == HashMap
 */
class MutableIdentityHashKey extends IdentityHashKey
{
    public MutableIdentityHashKey()
    {
        super(null);
    }
    
    public void setObject(Object object)
    {
        this.object = object;
    }
    
    public void clearObject()
    {
        object = null;
    }    
}
