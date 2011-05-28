/*
 * Karun, a package manager for ArchLinux based on 'pacman'.
 * Copyright (C) 2011  Bahman Movaqar
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, 
 * USA.
 */
package com.bahmanm.karun;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

/**
 * PacmanConfHelper
 * 
 * @author Bahman Movaqar (Bahman AT BahmanM.com)
 */
public class PacmanConfHelper {

	private static final String PACMAN_CONF_PATH = "/etc/pacman.conf";
	/** Absolute path to 'pacman.conf' */
	private String confPath;
	/** Pacman's DB path */
	private String dbPath = "/var/lib/pacman/";
	/** Pacman's cache directory */
	private String cacheDir = "/var/cache/pacman/pkg/";
	/** Pacman's repositories */
	private ArrayList<String> repos = new ArrayList<String>();
	/** Singleton object */
	private static PacmanConfHelper pacmanConfHelper;

	// Getter block
	public String getCacheDir() {
		return cacheDir;
	}

	public String getConfPath() {
		return confPath;
	}

	public String getDbPath() {
		return dbPath;
	}

	public ArrayList<String> getRepos() {
		return repos;
	}
	//
	
	/**
	 * Singleton constructor
	 * 
	 * @return Singleton object
	 * @throws PacmanConfPathException
	 * @throws FileNotFoundException
	 * @throws IOException 
	 */
	public synchronized static PacmanConfHelper get() throws PacmanConfPathException, FileNotFoundException, IOException {
		if (PacmanConfHelper.pacmanConfHelper == null)
			PacmanConfHelper.pacmanConfHelper = new PacmanConfHelper();
		return PacmanConfHelper.pacmanConfHelper;
	}

	/**
	 * Default constructor
	 */
	private PacmanConfHelper() throws PacmanConfPathException, FileNotFoundException, IOException {
		this(PACMAN_CONF_PATH);
	}

   /**
	 * Singleton constructor
	 * 
	 * @return Singleton object
	 * @throws PacmanConfPathException
	 * @throws FileNotFoundException
	 * @throws IOException 
	 */
	public synchronized static PacmanConfHelper get(String confPath) throws PacmanConfPathException, FileNotFoundException, IOException {
		if (PacmanConfHelper.pacmanConfHelper == null)
			PacmanConfHelper.pacmanConfHelper = new PacmanConfHelper(confPath);
		return PacmanConfHelper.pacmanConfHelper;
	}
	/**
	 * Constructor
	 * 
	 * @param confPath Absolute path to 'pacman.conf'.
	 */
	private PacmanConfHelper(String confPath) throws PacmanConfPathException, FileNotFoundException, IOException {
		// Check for existence
		File f = new File(confPath);
		if (!f.exists() || !f.canRead() || !f.isFile()) {
			throw new PacmanConfPathException(confPath);
		}

		this.confPath = confPath;
		extractInfo();
	}

	/**
	 * Extracts valuable info from 'pacman.conf'.
	 */
	private void extractInfo() throws FileNotFoundException, IOException {
		FileInputStream fis = new FileInputStream(confPath);
		DataInputStream dis = new DataInputStream(fis);
		BufferedReader br = new BufferedReader(new InputStreamReader(dis));
		String line;
		try {
			while ((line = br.readLine()) != null) {
				line = StringUtils.normalizeSpace(line);
				if (line.startsWith("#")) {
					continue;
				} else if (line.startsWith("[") && line.endsWith("]")) {
					String section = line.substring(1, line.length() - 1);
					if (!section.equals("options")) {
						repos.add(section);
					}
				} else if (line.startsWith("DBPath")) {
					dbPath = line.split("=")[1].trim();
				} else if (line.startsWith("CacheDir")) {
					cacheDir = line.split("=")[1].trim();
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(PacmanConfHelper.class.getName()).log(Level.SEVERE, null, ex);
			try {
				br.close();
				dis.close();
				fis.close();
			} catch (IOException ioex) {
				throw new IOException("Error closing stream or reader: "
						+ ioex.getMessage());
			}
		}
	}
	
}
