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

package com.toubassi.filebunker.vault;

import com.subx.common.NotificationCenter;
import com.subx.common.NotificationListener;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;
import com.toubassi.util.FileFindDelegate;
import com.toubassi.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

/**
 * This is the main class that implements the backend for FileBunker. All backup,
 * restore, and maintenance operations are provided here.
 * 
 * This class is thread safe.
 * 
 * @author garrick
 */
public class Vault implements NotificationListener, XMLSerializable
{
    /**
     * Posted when the availableBytes of this Vault changes.
     */
    public static final String AvailableBytesChangedNotification = FileStore.AvailableBytesChangedNotification;

    /**
     * Posted when the the Vault would like maintenance (via performMaintenance)
     * to occur at some point in the near future. It is really just a hint.
     */
    public static final String MaintenanceNeededNotification = FileStore.MaintenanceNeededNotification;

    /**
     * Posted when files are added or deleted from the vault.
     */
    public static final String ContentsChangedNotification = BackupDatabase.ContentsChangedNotification;

    private File configDirectory;
    private File configFile;
    private VaultConfiguration vaultConfig;
    private CoordinatingFileStore store;
    private BackupDatabase backupdb;

    
    public static boolean needsPassword(File configDirectory)
    {
        return (new File(configDirectory, "config.xml")).exists();
    }

    public Vault(File configDir, String password) throws VaultException
    {
        try {
            // Load the FileStore(s)
            configDirectory = configDir.getCanonicalFile();
            configFile = new File(configDirectory, "config.xml");

            vaultConfig = new VaultConfiguration();
            store = new CoordinatingFileStore();
            store.setConfiguration(vaultConfig);

            if (configFile.exists()) {
                load(password);
            }

            NotificationCenter.sharedCenter().register(store, this);

            // Create the index
            backupdb = new BackupDatabase(configDirectory);

            NotificationCenter.sharedCenter().register(backupdb, this);
        }
        catch (VaultException e) {
            throw e;
        }
        catch (Exception e) {
            throw new VaultException("Could not read vault configuration "
                    + configDirectory, e);
        }
    }

    public VaultConfiguration configuration()
    {
        return vaultConfig;
    }
    
    public boolean isConfigured()
    {
        return vaultConfig.isConfigured() && store.isConfigured();
    }

    public VaultConfiguration getEditableContents(List stores)
    {
        VaultConfiguration configCopy = vaultConfig.editableCopy();
        CoordinatingFileStore storeCopy = (CoordinatingFileStore) store
                .editableCopy();
        storeCopy.setConfiguration(configCopy);
        stores.clear();
        stores.addAll(storeCopy.stores());
        return configCopy;
    }

    public void updateContents(VaultConfiguration newVaultConfig, List stores)
            throws VaultException
    {
        vaultConfig = newVaultConfig;
        ArrayList deletedStores = store.updateStores(stores);
        store.setConfiguration(vaultConfig);

        try {
            save();

            // Files backed up to deleted stores must be cleaned up from the
            // backupdb.
            for (int i = 0, count = deletedStores.size(); i < count; i++) {
                FileStore store = (FileStore) deletedStores.get(i);

                backupdb.removeRevisionsWithHandlerName(store.name());
            }

            backupdb.saveIfNecessary();
        } catch (IOException e) {
            throw new VaultException("Couldn't save backup database", e);
        }

    }

	public void prepareForShutdown() throws VaultException
	{
	    store.prepareForShutdown();
	}

    public boolean isEmpty()
    {
        return backupdb.isEmpty();
    }

    public Node root()
    {
        return backupdb.root();
    }
    
    public void writeDatabaseXML(OutputStream out) throws IOException
    {
        backupdb.saveXML(out);
    }

    public Revision findRevision(File file, Date date)
    {
        return backupdb.findRevision(file, date);
    }

    public boolean doesStoreContainBackupFiles(FileStore store)
    {
        return backupdb.findRevisionWithHandlerName(store.name()) != null;
    }

    public BackupEstimate estimateBackup(BackupSpecification spec,
            FileOperationListener listener) throws VaultException
    {
        BackupEstimate estimate = new BackupEstimate(null, backupdb);

        spec.find(backupdb, estimate, listener);

        return estimate;
    }

    private FileRevision backupFile(File file, Date date,
            FileOperationListener listener) throws VaultException
    {
        try {
            file = file.getCanonicalFile();
            
            RevisionIdentifier identifier;
            FileDigest digest = new FileDigest(file);

            // See if there is an existing file in the backup repository
            // that has the same content.
            FileRevision existingRevision = backupdb.fileRevisionWithDigest(digest);
            if (existingRevision != null) {
                identifier = existingRevision.identifier();
            }
            else {
                identifier = new RevisionIdentifier(digest, file.length());
                store.backupFile(file, null, identifier, listener);
            }

            return backupdb.recordRevision(file, date, identifier);                
        }
        catch (IOException e) {
            throw new VaultException(file, e);
        }
    }
    
    private void updateDirectoryContents(File directory, Date date, String children[])
    	throws VaultException
    {
        try {
            backupdb.updateDirectoryMembership(directory, date, children);
        }
        catch (IOException e) {
            throw new VaultException(e);
        }
    }

    public void backup(BackupSpecification spec,
            BackupOperationListener listener, BackupResult result)
            throws VaultException
    {
        Date date = new Date();
        BackupFileFindDelegate findDelegate = new BackupFileFindDelegate(
                listener, date, result);

        boolean exceptionThrown = true;
        try {
            spec.find(backupdb, findDelegate);

            if (listener != null) {
                listener.finishedUserFiles();
            }
            
            // If we got here, no exception was thrown.
            exceptionThrown = false;
        }
        catch (RuntimeException e) {
            
            // Unwrap the VaultException since the BackupFileFindDelegate may wrap
            // a VaultException in a RuntimeException in order to percolate it up.
            
            VaultException vaultException = VaultException.extract(e);
            if (vaultException != null) {
                throw vaultException;
            } else {
                throw e;
            }            
        }
        finally {
            try {
                if (backupdb.saveIfNecessary()) {

                    if (listener != null) {
                        // Note we don't let the listener cancel out of this one.
                        listener.willProcessFile(backupdb.file());
                    }
                    
                    FileDigest digest = new FileDigest(backupdb.file());
                    long size = backupdb.file().length();
                    RevisionIdentifier identifier = new RevisionIdentifier(digest, size);
                    store.backupFile(backupdb.file(), "BackupIndex", identifier, listener);
                }
            }
            catch (VaultException e) {
                // Don't mask existing exception that took place in try/catch block
                if (!exceptionThrown) {
                    throw e;
                }
            }
            catch (Exception e) {
                // Don't mask existing exception that took place in try/catch block
                if (!exceptionThrown) {
                    throw new VaultException("Couldn't save backup database", e);
                }
            }
        }
    }

    /**
     * Restores the file associated with the specified revision as
     * restoreTarget. If overwrite is true, then any existing file at
     * restoreTarget is overwritten. Otherwise the file is uniqued (e.g.
     * File.doc -> File-restore.doc). Note Nonexistent directories specified in
     * restoreTarget will be made.
     */
    private void restoreFile(FileRevision revision, File restoreTarget,
            boolean overwrite, FileOperationListener listener,
            RestoreResult result) throws VaultException
    {
        try {
            // Skip the configuration files.
            if (isHidden(revision.node().file())) {
                return;
            }

            // We need to uniquify the target file if we are not overwriting files, or,
            // even if we are overwriting, the restoreTarget already exists as a directory.
            // This can happen if a restore is restoring a file "foo" and a file "foo/bar"
            // (meaning foo existed as a directory and file at different times.
            if (!overwrite || restoreTarget.isDirectory()) {
                restoreTarget = uniquifyRestoreTarget(restoreTarget);
            }

            File file = revision.node().file();

            if (listener != null && !listener.willProcessFile(file)) {
                throw new OperationCanceledVaultException();
            }

            File parent = restoreTarget.getParentFile();            
            if (!parent.isDirectory() && !parent.mkdirs()) {
                parent = uniquifyDirectory(parent);
                restoreTarget = new File(parent, restoreTarget.getName());
                
                if (!parent.mkdirs()) {
                    throw new VaultException("Cannot make directory " + parent.getPath());
                }
            }

            InputStream input = store.restoreFile(revision.identifier(), revision.date());
            
            if (input == null) {
                throw new FileNotFoundInStoreException(file);
            }
            
            FileProgressInputStream progressInput = new FileProgressInputStream(
                    input, file, listener);
            FileOutputStream output = new FileOutputStream(restoreTarget);

            try {
                byte[] buf = new byte[2048];
                int numRead;
                while ((numRead = progressInput.read(buf)) >= 0) {
                    output.write(buf, 0, numRead);
                }

                result.add(revision);
            } finally {
                output.close();
                progressInput.close();
            }
        } catch (OperationCanceledIOException e) {
            // Since the operation was canceled, delete the partially restored
            // file.
            restoreTarget.delete();
            throw new OperationCanceledVaultException();
        } catch (IOException e) {
            throw new VaultException(revision.node().file(), e);
        }
    }

    /**
     * Restores the files defined by spec under restoreRoot.
     */
    public void restore(RestoreSpecification spec, File restoreRoot,
            boolean overwrite, FileOperationListener listener,
            RestoreResult result) throws VaultException
    {
        Iterator i = spec.fileRevisions(this);

        while (i.hasNext()) {
            FileRevision revision = (FileRevision) i.next();

            File restorePath = spec.restorePathForRevision(revision,
                    restoreRoot);
            
            restoreFile(revision, restorePath, overwrite, listener, result);
        }
    }

    public RestoreEstimate estimateRestore(RestoreSpecification spec)
    {
        RestoreEstimate estimate = new RestoreEstimate();
        Iterator i = spec.fileRevisions(this);

        while (i.hasNext()) {
            FileRevision revision = (FileRevision) i.next();

            estimate.addFileRevision(revision);
        }

        return estimate;
    }

    public static File uniquifyRestoreTarget(File file)
    {
        return uniquifyFile(file, "restore");
    }
    
    public static File uniquifyFile(File file, String uniquingExtension)
    {
        if (!file.exists()) {
            return file;
        }

        String parent = file.getParent();
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        String basename, extension;

        if (lastDot == -1 || lastDot == 0) {
            basename = name;
            extension = "";
        } else {
            basename = name.substring(0, lastDot);
            extension = name.substring(lastDot);
        }

        int i = 1;
        do {
            if (i == 1) {
                file = new File(parent, basename + "-" + uniquingExtension + extension);
            } else {
                file = new File(parent, basename + "-" + uniquingExtension + i + extension);
            }

            i++;

        } while (file.exists());

        return file;
    }

    private static File uniquifyDirectory(File directory)
    {
        File parent = directory.getParentFile();
        
        if (parent != null) {
            parent = uniquifyDirectory(parent);
            directory = new File(parent, directory.getName());
        }        
        
        if (directory.isFile()) {
            directory = uniquifyFile(directory, "folder");
        }
        
        return directory;        
    }

    /**
     * Returns true if any data was removed from the store.  If multiple
     * revisions are sharing the same data in the store (2 files with the
     * same content) then this may amount to an "unlink" of a file with
     * multiple hard links.
     */
    private boolean deleteRevision(FileRevision revision) throws VaultException,
            IOException
    {
        if (backupdb.removeRevision(revision)) {
            store.deleteFile(revision.identifier(), revision.backedupSize());
            return true;
        }
        return false;
    }

    private void performDelete(File file, Date date) throws VaultException,
            IOException
    {
        file = file.getCanonicalFile();

        // Skip configuration files.
        if (isHidden(file)) {
            return;
        }

        Revision revision = backupdb.findRevision(file, date);
        if (revision.isDirectory()) {
            DirectoryRevision directoryRevision = (DirectoryRevision) revision;

            Iterator i = directoryRevision.children();
            while (i.hasNext()) {
                Node child = (Node) i.next();
                File childFile = child.file();
                performDelete(childFile, date);
            }
        } else {
            FileRevision fileRevision = (FileRevision) revision;

            deleteRevision(fileRevision);
        }
    }

    public void delete(File file, Date date) throws VaultException
    {
        try {
            try {
                performDelete(file, date);
            } finally {
                backupdb.saveIfNecessary();
            }
        } catch (IOException e) {
            throw new VaultException(file, e);
        }
    }

    public boolean needsBackup(File file)
    {
        try {
            return backupdb.needsBackup(file.getCanonicalFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to canonicalize " + file, e);
        }
    }

    public void performMaintenance() throws VaultException
    {
        store.performMaintenance();
    }

    public long recoverBytes(long bytes, FileOperationListener listener)
            throws VaultException
    {
        try {
            SortedSet set = backupdb.findLargestNodes(-1);
            long recoveredBytes = 0;

            while (recoveredBytes < bytes && set.size() > 0) {
                Node largest = (Node) set.first();

                // Its important that we remove it and re-add it (later) so that
                // the
                // sorting is recomputed.
                set.remove(largest);

                List revisions = largest.revisions();
                FileRevision oldestFileRevision = null;
                int numberOfFileRevisions = 0;

                for (int i = 0, count = revisions.size(); i < count; i++) {
                    Revision revision = (Revision) revisions.get(i);

                    if (!revision.isDirectory()) {
                        if (oldestFileRevision == null) {
                            oldestFileRevision = (FileRevision) revision;
                        }
                        numberOfFileRevisions++;
                        if (numberOfFileRevisions > 1) {
                            break;
                        }
                    }
                }

                if (numberOfFileRevisions < 2) {
                    // Don't add back in. This file has too few revisions to be
                    // pruned.
                    continue;
                }

                if (listener != null
                        && !listener.willProcessFile(largest.file())) {
                    throw new OperationCanceledVaultException();
                }

                deleteRevision(oldestFileRevision);
                recoveredBytes += oldestFileRevision.backedupSize();

                if (listener != null
                        && !listener.fileProgress(largest.file(),
                                oldestFileRevision.backedupSize())) {
                    throw new OperationCanceledVaultException();
                }

                // Add back this node in sorted order as it may still be the
                // largest.
                set.add(largest);
            }

            backupdb.saveIfNecessary();

            return recoveredBytes;
        } catch (IOException e) {
            throw new VaultException(e);
        }
    }

    public synchronized long availableBytes() throws VaultException
    {
        return store.availableBytes();
    }

    public long backedupBytes()
    {
        return backupdb.backedupBytes();
    }
    
    /**
     * Returns true if the specified file should be ignored
     * for purposes of backup and restore.
     */
    private boolean isHidden(File file)
    {
        boolean isHidden = FileUtil.isAncestor(configDirectory, file);
        return isHidden;
    }

    public void handleNotification(String notification, Object sender,
            Object argument)
    {
        if (FileStore.AvailableBytesChangedNotification.equals(notification)) {
            NotificationCenter.sharedCenter().post(
                    AvailableBytesChangedNotification, this, null);
        } else if (FileStore.MaintenanceNeededNotification.equals(notification)) {
            NotificationCenter.sharedCenter().post(
                    MaintenanceNeededNotification, this, null);
        } else if (BackupDatabase.ContentsChangedNotification.equals(notification)) {
            NotificationCenter.sharedCenter().post(ContentsChangedNotification,
                    this, null);
        }
        else if (sender instanceof FileStore) {
            // Forward all notifications from our internal stores.
            NotificationCenter.sharedCenter().post(notification, this, argument);
        }
    }

    private void load(String password) throws IOException, VaultException
    {
        try {
            XMLDeserializer deserializer = new XMLDeserializer(configFile);

            deserializer.putUserData(VaultConfiguration.ConfigurationPassword,
                    password);

            try {
                deserializer.parse(this);
            } finally {
                deserializer.close();
            }
            store.setConfiguration(vaultConfig);
        } catch (RuntimeException e) {
            VaultException vaultException = VaultException.extract(e);

            if (vaultException == null) {
                throw e;
            }

            throw vaultException;
        }
    }

    public void save() throws IOException
    {
        File tempFile = File.createTempFile("config", null, configFile
                .getParentFile());

        XMLSerializer serializer = null;

        try {
            serializer = new XMLSerializer(tempFile);
            serializer.putUserData(VaultConfiguration.ConfigurationPassword,
                    vaultConfig.currentPassword());
            serializeXML(serializer);
        } finally {
            if (serializer != null) {
                serializer.close();
            }
        }

        configFile.delete();
        if (!tempFile.renameTo(configFile)) {
            throw new IOException("Couldn't rename " + tempFile + " to "
                    + configFile);
        }
    }

    public void serializeXML(XMLSerializer serializer)
    {
        serializer.push("Configuration");
        vaultConfig.serializeXML(serializer);
        store.serializeXML(serializer);
        serializer.pop();
    }

    public XMLSerializable deserializeXML(XMLDeserializer deserializer,
            String container, String value)
    {
        if ("GlobalConfiguration".equals(container)) {
            return vaultConfig;
        }
        if ("FileStores".equals(container)) {
            return store;
        }
        return null;
    }

    class BackupFileFindDelegate implements FileFindDelegate
    {
        private FileOperationListener listener;

        BackupResult result;

        private Date date;

        public BackupFileFindDelegate(FileOperationListener listener,
                Date date, BackupResult result)
        {
            this.listener = listener;
            this.date = date;
            this.result = result;
        }

        public boolean shouldRecurseIntoDirectory(File directory)
        {
            return true;
        }

        public boolean processFile(File file)
        {
            if (listener != null && !listener.willProcessFile(file)) {
                return false;
            }

            try {
                // We skip the configuration files.  The database.xml file
                // is handled separately.
                if (!isHidden(file)) {

                    FileRevision revision = backupFile(file, date, listener);

                    if (result != null) {
                        result.add(revision);
                    }
                }
            } catch (VaultException e) {
                // Wrap the VaultException in a RuntimeException so I can percolate it back
                // up where it will be unwrapped and rethrown as a strongly typed exception.
                throw new RuntimeException(e);
            }
            return true;
        }

    	public void didProcessDirectoryContents(File directory, String children[])
    	{
    	    try {
    	        updateDirectoryContents(directory, date, children);
	        } catch (VaultException e) {
	            // Wrap the VaultException in a RuntimeException so I can percolate it back
	            // up where it will be unwrapped and rethrown as a strongly typed exception.
	            throw new RuntimeException(e);
	        }
    	}    	
    }    
}