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
package com.toubassi.filebunker.ui.test;

import com.toubassi.filebunker.ui.Log;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * @author garrick
 */
public class LogTest
{

    public static void b()
    {
        throw new NullPointerException("hello");
    }

    public static void a()
    {
        try {
            b();
        } catch (NullPointerException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void main(String[] args)
    {
        Log.setLogFileConfiguration(new File("."), "testlog");
        Log.setMaximumFileSize(10*1024);

        for (int i = 0; i < 7000; i++) {
            Log.log("Hello there my name is garrick what is yours");
        }

        try {
            a();
        }
        catch (Exception e) {
            Log.log(new InvocationTargetException(e), "test of nested exceptions");            
        }
    }
}
