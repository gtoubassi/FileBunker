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

*/package com.toubassi.filebunker.vault;

import java.io.File;

/**
 * @author garrick
 */
public class FileNotFoundInStoreException extends VaultException
{
    public FileNotFoundInStoreException()
    {
        this(null, null, null);
    }

    public FileNotFoundInStoreException(String message)
    {
        this(null, message, null);
    }

    public FileNotFoundInStoreException(Throwable cause)
    {
        this(null, null, cause);
    }

    public FileNotFoundInStoreException(String message, Throwable cause)
    {
        this(null, message, cause);
    }

    public FileNotFoundInStoreException(File file)
    {
        this(file, null, null);
    }

    public FileNotFoundInStoreException(File file, String message)
    {
        this(file, message, null);
    }

    public FileNotFoundInStoreException(File file, Throwable cause)
    {
        this(file, null, cause);
    }

    public FileNotFoundInStoreException(File file, String message, Throwable cause)
    {
        super(file, message, cause);
    }
}
