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
package com.toubassi.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author garrick
 */
public class ByteCountingOutputStream extends FilterOutputStream
{
    private long count;

    /**
     * @param out
     */
    public ByteCountingOutputStream(OutputStream out)
    {
        super(out);
    }
    
    public void clear()
    {
        count = 0;
    }
    
    public long byteCount()
    {
        return count;
    }

    public void write(int b) throws IOException
    {
        super.write(b);
        count++;
    }

    public void write(byte b[], int off, int len) throws IOException
    {
        out.write(b, off, len);
        count += len;
    }
}
