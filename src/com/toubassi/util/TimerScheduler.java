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
    
    public synchronized void runExpiredTimers()
    {
        if (timers.isEmpty()) {
            return;
        }
        
        while (timers.size() > 0) {
            Timer timer = (Timer)timers.first();
            
            if (!timer.runIfExpired()) {
                break;
            }
            timers.remove(timer);
            if (timer.start()) {
                timers.add(timer);
            }
            
            /*
            Iterator i = timers.iterator();
            System.out.println("===========");
            long base = System.currentTimeMillis();
            while (i.hasNext()) {
                NewTimer t = (NewTimer)i.next();
            }
            */
            
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
    
    public void waitUntilNextExpiration(Object waitOn)
    {
        long millis = millisUntilNextExpiration();
        
        if (millis > 0) {
            try {
                waitOn.wait(millis);
            }
            catch (InterruptedException e) {
            }
        }        
    }

    /**
     * Simple test
     */
    public synchronized static void main(String args[])
    {
        TimerScheduler scheduler = new TimerScheduler();
        Timer timer1 = new Timer(1000, 5, new Runnable() { public void run() { System.out.println("1 second"); } });
        Timer timer2 = new Timer(200, 10, new Runnable() { public void run() { System.out.println(".2 seconds"); } });
        
        scheduler.start(timer1);
        scheduler.start(timer2);
        
        int count = 0;
        while (scheduler.millisUntilNextExpiration() >= 0) {
            
            scheduler.waitUntilNextExpiration(TimerScheduler.class);
            
            scheduler.runExpiredTimers();
        }
        
        System.out.println("done");
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
