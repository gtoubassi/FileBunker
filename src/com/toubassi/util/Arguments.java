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
 * Created on Jul 5, 2004
 */
package com.toubassi.util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author garrick
 */
public class Arguments
{
    private String usage;
	private HashMap map;
	private ArrayList parameters;
	
	public Arguments(String args[]) throws ArgumentsException
	{
	    this(args, null);
	}
	
	public Arguments(String args[], String usage) throws ArgumentsException
	{
	    this.usage = usage;
		map = new HashMap();
		parameters = new ArrayList();
		
		for (int i = 0; i < args.length; i++) {
		    if (args[i].equals("-usage")) {
		        if (usage != null) {
		            System.out.println(usage);		            
		        }
		        else {
		            System.out.println("No usage provided.");		            
		        }
		        System.exit(0);
		    }
		    else if (args[i].startsWith("-")) {
				if (args.length == i + 1) {
					throw new ArgumentsException("Expected argument after " + args[i]);
				}
				map.put(args[i].substring(1), args[++i]);
			}
			else {
				parameters.add(args[i]);
			}
		}
	}
	
	public boolean hasFlag(String flag)
	{
		return map.containsKey(flag);
	}
	
	public String flagString(String flag) throws ArgumentsException
	{
		String value = (String)map.get(flag);
		if (value == null) {
			throw new ArgumentsException("Required argument -" + flag + " not found");
		}
		return value;
	}
	
	public String flagString(String flag, String defaultValue)
	{
		String value = (String)map.get(flag);
		return value == null ? defaultValue : value;
	}
	
	public int flagInt(String flag) throws ArgumentsException
	{
		String value = (String)map.get(flag);
		if (value == null) {
			throw new ArgumentsException("Required numeric argument -" + flag + " not found");
		}
		return Integer.parseInt(value);
	}

	public int flagInt(String flag, int defaultValue)
	{
		String value = (String)map.get(flag);
		return value == null ? defaultValue : Integer.parseInt(value);		
	}
	
	public boolean flagBoolean(String flag) throws ArgumentsException
	{
		String value = (String)map.get(flag);
		if (value == null) {
			throw new ArgumentsException("Required boolean argument -" + flag + " not found");
		}
		if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equals("1")) {
			return true;
		}
		return false;
	}
	
	public boolean flagBoolean(String flag, boolean defaultValue)
	{
		String value = (String)map.get(flag);
		if (value == null) {
			return defaultValue;
		}
		if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equals("1")) {
			return true;
		}
		return false;
	}
	
	public ArrayList parameters()
	{
		return parameters;
	}	
}
