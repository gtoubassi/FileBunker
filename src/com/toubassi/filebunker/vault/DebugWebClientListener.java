/*
 * Created on Nov 12, 2004
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
