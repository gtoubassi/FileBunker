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
 * Created on Aug 11, 2004
 */
package com.toubassi.filebunker.ui;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very simple logging facility.  This is in the ui package because it is
 * a temporary workaround.  We should move to java.util.logging
 * @author garrick
 */
public class Log
{
    private static boolean logToConsole = false;

    static final Pattern pattern = Pattern.compile(".*-(\\d+)\\.txt");
    private static final int MaxNumberOfFiles = 5;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");

    private static int maxLength = 1024*1024;
    private static File logFileDirectory;
    private static String logFileBaseName;
    private static File logFile;
    private static PrintWriter out; 
    
    public static synchronized void setLogFileConfiguration(File directory, String basename)
    {
        logFileDirectory = directory;
        logFileBaseName = basename;
        rotateLogFiles();
    }
    
    public static synchronized void setLogToConsole(boolean flag)
    {
        logToConsole = flag;
    }
    
    public static synchronized void setMaximumFileSize(int maxSize)
    {
        maxLength = maxSize;
    }
    
    public static synchronized void log(String message)
    {
        log(null, message);
    }
    
    public static synchronized void log(Throwable throwable)
    {
        log(throwable, null);
    }
    
    public static synchronized void log(Throwable throwable, String message)
    {
        if (logToConsole) {
            if (message != null) {
                System.out.println(message);
            }
            if (throwable != null) {
                throwable.printStackTrace();                
            }
            System.out.flush();
        }
        
        try {
            PrintWriter out = new PrintWriter(new FileWriter(logFile, true));

            Date date = new Date();
            out.print(dateFormat.format(date));
            out.print(": ");
            out.println(message);
            if (throwable != null) {
                throwable.printStackTrace(out);
            }
            out.close();            

            if (logFile.length() > maxLength) {
                rotateLogFiles();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }        
    }
    
    private static synchronized void rotateLogFiles()
    {
        File files[] = logFileDirectory.listFiles(new LogFileFilter(logFileBaseName));
        Arrays.sort(files, new LogFileComparator(logFileBaseName));

        if (files.length > MaxNumberOfFiles) {
            for (int i = 0; i < files.length - MaxNumberOfFiles; i++) {
                files[i].delete();
            }
        }
        
        int id = 1;
        if (files.length > 0) {
            Matcher matcher = pattern.matcher(files[files.length - 1].getPath());
            if (matcher.matches()) {
                id = Integer.parseInt(matcher.group(1));
                
                if (files[files.length - 1].length() > maxLength) {
                    id++;
                }
            }
        }
        logFile = new File(logFileDirectory, logFileBaseName + "-" + id + ".txt");
    }    
}

class LogFileComparator implements Comparator
{
    private String basename;
    private Matcher matcher;
    
    public LogFileComparator(String basename)
    {
        this.basename = basename;
        matcher = Log.pattern.matcher("");
    }

    public int compare(Object o1, Object o2)
    {
        File file1 = (File)o1;
        File file2 = (File)o2;

        matcher.reset(file1.getPath());
        if (matcher.matches()) {
            int file1Id = Integer.parseInt(matcher.group(1));
            
            matcher.reset(file2.getPath());
            if (matcher.matches()) {
                int file2Id = Integer.parseInt(matcher.group(1));
                
                return file1Id - file2Id;
            }
        }
        
        return 0;
    }
}

class LogFileFilter implements FileFilter
{
    private String basename;
    private Matcher matcher;
    
    public LogFileFilter(String basename)
    {
        this.basename = basename;
        matcher = Log.pattern.matcher("");
    }
    
    public boolean accept(File pathname)
    {
        matcher.reset(pathname.getPath());
        return matcher.matches();
    }
}
