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
 * Created on Jul 2, 2004
 */
package com.toubassi.filebunker.commandline;

import com.toubassi.filebunker.vault.BackupEstimate;
import com.toubassi.filebunker.vault.BackupResult;
import com.toubassi.filebunker.vault.BackupSpecification;
import com.toubassi.filebunker.vault.DirectoryRevision;
import com.toubassi.filebunker.vault.FileOperationListener;
import com.toubassi.filebunker.vault.FileRevision;
import com.toubassi.filebunker.vault.FileStoreUtil;
import com.toubassi.filebunker.vault.InvalidVaultPasswordException;
import com.toubassi.filebunker.vault.RestoreResult;
import com.toubassi.filebunker.vault.RestoreSpecification;
import com.toubassi.filebunker.vault.Revision;
import com.toubassi.filebunker.vault.Vault;
import com.toubassi.filebunker.vault.VaultException;
import com.toubassi.util.Arguments;
import com.toubassi.util.ArgumentsException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author garrick
 */
public class FileBunker
{
    private static final String usage =
		"usage: FileBunker -config <configdir>\n" +
		"                  -password <password>\n" +
		"                  -operation <operation> <arguments>\n" +
		"\n" +
		"    -usage     Print this message\n" + 
		"    -config    The directory that contains the FileBunker configuration\n" + 
		"    -password  The password needed to unlock the configuration\n" +
		"\n" +
		"Supported operations:\n" +
		"\n" +
		"    backup FileOrDir1 [FileOrDir2 ...]\n" +
		"        Backs up the specified files or directories.\n" +
		"\n" +
		"    restore DestinationFileOrDir FileOrDirToRestore\n" +
		"        Restores the most recent version of FileOrDirToRestore to the path\n" +
		"        DestinationFileOrDir\n" +
		"\n" +
		"    delete FileOrDir\n" +
		"        Deletes the most recently backed up version of FileOrDir from the\n" +
		"        GMail repository.\n" +
		"\n" +
		"    utilization\n" +
		"        Report how much space is available in the GMail repository.\n" +
		"\n" +
		"    recover NumberOfBytes\n" +
		"        Will attempt to recover the specified number of bytes from the GMail\n" +
		"        repository. This is done by removing redundant versions of files in\n" +
		"        your GMail repository.  The last version of a given file is never\n" +
		"        removed.\n" +
		"\n" +
		"    estimate FileOrDir1 [FileOrDir2 ...]\n" +
		"        Similar to backup, but simply computes and reports information about\n" +
		"        what such a backup would be like, without actually doing it.\n" +
		"        Statistics include the number of files, the size of those files, and\n" +
		"        the predicted compression ration.\n" +
		"\n" +
		"    performMaintenance\n" +
		"        Performs maintenance of the GMail repository.  Specifically this\n" +
		"        involves archiving backup emails that may be lingering in the inbox\n" +
		"        of any of the repository accounts.\n" +
		"\n" +
		"    dumpDatabase\n" +
		"        Dump the contents of the backup database in XML format.  This\n" +
		"        database describes the contents of your backup repository\n" +
		"        including all versions of all files backed up.\n" +
		"\n" +
		"    decrypt fileToDecrypt resultingFile\n" +
		"        Decrypt and decompress the raw file attachment.  file must be\n" +
		"        an attachment saved directly from a FileBunker backup email.\n" +
		"        Note the password specified is used to decrypt the file, but\n" +
		"        that may not be the current FileBunker password.  It should\n" +
		"        be the password at the time the file was backed up.  The\n" +
		"        -config flag is not necessary for this operation.\n" +
		"";
		
	public static void performBackup(String[] args) throws ArgumentsException, VaultException, IOException
	{
		Arguments arguments = new Arguments(args, usage);

		String operation = arguments.flagString("operation");
		
		// First try to handle operations which do not require a vault
		if (operation.equals("decrypt")) {
			ArrayList paths = arguments.parameters();
			
			if (paths.size() != 2) {
			    fatal("Must specify an existing file to decrypt, and a nonexistent file to decrypt as.");
			}
			
			File sourceFile = new File((String)paths.get(0));
			File destFile = new File((String)paths.get(1));
			
			if (!sourceFile.exists()) {
			    fatal(sourceFile.getPath() + " does not exist.");
			}
			
			if (destFile.exists()) {
			    fatal(destFile.getPath() + " already exist.");			    
			}
			
		    FileInputStream fileInput = new FileInputStream(sourceFile);
		    BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);
		    InputStream input = FileStoreUtil.restoreInputStream(bufferedInput, arguments.flagString("password"));
		    
		    FileOutputStream fileOutput = new FileOutputStream(destFile);

		    byte buffer[] = new byte[2048];
		    int numRead;
		    while ((numRead = input.read(buffer)) >= 0) {
		        fileOutput.write(buffer, 0, numRead);
		    }
		    
		    fileOutput.close();
		    input.close();
		    
		    return;
		}

		
		Vault vault = null;		
		try {
		    vault = new Vault(new File(arguments.flagString("config")), arguments.flagString("password"));
		}
		catch (InvalidVaultPasswordException e) {
		    System.err.println("Password does not match");
		    System.exit(1);		    
		}
		
		if (operation.equals("restore")) {
			if (arguments.parameters().size() != 2) {
				fatal("For a restore, must specify a destination file and a file to restore");
			}
			File file = new File((String)arguments.parameters().get(0));
			File restoreFile = new File((String)arguments.parameters().get(1));
			RestoreResult result = new RestoreResult();
			RestoreSpecification spec = new RestoreSpecification();
			Revision revision = vault.findRevision(file, null);
			if (revision.isDirectory()) {
			    spec.add((DirectoryRevision)revision, null);
			}
			else {
			    spec.add((FileRevision)revision);
			}
			vault.restore(spec, restoreFile, false, new GenericFileOperationListener(), result);
			System.out.println(result);
		}
		else if (operation.equals("delete")) {
		    ArrayList parameters = arguments.parameters();
		    
			if (parameters.isEmpty()) {
			    fatal("Must specify one or more files/directories to delete.");
			}
			for (int i = 0, count = parameters.size(); i < count; i++) {
				File file = new File((String)parameters.get(i));
			    System.out.println("Deleting " + file.getPath());
			    vault.delete(file, new Date());
			}
		}
		else if (operation.equals("utilization")) {
		    System.out.println("Total bytes backed up:       " + vault.backedupBytes() + " bytes");
		    System.out.println("Total available vault space: " + vault.availableBytes() + " bytes");
		}
		else if (operation.equals("recover")) {
		    
		    if (arguments.parameters().size() != -1) {
		        fatal("Must specify a number of bytes to recover.");
		    }
		    
		    int size = Integer.parseInt((String)arguments.parameters().get(0));
		    
		    if (size < 1) {
		        throw new IllegalArgumentException("The number passed to -recover must be greater than zero.");
		    }
		    
		    long originalSize = vault.backedupBytes();
		    long recovered = vault.recoverBytes(size, new GenericFileOperationListener());
		    System.out.println("     Original vault size: " + originalSize);
		    System.out.println("          New vault size: " + vault.backedupBytes());
		    System.out.println("Computed recovered bytes: " + (originalSize - vault.backedupBytes()));
		    System.out.println("Reported recovered bytes: " + recovered);
		}
		else if (operation.equals("estimate")) {
			
		    if (arguments.parameters().isEmpty()) {
				fatal("Must specify one or more files or directories to estimate.");
			}
			ArrayList paths = arguments.parameters();

			BackupSpecification spec = new BackupSpecification();
			spec.setIsIncremental(arguments.flagBoolean("incremental", false));
						
			for (int i = 0, count = paths.size(); i < count; i++) {
			    String path = (String)paths.get(i);
			    spec.addFile(new File(path));
			}
			
			BackupEstimate estimate = vault.estimateBackup(spec, new GenericFileOperationListener());
		}
		else if (operation.equals("performMaintenance")) {
		    vault.performMaintenance();
		}
		else if (operation.equals("dumpDatabase")) {
		    vault.writeDatabaseXML(System.out);
		}
		else if (operation.equals("backup")){

			BackupSpecification spec = new BackupSpecification();
			spec.setIsIncremental(arguments.flagBoolean("incremental", false));

			ArrayList paths = arguments.parameters();
			if (paths.isEmpty()) {
			    fatal("Must specify one or more files/directories to backup.");
			}
			
			for (int i = 0, count = paths.size(); i < count; i++) {
			    String path = (String)paths.get(i);
			    spec.addFile(new File(path));
			}

			BackupResult result = new BackupResult();
			vault.backup(spec, new GenericFileOperationListener(), result);
			System.out.println(result);
		}				
	}

	
	public static void fatal(String error)
	{
		if (error != null) {
			System.err.println(error);
			System.err.println();
		}
		System.err.print(usage);
		System.exit(1);		
	}
	
	public static void main(String[] args)
	{
		try {
			performBackup(args);
		}
		catch (ArgumentsException e) {
			fatal(e.getMessage());
		}
		catch (Exception e) {
			e.printStackTrace();
			fatal(null);
		}
	}
}

class GenericFileOperationListener implements FileOperationListener
{
    private int count;
    
    public boolean fileProgress(File file, long bytesProcessed)
    {           
        System.out.println(file.getPath() + " processed " + bytesProcessed);
        return true;
    }

    public boolean willProcessFile(File file)
    {
        System.out.println(file.getPath());
        return true;
    }
}