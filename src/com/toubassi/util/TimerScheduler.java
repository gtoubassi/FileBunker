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

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author garrick
 */
public class TimerScheduler
{
    private SortedSet timers;
    
    public TimerScheduler()
    {
        timers = new TreeSet(new TimerComparator());
    }
    
    public synchronized void start(Timer timer)
    {
        // Make sure it gets resorted.
        cancel(timer);
        
        if (timer.start()) {
            timers.add(timer);
        }
        
        // If a thread is in waitUntilNextExpiration we need
        // to tickle it so it doesn't sleep past this new
        // timer's expiration.
        notifyAll();
    }
    
    public synchronized void cancel(Timer timer)
    {
        timers.remove(timer);
    }
    
    /**
     * Returns true if the timer is currently known to the scheduler,
     * meaning it will run at some point in the future.
     */
    public synchronized boolean isActive(Timer timer)
    {
        return timers.contains(timer);
    }
    
    public void runExpiredTimers()
    {
        // This loop is a little obfuscated due to our desire to NOT
        // hold the scheduler lock when calling out to client code
        // (Timer's runnable).  This is to avoid potential deadlocks.
        while (true) {
            Timer timer = null;
            
            synchronized (this) {
                if (timers.isEmpty()) {
                    break;
                }
                
                timer = (Timer)timers.first();
                if (!timer.isExpired()) {
                    // Since the collection is sorted, once we hit
                    // the first unexpired timer, we are done.
                    break;
                }
            }
            
            // It is critically important that we don't foolishly hold onto the
            // TimerScheduler lock while calling out to timer runnable's.  We don't
            // know what kind of locks that code will try to acquire, and it may
            // innocently cause deadlock.
            timer.run();
            
            synchronized (this) {
                
                // Must remove and re-add to get the timer resorted.
                timers.remove(timer);

                if (timer.start()) {
                    timers.add(timer);
                }                
            }            
        }
    }

    public synchronized long millisUntilNextExpiration()
    {
        if (timers.isEmpty()) {
            return -1;
        }
        
        long current = System.currentTimeMillis();
        Timer first = (Timer)timers.first();
        long expiration = first.expirationTime();
        if (expiration >= current) {
            return expiration - current;
        }
        return 0;
    }
    
    public synchronized void waitUntilNextExpiration()
    {
        long millis = millisUntilNextExpiration();
        
        if (millis > 0) {
            try {
                wait(millis);
            }
            catch (InterruptedException e) {
            }
        }        
    }
}

class TimerComparator implements Comparator
{
    public int compare(Object o1, Object o2)
    {
        Timer timer1 = (Timer)o1;
        Timer timer2 = (Timer)o2;
        
        int order = (int)(timer1.expirationTime() - timer2.expirationTime());
        if (order == 0 && !timer1.equals(timer2)) {
            order = timer1.hashCode() - timer2.hashCode();
        }
        return order;
    }
}
