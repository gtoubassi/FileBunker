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
 * Created on Aug 13, 2004
 */
package com.toubassi.filebunker.vault;

import com.toubassi.io.DESCipherInputStream;
import com.toubassi.io.DESCipherOutputStream;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;
import com.toubassi.util.Password;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author garrick
 */
public class VaultConfiguration implements XMLSerializable
{
	private static final SimpleDateFormat dateFormat =
		new SimpleDateFormat("d MMM yyyy HH:mm:ss:S z");

	/**
     * The key under which the password is placed in the XMLSerializer/Deserializer
     * during serialization.  This key is used during deserialization to authenticate
     * the configuration.  See deserializeXML.
     */
    public static final String ConfigurationPassword = "ConfigurationPassword";

    private ArrayList passwords = new ArrayList();
    private ArrayList passwordDates = new ArrayList();
    private HashMap parameters = new HashMap();
    private HashMap secureParameters = new HashMap();

    public VaultConfiguration editableCopy()
    {
        VaultConfiguration copy = new VaultConfiguration();
        copy.passwords = (ArrayList)passwords.clone();
        copy.passwordDates = (ArrayList)passwordDates.clone();
        copy.parameters.putAll(parameters);
        copy.secureParameters.putAll(secureParameters);
        return copy;
    }

    public boolean isConfigured()
    {
        return passwords.size() > 0;
    }
    
    public String currentPassword()
    {
        if (passwords.isEmpty()) {
            return null;
        }
        return (String)passwords.get(passwords.size() - 1);
    }
    
    public void setPassword(String password)
    {
        passwords.add(password);
        passwordDates.add(new Date());
    }
    
    public String passwordForDate(Date date)
    {
        for (int i = 0, count = passwordDates.size(); i < count; i++) {
            Date aDate = (Date)passwordDates.get(i);
            
            if (date.compareTo(aDate) < 0) {
                if (i == 0) {
                    throw new RuntimeException("Could not find password for " + date);
                }
                return (String)passwords.get(i - 1);
            }
        }
        // Return the last one.
        return (String)passwords.get(passwords.size() - 1);
    }
    
    public String parameterForKey(String key)
    {
        String parameter = (String)parameters.get(key);

        if (parameter == null) {
            return (String)secureParameters.get(key);
        }
        return parameter; 
    }
    
    public String requiredParameterForKey(String key) throws NoSuchVaultConfigurationParameterException
    {
        String parameter = parameterForKey(key);
        if (parameter == null) {
            throw new NoSuchVaultConfigurationParameterException(key);
        }
        return parameter;
    }
    
    public void setParameterForKey(String key, String value, boolean secure)
    {
        if (secure) {
            parameters.remove(key);
            secureParameters.put(key, value);
        }
        else {
            secureParameters.remove(key);
            parameters.put(key, value);            
        }
    }
    
    public void removeParameterForKey(String key)
    {
        parameters.remove(key);
        secureParameters.remove(key);
    }
    
    public void setParameterForKey(String key, String value)
    {
        setParameterForKey(key, value, false);
    }
    
    public void serializeXML(XMLSerializer serializer)
    {
        serializer.push("GlobalConfiguration");

        String currentPassword = null;
        synchronized (dateFormat) {
	        if (passwords.size() > 0) {
	            int last = passwords.size() - 1;
	            currentPassword = (String)passwords.get(last);
	            serializer.write("CurrentPassword", Password.encrypt(currentPassword));
	            serializer.write("CurrentPasswordDate", dateFormat.format(passwordDates.get(last)));
	            
	            for (int i = 0; i < last; i++) {
	                String password = (String)passwords.get(i);
	                Date date = (Date)passwordDates.get(i);
	                
	                serializer.write("Password", DESCipherOutputStream.encrypt(currentPassword, password));
                    serializer.write("PasswordDate", dateFormat.format(date));
	            }
	        }
        }
        
        Iterator i = parameters.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry)i.next();
            serializer.write((String)entry.getKey(), entry.getValue());
        }
        
        new SecureParameterSerializer(secureParameters, currentPassword).serializeXML(serializer);
        
        serializer.pop();
    }
    
    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        if ("CurrentPassword".equals(container)) {
            String plainText = (String)deserializer.getUserData(ConfigurationPassword);
            if (!Password.compare(plainText, value)) {
                throw new RuntimeException(new InvalidVaultPasswordException());
            }
            passwords.add(plainText);
        }
        else if ("Password".equals(container)) {
            String currentPassword = (String)passwords.get(passwords.size() - 1);
        	String password = DESCipherInputStream.decrypt(currentPassword, value);
            passwords.add(passwords.size() - 1, password);            
        }
        else if ("CurrentPasswordDate".equals(container)) {
    	    synchronized(dateFormat) {
    	        Date date = (Date)dateFormat.parseObject(value, new ParsePosition(0));
      	        passwordDates.add(date);            
    	    }
        }
        else if ("PasswordDate".equals(container)) {
    	    synchronized(dateFormat) {
    	        Date date = (Date)dateFormat.parseObject(value, new ParsePosition(0));
      	        passwordDates.add(passwordDates.size() - 1, date);            
    	    }
        }
        else if ("SecureParameters".equals(container)) {
            if (passwords.size() > 0) {
                String password = (String)passwords.get(passwords.size() - 1);
                
                return new SecureParameterSerializer(secureParameters, password);
            }
        }
        else {
            parameters.put(container, value);
        }
        return null;
    }
}

class SecureParameterSerializer implements XMLSerializable
{
    private Map parameters;
    private String password;
    
    public SecureParameterSerializer(Map parameters, String password)
    {
        this.parameters = parameters;
        this.password = password;
        if (password != null && password.length() == 0) {
            this.password = null;
        }
    }
    
    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        if (password != null) {
            String decryptedValue = DESCipherInputStream.decrypt(password, value);
            parameters.put(container, decryptedValue);
        }
        return null;
    }

    public void serializeXML(XMLSerializer serializer)
    {
        if (password != null && parameters.size() > 0) {
            serializer.push("SecureParameters");

	        Iterator i = parameters.entrySet().iterator();
	        while (i.hasNext()) {
	            Map.Entry entry = (Map.Entry)i.next();
	            String encrypted = DESCipherOutputStream.encrypt(password, (String)entry.getValue());
	            serializer.write((String)entry.getKey(), encrypted);
	        }        
            
            serializer.pop();            
        }
    }
}
