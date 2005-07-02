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
 * Created on Aug 3, 2004
 */
package com.toubassi.filebunker.ui;

import com.subx.common.NotificationCenter;
import com.subx.common.NotificationListener;
import com.toubassi.filebunker.ui.backup.BackupController;
import com.toubassi.filebunker.ui.configuration.ConfigurationDialog;
import com.toubassi.filebunker.ui.restore.RestoreController;
import com.toubassi.filebunker.vault.GMailFileStore;
import com.toubassi.filebunker.vault.InvalidVaultPasswordException;
import com.toubassi.filebunker.vault.Vault;
import com.toubassi.filebunker.vault.VaultException;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;
import com.toubassi.jface.PasswordDialog;
import com.toubassi.util.Arguments;
import com.toubassi.util.ArgumentsException;
import com.toubassi.util.Platform;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * @author garrick
 */
public class FileBunker extends ApplicationWindow implements Window.IExceptionHandler, XMLSerializable, PasswordDialog.Authenticator, NotificationListener
{
    private File configDirectory;
    private File uiStateFile;
    private Vault vault;
    
    private BackupController backupController;
    private RestoreController restoreController;
    private AvailableBytesStatusItem availableBytesStatusItem;
    private TabFolder tabFolder;
    private TabItem backupTab;
    private TabItem restoreTab;
    private Daemon daemon;
    
    
    public FileBunker(Arguments arguments) throws VaultException, ArgumentsException
    {
        super(null);
        
        configDirectory = new File(arguments.flagString("config"));
        
        if (!configDirectory.isDirectory() && !configDirectory.mkdirs()) {
            throw new VaultException("Could not make directory " + configDirectory.getPath());
        }

        Log.setLogFileConfiguration(configDirectory, "log");
        Log.setLogToConsole(arguments.flagBoolean("logToConsole", false));

        uiStateFile = new File(configDirectory, "ui.xml");
        
        addMenuBar();
        addStatusLine();
        
        setExceptionHandler(this);
    }

    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText("FileBunker");        
    }
    
    public void create()
    {
        super.create();
        
        boolean didLoadFromFile = loadUIState();
        
        if (!didLoadFromFile) {
            getShell().setSize(800, 600);
        }
        
        backupController.didLoadUIState(didLoadFromFile);

        getShell().getDisplay().timerExec(1, new Runnable() 
        {
            public void run()
            {
                didStart();
            }            
        });
    }
    
    protected void didStart()
    {
        // By default we verify the runtime, but just in case somebody knows
        // what they are doing and wants to subvert this...
        if (!"false".equals(System.getProperty("com.toubassi.filebunker.VerifyRuntimeVersion"))) {
            verifyJavaRuntimeVersion(getShell());
        }
        
        if (Vault.needsPassword(configDirectory)) {
            if (!authenticate()) {
                close();
                return;
            }
            else {

                // Start the daemon now that we have a Vault.  In the case of
                // a new Vault, we start when it is configured.
                daemon = new Daemon(vault, availableBytesStatusItem);                
            }
        }
        else {
            try {
                vault = new Vault(configDirectory, null);
            }
            catch (VaultException e) {
                throw new RuntimeException(e);
            }
        }
        
        backupController.setVault(vault);
        restoreController.setVault(vault);

        NotificationCenter.sharedCenter().register(vault, this);

        /*
        // For testing character image authentication captcha.  Comment out the
        // call to checkInWebConversation in GMailFileStore.cleanupInbox.
        for (int i = 0; i < 100; i++) {
            System.out.println("Attempt " + i);
            try {
                vault.performMaintenance();
            }
            catch (VaultException e) {
                e.printStackTrace();
            }
        }
        */
    }
    
    /**
     * We require 1.4.2_05 or later due to our dependence on https.  Make sure
     * we are running against that VM, or a later one.
     * 
     * @param shell
     */
    private void verifyJavaRuntimeVersion(Shell shell)
    {
        String version = System.getProperty("java.runtime.version");
        
        // Skip any leading text like "Blackdown" (see
        // http://sourceforge.net/tracker/index.php?func=detail&aid=1181135&group_id=118802&atid=682209
        for (int i = 0, count = version.length(); i < count; i++) {
            char ch = version.charAt(i);
            if (Character.isDigit(ch)) {
                version = version.substring(i);
                break;
            }
        }
        
        StringTokenizer tokenizer = new StringTokenizer(version, "._-");
        boolean ok = false;

        try {
	        if (tokenizer.hasMoreTokens()) {
	            String major = tokenizer.nextToken();
	            if (Integer.parseInt(major) > 2) {
	                ok = true;
	            }
	        }
	        if (!ok && tokenizer.hasMoreTokens()) {
	            String minor = tokenizer.nextToken();
	            if (Integer.parseInt(minor) > 4) {
	                ok = true;
	            }
	        }
	        if (!ok && tokenizer.hasMoreTokens()) {
	            String patch = tokenizer.nextToken();
	            if (Integer.parseInt(patch) > 2) {
	                ok = true;
	            }
	        }
	        if (!ok && tokenizer.hasMoreTokens()) {
	            String subpatch = tokenizer.nextToken();
	            if (Integer.parseInt(subpatch) >= 5) {
	                ok = true;
	            }
	        }
        }
        catch (NumberFormatException e) {
            Log.log(e, "Exception parsing runtime version: '" + version + "'");
            throw e;
        }
        
        if (!ok) {
	        String[] buttons = new String[] {"Quit", "Continue"};
	        String message =
	            	"FileBunker requires Java Runtime version 1.4.2_05 or later.  " +
	        		"You are currently running " + version + ".  If you continue, " +
	        		"the application is unlikely to function properly.  It is " +
	        		"strongly recommended that you quit and install a more recent " +
	        		"Java Runtime.";
	        MessageDialog dialog = new MessageDialog(shell, "Error", shell.getDisplay().getSystemImage(SWT.ICON_ERROR), message, MessageDialog.ERROR, buttons, 0);
	        if (dialog.open() == 0) {
	            System.exit(0);
	        }
        }
    }

    protected Control createContents(Composite parent)
    {
        backupController = new BackupController(configDirectory, new ConfigurationAction());
        restoreController = new RestoreController(new ConfigurationAction());

        Composite contentsComposite = new Composite(parent, SWT.NONE);
        contentsComposite.setLayout(new MainContentLayout());

        restoreController.createTabControls(contentsComposite);
        
        tabFolder = new TabFolder(contentsComposite, SWT.NONE);
        tabFolder.setBounds(0, 0, 300, 300);
        FormData tabFolderFormData = new FormData();
        tabFolderFormData.top = new FormAttachment(0, 5);
        tabFolderFormData.left = new FormAttachment(0, 5);
        tabFolderFormData.bottom = new FormAttachment(100, -5);
        tabFolderFormData.right = new FormAttachment(100, -5);
        tabFolder.setLayoutData(tabFolderFormData);

        backupTab = new TabItem(tabFolder, SWT.DEFAULT);
        backupTab.setText("Backup");

        restoreTab = new TabItem(tabFolder, SWT.DEFAULT);
        restoreTab.setText("Restore");

        Composite backupComposite = new Composite(tabFolder, SWT.NONE);
        backupController.createContents(backupComposite);
        backupTab.setControl(backupComposite);

        Composite restoreComposite = new Composite(tabFolder, SWT.NONE);
        restoreController.createContents(restoreComposite);
        restoreTab.setControl(restoreComposite);

        tabFolder.addSelectionListener(new SelectionAdapter ()
        {
            public void widgetSelected(SelectionEvent e)
            {
                if (e.item == backupTab) {
                    backupController.willShow();
                    restoreController.didHide();
                }
                else if (e.item == restoreTab) {
                    restoreController.willShow();                    
                    backupController.didHide();
                }
            }
        });
        
        return tabFolder;
    }
    
    protected MenuManager createMenuManager()
    {
      MenuManager menuBar = new MenuManager("");

      // File menu
      MenuManager fileMenu = new MenuManager("&File");

      menuBar.add(fileMenu);
      
      fileMenu.add(new ConfigurationAction());      
      fileMenu.add(new Separator());
      fileMenu.add(new ExitAction(this));

      // View menu
      MenuManager viewMenu = new MenuManager("&View");
      
      menuBar.add(viewMenu);
      
      viewMenu.add(new RefreshAction());
      
      // Help menu
      MenuManager helpMenu = new MenuManager("&Help");
      
      menuBar.add(helpMenu);
      
      helpMenu.add(new ShowDocumentAction("Help", "Help.html"));
      helpMenu.add(new ShowDocumentAction("Frequently Asked Questions", "FAQ.html"));
      helpMenu.add(new Separator());
      helpMenu.add(new AboutAction());

      return menuBar;
    }

    protected StatusLineManager createStatusLineManager()
    {
        StatusLineManager statusLineManager = new StatusLineManager();
        availableBytesStatusItem = new AvailableBytesStatusItem(vault, "Available");
        statusLineManager.add(availableBytesStatusItem);
        return statusLineManager;
    }

    private void configurationClicked()
    {
        ConfigurationDialog configDialog = new ConfigurationDialog(getShell(), vault, backupController.visibleRoots());
        if (configDialog.open() == IDialogConstants.OK_ID) {
            if (daemon == null) {
                daemon = new Daemon(vault, availableBytesStatusItem);                            
            }
            
            if (Platform.isWindows()) {
                backupController.setVisibleRoots(configDialog.visibleDrives());
            }
        }
    }

    public void handleException(final Throwable throwable)
    {        
        Log.log(throwable, "Unhandled Exception");

        // If this is invoked after the app is closed, then we can't display
        // a dialog.  Make sure we still have a display before trying.
        
        Shell shell = getShell();
        if (shell == null) {
            return;
        }
        
        Display display = shell.getDisplay();
        if (display == null) {
            return;
        }
        
        display.syncExec(new Runnable() {
            
            public void run()
            {
                Throwable exception;

                // If we can dig up a VaultException, use that message.
                VaultException vaultException = VaultException.extract(throwable);
                
                if (vaultException != null) {
                    exception = vaultException;
                }
                else {
                    exception = throwable;
                }
                
                String message = exception.getLocalizedMessage();

                if (message == null) {
                    message = "An unexpected error occurred.";
                }

                MessageDialog.openError(getShell(), "Error", message);        
            }            
        });
    }
    
    public boolean close()
    {
        boolean returnValue = false;

        saveUIState();
        try {
            backupController.save();
            returnValue = super.close();
            if (vault != null) {
                vault.prepareForShutdown();
            }
        }
        catch (Exception e) {
            Log.log(e);
        }
        
        return returnValue;
    }
    
    public void handleNotification(String notification, Object sender, Object argument)
    {
        if (GMailFileStore.UserAuthenticationNotification.equals(notification)) {
            final Map map = (Map)argument;
            final URL imageUrl = (URL)map.get(GMailFileStore.ImageUrlKey);

            getShell().getDisplay().syncExec(new Runnable()
            {
                public void run()
                {
                    try {
                        ImageAuthenticationDialog dialog = new ImageAuthenticationDialog(getShell(), imageUrl);
                    
                        String text = dialog.run();
        	            if (text != null) {
        	                map.put(GMailFileStore.ImageTextKey, text);
        	            }            
                    }
                    catch (IOException e) {
                        Log.log(e);
                    }                    
                }
            });            
        }
    }
    
    public boolean authenticate(String password)
    {
        try {
    		vault = new Vault(configDirectory, password);
        }
        catch (InvalidVaultPasswordException e) {
            return false;
        }
        catch (VaultException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean authenticate()
    {
        PasswordDialog dialog = new PasswordDialog(getShell(), "Enter your FileBunker password", this);
        return dialog.open() == IDialogConstants.OK_ID;
    }
    
    private boolean loadUIState()
    {
        if (uiStateFile.exists()) {
	        try {
	            XMLDeserializer.parse(uiStateFile, this);
	            return true;
	        } catch (Exception e) {
	            Log.log(e, "While loading UI state");
	        }
        }
        return false;
    }
    
    private void saveUIState()
    {
        try {
	        XMLSerializer writer = new XMLSerializer(uiStateFile);
	
	        serializeXML(writer);
	        writer.close();
        }
        catch (IOException e) {
            Log.log(e, "While saving UI state");
        }
    }
    
	public void serializeXML(XMLSerializer writer)
	{
	    Shell shell = getShell();
	    Rectangle bounds = shell.getBounds();
	    
	    writer.push("UserInterfaceState");
	    writer.write("windowX", Integer.toString(bounds.x));
	    writer.write("windowY", Integer.toString(bounds.y));
	    writer.write("windowWidth", Integer.toString(bounds.width));
	    writer.write("windowHeight", Integer.toString(bounds.height));
	    
	    backupController.serializeXML(writer);
	    restoreController.serializeXML(writer);
	    
	    writer.pop();
	}

    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        if ("windowX".equals(container)) {
            Point origin = getShell().getLocation();
            origin.x = Integer.parseInt(value);
            getShell().setLocation(origin);
        }
        else if ("windowY".equals(container)) {
            Point origin = getShell().getLocation();
            origin.y = Integer.parseInt(value);
            getShell().setLocation(origin);
        }
        else if ("windowWidth".equals(container)) {
            Point size = getShell().getSize();
            size.x = Integer.parseInt(value);
            getShell().setSize(size);
        }
        else if ("windowHeight".equals(container)) {
            Point size = getShell().getSize();
            size.y = Integer.parseInt(value);
            getShell().setSize(size);
        }
        else if ("Backup".equals(container)) {
            return backupController;
        }
        else if ("Restore".equals(container)) {
            return restoreController;
        }
        return null;
    }
    
	public static void main(String[] args) throws ArgumentsException, VaultException
    {
		Arguments arguments = new Arguments(args);
				
        FileBunker w = new FileBunker(arguments);
        w.setBlockOnOpen(true);
        w.open();
        Display.getCurrent().dispose();
    }


	class ConfigurationAction extends Action
	{
	    public ConfigurationAction()
	    {
	        setText("Configuration");
	    }
	    
	    public void run()
	    {
	        configurationClicked();
	    }
	}

	class RefreshAction extends Action
	{
	    public RefreshAction()
	    {
	        setText("Refresh");
	        setAccelerator(Action.findKeyCode("F5"));
	    }
	    
	    public void run()
	    {
	        TabItem items[] = tabFolder.getSelection();
	        
	        for (int i = 0; i < items.length; i++) {
	            if (items[i] == backupTab) {
	                backupController.refresh();
	            }
	            else if (items[i] == restoreTab) {
	                restoreController.refresh();	                
	            }
	        }
	    }
	}

	class AboutAction extends Action
	{
	    public AboutAction()
	    {
	        setText("About");
	    }
	    
	    public void run()
	    {
	        new AboutDialog(getShell()).open();
	    }
	}
}

class MainContentLayout extends Layout
{
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache)
    {
        return composite.computeSize(wHint, hHint, flushCache);
    }

    protected void layout(Composite composite, boolean flushCache)
    {
        Point size = composite.getSize();

        Control[] children = composite.getChildren();
        
        // The first view is put in the upper right corner less a 5 pixel
        // margin on the right
        
        Control first = children[0];
        
        Point firstSize = first.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        
        first.setBounds(size.x - firstSize.x - 5, 0, firstSize.x, firstSize.y);        
        
        // The last view fills up the composite less a 5 pixel border around the
        // edges
        Control last = children[children.length - 1];
        
        last.setBounds(5, 5, size.x - 10, size.y - 10);
    }
}


