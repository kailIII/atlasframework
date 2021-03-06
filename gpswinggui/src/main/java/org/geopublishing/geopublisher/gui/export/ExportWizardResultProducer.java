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
package org.geopublishing.geopublisher.gui.export;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.geopublishing.atlasViewer.AtlasCancelException;
import org.geopublishing.atlasViewer.swing.AVSwingUtil;
import org.geopublishing.geopublisher.AtlasConfigEditable;
import org.geopublishing.geopublisher.GPProps;
import org.geopublishing.geopublisher.GPProps.Keys;
import org.geopublishing.geopublisher.export.GpHosterServerSettings;
import org.geopublishing.geopublisher.export.JarExportUtil;
import org.geopublishing.geopublisher.export.gphoster.GpFtpAtlasExport;
import org.geopublishing.geopublisher.export.gphoster.GpHosterClient;
import org.geopublishing.geopublisher.gui.settings.GpHosterServerList;
import org.geopublishing.geopublisher.swing.GeopublisherGUI;
import org.geopublishing.geopublisher.swing.GpSwingUtil;
import org.netbeans.spi.wizard.DeferredWizardResult;
import org.netbeans.spi.wizard.ResultProgressHandle;
import org.netbeans.spi.wizard.Summary;
import org.netbeans.spi.wizard.WizardException;
import org.netbeans.spi.wizard.WizardPage.WizardResultProducer;

import com.enterprisedt.net.ftp.FTPException;

import de.schmitzm.io.IOUtil;
import de.schmitzm.swing.ExceptionDialog;
import de.schmitzm.swing.SwingUtil;

/**
 * This class is using the values collected during the {@link ExportWizard} to export the {@link AtlasConfigEditable}.
 * 
 * 
 * 
 * @author Stefan A. Tzeggai
 */
public class ExportWizardResultProducer implements WizardResultProducer {

	private static final Logger LOGGER = Logger.getLogger(ExportWizardResultProducer.class);

	@Override
	public boolean cancel(Map settings) {
		return true;
	}

	@Override
	public Object finish(Map wizardData) throws WizardException {

		final AtlasConfigEditable ace = (AtlasConfigEditable) wizardData.get(ExportWizard.ACE);

		final Boolean isJws = (Boolean) wizardData.get(ExportWizard.JWS_CHECKBOX);
		final Boolean isFtp = (Boolean) wizardData.get(ExportWizard.FTP_CHECKBOX);
		final Boolean isDisk = (Boolean) wizardData.get(ExportWizard.DISK_CHECKBOX);
		final boolean isDiskZip = (Boolean) wizardData.get(ExportWizard.DISKZIP_CHECKBOX);
		final String exportDir = (String) wizardData.get(ExportWizard.EXPORTFOLDER);
		final Boolean copyJRE = (Boolean) wizardData.get(ExportWizard.COPYJRE);
		final Boolean isPublic = (Boolean) wizardData.get(ExportWizard.GpHosterAuth);
		final boolean gpHosterAuth = isPublic != null ? !isPublic : true;

		final GpHosterClient gphc = (GpHosterClient) wizardData.get(ExportWizard.GPHC);

		final String urlForAtlas = (String) wizardData.get(ExportWizardFTPBrancher.URL_FOR_ATLAS);

		/**
		 * Store stuff to the geopublisher.properties
		 */
		{
			if (isJws) {
				// GPProps.set(GPProps.Keys.jnlpURL, (String) wizardData
				// .get(ExportWizard.JNLPURL));
				ace.setJnlpBaseUrl((String) wizardData.get(ExportWizard.JNLPURL));
			}

			if (exportDir != null)
				GPProps.set(Keys.LastExportFolder, exportDir);
			GPProps.set(Keys.LastExportFtp, isFtp);

			GPProps.set(Keys.LastExportDisk, isDisk);
			GPProps.set(Keys.LastExportDiskZipped, isDiskZip);
			GPProps.set(Keys.LastExportJWS, isJws);

			if (isFtp) {
				// Weil jetzt eine GPHoster Verbindung erfolgreicht war, ist ein
				// guter zeitpunkt Benutzername und
				// Passwort zu speichern

				GpHosterServerList liste = new GpHosterServerList(GPProps.get(Keys.gpHosterServerList));
				for (GpHosterServerSettings s : liste) {
					if (gphc.getServerSettings().getAlias().equals(s.getAlias())
							&& gphc.getServerSettings().getRestUrl().equals(s.getRestUrl())) {
						s.setPassword(gphc.getPassword());
						s.setUsername(gphc.getUserName());
					}
				}
				GPProps.set(Keys.gpHosterServerList, liste.toPropertiesString());
			}

			// if (gphc.getUserName() != null)
			// GPProps.set(Keys.GPH_Username, gphc.getUserName());
			// if (gphc.getPassword() != null)
			// GPProps.set(Keys.GPH_Password, gphc.getPassword());

			GPProps.store();
		}

		ace.setGpHosterAuth(gpHosterAuth);

		if (!GpSwingUtil.save(ace, GeopublisherGUI.getInstance().getJFrame(), false))
			return null; // TODO what should be return here?

		/**
		 * Start the export as a DeferredWizardResult
		 */
		DeferredWizardResult result = new DeferredWizardResult(false, true) {

			private JarExportUtil jarExportUtil;
			private ResultProgressHandle progress;
			private GpFtpAtlasExport gpFtpAtlasExport;

			/**
			 * If the user aborts the export, we tell it to JarExportUtil instance
			 */
			@Override
			public void abort() {
				if (jarExportUtil != null)
					jarExportUtil.cancel.set(true);
				if (gpFtpAtlasExport != null)
					gpFtpAtlasExport.cancel.set(true);
			};

			@Override
			public void start(Map wizardData, ResultProgressHandle progress) {
				this.progress = progress;

				try {
					if (isFtp) {
						gpFtpAtlasExport = new GpFtpAtlasExport(ace, gphc, progress);
						gpFtpAtlasExport.export();
					}
					if (isDisk || isJws) {

						jarExportUtil = new JarExportUtil(ace, progress, new File(exportDir), isDisk, isJws, copyJRE);
						jarExportUtil.setZipDiskAfterExport(isDiskZip);
						jarExportUtil.export();
					}
				} catch (AtlasCancelException e) {
					LOGGER.info("Export aborted by user:", e);
					progress.finished(getAbortSummary());
					return;
				} catch (FTPException ftpe) {
					progress.failed(GeopublisherGUI.R("ExportWizard.Result.FTPException"), false);
					return;
				} catch (Exception e) {
					if (e.getMessage() != null && e.getMessage().contains("400")) {
						progress.failed(
								GeopublisherGUI.R("ExportWizard.Result.AtlasBelongsToAnotherUser", ace.getBaseName()),
								false);
					} else {
						exportFailed(progress, e);
					}
					return;
				}

				progress.finished(Summary.create(getSummaryJPanel(), null));
				// progress.finished(Summary.create(getSummaryJPanel(), new
				// File(
				// exportDir)));

			}

			private void exportFailed(ResultProgressHandle progress, Exception e) {
				LOGGER.error("Export failed!", e);
				progress.failed(e.getMessage(), false);
				// progress.finished(getErrorPanel(e));
				ExceptionDialog.show(null, e);
			}

			private JPanel getErrorPanel(Exception e) {
				JPanel panel = new JPanel(new MigLayout("wrap 1"));

				panel.add(new JTextArea(e.getLocalizedMessage()));

				return panel;
			}

			/**
			 * Generates the last WizardPage
			 */
			private JPanel getSummaryJPanel() {
				JPanel panel = new JPanel(new MigLayout("wrap 1"));

				if (exportDir != null) {
					// Es wurde nach DISK oder JWS exportiert

					String exportJWSandDISKdirRepresentation = IOUtil.escapePath(exportDir);

					panel.add(new JLabel(GeopublisherGUI.R("Export.Dialog.Finished.Msg",
							exportJWSandDISKdirRepresentation)));

					final JButton openFolderButton = new JButton(
							GeopublisherGUI.R("ExportWizard.Result.OpenFolderButton.Label"));
					openFolderButton.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							try {
								AVSwingUtil.lauchHTMLviewer(null, new URL(exportDir));
							} catch (MalformedURLException e1) {
								SwingUtil.openOSFolder(new File(exportDir));
							}
							openFolderButton.setEnabled(false);

							// TODO Here it would be nice to close the Wizard...
							// but
							// how??
						}

					});

					panel.add(openFolderButton, "align center");
				}

				if (isFtp) {
					panel.add(new JLabel(GeopublisherGUI.R("ExportWizard.Result.FtpUploadSuccess.Label")));

					final JButton openURLButton = new JButton(urlForAtlas);
					openURLButton.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							try {
								AVSwingUtil.lauchHTMLviewer(null, new URL(urlForAtlas));
							} catch (MalformedURLException e1) {
								ExceptionDialog.show(e1);
							}
							openURLButton.setEnabled(false);
						}

					});
					panel.add(openURLButton, "align center");
				}

				return panel;
			}
		};

		// HACK proper aborting:
		wizardData.put(ExportWizard.RESULTPRODUCER_WORKING, result);

		return result;
	}

	protected Summary getAbortSummary() {
		JPanel aborted = new JPanel(new MigLayout());
		aborted.add(new JLabel(GeopublisherGUI.R("ExportWizard.ResultProducer.ExportCanceled")));

		return Summary.create(aborted, null);
	}
}
