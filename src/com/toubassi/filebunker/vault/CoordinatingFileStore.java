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
 * Created on Jul 28, 2004
 */
package com.toubassi.filebunker.vault;

import com.subx.common.NotificationCenter;
import com.subx.common.NotificationListener;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;
import com.toubassi.util.ClassUtil;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * CoordinatingFileStore implements the FileStore interface by wrapping
 * one or more other FileStores.  It will delegate all operations to
 * the appropriate "sub" FileStore.
 * 
 * Once loaded, this class is thread safe.
 * 
 * @author garrick
 */
public class CoordinatingFileStore implements FileStore, NotificationListener
{
    // For debugging
    private static boolean useNoopFileStore = false;

    private static HashMap registeredFileStoreClasses;
    
    private VaultConfiguration vaultConfig;
    private ArrayList stores;

    public synchronized static void registerFileStore(Class fileStoreClass)
    {
        if (registeredFileStoreClasses == null) {
            registeredFileStoreClasses = new HashMap();
        }
        String name = ClassUtil.nameWithoutPackage(fileStoreClass);

        registeredFileStoreClasses.put(name, fileStoreClass);
    }
    
    public static FileStore createFileStore(String name)
    {
        if (useNoopFileStore) {
            return new NoopFileStore();
        }
        
        Class fileStoreClass = (Class)registeredFileStoreClasses.get(name);
        if (fileStoreClass != null) {
            try {
                return (FileStore)fileStoreClass.newInstance();            
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
    
    static {
        registerFileStore(GMailFileStore.class);
        registerFileStore(LocalDiskFileStore.class);
    }
    
    public CoordinatingFileStore()
    {
        stores = new ArrayList();
    }

    public boolean isConfigured()
    {
        if (stores.isEmpty()) {
            return false;
        }
        
        for (int i = 0, count = stores.size(); i < count; i++) {
            FileStore store = (FileStore)stores.get(i);
            
            if (!store.isConfigured()) {
                return false;
            }
        }
        
        return true;
    }

    public String name()
    {
        throw new RuntimeException("CoordinatingFileStores do not have a name");
    }
    
    public FileStore editableCopy()
    {
        CoordinatingFileStore copy = new CoordinatingFileStore();

        for (int i = 0, count = stores.size(); i < count; i++) {
            FileStore store = (FileStore)stores.get(i);
            
            copy.addStore(store.editableCopy());
        }
        
        return copy;
    }
    
    ArrayList stores()
    {
        return stores;
    }
    
    private void addStore(FileStore newStore)
    {
        for (int i = 0, count = stores.size(); i < count; i++) {
            FileStore store = (FileStore)stores.get(i);
            
            if (store.name().equals(newStore.name())) {
                throw new IllegalArgumentException("Two stores cannot have the same name");
            }
        }
        stores.add(newStore);
        NotificationCenter.sharedCenter().register(newStore, this);        
    }
    
    private void removeStore(FileStore store)
    {
        NotificationCenter.sharedCenter().unregister(store, this);        
        stores.remove(store);
    }
    
    private void removeAllStores()
    {
        for (int i = stores.size() - 1; i >= 0; i--) {
            removeStore((FileStore)stores.get(i));
        }
    }
    
    private void addStores(List otherStores)
    {
        for (int i = 0, count = otherStores.size(); i < count; i++) {
            FileStore store = (FileStore)otherStores.get(i);
            addStore(store);
        }
    }
    
    /**
     * Returns a list of stores that were removed as a part of this
     * update.  Stores are identified by their name(), and any
     * stores that are not mentioned (by name) in the newStores list
     * are considered removed.
     */
    ArrayList updateStores(List newStores)
    {
        ArrayList deletedStores = new ArrayList();

        for (int i = 0, count = stores.size(); i < count; i++) {
            FileStore store = (FileStore)stores.get(i);
            boolean wasDeleted = true;
            
            for (int j = 0, jcount = newStores.size(); j < jcount; j++) {
                FileStore newStore = (FileStore)newStores.get(j);
                
                if (newStore.name().equals(store.name())) {
                    wasDeleted = false;
                    break;
                }
            }
            
            if (wasDeleted) {
                deletedStores.add(store);
            }
        }
        
        removeAllStores();
        addStores(newStores);

        NotificationCenter.sharedCenter().post(AvailableBytesChangedNotification, this, null);
        
        return deletedStores;
    }
    
    public FileStore storeForRevision(RevisionIdentifier identifier)
    {
        for (int i = 0, count = stores.size(); i < count; i++) {
            FileStore store = (FileStore)stores.get(i);
            
            if (store.canHandleRevision(identifier)) {
                return store;
            }
        }
        return null;
    }

    public void setConfiguration(VaultConfiguration configuration)
    {
        vaultConfig = configuration;
        for (int i = 0, count = stores.size(); i < count; i++) {
            FileStore store = (FileStore)stores.get(i);

            store.setConfiguration(configuration);
        }          
    }

    public boolean canHandleRevision(RevisionIdentifier identifier)
    {
        return storeForRevision(identifier) != null;
    }

    private FileStore storeThatCanBackupFile(File file) throws VaultException
    {
        for (int i = 0, count = stores.size(); i < count; i++) {
            FileStore store = (FileStore)stores.get(i);

            if (store.canBackupFile(file)) {
                return store;
            }
        }          
        return null;
    }

    public boolean canBackupFile(File file) throws VaultException
    {
        return storeThatCanBackupFile(file) != null;
    }

	public void backupFile(File file, String name, RevisionIdentifier identifier, FileOperationListener listener) throws VaultException
    {
        FileStore store = storeThatCanBackupFile(file);
        
        if (store != null) {
            store.backupFile(file, name, identifier, listener);
            return;
        }

        throw new OutOfSpaceException(file);
    }

    public InputStream restoreFile(RevisionIdentifier identifier, Date date) throws VaultException
    {
        FileStore store = storeForRevision(identifier);
        if (store == null) {
            throw new UnknownFileStoreException();
        }
        
        return store.restoreFile(identifier, date);
    }

    public void deleteFile(RevisionIdentifier identifier, long backedupSize) throws VaultException
    {
        FileStore store = storeForRevision(identifier);
        if (store == null) {
            throw new UnknownFileStoreException();
        }
        
        store.deleteFile(identifier, backedupSize);
    }

    public synchronized long availableBytes() throws VaultException
    {
        long total = 0;

        for (int i = 0, count = stores.size(); i < count; i++) {
            FileStore store = (FileStore)stores.get(i);
            
            total += store.availableBytes();
        }
        
        return total;
    }

    public void performMaintenance() throws VaultException
    {
        for (int i = 0, count = stores.size(); i < count; i++) {
            FileStore store = (FileStore)stores.get(i);
            
            store.performMaintenance();
        }
    }
    
	public void prepareForShutdown() throws VaultException
	{
        for (int i = 0, count = stores.size(); i < count; i++) {
            FileStore store = (FileStore)stores.get(i);
            
            store.prepareForShutdown();
        }	    
	}

    public void handleNotification(String notification, Object sender, Object argument)
    {
        // Relay notifications from internal stores.
        if (stores.contains(sender)) {
            NotificationCenter.sharedCenter().post(notification, this, argument);
        }
    }

    public void serializeXML(XMLSerializer serializer)
	{
	    serializer.push("FileStores");
	    for (int i = 0; i < stores.size(); i++) {
	        FileStore store = (FileStore)stores.get(i);
	        store.serializeXML(serializer);
	    }
	    serializer.pop();
	}
	
    public XMLSerializable deserializeXML(XMLDeserializer deserializer, String container, String value)
    {
        FileStore store = createFileStore(container);
        store.setConfiguration(vaultConfig);
        addStore(store);
        
        return store;
    }
}
