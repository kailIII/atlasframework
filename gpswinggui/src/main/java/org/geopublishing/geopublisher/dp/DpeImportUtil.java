/*******************************************************************************
 * Copyright (c) 2010 Stefan A. Tzeggai.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan A. Tzeggai - initial API and implementation
 ******************************************************************************/
package org.geopublishing.geopublisher.dp;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

import org.geopublishing.atlasViewer.GpCoreUtil;
import org.geopublishing.atlasViewer.dp.DpEntry;
import org.geopublishing.atlasViewer.exceptions.AtlasImportException;
import org.geopublishing.atlasViewer.swing.AVSwingUtil;
import org.geopublishing.geopublisher.DpEditableInterface;
import org.geopublishing.geopublisher.GpUtil;
import org.geopublishing.geopublisher.exceptions.AtlasImportCancelledException;
import org.geotools.data.DataUtilities;

import de.schmitzm.i18n.Translation;
import de.schmitzm.io.IOUtil;
import de.schmitzm.swing.TranslationAskJDialog;
import de.schmitzm.swing.TranslationEditJPanel;
import de.schmitzm.swing.swingworker.AtlasStatusDialog;
import de.schmitzm.swing.swingworker.AtlasSwingWorker;

public class DpeImportUtil {

	/**
	 * Before file(s) are being imported/copied into Geopublisher, we ask the
	 * user for their descriptions.
	 */
	public static void askTranslationsBeforeCopy(DpEntry<?> dpe, Component owner) {

		// ****************************************************************************
		// Edit the Title, Description and Keywords modally. If the user
		// cancels here, we don't even copy stuff over...
		// ****************************************************************************
		final List<String> langs = dpe.getAtlasConfig().getLanguages();
		if (dpe.getTitle().isEmpty())
			dpe.setTitle(new Translation(langs, dpe.getFilename()));
		// if (dpe.getDesc().isEmpty())
		// dpe.setDesc(new Translation());
		// if (dpe.getKeywords().isEmpty())
		// dpe.setKeywords(new Translation());

		TranslationAskJDialog translationAskJDialog = new TranslationAskJDialog(
				owner, new TranslationEditJPanel(
						GpUtil.R("EditDPEDialog.TranslateTitle"),
						dpe.getTitle(), langs), new TranslationEditJPanel(
						GpUtil.R("EditDPEDialog.TranslateDescription"),
						dpe.getDesc(), langs), new TranslationEditJPanel(
						GpUtil.R("EditDPEDialog.TranslateKeywords"),
						dpe.getKeywords(), langs));

		translationAskJDialog.setTitle(dpe.getFilename());
		translationAskJDialog.setModal(true);
		translationAskJDialog.setVisible(true);

		if (translationAskJDialog.isCancelled())
			throw new AtlasImportCancelledException();
	}

	public static void copyFilesWithOrWithoutGUI(
			final DpEditableInterface dped, final URL sourceUrl,
			final Component owner, final File targetDir)
			throws AtlasImportException {
		// Creating a dialog to show while copying data

		final Boolean guiInteraction = owner != null;

		final DpEntry<?> dpe = (DpEntry<?>) dped;
		if (guiInteraction) {

			DpeImportUtil.askTranslationsBeforeCopy(dpe, owner);

			final AtlasStatusDialog atlasStatusDialog = new AtlasStatusDialog(
					owner, GpCoreUtil.R("dialog.title.wait"), GpUtil.R(
							"ImportingDPE.StatusMessage", dpe.getTitle()
									.toString()));

			AtlasSwingWorker<Exception> copyWorker = new AtlasSwingWorker<Exception>(
					atlasStatusDialog) {

				@Override
				protected Exception doInBackground() throws Exception {
					try {
						doCopy(dped, sourceUrl, owner, targetDir, dpe,
								atlasStatusDialog);
					} catch (Exception e) {
						return e;
					}
					return null;
				}

			};

			Exception execEx;
			try {
				execEx = copyWorker.executeModal();
			} catch (Exception e) {
				execEx = e;
			}

			if (execEx != null) {
				if (execEx instanceof AtlasImportException)
					throw (AtlasImportException) execEx;
				else
					throw new AtlasImportException(execEx);
			}

		} else {
			// We are not doing GUI
			try {
				doCopy(dped, sourceUrl, owner, targetDir, dpe, null);
				// targetDir.mkdirs();
				// dped.copyFiles(sourceUrl, owner, targetDir, null);
			} catch (Exception e) {
				if (e instanceof AtlasImportException)
					throw (AtlasImportException) e;
				else
					throw new AtlasImportException(e);
			}
		}
	}

	private static void doCopy(final DpEditableInterface dped,
			final URL sourceUrl, final Component owner, final File targetDir,
			final DpEntry<?> dpe, final AtlasStatusDialog atlasStatusDialog)
			throws IOException, Exception {
		targetDir.mkdirs();

		AVSwingUtil.copyHTMLInfoFiles(atlasStatusDialog,
				DataUtilities.urlToFile(sourceUrl), dpe.getAtlasConfig(),
				targetDir, null);

		if (!targetDir.exists())
			throw new IOException("Couldn't create "
					+ IOUtil.escapePath(targetDir));

		// Calling the specific files for this DPEntry
		dped.copyFiles(sourceUrl, owner, targetDir, atlasStatusDialog);
	}

	public static void copyFilesWithOrWithoutGUI(DpEditableInterface dped,
			File sourceFile, Component owner, File targetDir) {
		copyFilesWithOrWithoutGUI(dped, DataUtilities.fileToURL(sourceFile),
				owner, targetDir);
	}

	/**
	 * Copies the source file to a temp-file and replaces all comata with
	 * periods while doing it. This is useful for ArcASCII files that have been
	 * exported by German ArcGIS.
	 * 
	 * @return a reference to a temp file without any comata. The file is
	 *         registered fo deletion when the JRE stops.
	 */
	public static File copyFileReplaceCommata(File source) throws IOException {
		return copyFileReplaceCommata(new FileInputStream(source),
				source.getName());
	}

	/**
	 * Copies the source file to a temp-file and replaces all comata with
	 * periods while doing it. This is useful for ArcASCII files that have been
	 * exported by German ArcGIS.
	 * 
	 * @return a reference to a temp file without any comata. The file is
	 *         registered fo deletion when the JRE stops.
	 */
	public static File copyFileReplaceCommata(URL source) throws IOException {
		return copyFileReplaceCommata(source.openStream(),
				new File(source.getFile()).getName());
	}

	/**
	 * Copies the source file to a temp-file and replaces all comata with
	 * periods while doing it. This is useful for ArcASCII files that have been
	 * exported by German ArcGIS.
	 * 
	 * @return a reference to a temp file without any comata. The file is
	 *         registered fo deletion when the JRE stops.
	 */
	public static File copyFileReplaceCommata(InputStream source,
			String fileName) throws IOException {
		File tempFile = new File(IOUtil.getTempDir(), fileName);

		if (tempFile.exists())
			tempFile.delete();

		try {
			OutputStream out = new FileOutputStream(tempFile);
			try {
				// Transfer bytes from in to out
				byte[] buf = new byte[2048];
				int len;
				while ((len = source.read(buf)) > 0) {

					for (int i = 0; i < 2048; i++) {
						if ((char) buf[i] == ',') {
							buf[i] = '.';
						}
					}
					out.write(buf, 0, len);
				}
			} finally {
				out.close();
			}
		} finally {
			source.close();
		}

		tempFile.deleteOnExit();

		return tempFile;
	}

}
