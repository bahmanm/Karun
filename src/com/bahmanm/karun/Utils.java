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

import java.awt.Frame;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Utils
 *
 * @author Bahman Movaqar (Bahman AT BahmanM.com)
 */
public class Utils {

	/**
	 * Copies srcPath to destPath.
	 * 
	 * @param srcPath Path to source file
	 * @param destPath Path to destination file
	 * @throws FileNotFoundException
	 * @throws IOException 
	 */
	public synchronized static void copyFile(String srcPath, String destPath) throws FileNotFoundException, IOException {
		FileInputStream in = new FileInputStream(srcPath);
		FileOutputStream out = new FileOutputStream(destPath);
		IOUtils.copy(in, out);
		in.close();
		out.close();
	}

	/**
	 * Extracts a gzip'ed tar archive.
	 * 
	 * @param archivePath Path to archive
	 * @param destDir Destination directory
	 * @throws IOException 
	 */
	public synchronized static void extractTarGz(String archivePath, File destDir) throws IOException, ArchiveException {
		// copy
		File tarGzFile = File.createTempFile("karuntargz", "", destDir);
		copyFile(archivePath, tarGzFile.getAbsolutePath());

		// decompress
		File tarFile = File.createTempFile("karuntar", "", destDir);
		FileInputStream fin = new FileInputStream(tarGzFile);
		BufferedInputStream bin = new BufferedInputStream(fin);
		FileOutputStream fout = new FileOutputStream(tarFile);
		GzipCompressorInputStream gzIn = new GzipCompressorInputStream(bin);
		final byte[] buffer = new byte[1024];
		int n = 0;
		while (-1 != (n = gzIn.read(buffer))) {
			fout.write(buffer, 0, n);
		}
		bin.close();
		fin.close();
		gzIn.close();
		fout.close();

		// extract
		final InputStream is = new FileInputStream(tarFile);
		ArchiveInputStream ain = new ArchiveStreamFactory().createArchiveInputStream("tar", is);
		TarArchiveEntry entry = null;
		while ((entry = (TarArchiveEntry) ain.getNextEntry()) != null) {
			OutputStream out;
			if (entry.isDirectory()) {
				File f = new File(destDir, entry.getName());
				f.mkdirs();
				continue;
			} else
				out = new FileOutputStream(new File(destDir, entry.getName()));
			IOUtils.copy(ain, out);
			out.close();
		}
		ain.close();
		is.close();
	}

	/**
	 * Pops up a dialog containing useful information from exception such as
	 * stack trace.
	 * 
	 * @param parent Parent component
	 * @param ex Exception
	 */
	public static void showExceptionDialog(Frame parent, Exception ex) {
		String topic = "";
		if (ex instanceof IOException)
			topic = "An I/O error occured.";
		else
			topic = "An error occured.";
		topic += "  [" + ex.getClass().getName() + "]";
		if (ex.getMessage() != null)
			topic += "\n" + ex.getMessage();
		
		StackTraceElement[] stackTrace = ex.getStackTrace();
		String stacktraceStr = "";
		for (int i=0; i<stackTrace.length; i++)
			stacktraceStr += stackTrace[i] + "\n";
		
		ExceptionDialog edialog = new ExceptionDialog(parent, true, topic, stacktraceStr);
		edialog.setVisible(true);
	}
	
	/**
	 * Creates a temporary directory.
	 * 
	 * @return Temporary directory
	 */
	public static File createTempDir() throws IOException {
		//final File sysTempDir = new File(System.getProperty("java.io.tmpdir"));
		File newTempDir;
		final int maxAttempts = 9;
		int attemptCount = 0;
		do {
			attemptCount++;
			if (attemptCount > maxAttempts) {
				throw new IOException("Failed to create a unique temporary directory.");
			}
			String dirName = UUID.randomUUID().toString();
			newTempDir = new File("/tmp/karun/", dirName);
		} while (newTempDir.exists());

		if (newTempDir.mkdirs()) {
			return newTempDir;
		} else {
			throw new IOException("Failed to create temporary directory named "
					+ newTempDir.getAbsolutePath());
		}
	}
}
