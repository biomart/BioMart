/*
 Copyright (C) 2006 EBI
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the itmplied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.biomart.common.utils;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Settings;

/**
 * Sends messages.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.4 $, $Date: 2007/10/03 10:41:02 $, modified by 
 * 			$Author: rh4 $
 * @since 0.6
 */
public class SendMail {

	/**
	 * Send an email. Uses the smtp.hostname, smtp.username and smtp.password
	 * properties to work out how to connect to the server. Authentication is
	 * only used if a username and/or password is supplied, otherwise an
	 * anonymous connection is made. The from address is taken from the
	 * mail.from property.
	 * <p>
	 * If debugging logs are turned on, then it will write SMTP messages to
	 * STDERR.
	 * 
	 * @param recipients
	 *            the TO recipients.
	 * @param subject
	 *            the subject.
	 * @param message
	 *            the message.
	 * @throws MessagingException
	 *             if it could not be sent.
	 */
	public static void sendSMTPMail(final String recipients[],
			final String subject, final String message)
			throws MessagingException {

		final String hostname = Settings.getProperty("smtp.hostname");
		final String username = Settings.getProperty("smtp.username");
		final String password = Settings.getProperty("smtp.password");
		final String from = Settings.getProperty("mail.from");

		if (hostname == null || from == null) {
			Log.debug("No hostname/from address supplied. Not sending mail.");
			return;
		}

		final Properties props = new Properties();
		props.setProperty("mail.transport.protocol", "smtp");
		props.setProperty("mail.host", hostname);
		if (username != null)
			props.setProperty("mail.user", username);
		if (password != null)
			props.setProperty("mail.password", password);
		if (Log.isDebug())
			props.setProperty("mail.debug", "true");

		final Session mailSession = Session.getDefaultInstance(props, null);
		if (Log.isDebug()) {
			mailSession.setDebug(true);
			mailSession.setDebugOut(System.err);
		}
		final Transport transport = mailSession.getTransport();

		final MimeMessage msg = new MimeMessage(mailSession);
		msg.setFrom(new InternetAddress(from));
		msg.setSubject(subject);
		msg.setContent(message, "text/plain");
		for (int i = 0; i < recipients.length; i++)
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
					recipients[i]));

		transport.connect();
		transport.sendMessage(msg, msg.getRecipients(Message.RecipientType.TO));
		transport.close();
	}

	// Static class cannot be instantiated.
	private SendMail() {
	}
}
