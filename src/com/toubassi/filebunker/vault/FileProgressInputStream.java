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
 * Created on Aug 10, 2004
 */
package com.toubassi.filebunker.vault;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author garrick
 */
public class FileProgressInputStream extends FilterInputStream
{
    private long reportingFrequency;
    private long bytesReadSinceLastReport;
	private File file;
	private FileOperationListener listener;
	
	public FileProgressInputStream(InputStream in, File file, FileOperationListener listener)
	{
	    this(in, file, 1024, listener);
	}
	
	public void close() throws IOException
	{
	    super.close();
	    reportIfNecessary(-1);
	}

	/**
	 * Will invoke listener.fileProgress every so often as input is read.
	 * reportingFrequency indicates the minimum amount of bytes that should
	 * be read before invoking the listener.
	 */
	public FileProgressInputStream(InputStream in, File file, long reportingFrequency, FileOperationListener listener)
	{
		super(in);
		this.listener = listener;
		this.file = file;
		this.reportingFrequency = reportingFrequency;
	}
	
	public int read() throws IOException
	{
		int result = super.read();
		reportIfNecessary(result == -1 ? -1 : 1);
		return result;
	}

	public int read(byte buf[], int off, int len) throws IOException
	{
		int result = super.read(buf, off, len);
	    reportIfNecessary(result);
		return result;
	}
	
	private void reportIfNecessary(int byteCount) throws IOException
	{
	    if (listener == null) {
	        return;
	    }

	    if (byteCount > 0) {
	        bytesReadSinceLastReport += byteCount;
	    }

	    // Always report at end of file.
	    if ((byteCount == -1 && bytesReadSinceLastReport > 0) || bytesReadSinceLastReport > reportingFrequency) {

	        boolean cancel = !listener.fileProgress(file, bytesReadSinceLastReport);

	        bytesReadSinceLastReport = 0;

	        if (cancel) {
	            throw new OperationCanceledIOException();
	        }
	    }
	}
}
