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
 * Created on Jul 22, 2004
 */
package com.toubassi.filebunker.vault;

import com.toubassi.io.XMLSerializable;

import java.io.File;
import java.io.InputStream;
import java.util.Date;

/**
 * @author garrick
 */
public interface FileStore extends XMLSerializable
{    
    /**
     * Notification posted by the FileStore if its availableBytes changed.
     * This is thrown when a file is backed up or deleted.
     */
    public static final String AvailableBytesChangedNotification = "AvailableBytesChangedNotification";

    /**
     * Notification posted by the FileStore when it thinks it needs maintenance.
     * Listeners should respond by calling performMaintenance at some point in
     * the near future.  It is really just a hint.
     */
    public static final String MaintenanceNeededNotification = "MaintenanceNeededNotification";

    
    /**
     * Returns a unique name for this store.  It is likely based on some
     * parameter of the store like an email address.
     */
    public String name();

    /**
     * Returns true if the store is fully configured and ready to perform its
     * functions.
     */
    public boolean isConfigured();

    /**
     * The VaultConfiguration has information like the password to be used for
     * encryption.  Its parameters may also include relevant information
     * for this particular store.
     */
    public void setConfiguration(VaultConfiguration configuration);
    
    /**
     * Returns a copy suitable for editing.
     */
    public FileStore editableCopy();

    /**
     * @param 	revision
     * @return	Returns true if this FileStore can handle the specified
     * 			revision (by checkings its handlerName)
     */
    public boolean canHandleRevision(RevisionIdentifier identifier);
    
    /**
     * Checks whether the store has enough storage (for example) or other
     * capabilities to backup the file.  This is generally a quick operation
     * 
     * @param 	file
     * @return 	Returns true if this store can backup the specified file.
     */
    public boolean canBackupFile(File file) throws VaultException;
    
	/**
	 * @param file		 The file to backup (must exist in the file system)
	 * @param name		 An optional name that will be used to tag the
	 * 					 file.  In the future it may be possible to restore
	 * 					 a file by name (only the most recent version).
	 * @param listener	 An optional listener that will be notified periodically
	 * 					 during the upload via the fileProgress method.  The
	 * 					 bytesProcessed number is a number of bytes as a fraction
	 * 		 			 of the total size of the file.
	 * @param identifier An in/out parameter.  The store is expected to set the name
	 * 					 and backedupSize on the identifier.
	 * 
	 * @return 			 The RevisionIdentifier for the backed up file.
	 * @throws			 VaultException
	 */
	public void backupFile(File file, String name, RevisionIdentifier identifier, FileOperationListener listener) throws VaultException;
	
	/**
	 * The specified date is used to identify which password should be used
	 * to decrypt the contents of the file (by calling
	 * VaultConfiguration.passwordForDate()).
	 * @return an InputStream representing the content of the restored file.
	 */
	public InputStream restoreFile(RevisionIdentifier identifier, Date date) throws VaultException;

	/**
	 * Deletes the file permanently from the store.  The backed up size is
	 * handed to the store so it can update its internal record of the
	 * availableBytes in the store.
	 * 
	 * @param identifier 		The identifier of the file to be deleted.
	 * @param backedupSize 		The size of the file in the store.
	 * @throws VaultException	If there is an error deleting the file
	 */
	public void deleteFile(RevisionIdentifier identifier, long backedupSize) throws VaultException;
	
	/**
	 * @return The number of bytes available in this FileStore
	 * @throws VaultException
	 */
	public long availableBytes() throws VaultException;
	
	/**
	 * Perform whatever specific "regular" maintenance the FileStore needs.
	 * @throws VaultException
	 */
	public void performMaintenance() throws VaultException;
	
	/**
	 * Perform any cleanup in preparation for process termination.  For instance,
	 * cached web sessions should be logged out.
	 */
	public void prepareForShutdown() throws VaultException;
}
