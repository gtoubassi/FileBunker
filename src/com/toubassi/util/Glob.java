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
package com.toubassi.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Supports matching glob expressions.  The only special characters that
 * are handled are '*' (i.e. '.*' in regex) and '?' (i.e. '.' in regex).
 * @author garrick
 */
public class Glob
{
    private static final String SpecialCharacters = ".\\[]()";

    private String globExpression;
    private Matcher matcher;
    
    public Glob(String globExpression)
    {
        setGlobExpression(globExpression);
    }
    
    /** Private because Glob is immutable.  */
    private void setGlobExpression(String globExpression)
    {
        this.globExpression = globExpression;
        matcher = compile(globExpression).matcher("");
    }
    
    public boolean matches(String string)
    {
        matcher.reset(string);
        return matcher.matches();
    }
    
    public String globExpression()
    {
        return globExpression;
    }
    
    private static Pattern compile(String globExpression)
    {
        StringBuffer buffer = new StringBuffer();
        
        for (int i = 0, count = globExpression.length(); i < count; i++) {
            char ch = globExpression.charAt(i);
            
            if (SpecialCharacters.indexOf(ch) != -1) {
                buffer.append('\\');                
                buffer.append(ch);
            }
            else if (ch == '*') {
                buffer.append(".*");
            }
            else if (ch == '?') {
                buffer.append('.');
            }
            else {
                buffer.append(ch);
            }
        }
        
        return Pattern.compile(buffer.toString());        
    }
}
