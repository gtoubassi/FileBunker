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
 * Created on Jul 29, 2004
 */
package com.toubassi.filebunker.vault;

import com.toubassi.util.ExceptionUtil;

import java.io.File;

/**
 * VaultException is the base class for exceptions raised from within
 * the Vault backend (including Vault, FileStore, etc).
 * A VaultException can optionally have a File associated with it if the
 * exception occurred when processing a specific file (e.g. backing it up,
 * restoring it, etc).
 * 
 * @author garrick
 */
public class VaultException extends Exception
{
    private File file;
    
    public static VaultException extract(Throwable throwable)
    {
        return (VaultException)ExceptionUtil.extract(VaultException.class, throwable);
    }
    
    public VaultException()
    {
        this(null, null, null);
    }

    public VaultException(String message)
    {
        this(null, message, null);
    }

    public VaultException(Throwable cause)
    {
        this(null, null, cause);
    }

    public VaultException(String message, Throwable cause)
    {
        this(null, message, cause);
    }

    public VaultException(File file)
    {
        this(file, null, null);
    }

    public VaultException(File file, String message)
    {
        this(file, message, null);
    }

    public VaultException(File file, Throwable cause)
    {
        this(file, null, cause);
    }

    public VaultException(File file, String message, Throwable cause)
    {
        super(message, cause);
        this.file = file;
    }
    
    /**
     * Returns the file that was being processes when this exception occurred.
     * Note the file may be null.
     */
    public File file()
    {
        return file;
    }    
    
    public String getMessage()
    {
        String message = super.getMessage();
        if (file != null) {
            message += "(" + file.getPath() + ")";
        }
        return message;
    }
    
    public String getLocalizedMessage()
    {
        return super.getMessage();
    }
}
