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
 * Created on Jul 1, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.toubassi.filebunker.vault;

import com.subx.common.NotificationCenter;
import com.toubassi.io.ByteCountingInputStream;
import com.toubassi.io.ChunkedInputStream;
import com.toubassi.io.DESCipherInputStream;
import com.toubassi.io.DESCipherOutputStream;
import com.toubassi.io.XMLDeserializer;
import com.toubassi.io.XMLSerializable;
import com.toubassi.io.XMLSerializer;
import com.toubassi.util.ClassUtil;
import com.toubassi.util.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Abstract base class for FileStores using a webmail account for their
 * storage. This class has support for backing up files to the webmail
 * account by packaging the file as (1 or more) mime attachments. 
 * Concrete subclasses will implement the specific behavior for scraping
 * the webmail account for implementing restore functionality, checking
 * how much space is free in the account, performing maintenance (which
 * for a webmail account usually means moving backup emails out of the
 * inbox), etc.
 * 
 * This class is thread safe.
 * 
 * @author garrick
 */
public abstract class WebMailFileStore implements FileStore
{
	private static final Pattern subjectPattern = Pattern.compile("\"[^\"]*FileBunker[^:]*: " + RevisionIdentifier.guidCharacterClass() + "+ \\[(\\d+)\\]");

	public static final String SmtpServerKey = "Smtp";
	public static final String SmtpUseAuthenticationKey = "SmtpUseAuthentication";
	public static final String SmtpUsernameKey = "SmtpUser";
	public static final String SmtpPasswordKey = "SmtpPassword";
	public static final String OutgoingMessageSizeLimitKey = "OutgoingMessageSizeLimit";

    public static final String FromEmailKey = "FromEmail";

    protected VaultConfiguration vaultConfig;

    private InternetAddress email;

    private String accountPassword;

    private Session session;

    private long availableBytes = -1;

    private long availableBytesAdjustment = 0;

    // For checkIn/checkOutConversation pooling.  Must be synchronized
    private ArrayList cachedConversations = new ArrayList();

    public FileStore editableCopy()
    {
        try {
            WebMailFileStore copy;

            copy = (WebMailFileStore) getClass().newInstance();
            copy.setEmailAddress(email());
            copy.accountPassword = accountPassword;
            copy.availableBytes = availableBytes;
            copy.availableBytesAdjustment = availableBytesAdjustment;

            return copy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setEmailAddress(String emailAddress) throws AddressException
    {
        email = new InternetAddress(emailAddress);
        disposeCachedConversations();
    }

    public String email()
    {
        return email == null ? "" : email.getAddress();
    }

    public String name()
    {
        return email();
    }

    public boolean isConfigured()
    {
        return email != null && accountPassword != null && vaultConfig != null
                && vaultConfig.parameterForKey(SmtpServerKey) != null;
    }

    /**
     * @return By default strips off the username from the email address (e.g. foo@bar.com --> foo)
     */
    public String username()
    {
        if (email != null) {
            String email = email();
            String username;

            int firstAt = email.indexOf('@');

            if (firstAt != -1) {
                return email.substring(0, email.indexOf('@'));
            }
        }
        return null;
    }

    public void setAccountPassword(String password) throws VaultException
    {
        accountPassword = password;
    }

    public String accountPassword() throws VaultException
    {
        return accountPassword;
    }

    /**
     * Returns a WebConversation which is logged into webmail account.
     * Callers should (but are not strictly required to) return this
     * WebConversation by calling checkInConversation().
     */
    protected TimeOutWebConversation checkOutConversation()
            throws VaultException
    {
        synchronized (cachedConversations) {

            for (int i = cachedConversations.size() - 1; i >= 0; i--) {
                TimeOutWebConversation wc = (TimeOutWebConversation)cachedConversations.remove(i);

                if (!wc.hasTimedOut()) {
                    return wc;
                }
                else {
                    try {
                        disposeConversation(wc);
                    }
                    catch (VaultException e) {
                        // swallow
                    }
                }
            }

        }
        return createLoggedInConversation();
    }

    /**
     * Indicates that the specified conversation can be returned to the
     * cache of shared conversations.
     */
    protected void checkInConversation(TimeOutWebConversation conversation)
    {
        if (!conversation.hasTimedOut()) {
            synchronized (cachedConversations) {
                cachedConversations.add(conversation);
            }
        }
    }

    /**
     * Returns a TimeOutWebConversation that is logged into the webmail account
     * for this FileStore.
     */
    protected abstract TimeOutWebConversation createLoggedInConversation()
            throws VaultException;

    /**
     * The concrete subclass is expected to log off of this conversation.
     * Note the conversation may already be timed out.
     */
    protected abstract void disposeConversation(TimeOutWebConversation wc)
    		throws VaultException;
    
    protected Session session() throws VaultException
    {
        if (session == null) {
            // create some properties and get the default Session
            Properties properties = new Properties();
            properties.put("mail.smtp.host", vaultConfig.requiredParameterForKey(SmtpServerKey));
            
            String useAuthentication = vaultConfig.parameterForKey(SmtpUseAuthenticationKey);
            Authenticator authenticator = null;
            
            if ("true".equals(useAuthentication)) {
                properties.put("mail.smtp.auth", "true");
                authenticator = new SimpleSmtpAuthenticator(vaultConfig);
            }
            
            session = Session.getInstance(properties, authenticator);
            //session.setDebug(true);
        }
        return session;
    }

    public void setConfiguration(VaultConfiguration configuration)
    {
        vaultConfig = configuration;
        // Make sure to invalidate the session since our config is changing.
        session = null;
    }
    
    /** Returns 0 if no limit. */
    private int outgoingMessageSizeLimit()
    {
        String limitString = vaultConfig.parameterForKey(OutgoingMessageSizeLimitKey);
        if (limitString == null) {
            return 0;
        }
        // We correct for the base64 encoding that the attachment will
        // undergo (is this right?).  Which should be a 6/8 ratio plus
        // 10% slop.
        float limit = Integer.parseInt(limitString);
        return (int)(limit * 5.5/8.0 * .9);
    }

    public boolean canHandleRevision(RevisionIdentifier identifier)
    {
        return email() != null && email().equals(identifier.handlerName());
    }

    public boolean canBackupFile(File file) throws VaultException
    {
        // We decide conservatively based on uncompressed length.
        return file.length() < availableBytes();
    }

	public void backupFile(File file, String name, RevisionIdentifier identifier, FileOperationListener listener) throws VaultException
    {
        ChunkedInputStream chunkedStream = null;

        try {

            identifier.setHandlerName(name());
            
            String encryptionPassword = vaultConfig.currentPassword();
            ByteCountingInputStream countingStream = FileStoreUtil.backupInputStream(file, encryptionPassword, listener);
            int maxMessageSize = maximumMessageSize();
            
            int outgoingLimit = outgoingMessageSizeLimit();            
            if (outgoingLimit > 0) {
                maxMessageSize = Math.min(maxMessageSize, outgoingLimit);
            }
            
            chunkedStream = new ChunkedInputStream(countingStream, maxMessageSize);

            // Send as many parts as are necessary
            for (int part = 0; chunkedStream.hasMoreChunks(); chunkedStream.nextChunk(), part++) {

                if (part >= maximumNumberOfParts()) {
                    throw new IOException("Exceeded number of chunks for " + getClass().getName());
                }

                // Construct a name for this part
                String attachmentName = "file";
                if (part > 0) {
                    attachmentName += "." + part;
                }
                attachmentName += ".gz.crypt";

                sendPart(file, name, attachmentName, part, identifier.guid(), chunkedStream);
            }

            long backedupSize = countingStream.byteCount();

            // Round up for slop (headers, etc)
            adjustAvailableBytes(-(backedupSize + 1024));

            identifier.setBackedupSize(backedupSize);
            
            NotificationCenter.sharedCenter().post(
                    MaintenanceNeededNotification, this, null);

        } catch (VaultException e) {
            throw e;
        } catch (MessagingException e) {
            if (ExceptionUtil.extract(OperationCanceledIOException.class, e) != null) {
                throw new OperationCanceledVaultException(file);
            }
            throw new VaultException(file, "Error detected sending file", e);
        } catch (OperationCanceledIOException e) {
            // This is unlikely to occur, it will most likely be packaged in
            // a MessagingException handled above.
            throw new OperationCanceledVaultException(file);
        } catch (IOException e) {
            throw new VaultException(file, e);
        } finally {
            if (chunkedStream != null) {
                try {
                    chunkedStream.reallyClose();
                } catch (IOException e) {
                    // Swallow it.
                }
            }
        }
    }

    protected void sendPart(File file, String name, String attachmentName,
            int part, String guid, InputStream inputStream)
            throws VaultException, MessagingException
    {
        Session session = session();

        // create a message
        MimeMessage msg = new MimeMessage(session);
        
        String fromEmail = vaultConfig.parameterForKey(FromEmailKey);

        if (fromEmail == null) {
            // from = to
            msg.setFrom(email);
        }
        else {
            msg.setFrom(new InternetAddress(fromEmail));            
        }

        InternetAddress[] addresses = { email };
        msg.setRecipients(Message.RecipientType.TO, addresses);

        //
        // NOTE: If the subject changes, then subjectPattern must change!
        //
        
        StringBuffer subject = new StringBuffer("FileBunker");
        
        if (name != null) {
            if (name.indexOf(':') != -1) {
                throw new IllegalArgumentException("Backup file name (" + name + ") cannot have colon in it.");
            }
            subject.append(' ');
            subject.append(name);
        }
        subject.append(": ");
        subject.append(guid);
        subject.append(" [");
        subject.append(part);
        subject.append(']');
        msg.setSubject(subject.toString());
        msg.setSentDate(new Date());

        // create and fill the first message part
        MimeBodyPart mbp1 = new MimeBodyPart();
        StringBuffer textBuffer = new StringBuffer();

        textBuffer.append("<file>\n");

        textBuffer.append("    <identifier>");
        textBuffer.append(guid);
        textBuffer.append("</identifier>\n");

        if (part > 0) {
            textBuffer.append("    <part>");
            textBuffer.append(part);
            textBuffer.append("</part>\n");
        }

        textBuffer.append("</file>\n");

        mbp1.setText(textBuffer.toString());

        // create and fill the second message part
        MimeBodyPart mbp2 = new MimeBodyPart();
        DataSource source = new InputStreamDataSource(attachmentName, inputStream);
        mbp2.setDataHandler(new DataHandler(source));
        mbp2.addHeader("Content-Transfer-Encoding", "base64");
        mbp2.setFileName(source.getName());

        // create the Multipart and its parts to it
        Multipart mp = new MimeMultipart();
        mp.addBodyPart(mbp1);
        mp.addBodyPart(mbp2);

        // add the Multipart to the message
        msg.setContent(mp);

        // send the message
        Transport.send(msg);
    }

    // For subclassers that are lazy and can only handle a limited number of parts
    protected int maximumNumberOfParts()
    {
        return Integer.MAX_VALUE;
    }

    public void serializeXML(XMLSerializer serializer)
    {
        serializer.push(ClassUtil.nameWithoutPackage(getClass()));

        serializer.write("Email", email());
        String configPassword = (String) serializer
                .getUserData(VaultConfiguration.ConfigurationPassword);
        String encrypted = DESCipherOutputStream.encrypt(configPassword,
                accountPassword);
        serializer.write("Password", encrypted);
        serializer.pop();
    }

    public XMLSerializable deserializeXML(XMLDeserializer deserializer,
            String container, String value)
    {
        if ("Email".equals(container)) {
            try {
                setEmailAddress(value);
            } catch (AddressException e) {
                throw new RuntimeException(e);
            }
        } else if ("Password".equals(container)) {
            String configPassword = (String) deserializer
                    .getUserData(VaultConfiguration.ConfigurationPassword);
            accountPassword = DESCipherInputStream.decrypt(configPassword,
                    value);
        }
        return null;
    }

    public synchronized long availableBytes() throws VaultException
    {
        if (availableBytes == -1) {
            // Perhaps refetch this once in awhile?  Maybe if we were long running.
            availableBytes = computeAvailableBytes();
            availableBytesAdjustment = 0;
        }
        return availableBytes + availableBytesAdjustment;
    }

    protected abstract long computeAvailableBytes() throws VaultException;

    protected abstract int maximumMessageSize();

    public void deleteFile(RevisionIdentifier identifier, long backedupSize)
            throws VaultException
    {
        if (deleteFile(identifier)) {
            adjustAvailableBytes(backedupSize + 1024);
        }
    }

    protected abstract boolean deleteFile(RevisionIdentifier identifier)
            throws VaultException;

    public void performMaintenance() throws VaultException
    {
        cleanupInbox();
    }

    protected abstract void cleanupInbox() throws VaultException;

	public void prepareForShutdown() throws VaultException
	{
	    disposeCachedConversations();
	}
	
	protected void disposeCachedConversations()
	{
        synchronized (cachedConversations) {

            for (int i = cachedConversations.size() - 1; i >= 0; i--) {
                TimeOutWebConversation wc = (TimeOutWebConversation)cachedConversations.remove(i);

                try {
                    disposeConversation(wc);
                }
                catch (VaultException e) {
                    // swallow
                }
            }

        }	    	    
	}

    /**
     * Returns a pattern that will match the subject line of an email which is
     * part of a backup file (i.e. an email generated from a call to backupFile).
     * Note the first group in the pattern matches the part number.
     * @return
     */
    protected Pattern backupFileSubjectPattern()
    {
        return subjectPattern;
    }
    
    /**
     * Called by this class or subclassers when they perform operations that
     * change the availableBytes of the FileStore.
     * 
     * @param deltaBytes	A positive deltaBytes implies that bytes were "freed",
     * 						and negative deltaBytes implies bytes were used.
     */
    protected synchronized void adjustAvailableBytes(long deltaBytes)
    {
        availableBytesAdjustment += deltaBytes;
        NotificationCenter.sharedCenter().post(
                AvailableBytesChangedNotification, this, null);
    }
}

class SimpleSmtpAuthenticator extends Authenticator
{
    private String username;
    private String password;
    
    public SimpleSmtpAuthenticator(VaultConfiguration config) throws VaultException
    {
        username = config.requiredParameterForKey(WebMailFileStore.SmtpUsernameKey);
        password = config.requiredParameterForKey(WebMailFileStore.SmtpPasswordKey);
    }
    
    protected PasswordAuthentication getPasswordAuthentication()
    {
        return new PasswordAuthentication(username, password);
    }
}

