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
 * Created on Aug 8, 2004
 */
package com.toubassi.filebunker.vault;

import com.meterware.httpunit.WebClient;
import com.meterware.httpunit.WebClientListener;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author garrick
 */
public class TimeOutWebConversation extends WebConversation
{
    private static boolean proxyEnabled = false;
    
    private TimeOutConversationListener timeOutListener;
    
    public TimeOutWebConversation(int timeOutSeconds)
    {
        timeOutListener = new TimeOutConversationListener(timeOutSeconds);
        addClientListener(timeOutListener);
    }
    
    public boolean hasTimedOut()
    {
        return timeOutListener.hasTimedOut();
    }
    
    public void setEnableProxy(String host, int port)
    {
        if (!proxyEnabled) {
            proxyEnabled = true;

    	    TrustManager[] trustAllCerts = new TrustManager[]{
	            new X509TrustManager() {
	                public X509Certificate[] getAcceptedIssuers()
	                {
	                    return null;
	                }
	                public void checkClientTrusted(X509Certificate[] certs, String authType)
	                {
	                }
	                public void checkServerTrusted(X509Certificate[] certs, String authType)
	                {
	                }
	            }
	        };
    	        
	        // Install the all-trusting trust manager
	        try {
	            SSLContext sc = SSLContext.getInstance("SSL");
	            sc.init(null, trustAllCerts, new java.security.SecureRandom());
	            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	            // This line commented out to avoid deprecated warning 
	            // Need this to debug with Charles.exe
	            //com.sun.net.ssl.internal.www.protocol.https.HttpsURLConnectionOldImpl.setDefaultSSLSocketFactory(sc.getSocketFactory()); 
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
        }

		setProxyServer(host, port);
		System.setProperty("http.proxyHost", host);
		System.setProperty("http.proxyPort", Integer.toString(port));
		System.setProperty("https.proxyHost",host);
		System.setProperty("https.proxyPort", Integer.toString(port));
    }

}

class TimeOutConversationListener implements WebClientListener
{
    private long lastTouchedMillis = 0;
    private long timeOutMillis;
    
    public TimeOutConversationListener(int timeOutSeconds)
    {
        timeOutMillis = timeOutSeconds * 1000;
    }

    public boolean hasTimedOut()
    {
        return System.currentTimeMillis() > lastTouchedMillis + timeOutMillis;
    }
    
    public void requestSent(WebClient src, WebRequest req)
    {
        lastTouchedMillis = System.currentTimeMillis();
        /*
        try {
            System.out.println("==================\nREQUEST: " + req.getURL());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        */
    }

    public void responseReceived(WebClient src, WebResponse resp)
    {
        /*
        try {
            int len = resp.getText().length();
	        System.out.println("RESPONSE: " + resp.getURL() + "\n" + len);
	        if (len < 20000) {
	            System.out.println(resp.getText());
	        }
	    }
	    catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	    */
    }
}