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
 * Created on Jul 23, 2004
 */
package com.toubassi.filebunker.vault;

import com.meterware.httpunit.WebResponse;

import java.io.IOException;
import java.io.InputStream;


/**
 * @author garrick
 */
public class GMailMultiPartInputStream extends InputStream
{
    private GMailFileStore store;
    private TimeOutWebConversation wc;
    private String[] messageIds;
    private InputStream currentInput;
    private int currentMessage;
    
    public GMailMultiPartInputStream(GMailFileStore store, TimeOutWebConversation wc, String[] messageIds) throws IOException
    {
        this.store = store;
        this.wc = wc;
        this.messageIds = messageIds;
        currentMessage = -1;
        try {
            nextPart();
        }
        finally {
	        store.checkInConversation(wc);
	        wc = null;
        }
    }
    
    protected void finalize()
    {
        try {
            close();
        }
        catch (IOException e) {
            // swallow
        }
    }
    
    public void close() throws IOException
    {
        if (currentInput != null) {
            currentInput.close();
            currentInput = null;
        }
        
        if (wc != null) {
	        store.checkInConversation(wc);
	        wc = null;
        }
    }
    
	public int read() throws IOException
	{
	    if (wc == null) {
	        return -1;
	    }
	    
	    int result;
	    do {
	        result = currentInput.read();
	    } while (result == -1 && nextPart());
	    return result;
	}

    public int read(byte b[], int off, int len) throws IOException
    {
	    if (wc == null) {
	        return -1;
	    }

	    int result;
	    do {
	        result = currentInput.read(b, off, len);
	    } while (result == -1 && nextPart());
	    return result;
    }
    
    private boolean nextPart() throws IOException
    {
        try {
	        if (currentMessage >= messageIds.length - 1) {
	            return false;
	        }
	        currentMessage++;
	        
			String attachmentUrl = "http://gmail.google.com/gmail?view=att&disp=inlined&attid=0.1&th=" + messageIds[currentMessage];
			WebResponse wr = wc.getResponse(attachmentUrl);
			
	        if (currentInput != null) {
	            currentInput.close();
	        }
	        currentInput = wr.getInputStream();
	        return true;
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            IOException wrapper = new IOException("Error retrieving part " + currentMessage + " of the file.");
            wrapper.initCause(e);
            throw wrapper;
        }
    }
}
