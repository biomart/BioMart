/*
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

package org.biomart.runner.view.cli;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.runner.controller.JobHandler;
import org.biomart.runner.controller.MartRunnerProtocol;
import org.biomart.runner.exceptions.JobException;
import org.biomart.runner.exceptions.ProtocolException;

/**
 * The main app housing the MartRunner CLI. The {@link #main(String[])} method
 * starts the CLI.
 * <p>
 * The CLI syntax is:
 * 
 * <pre>
 *             java org.biomart.runner.view.cli.MartRunner &lt;port&gt;
 * </pre>
 * 
 * where <tt>&lt;port&gt;</tt> is the port that this server should listen on.
 * 
 */
public class MartRunner extends BioMartCLI {
	private static final long serialVersionUID = 1L;

	private final Collection<ClientWorker> workers = new HashSet<ClientWorker>();

	private ServerSocket serverSocket = null;

	/**
	 * Run this application and open the main window. The window stays open and
	 * the application keeps running until the window is closed.
	 * 
	 * @param args
	 *            any command line arguments that the user specified will be in
	 *            this array.
	 */
	public static void main(final String[] args) {
		// Initialise resources.
		Settings.setApplication(Settings.MARTRUNNER);
		Resources.setResourceLocation("org/biomart/runner/resources");
		// Start the app.
		try {
			new MartRunner(args).launch();
		} catch (final Throwable t) {
			Log.fatal(t);
		}
	}

	/**
	 * Start a MartBuilder CLI server listening using the given socket.
	 * 
	 * @param args
	 *            the command line arguments. It expects the first one to be a
	 *            socket number to listen on.
	 * @throws ValidationException
	 *             if the arguments were invalid.
	 * @throws JobException
	 *             it if could not set things up.
	 */
	public MartRunner(final String[] args) throws ValidationException,
			JobException {
		super();
		// Check port number argument was supplied.
		if (args.length < 1)
			throw new ValidationException(Resources.get("serverPortMissing"));
		// Find and update all crashed jobs and mark them as stopped.
		Log.debug("Finding crashed jobs");
		final int crashedJobs = JobHandler.stopCrashedJobs();
		if (crashedJobs > 0)
			Log.info("Found crashed jobs: " + crashedJobs);
		// Establish the socket and start listening.
		try {
			final int port = Integer.parseInt(args[0]);
			Log.info("Server listening on " + port);
			this.serverSocket = new ServerSocket(port);
		} catch (final IOException e) {
			throw new ValidationException(Resources.get("serverPortBroken",
					args[0]), e);
		} catch (final NumberFormatException e) {
			throw new ValidationException(Resources.get("serverPortInvalid",
					args[0]), e);
		}
	}

	public boolean poll() throws Throwable {
		final Socket clientSocket = this.serverSocket.accept();
		final Runnable worker = new Runnable() {
			public void run() {
				final ClientWorker worker = new ClientWorker(clientSocket);
				synchronized (MartRunner.this.workers) {
					MartRunner.this.workers.add(worker);
				}
				try {
					worker.handleClient();
				} catch (final ProtocolException e) {
					throw new RuntimeException(e);
				}
				synchronized (MartRunner.this.workers) {
					MartRunner.this.workers.remove(worker);
				}
				worker.close();
			}
		};
		try {
			worker.run();
		} catch (final RuntimeException e) {
			throw e.getCause();
		}
		// We never have cause to exit.
		return true;
	}

	public void finalize() {
		// In case the user ctrl-C's us we can tidy up nicely.
		this.requestExitApp();
	}

	public boolean confirmExitApp() {
		try {
			synchronized (MartRunner.this.workers) {
				for (final Iterator<ClientWorker> i = this.workers.iterator(); i.hasNext();)
					i.next().close();
			}
			this.serverSocket.close();
		} catch (final IOException e) {
			// We don't really care. We're exiting anyway.
		}
		return true;
	}

	private static class ClientWorker {
		private Socket clientSocket;

		private ClientWorker(final Socket clientSocket) {
			this.clientSocket = clientSocket;
			Log.info("Client connected on "
					+ this.clientSocket.getInetAddress());
		}

		/**
		 * Listen to the client and do what they ask.
		 * 
		 * @throws ProtocolException
		 *             if anything went wrong.
		 */
		public void handleClient() throws ProtocolException {
			try {
				MartRunnerProtocol.handleClient(this.clientSocket);
			} finally {
				this.close();
			}
		}

		/**
		 * Tell this client worker to shut down and close the socket.
		 */
		public void close() {
			try {
				this.clientSocket.close();
			} catch (final IOException e) {
				// We don't really care. We're exiting anyway.
			} finally {
				Log.info("Client disconnected from"
						+ this.clientSocket.getInetAddress());
			}
		}
	}
}
