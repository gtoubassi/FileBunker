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
 * Created on Jul 10, 2004
 */
package com.toubassi.test;

import com.meterware.httpunit.cookies.CookieListener;
import com.meterware.httpunit.cookies.CookieProperties;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebClient;
import com.meterware.httpunit.WebClientListener;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author garrick
 */
public class HTTPUnitTest
{
	private static void testYahoo() throws Exception
	{
		CookieProperties.setDomainMatchingStrict(false);
		CookieProperties.setPathMatchingStrict(false);

//		HttpUnitOptions.setScriptingEnabled(false);
		
		WebConversation wc = new WebConversation();
		wc.addClientListener(new MyClientListener());
		wc.getClientProperties().setUserAgent("Jackass");
//		wc.getClientProperties().setUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; Q312461; .NET CLR 1.1.4322)");
//		wc.getClientProperties().setUserAgent("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.4b) Gecko/20030430 Mozilla Firebird/0.6");
//		wc.setProxyServer("localhost", 8888);
		WebResponse wr = wc.getResponse( "http://mail.yahoo.com" );

		WebForm form = wr.getFormWithName("login_form");
		form.setParameter("login", "gtoubassi");
		form.setParameter("passwd", "xxx");
		wr = form.submit();

//		System.out.println(wr.getText());
	}
	
	public static void main(String[] args) throws Exception
	{
		CookieProperties.addCookieListener(new MyCookieListener());
		
//		testYahoo();

		HttpUnitOptions.setScriptingEnabled(false);
		WebConversation wc = new WebConversation();
		wc.getClientProperties().setUserAgent("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.4b) Gecko/20030430 Mozilla Firebird/0.6");
		WebResponse wr = wc.getResponse( "http://gmail.google.com" );
		
		String[] frameNames = wc.getFrameNames();
		for (int i = 0; wr.getForms().length == 0 && i < frameNames.length; i++) {
			wr = wc.getFrameContents(frameNames[i]);
		}
		WebForm loginForm = wr.getForms()[0];
		loginForm.setParameter("Email", "gtoubassi");
		loginForm.setParameter("Passwd", "xxx");
		wr = loginForm.submit();
		
		String text = wr.getText();
		Pattern pattern = Pattern.compile("var *cookieVal *= *\"([^\"]+)\"");
		Matcher matcher = pattern.matcher(text);
		
		if (!matcher.find()) {
			throw new RuntimeException("no dice");
		}
		
		String cookie = matcher.group(1);

		wc.addCookie("GV", cookie);
		
		WebLink link = wr.getLinkWith("click here to continue");
		wr = link.click();

//		System.out.println(wr.getURL());		
//		System.out.println(wr.getText());		

		
		WebRequest refreshRequest = wr.getRefreshRequest();
//System.out.println("REFRESH: " + refreshRequest.getURL());

//		wr = wc.getResponse(refreshRequest);

//		System.out.println(wr.getText());		

		wr = wc.getResponse("http://gmail.google.com/gmail?search=inbox&view=tl&start=0&init=1");

		text = wr.getText();
		pattern = Pattern.compile("D\\(\\[\"qu\",\"(\\d+) MB\",\"(\\d+) MB");
		matcher = pattern.matcher(text);
		
		if (!matcher.find()) {
			throw new RuntimeException("no dice");
		}
		
		int usedSpace = Integer.parseInt(matcher.group(1));
		int totalSpace = Integer.parseInt(matcher.group(2));
		System.out.println("" + usedSpace + " of " + totalSpace);
		
		//String queryUrl = "http://gmail.google.com/gmail?search=query&q=" + fileIdentifier + "&view=tl&start=0";
		String queryUrl = "http://gmail.google.com/gmail?search=query&q=in%3Ainbox%20FileBunker%3A&view=tl&start=0";
		
		wr = wc.getResponse(queryUrl);

		text = wr.getText();
		pattern = Pattern.compile("D\\(\\[\"ts\".*Search results for:.*,.*,(\\d+)]");
		matcher = pattern.matcher(text);
		
		if (!matcher.find()) {
			throw new RuntimeException("no dice");
		}

		int hits = Integer.parseInt(matcher.group(1));
		
		System.out.println(wr.getText());
		System.out.println("Found: " + hits);		

		pattern = Pattern.compile("D\\(\\[\"t\",\\[\"([^\"]+)\"");
		matcher = pattern.matcher(text);
		if (!matcher.find()) {
			throw new RuntimeException("no dice");
		}
		
		String attachmentId = matcher.group(1);
		String attachmentUrl = "http://gmail.google.com/gmail?view=att&disp=inlined&attid=0.1&th=" + attachmentId;
		wr = wc.getResponse(attachmentUrl);
		
		FileOutputStream out = new FileOutputStream("c:\\temp\\file.gzc");
		InputStream in = wr.getInputStream();
		byte[] buf = new byte[1024];
		int numRead;
		while ((numRead = in.read(buf)) >= 0) {
			out.write(buf, 0, numRead);
		}
		
		in.close();
		out.close();
		
		
		/*
		frameNames = wc.getFrameNames();
		for (int i = 0; i < frameNames.length; i++) {
			System.out.println("===========================");
			System.out.println(wc.getFrameContents(frameNames[i]).getText());
			java.io.FileOutputStream fout = new java.io.FileOutputStream("c:/tmp/frame" + i + ".html");
			java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(fout);
			writer.write(wc.getFrameContents(frameNames[i]).getText());
			writer.close();
		}
		/**/
	}
}

class MyClientListener implements WebClientListener
{
	public void requestSent(WebClient src, WebRequest req) 
	{
		System.out.println("REQUEST: " + req);
		System.out.println("  HEADERS: " + req.getHeaders());
	}

	public void responseReceived(WebClient src, WebResponse resp)
	{
		System.out.println("RESPONSE: " + resp.getResponseCode() + " " + resp);
		try {
			System.out.println(resp.getText());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}

class MyCookieListener implements CookieListener
{
	public void cookieRejected(java.lang.String cookieName, int reason, java.lang.String attribute)
	{
		System.out.println("REJECTING COOKIE: " + cookieName + " " + reason + " " + attribute);
	}
		
}
	
