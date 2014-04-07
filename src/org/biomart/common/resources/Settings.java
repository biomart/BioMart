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

package org.biomart.common.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.biomart.common.utils.FileUtils;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.utils.McUtils;
import org.biomart.processors.ProcessorRegistry;

/**
 * Manages the on-disk cache of user settings.
 * <p>
 * Settings are contained in a folder called <tt>.biomart</tt> in the user's
 * home directory, inside which there is a second folder for each of the BioMart
 * applications. In there are two files - one called <tt>properties</tt> which
 * contains general configuration settings such as look and feel, and the other
 * called <tt>cache</tt> which is a directory containing history settings for
 * various classes.
 * <p>
 * You should only ever need to modify the <tt>properties</tt> file, and
 * <tt>cache</tt> should be left alone.

 */
public class Settings {

	/**
	 * App reference for MartBuilder.
	 */
	public static final String MARTCONFIGURATOR = "martconfigurator";

	/**
	 * App reference for MartRunner.
	 */
	public static final String MARTRUNNER = "martrunner";

	private static String application;

	private static final File homeDir = new File(System
			.getProperty("user.home"), ".biomart");

	private static File appDir;

	private static final Map<Class<? extends Object>, Map<String,Properties>> classCache = 
		new HashMap<Class<? extends Object>, Map<String,Properties>>();

	public static File classCacheDir;

	private static int classCacheSize = 10;

	private static boolean initialising = true;

	private static final Properties properties = new Properties();

	private static final File propertiesFile = new File(Settings.homeDir,
			"properties");

	private static final Object SAVE_LOCK = new String("__SAVE__LOCK");

	private static final String manifestName = "__MANIFEST.txt";

	// Create the bits we need on start-up.
	static {
		try {
			if (!Settings.homeDir.exists())
				Settings.homeDir.mkdir();
			if (!Settings.propertiesFile.exists())
				Settings.propertiesFile.createNewFile();
		} catch (final Throwable t) {
			Log.error("Failed to initialise settings cache");
		}
	}

	
	/**
	 * Set the current application.
	 * 
	 * @param app
	 *            the current application.
	 */
	public static void setApplication(final String app) {
		Settings.application = app;
		// Make the home directory.
		Settings.appDir = new File(Settings.homeDir, app);
		if (!Settings.appDir.exists())
			Settings.appDir.mkdir();

		// Set up the logger.
		Log.configure(app, Settings.appDir);
		// Make the class cache directory.
		Settings.classCacheDir = new File(Settings.appDir, "objectCache");
		final File classCacheVersion = new File(Settings.appDir,
				"objectCacheVersion.txt");
		// Remove it if pre-current version.
		if (Settings.classCacheDir.exists()) {
			// Load and check class cache version.
			String oldVersion = null;
			if (classCacheVersion.exists())
				try {
					final FileInputStream fis = new FileInputStream(
							classCacheVersion);
					final byte[] oldVersionBytes = new byte[Resources.BIOMART_VERSION
							.length()];
					final int readLength = fis.read(oldVersionBytes);
					if (readLength >= 0)
						oldVersion = new String(oldVersionBytes, 0, readLength);
					fis.close();
				} catch (final IOException e) {
					// Assume if failed that it is an out-of-date version.
					oldVersion = null;
				}
			// If differs, delete cache.
			if (!Resources.BIOMART_VERSION.equals(oldVersion))
				try {
					FileUtils.delete(Settings.classCacheDir);
				} catch (final IOException e) {
					// Not much we can do apart from warn.
					StackTrace.showStackTrace(e);
				}
		}
		
		// If doesn't exist, or has been removed, (re)create it.
		if (!Settings.classCacheDir.exists())
			try {
				Settings.classCacheDir.mkdir();
				// Create/Overwrite version file.
				final FileOutputStream fos = new FileOutputStream(
						classCacheVersion);
				fos.write(Resources.BIOMART_VERSION.getBytes());
				fos.close();
			} catch (final IOException e) {
				// Not much we can do apart from warn.
				StackTrace.showStackTrace(e);
			}
	}

	/**
	 * Gets the current application
	 * 
	 * @return the current application.
	 */
	public static String getApplication() {
		return Settings.application;
	}

	/**
	 * Obtain the current application's storage directory.
	 * 
	 * @return the directory.
	 */
	public static File getStorageDirectory() {
		return Settings.appDir;
	}
	
	/**
	 * Saves the current cache of settings to disk as a set of files at
	 * <tt>~/.biomart/&lt;appname&gt;</tt>.
	 */
	public static void save() {
		// Don't save if we're still loading.
		if (Settings.initialising) {
			Log.debug("Still loading settings, so won't save settings yet");
			return;
		}

		synchronized (Settings.SAVE_LOCK) {

			try {
				Log.debug("Saving settings to "
						+ Settings.propertiesFile.getPath());
				Settings.properties.store(new FileOutputStream(
						Settings.propertiesFile), Resources
						.get("settingsCacheHeader"));
				// Save the class-by-class properties.
				Log.debug("Saving class caches");
				for (final Iterator<Map.Entry<Class<? extends Object>, Map<String,Properties>>> i = Settings.classCache.entrySet()
						.iterator(); i.hasNext();) {
					final Map.Entry<Class<? extends Object>, Map<String,Properties>> classCacheEntry = i.next();
					final Class<? extends Object> clazz = classCacheEntry.getKey();
					final File classDir = new File(Settings.classCacheDir,
							clazz.getName());
					Log.debug("Creating class cache directory for "
							+ clazz.getName());
					classDir.mkdir();
					// Remove existing files.
					Log.debug("Clearing existing class cache files");
					final File[] files = classDir.listFiles();
					for (int j = 0; j < files.length; j++)
						files[j].delete();
					// Save current set. Must use Map.Entry else each
					// call for map keys and values will change the
					// structure of the LRU cache map, and hence cause
					// ConcurrentModificationExceptions.
					final List<String> manifestList = new ArrayList<String>();
					for (final Iterator<Map.Entry<String,Properties>> j = (classCacheEntry.getValue())
							.entrySet().iterator(); j.hasNext();) {
						final Map.Entry<String,Properties> entry = j.next();
						final String name = (String) entry.getKey();
						manifestList.add(name);
						final Properties props = (Properties) entry.getValue();
						final File propsFile = new File(classDir, name);
						Log.debug("Saving properties to "+ propsFile.getPath());
						props.store(new FileOutputStream(propsFile), Resources
								.get("settingsCacheHeader"));
					}
					// Write manifest speciying order of keys.
					final File manifest = new File(classDir,
							Settings.manifestName);
					final FileOutputStream fos = new FileOutputStream(manifest);
					final ObjectOutputStream oos = new ObjectOutputStream(fos);
					oos.writeObject(manifestList);
					oos.flush();
					fos.close();
				}
			} catch (final Throwable t) {
				Log.error("Failed to save settings", t);
			}

			Log.info("Done saving settings");
		}
	}

	/**
	 * Given a class, return the set of names of properties from the history map
	 * that correspond to that class.
	 * 
	 * @param clazz
	 *            the class to look up.
	 * @return the names of the properties sets in the history that match that
	 *         class. May be empty but never <tt>null</tt>.
	 */
	public static List<String> getHistoryNamesForClass(final Class<? extends Object> clazz) {
		final Map<String,Properties> map = Settings.classCache.get(clazz);
		// Use copy of map keys in order to prevent concurrent modifications.
		return map == null ? new ArrayList<String>() : new ArrayList<String>(map
				.keySet());
	}

	/**
	 * Given a class and a group name, return the set of properties from history
	 * that match.
	 * 
	 * @param clazz
	 *            the class to look up.
	 * @param name
	 *            the name of the property set in the history.
	 * @return the properties that match. <tt>null</tt> if there is no match.
	 */
	public static Properties getHistoryProperties(final Class<? extends Object> clazz,
			final String name) {
		final Map<String,Properties> map =  Settings.classCache.get(clazz);
		return map == null ? null : (Properties) map.get(name);
	}

	/**
	 * Given a property name, return that property based on the contents of the
	 * cache file <tt>properties</tt>.
	 * 
	 * @param property
	 *            the property name to look up.
	 * @return the value, or <tt>null</tt> if not found.
	 */
	public static String getProperty(final String property) {
		String value = System.getProperty(property);
		if(McUtils.isStringEmpty(value))
			value = (String)Settings.properties.getProperty(property);
		return value;
	}

	/**
	 * Loads the current cache of settings from disk, from the files in
	 * <tt>~/.biomart/&lt;appname&gt;</tt>.
	 */
	public static synchronized void load() {
		Settings.initialising = true;

		// Clear the existing settings.
		Log.debug("Clearing existing settings");
		Settings.properties.clear();

		// Load the settings.
		try {
			Log.debug("Loading settings from "
					+ Settings.propertiesFile.getPath());
			Settings.properties.load(new FileInputStream(
					Settings.propertiesFile));
		} catch (final Throwable t) {
			Log.error("Failed to load settings", t);
		}

		// Set up the cache.
		final String newClassCacheSize = Settings.properties
				.getProperty("classCacheSize");
		try {
			Log.debug("Setting class cache size to " + newClassCacheSize);
			Settings.classCacheSize = Integer.parseInt(newClassCacheSize);
		} catch (final NumberFormatException e) {
			// Ignore and use the default.
			Settings.classCacheSize = 10;
			Log.debug("Using default class cache size of "
					+ Settings.classCacheSize);
			Settings
					.setProperty("classCacheSize", "" + Settings.classCacheSize);
		}

		// Loop over classCacheDir to find classes.
		Log.debug("Loading class caches");
		final String[] classes = Settings.classCacheDir.list();
		if (classes != null)
			for (int i = 0; i < classes.length; i++)
				try {
					final Class<? extends Object> clazz = Class.forName(classes[i]);
					Log.debug("Loading class cache for " + clazz.getName());
					final File classDir = new File(Settings.classCacheDir,
							classes[i]);
					// Write manifest speciying reverse order of keys.
					List<String> manifestList;
					try {
						final File manifest = new File(classDir,
								Settings.manifestName);
						final FileInputStream fis = new FileInputStream(
								manifest);
						final ObjectInputStream ois = new ObjectInputStream(fis);
						manifestList = (List<String>) ois.readObject();
						fis.close();
					} catch (final Exception e) {
						// No manifest? Use natural order instead.
						manifestList = Arrays.asList(classDir.list());
					}
					// Load files in order specified in manifest.
					for (final Iterator<String> j = manifestList.iterator(); j
							.hasNext();) {
						final String entry = (String) j.next();
						final Properties props = new Properties();
						final File propsFile = new File(classDir, entry);
						Log.debug("Loading properties from "
								+ propsFile.getPath());
						props.load(new FileInputStream(propsFile));
						Settings.saveHistoryProperties(clazz, entry, props);
					}
				} catch (final ClassNotFoundException e) {
					// Ignore. We don't care as these settings are
					// now irrelevant if the class no longer exists.
				} catch (final Throwable t) {
					Log.error("Failed to load settings", t);
				}

		Settings.initialising = false;
		Log.info("Done loading settings");
	}

	/**
	 * Given a bunch of properties, save them in the history of the given class
	 * with the given name. If the history contains very old stuff, age it out.
	 * 
	 * @param clazz
	 *            the class of the history properties to store.
	 * @param name
	 *            the name to give the history entry.
	 * @param properties
	 *            the properties to store.
	 */
	public static void saveHistoryProperties(final Class<? extends Object> clazz,
			final String name, final Properties properties) {
		Log.debug("Adding history entry for " + clazz.getName() + ":" + name);
		if (!Settings.classCache.containsKey(clazz)) {
			Log.debug("Creating new cache for class " + clazz.getName());
			final LinkedHashMap<String,Properties> history = new LinkedHashMap<String, Properties>(
					Settings.classCacheSize, 0.75f, true) {
				private static final long serialVersionUID = 1;

				protected boolean removeEldestEntry(Map.Entry<String,Properties> eldest) {
					return this.size() > Settings.classCacheSize;
				}
			};
			Settings.classCache.put(clazz, history);
		}
		Log.debug("History properties are: " + properties);
		Settings.classCache.get(clazz).put(name, properties);
		Settings.save();
	}

	/**
	 * Given a property name, sets that property.
	 * 
	 * @param property
	 *            the property name to set.
	 * @param value
	 *            the value to give it.
	 */
	public static void setProperty(final String property, final String value) {
		if(McUtils.isStringEmpty(System.getProperty(property))) {
			Log.debug("Setting property " + property + "=" + value);
			Settings.properties.setProperty(property, value);
		} else
			System.setProperty(property, value);
	}

	// Private means that this class is a static singleton.
	private Settings() {
	}

	public static void loadGUIConfigProperties() {
		//load into system property
		Properties properties = new Properties();
		try {
		    properties.load(new FileInputStream("conf/xml/biomart.gui.properties"));
		} catch (IOException e) {
			Log.error("load properties error");
		}
		@SuppressWarnings("unchecked")
		//some properties may override the Settings.load();
		Enumeration<?> e = properties.propertyNames();
		while (e.hasMoreElements()) {
		      String key = (String) e.nextElement();
		      System.setProperty(key,properties.getProperty(key));
		}

	}
	
	public static void loadAllConfigProperties() {
		//load into system property
		Properties properties = new Properties();
		try {
		    properties.load(new FileInputStream("conf/xml/biomart.all.properties"));
		} catch (IOException e) {
			Log.error("load properties error");
		}
		@SuppressWarnings("unchecked")
		//some properties may override the Settings.load();
		Enumeration<?> e = properties.propertyNames();
		while (e.hasMoreElements()) {
		      String key = (String) e.nextElement();
		      System.setProperty(key,properties.getProperty(key));
		}
	}
	
	public static void loadWebConfigProperties() {
		//load into system property
		Properties properties = new Properties();
		try {
		    properties.load(new FileInputStream("conf/xml/biomart.web.properties"));
		} catch (IOException e) {
			Log.error("load properties error");
		}
		@SuppressWarnings("unchecked")
		//some properties may override the Settings.load();
		Enumeration<?> e = properties.propertyNames();
		while (e.hasMoreElements()) {
		      String key = (String) e.nextElement();
		      System.setProperty(key,properties.getProperty(key));
		}
	}

	public static File getClassCacheDir() {
		return Settings.classCacheDir;
	}
}
