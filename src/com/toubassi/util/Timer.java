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
package com.toubassi.util;

/**
 * @author garrick
 */
public class Timer
{
    private Runnable runnable;
    private long millis;
    private int repeatCount;
    private long expirationTime;
    
    /**
     * repeatCount of 0 means run indefinately.
     */
    public Timer(long millis, int repeatCount, Runnable runnable)
    {
        this.runnable = runnable;
        this.millis = millis;
        // Internally repeatCount 0 means this thing is done firing forever.
        // -1 means run forever.
        if (repeatCount == 0) {
            repeatCount = -1;
        }
        this.repeatCount = repeatCount;
    }
    
    public boolean start()
    {
        if (repeatCount == 0) {
            return false;
        }
        if (repeatCount > 0) {
            repeatCount--;
        }
        expirationTime = System.currentTimeMillis() + millis;        
        return true;
    }
    
    public boolean runIfExpired()
    {
        if (System.currentTimeMillis() >= expirationTime) {
            runnable.run();            
            return true;
        }
        return false;
    }
    
    public long expirationTime()
    {
        return expirationTime;
    }
}
