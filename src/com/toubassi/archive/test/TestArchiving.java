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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author garrick
 */
public class TestArchiving
{
    public static void testDates() throws IOException
    {
        Date[] dates = new Date[1100];
        int index = 0;
        
        long millis = System.currentTimeMillis();
        
        dates[index++] = new Date();
        dates[index++] = new Date(Long.MAX_VALUE);
        dates[index++] = new Date(Long.MIN_VALUE);
        
        for (int i = -500; i < 500; i++) {
            dates[index++] = new Date(millis + i * 100);
        }

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ArchiveOutputStream out = new ArchiveOutputStream(bytesOut);
        for (int i = 0; i < index; i++) {
            out.writeObject(dates[i], Archivable.StrictlyTypedReference);
        }
        out.close();

        ArchiveInputStream in = new ArchiveInputStream(new ByteArrayInputStream(bytesOut.toByteArray()));
        for (int i = 0; i < index; i++) {
            Date d = (Date)in.readObject(Archivable.StrictlyTypedReference, Date.class);
            assert d.equals(dates[i]);
        }
        
        in.close();
    }

    public static void testCompactInts() throws IOException
    {
        int[] ints = new int[2100000];
        int index = 0;

        ints[index++] = ((int)Byte.MIN_VALUE) - 1;
        ints[index++] = Byte.MIN_VALUE;
        ints[index++] = Byte.MIN_VALUE + 1;
        ints[index++] = Byte.MAX_VALUE;
        ints[index++] = ((int)Byte.MAX_VALUE) + 1;
        ints[index++] = Byte.MAX_VALUE - 1;
        ints[index++] = ((int)Short.MIN_VALUE) - 1;
        ints[index++] = Short.MIN_VALUE;
        ints[index++] = Short.MIN_VALUE + 1;
        ints[index++] = Short.MAX_VALUE;
        ints[index++] = ((int)Short.MAX_VALUE) + 1;
        ints[index++] = Short.MAX_VALUE - 1;
        ints[index++] = Integer.MIN_VALUE;
        ints[index++] = Integer.MIN_VALUE + 1;
        ints[index++] = Integer.MAX_VALUE;
        ints[index++] = Integer.MAX_VALUE - 1;
        
        ints[index++] = (1 << 6) - 1;
        ints[index++] = (1 << 6);
        ints[index++] = (1 << 6) + 1;
        ints[index++] = (1 << 13) - 1;
        ints[index++] = (1 << 13);
        ints[index++] = (1 << 13) + 1;
        ints[index++] = (1 << 20) - 1;
        ints[index++] = (1 << 20);
        ints[index++] = (1 << 20) + 1;
        ints[index++] = (1 << 27) - 1;
        ints[index++] = (1 << 27);
        ints[index++] = (1 << 27) + 1;
        ints[index++] = (1 << 31) - 1;
        ints[index++] = (1 << 31);
        ints[index++] = (1 << 31) + 1;

        for (int i = -500000; i < 500000; i++) {
            ints[index++] = i;
        }
        
        for (int i = Integer.MAX_VALUE - 1000000 - 1; i < Integer.MAX_VALUE - 1; i++) {
            ints[index++] = i;            
        }

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ArchiveOutputStream out = new ArchiveOutputStream(bytesOut);
        for (int i = 0; i < index; i++) {
            out.writeCompactInt(ints[i]);
        }
        out.close();

        ArchiveInputStream in = new ArchiveInputStream(new ByteArrayInputStream(bytesOut.toByteArray()));
        for (int i = 0; i < index; i++) {
            int n = in.readCompactInt();
            assert (ints[i] == n);
        }
        
        in.close();
    }
    
    public static void testCompactLongs() throws IOException
    {
        long[] longs = new long[1000000];
        int index = 0;

        longs[index++] = ((long)Byte.MIN_VALUE) - 1;
        longs[index++] = Byte.MIN_VALUE;
        longs[index++] = Byte.MIN_VALUE + 1;
        longs[index++] = Byte.MAX_VALUE;
        longs[index++] = ((long)Byte.MAX_VALUE) + 1;
        longs[index++] = Byte.MAX_VALUE - 1;
        longs[index++] = ((long)Short.MIN_VALUE) - 1;
        longs[index++] = Short.MIN_VALUE;
        longs[index++] = Short.MIN_VALUE + 1;
        longs[index++] = Short.MAX_VALUE;
        longs[index++] = ((long)Short.MAX_VALUE) + 1;
        longs[index++] = Short.MAX_VALUE - 1;
        longs[index++] = ((long)Integer.MIN_VALUE) - 1;
        longs[index++] = Integer.MIN_VALUE;
        longs[index++] = Integer.MIN_VALUE + 1;
        longs[index++] = Integer.MAX_VALUE;
        longs[index++] = ((long)Integer.MAX_VALUE) + 1;
        longs[index++] = Integer.MAX_VALUE - 1;
        longs[index++] = Long.MIN_VALUE;
        longs[index++] = Long.MIN_VALUE + 1;
        longs[index++] = Long.MAX_VALUE;
        longs[index++] = Long.MAX_VALUE - 1;

        longs[index++] = (1 << 6) - 1;
        longs[index++] = (1 << 6);
        longs[index++] = (1 << 6) + 1;
        longs[index++] = (1 << 13) - 1;
        longs[index++] = (1 << 13);
        longs[index++] = (1 << 13) + 1;
        longs[index++] = (1 << 20) - 1;
        longs[index++] = (1 << 20);
        longs[index++] = (1 << 20) + 1;
        longs[index++] = (1 << 27) - 1;
        longs[index++] = (1 << 27);
        longs[index++] = (1 << 27) + 1;
        longs[index++] = (1 << 34) - 1;
        longs[index++] = (1 << 34);
        longs[index++] = (1 << 34) + 1;
        
        longs[index++] = (1 << 41) - 1;
        longs[index++] = (1 << 41);
        longs[index++] = (1 << 41) + 1;
        
        longs[index++] = (1 << 48) - 1;
        longs[index++] = (1 << 48);
        longs[index++] = (1 << 48) + 1;
        
        longs[index++] = (1 << 55) - 1;
        longs[index++] = (1 << 55);
        longs[index++] = (1 << 55) + 1;
        
        longs[index++] = (1 << 62) - 1;
        longs[index++] = (1 << 62);
        longs[index++] = (1 << 62) + 1;
        
        longs[index++] = (1 << 63) - 1;
        longs[index++] = (1 << 63);
        longs[index++] = (1 << 63) + 1;
        
        for (int i = 0; i < 99900; i++) {
            longs[index++] = i;
        }

        for (int i = -450000; i < 450000; i++) {
            longs[index++] = i;
        }

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ArchiveOutputStream out = new ArchiveOutputStream(bytesOut);
        for (int i = 0; i < index; i++) {
            out.writeCompactLong(longs[i]);
        }
        out.close();

        ArchiveInputStream in = new ArchiveInputStream(new ByteArrayInputStream(bytesOut.toByteArray()));
        for (int i = 0; i < index; i++) {
            long l = in.readCompactLong();
            if (longs[i] != l) {
                System.out.println(longs[i] + " != " + l);
                assert false;
            }
        }
        
        in.close();
    }
    
    public static void main(String[] args) throws IOException
    {
        List list = new ArrayList();
        
        Simple sOut = new Simple();      
        Simple s2Out = new Simple();
        Simple s3Out = new Simple();
        Simple s4Out = new Simple();
        list.add(new Simple());
        list.add(new Simple());
        
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ArchiveOutputStream out = new ArchiveOutputStream(bytesOut);
        out.writeObject(sOut, Archivable.PolymorphicReference);
        out.writeObject(sOut, Archivable.PolymorphicReference);
        out.writeObject(s2Out, Archivable.StrictlyTypedReference);
        out.writeObject(s2Out, Archivable.StrictlyTypedReference);
        out.writeObject(s3Out, Archivable.PolymorphicValue);
        out.writeObject(s3Out, Archivable.PolymorphicValue);
        out.writeObject(s4Out, Archivable.StrictlyTypedValue);
        out.writeObject(s4Out, Archivable.StrictlyTypedValue);
        
        out.writeObject(null, Archivable.PolymorphicReference);
        out.writeObject(null, Archivable.StrictlyTypedReference);

        out.writeList(list, Archivable.PolymorphicReference);
        
        out.close();
        
        ArchiveInputStream in = new ArchiveInputStream(new ByteArrayInputStream(bytesOut.toByteArray()));
        
        Simple sIn = (Simple)in.readObject(Archivable.PolymorphicReference, null);
        assert sIn == in.readObject(Archivable.PolymorphicReference, null);
        assert sIn.equals(sOut);

        Simple s2In = (Simple)in.readObject(Archivable.StrictlyTypedReference, Simple.class);
        assert s2In == in.readObject(Archivable.StrictlyTypedReference, Simple.class);
        assert s2In.equals(s2Out);
        
        Simple s3In = (Simple)in.readObject(Archivable.PolymorphicValue, null);
        Simple s3In2 = (Simple)in.readObject(Archivable.PolymorphicValue, null);
        assert s3In.equals(s3In2) && s3In != s3In2;
        assert s3In.equals(s3Out);        
        
        Simple s4In = (Simple)in.readObject(Archivable.StrictlyTypedValue, Simple.class);
        Simple s4In2 = (Simple)in.readObject(Archivable.StrictlyTypedValue, Simple.class);
        assert s4In.equals(s4In2) && s4In != s4In2;
        assert s4In.equals(s4Out);        

        Simple nullS = (Simple)in.readObject(Archivable.PolymorphicReference, null);
        assert nullS == null;
        assert nullS == in.readObject(Archivable.StrictlyTypedReference, null);
        
        List listIn = in.readList(Archivable.PolymorphicReference, null);
        assert list.size() == listIn.size();
        assert list.get(0).equals(listIn.get(0));
        assert list.get(1).equals(listIn.get(1));

        in.close();
        
        testCompactLongs();
        testCompactInts();
        testDates();
    }
}
