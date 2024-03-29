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

import com.meterware.httpunit.WebClient;
import com.meterware.httpunit.WebClientListener;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * @author garrick
 */
public class DebugWebClientListener implements WebClientListener
{
    public void requestSent(WebClient client, WebRequest request)
    {
        System.out.println("Request Sent=======================");
        try {
            System.out.println(request.getURL());
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        System.out.println(request.getHeaders());
        System.out.println();
    }

    public void responseReceived(WebClient client, WebResponse response)
    {
        System.out.println("Response Received=======================");
        try {
            System.out.println(response.getText());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println();
    }
}
