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
 * Created on Aug 20, 2004
 */
package com.toubassi.filebunker.ui;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * @author garrick
 */
public class LabelUtil
{
    public static final SimpleDateFormat shortDateTimeSecondsFormat = new SimpleDateFormat("M/d/yy h:mm:ss a");
    private static final DecimalFormat memoryFormat = new DecimalFormat("#,##0");
    private static final DecimalFormat timePartFormat = new DecimalFormat("00");

    public synchronized static String formatMemorySize(long size)
    {
        long sizeInKB;
        
        if (size == 0) {
            sizeInKB = 0;
        }
        else {
            sizeInKB = size / 1024;
            if (size % 1024 != 0) {
                sizeInKB++;
            }
            
            // If size > 10MB, then go to MB as the unit
            if (sizeInKB > 10*1024) {
                long sizeInMB = size  / (1024*1024);
                return "" + memoryFormat.format(sizeInMB) + " MB";
            }
        }
        return "" + memoryFormat.format(sizeInKB) + " KB";        
    }
    
    public static String formatHours(long millis)
    {
        long hours = millis / (3600*1000);
        long leftover = millis - hours * (3600*1000);
        long minutes = leftover / (60*1000);
        leftover -= minutes * (60*1000);
        long seconds = leftover / 1000;
        
        
        if (hours > 0) {
            return "" + hours + ":" + timePartFormat.format(minutes) + ":" + timePartFormat.format(seconds);
        }
        if (minutes > 0) {
            return minutes + ":" + timePartFormat.format(seconds);
        }
        if (seconds == 0) {
            return "less than 1 second";
        }
        if (seconds == 1) {
            return "1 second";
        }
        return "" + seconds + " seconds";
    }
    
    public static String ellidedPath(String path)
    {
        // Could use String.split but that wouldn't quite do what
        // I want visa-vis c:/foo vs /user/foo or /foo/bar vs /foo/bar/
        StringTokenizer tokenizer = new StringTokenizer(path, File.separator);
        ArrayList parts = new ArrayList();
        while (tokenizer.hasMoreTokens()) {
            parts.add(tokenizer.nextToken());
        }
        if (parts.size() < 6) {
            return path;
        }
        
        StringBuffer buffer = new StringBuffer();
        buffer.append(parts.get(0));
        buffer.append(File.separator);
        buffer.append(parts.get(1));
        buffer.append(File.separator);
        buffer.append("...");
        buffer.append(File.separator);
        buffer.append(parts.get(parts.size() - 2));
        buffer.append(File.separator);
        buffer.append(parts.get(parts.size() - 1));
        return buffer.toString();
    }
    
    public static void main(String args[])
    {
        System.out.println(formatHours(500));
        System.out.println(formatHours(1000));
        System.out.println(formatHours(1001));
        System.out.println(formatHours(1000*64));
        System.out.println(formatHours(1000*(8*3600 + 27)));
        System.out.println(formatHours(1000*(8*3600 + 4*60 + 27)));
        System.out.println(formatHours(1000*(8*3600 + 15*60 + 27)));
        
        System.out.println(formatMemorySize(0));
        System.out.println(formatMemorySize(1));
        System.out.println(formatMemorySize(1025));
        System.out.println(formatMemorySize(1024*10));
        System.out.println(formatMemorySize(1024*100));
        System.out.println(formatMemorySize(1024*1024));
        System.out.println(formatMemorySize(1024*9543));
        System.out.println(formatMemorySize(1024*11*1024));
        System.out.println(formatMemorySize(1024*11*1024*500));
    }
}
