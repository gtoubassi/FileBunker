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
package com.toubassi.filebunker.vault;

import java.io.File;

/**
 * UncancelableFileOperationListener is a "wrapper" listener that will pass
 * on all methods to the underlying listener provided to the constructor, but
 * will not pass back any desire to cancel that is indicated by the return
 * values from the underlying listener.
 * @author garrick
 */
public class UncancelableFileOperationListener implements FileOperationListener
{
    private FileOperationListener listener;
    
    public UncancelableFileOperationListener(FileOperationListener listener)
    {
        this.listener = listener;
    }
    
    public FileOperationListener listener()
    {
        return listener;
    }

    public boolean fileProgress(File file, long bytesProcessed)
    {
        listener.fileProgress(file, bytesProcessed);
        return true;
    }

    public boolean willProcessFile(File file)
    {
        listener.willProcessFile(file);
        return true;
    }
}
