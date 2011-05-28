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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.lang.StringUtils;

/**
 * PackageCollection
 *
 * @author Bahman Movaqar (Bahman AT BahmanM.com)
 */
public class PackageCollection {

	/** Repository */
	private final String repo;
	/** Directory of sync and local database */
	private final String dbPathSystem;
	/** Temp directory containing extracted sync database(s) */
	private final File dbPathTempSync;
	/** Package collection */
	private final HashMap<String, Package> collection = new HashMap<String, Package>();

	public HashMap<String, Package> getCollection() {
		return collection;
	}

	/**
	 * Constructor
	 * 
	 * @param repo Repository name e.g. 'community' or '*all*'
	 * @param dbPath Absolute path to directory of repository database files.
	 */
	public PackageCollection(String repo, String dbPath) throws IOException, FileNotFoundException, ArchiveException, PacmanConfPathException {
		this.repo = repo;
		this.dbPathSystem = dbPath;
		dbPathTempSync = Utils.createTempDir();
		if (repo.equals("*all*")) {
			extractAllDbArchives();
			populateCollection();
		} else {
			extractDbArchive(repo);
			populateCollectionRepo(repo);
		}
	}

	/**
	 * Builds package collection for all repositories.
	 */
	private void populateCollection() throws FileNotFoundException, IOException, PacmanConfPathException {
		ArrayList<String> repos = PacmanConfHelper.get().getRepos();
		for (int i=0; i<repos.size(); i++)
			addSyncPackages(repos.get(i));
		addLocalPackages(false);
	}

	/**
	 * Builds package collection.
	 * @param repo The repository
	 */
	private void populateCollectionRepo(String repo) throws IOException, FileNotFoundException, ArchiveException {
		addSyncPackages(repo);
		addLocalPackages(true);
	}

	/**
	 * Adds packages in a 'sync' db to package collection.
	 * @param dbFilePath Absolute path to .db file 
	 */
	private void addSyncPackages(final String repo) throws FileNotFoundException, IOException {
		traversPkgDir(new File(dbPathTempSync.getAbsolutePath() + "/" + repo),
				new PackageAction() {

					@Override
					public void action(Package pkg) {
						pkg.setRepo(repo);
						collection.put(pkg.getName(), pkg);
					}
				});
	}

	/**
	 * Adds packages in local database to collection.
	 * 
	 * @param onlyMatches Search only for those packages already in collection
	 */
	private void addLocalPackages(final boolean onlyMatches) throws FileNotFoundException, IOException {
		traversPkgDir(new File(dbPathSystem + "/local/"), new PackageAction() {

			@Override
			public void action(Package pkg) {
				if (collection.containsKey(pkg.getName())) {
					Package p = collection.get(pkg.getName());
					p.setLocalVersion(pkg.getRepoVersion());
				} else {
					if (!onlyMatches) {
						collection.put(pkg.getName(), pkg);
					}
				}
			}
		});
	}

	/**
	 * Extracts all .db archives in temp directory.
	 * 
	 * @throws IOException
	 * @throws ArchiveException 
	 */
	private void extractAllDbArchives() throws IOException, ArchiveException {
		File dbDir = new File(dbPathSystem + "/sync/");
		File[] dbFiles = dbDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".db");
			}
		});
		for (int i = 0; i < dbFiles.length; i++) {
			String fileName = dbFiles[i].getName();
			File dir = new File(dbPathTempSync.getAbsolutePath() + "/"
					+ fileName.substring(0, fileName.length() - 3));
			if (dir.exists())
				return;
			dir.mkdir();
			Utils.extractTarGz(dbFiles[i].getAbsolutePath(), dir);
		}
	}
	
	/**
	 * Extracts a .db archive in temp directory.
	 * 
	 * @param repo Repository
	 * @throws IOException
	 * @throws ArchiveException 
	 */
	private void extractDbArchive(String repo) throws IOException, ArchiveException {
		File dbFile = new File(dbPathSystem + "/sync/" + repo + ".db");
		File dir = new File(dbPathTempSync.getAbsolutePath() + "/" + repo);
		if (dir.exists())
			return;
		dir.mkdir();
		Utils.extractTarGz(dbFile.getAbsolutePath(), dir);
	}

	/**
	 * Traverses a package directory and performs an action on each package found.
	 * 
	 * @param dir Package directory
	 * @param packageAction Action to perform on packages
	 */
	private void traversPkgDir(File dir, PackageAction packageAction) throws FileNotFoundException, IOException {
		String[] fileList = dir.list();
		for (int i = 0; i < fileList.length; i++) {
			File f = new File(dir.getAbsolutePath() + "/" + fileList[i]);
			if (f.isDirectory()) {
				Package p = readPackage(f);
				packageAction.action(p);
			}
		}
	}

	/**
	 * Reads package information from a package directory.
	 * 
	 * @param pkgDir Package directory
	 * @return Package
	 */
	private Package readPackage(File pkgDir) throws FileNotFoundException, IOException {
		File f = new File(pkgDir.getAbsolutePath() + "/desc");
		FileInputStream fis = new FileInputStream(f);
		DataInputStream dis = new DataInputStream(fis);
		BufferedReader br = new BufferedReader(new InputStreamReader(dis));
		String line = null;
		Package pkg = new Package();
		try {
			boolean name = false;
			boolean desc = false;
			boolean version = false;
			while ((line = br.readLine()) != null) {
				line = StringUtils.normalizeSpace(line);
				if (line.equals("%NAME%")) {
					name = name ? false : true;
				} else if (line.equals("%VERSION%")) {
					version = version ? false : true;
				} else if (line.equals("%DESC%")) {
					desc = desc ? false : true;
				} else if (name) {
					pkg.setName(line);
					name = false;
				} else if (version) {
					pkg.setRepoVersion(line);
					version = false;
				} else if (desc) {
					pkg.setDescription(line);
					desc = false;
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(PackageCollection.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				br.close();
				dis.close();
				fis.close();
			} catch (IOException ioex) {
				throw new IOException("Error closing stream or reader: "
						+ ioex.getMessage());
			}
		}
		return pkg;
	}

	/**
	 * What to do with a package
	 */
	protected interface PackageAction {

		/**
		 * What to do with a package
		 * 
		 * @param pkg The package
		 */
		public abstract void action(Package pkg);
	}

	/**
	 * Minimal representation of a package
	 */
	public class Package {

		private String name = "";
		private String repo = "";
		private String localVersion = "";
		private String repoVersion = "";
		private String description = "";

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getLocalVersion() {
			return localVersion;
		}

		public void setLocalVersion(String localVersion) {
			this.localVersion = localVersion;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getRepo() {
			return repo;
		}

		public void setRepo(String repo) {
			this.repo = repo;
		}

		public String getRepoVersion() {
			return repoVersion;
		}

		public void setRepoVersion(String repoVersion) {
			this.repoVersion = repoVersion;
		}
	} // Package
}
