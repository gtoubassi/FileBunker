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
package com.toubassi.filebunker.ui;

import com.subx.common.NotificationCenter;
import com.subx.common.NotificationListener;
import com.toubassi.filebunker.vault.Vault;
import com.toubassi.filebunker.vault.VaultException;
import com.toubassi.util.Timer;
import com.toubassi.util.TimerScheduler;

// NOTE: The locking can be tricky in the Daemon.  On the one hand it is
// used from a thread to do various things based on timers.  On the other
// hand it responds to notifications from the vault that are coming from
// the main thread.  Deadlock can occur between the two.  Specifically
// if the Daemon holds a lock (say by synchronizing on itself), when calling
// into the Vault to, say, performMaintenance.  And then say in
// handleNotification, which is called from the vault why those locks are held
// we also synchronize on ourselves.  We will deadlock due to a conflict in
// the order we acquire locks.  The daemon thread take a lock on itself, then
// tries to acquire the Vault lock by calling performMaintenance.  Meanwhile
// another thread already has the Vault lock but calls handleNotification
// which tries to acquire the daemon's lock.
//
public class Daemon implements Runnable, NotificationListener
{
    /**
     * Our regularly scheduled vault maintenance.
     */
    private static long DefaultMaintenanceMillis = 5*60*1000;
    
    /**
     * When we are notified that maintenance is required, we temporarily
     * speed up our maintenance.
     */
    private static long FrequentMaintenanceMillis = 15*1000;

    /**
     * When we update the available bytes UI, we delay for 10 seconds
     * because these updates tend to come in batches.
     */
    private static long UpdateAvailableBytesMillis = 10*1000;
    
    /**
     * When the daemon first starts, how long do we wait before
     * updating the available bytes.
     */
    private static long InitialUpdateAvailableBytesMillis = 1*1000;
    
    private Vault vault;
    private AvailableBytesStatusItem availableBytesStatusItem;

    private TimerScheduler scheduler;
    private Timer updateAvailableBytesTimer;
    private Timer frequentMaintenanceTimer;

    private int errorCount;
    
    public Daemon(Vault vault, AvailableBytesStatusItem availableBytesStatusItem)
    {
        this.vault = vault;
        this.availableBytesStatusItem = availableBytesStatusItem;
                
        scheduler = new TimerScheduler();
        
        NotificationCenter.sharedCenter().register(vault, this);
        
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    private void performMaintenance()
    {
        if (!vault.isConfigured()) {
            return;
        }
        
        try {
            vault.performMaintenance();	          
        }
        catch (VaultException e) {
            errorCount++;
            Log.log(e, "In availability update thread: " + errorCount);            
        }
    }

    private void updateAvailableBytes()
    {
        if (!vault.isConfigured()) {
            availableBytesStatusItem.clearAvailableBytes();
            return;
        }
        
        try {
	        long availableBytes = vault.availableBytes();
	        availableBytesStatusItem.updateAvailableBytes(availableBytes);
        }
        catch (VaultException e) {
            errorCount++;
            Log.log(e, "In availability update thread: " + errorCount);            
        }
    }

    public void run()
    {
        scheduler.start(new Timer(DefaultMaintenanceMillis, 0, new PerformMaintenanceRunnable()));

        updateAvailableBytesTimer = new Timer(InitialUpdateAvailableBytesMillis, 1, new UpdateAvailableBytesRunnable());
        scheduler.start(updateAvailableBytesTimer);
                
        int errorCount = 0;
        
        while (scheduler.millisUntilNextExpiration() >= 0 && errorCount < 10) {

            scheduler.waitUntilNextExpiration();
            
            // Mustn't hold locks when going in here that we may need to
            // acquire in handleNotification.  See comment at top.
            scheduler.runExpiredTimers();
        }
        
        Log.log("Daemon thread aborting after too many errors (" + errorCount + ")");
    }
    
    // handleNotification must not allocate any locks that are held by the
    // daemon thread when calling into the vault.  See comment on top.
    public void handleNotification(String notification, Object sender, Object argument)
    {
        if (Vault.AvailableBytesChangedNotification.equals(notification)) {

            // If we don't have an active timer for this, then start one.
            if (updateAvailableBytesTimer == null || !scheduler.isActive(updateAvailableBytesTimer)) {
                updateAvailableBytesTimer = new Timer(UpdateAvailableBytesMillis, 1, new UpdateAvailableBytesRunnable());
                scheduler.start(updateAvailableBytesTimer);            
            }
        }
        else if (Vault.MaintenanceNeededNotification.equals(notification)) {

            if (frequentMaintenanceTimer != null) {
                frequentMaintenanceTimer.setRepeatCount(20);
            }
            if (frequentMaintenanceTimer == null || !scheduler.isActive(frequentMaintenanceTimer)) {
                frequentMaintenanceTimer = new Timer(FrequentMaintenanceMillis, 20, new PerformMaintenanceRunnable());                
                scheduler.start(frequentMaintenanceTimer);
            }            
        }
    }
    
    
    class PerformMaintenanceRunnable implements Runnable
    {    
        public void run()
        {
            performMaintenance();
        }
    }

    class UpdateAvailableBytesRunnable implements Runnable
    {    
        public void run()
        {
            updateAvailableBytes();
        }
    }
}

