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
 * Created on Aug 21, 2004
 */
package com.toubassi.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * @author garrick
 */
public class ExceptionUtil
{
    /**
     * Traverse the causal chain of exceptions starting at the specified
     * throwable, returning the deepest exception that is an instanceof
     * the specified throwableClass.  null is returned if no such exception
     * is found in the chain.
     */
    public static Throwable extract(Class throwableClass, Throwable throwable)
    {
        // Find the first in the chain
        while (throwable != null && !(throwableClass.isAssignableFrom(throwable.getClass()))) {
            throwable = getCause(throwable);
        }
        
        // Find the last one matching
        if (throwable != null) {
            while (true) {
                Throwable next = getCause(throwable);
                
                if (next == null || !throwableClass.isAssignableFrom(next.getClass())) {
                    break;
                }
                throwable = next;
            }
        }
        return throwable;
    }

    /**
     * Returns the next Throwable in the causal chain.  This is morally
     * the same as calling throwable.getCause(), except in this method,
     * if getCause fails, we try getNextException, which some legacy
     * APIs implement.
     */
    private static Throwable getCause(Throwable throwable)
    {
        Throwable next = throwable.getCause();
        
        // Some shenanigans to deal with legacy apis like javax.mail.MessagingException
        if (next == null) {
            try {
                Method getter = throwable.getClass().getMethod("getNextException", null);
                
                Class returnType = getter.getReturnType();

                if (Throwable.class.isAssignableFrom(returnType)) {
                    next = (Throwable)getter.invoke(throwable, null);                        
                }
            }
            catch (NoSuchMethodException e) {
            }
            catch (SecurityException e) {
            }                
            catch (IllegalAccessException e) {                    
            }
            catch (InvocationTargetException e) {                    
            }
        }
        
        return next;
    }

}
