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
 * Created on Aug 25, 2004
 */
package com.toubassi.filebunker.vault;

import com.toubassi.io.ByteCountingInputStream;
import com.toubassi.io.DESCipherInputStream;
import com.toubassi.io.DESCipherOutputStream;
import com.toubassi.io.OutputStreamFilteredInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author garrick
 */
public class FileStoreUtil
{
    public static ByteCountingInputStream backupInputStream(File file, String password, FileOperationListener listener) throws IOException
    {
		FileInputStream fileInput = new FileInputStream(file);
		FileProgressInputStream progressInput = new FileProgressInputStream(fileInput, file, listener);
		BufferedInputStream bufferedStream = new BufferedInputStream(progressInput, 2048);
		OutputStreamFilteredInputStream filteredStream = new OutputStreamFilteredInputStream(bufferedStream);
		GZIPOutputStream compressStream;

		if (password != null) {
			DESCipherOutputStream cipherStream = new DESCipherOutputStream(filteredStream.finalOutputStream(), password);
			compressStream = new GZIPOutputStream(cipherStream);		
		}
		else {
			compressStream = new GZIPOutputStream(filteredStream.finalOutputStream());		
		}
        
		filteredStream.setFilterStream(compressStream);
		return new ByteCountingInputStream(filteredStream);
    }
    
    public static InputStream restoreInputStream(InputStream input, String password) throws IOException
    {
		DESCipherInputStream cipherInput = new DESCipherInputStream(input, password);
		return new GZIPInputStream(cipherInput);			
    }
}
