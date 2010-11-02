package org.geopublishing.atlasStyler.swing.importWizard;

import java.awt.Component;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JScrollPane;

import org.geopublishing.atlasStyler.AtlasStyler;
import org.geopublishing.atlasStyler.swing.AtlasStylerGUI;
import org.geopublishing.atlasViewer.swing.AVSwingUtil;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.netbeans.spi.wizard.DeferredWizardResult;
import org.netbeans.spi.wizard.ResultProgressHandle;
import org.netbeans.spi.wizard.Summary;
import org.netbeans.spi.wizard.WizardException;
import org.netbeans.spi.wizard.WizardPage.WizardResultProducer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import schmitzm.geotools.io.GeoImportUtil;
import schmitzm.io.IOUtil;
import schmitzm.swing.ExceptionDialog;
import skrueger.geotools.StyledFS;

public class ImportWizardResultProducer_FILE extends ImportWizardResultProducer
		implements WizardResultProducer {

	public ImportWizardResultProducer_FILE() {
		super();
	}

	@Override
	public Object finish(Map wizardData) throws WizardException {

		// Read stuff from the wizard map
		final String selectedFilePath = (String) wizardData
				.get(ImportWizard.IMPORT_FILE);

		final AtlasStylerGUI asg = (AtlasStylerGUI) wizardData
				.get(ImportWizard.ATLAS_STYLER_GUI);

		final File importFile = new File(selectedFilePath);

		/**
		 * Start the export as a DeferredWizardResult
		 */
		DeferredWizardResult result = new DeferredWizardResult(true) {

			private ResultProgressHandle progress;

			@Override
			public void start(Map wizardData, ResultProgressHandle progress) {
				this.progress = progress;
				try {
					long startTime = System.currentTimeMillis();
					progress.setBusy(importFile.getName());

					schmitzm.swing.SwingUtil.checkNotOnEDT();

					// boolean added = addShapeLayer(asg, new File(
					// selectedFilePath), asg);

					URL urlToShape;

					File openFile = new File(selectedFilePath);

					if (openFile.getName().toLowerCase().endsWith("zip")) {
						urlToShape = GeoImportUtil.uncompressShapeZip(openFile);
					} else {
						urlToShape = DataUtilities.fileToURL(openFile);
					}

					Map<Object, Object> params = new HashMap<Object, Object>();
					params.put("url", urlToShape);

					/*
					 * Test whether we have write permissions to create any .fix
					 * file
					 */
					if (!IOUtil.changeFileExt(openFile, "fix").canWrite()) {
						// If the file is not writable, we max not try to create
						// an
						// index. Even if the file already exists, it could be
						// that
						// the index has to be regenerated.
						params.put(
								ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key,
								Boolean.FALSE);
					}

					ShapefileDataStore dataStore = (ShapefileDataStore) DataStoreFinder
							.getDataStore(params);

					if (dataStore == null)
						throw new RuntimeException(
								"Could not read as ShapefileDataStore: "
										+ openFile.getAbsolutePath());

					Charset stringCharset = GeoImportUtil
							.readCharset(urlToShape);
					if (stringCharset != null)
						dataStore.setStringCharset(stringCharset);

					// test for any .prj file
					CoordinateReferenceSystem prjCRS = null;
					File prjFile = IOUtil.changeFileExt(openFile, "prj");
					if (prjFile.exists()) {
						try {
							prjCRS = GeoImportUtil.readProjectionFile(prjFile);
						} catch (Exception e) {
							prjCRS = null;
							if (!AVSwingUtil
									.askOKCancel(
											asg,
											AtlasStyler
													.R("AtlasStylerGUI.importShapePrjBrokenWillCreateDefaultFor",
															e.getMessage(),
															prjFile.getName(),
															GeoImportUtil
																	.getDefaultCRS()
																	.getName())))
								dataStore.dispose();
							abort();
						}
					} else {
						if (!AVSwingUtil
								.askOKCancel(
										asg,
										AtlasStyler
												.R("AtlasStylerGUI.importShapePrjNotFoundWillCreateDefaultFor",
														prjFile.getName(),
														GeoImportUtil
																.getDefaultCRS()
																.getName())))
							dataStore.dispose();
						abort();
					}

					if (prjCRS == null) {
						dataStore.forceSchemaCRS(GeoImportUtil.getDefaultCRS());
					}

					// After optionally forcing the CRS we get the FS
					FeatureSource<SimpleFeatureType, SimpleFeature> fs = dataStore
							.getFeatureSource(dataStore.getTypeNames()[0]);

					int countFeatures = countFeatures(fs, true);

					String id = urlToShape.toString();
					StyledFS styledFS = new StyledFS(fs, id);

					File sldFile = IOUtil.changeFileExt(openFile, "sld");

					File importedSld = setSldFileAndAskImportIfExists(asg,
							IOUtil.changeFileExt(openFile, "sld").getName(),
							styledFS, sldFile);

					asg.addOpenDatastore(styledFS.getId(), dataStore);

					boolean added = asg.addLayer(styledFS);

					if (added == false) {
						abort();
						return;
					}

					Summary summary = Summary.create(
							new JScrollPane(getSummaryPanel(startTime,
									countFeatures, styledFS, importedSld)),
							"ok");

					progress.finished(summary);
				} catch (Exception e) {
					progress.finished(Summary.create(getErrorPanel(e), "error"));
				}
			}

			/**
			 * If the user aborts the export, we tell it to JarImportUtil
			 * instance
			 */
			@Override
			public void abort() {
				// jarImportUtil.abort();
				progress.finished(getAbortSummary());
			};
		};

		return result;
	}

	/**
	 * Basic method to add a Shapefile to the legend/map
	 * 
	 * @param openFile
	 *            the file to open. May be a ZIP that contains a Shape.
	 */
	public static boolean addShapeLayer(Component owner, File openFile,
			AtlasStylerGUI asg) {
		try {
			URL urlToShape;

			if (openFile.getName().toLowerCase().endsWith("zip")) {
				urlToShape = GeoImportUtil.uncompressShapeZip(openFile);
			} else {
				urlToShape = DataUtilities.fileToURL(openFile);
			}

			Map<Object, Object> params = new HashMap<Object, Object>();
			params.put("url", urlToShape);

			/*
			 * Test whether we have write permissions to create any .fix file
			 */
			if (!IOUtil.changeFileExt(openFile, "fix").canWrite()) {
				// If the file is not writable, we max not try to create an
				// index. Even if the file already exists, it could be that
				// the index has to be regenerated.
				params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key,
						Boolean.FALSE);
			}

			ShapefileDataStore dataStore = (ShapefileDataStore) DataStoreFinder
					.getDataStore(params);

			if (dataStore == null)
				return false;

			try {

				Charset stringCharset = GeoImportUtil.readCharset(urlToShape);
				dataStore.setStringCharset(stringCharset);

				// test for any .prj file
				CoordinateReferenceSystem prjCRS = null;
				File prjFile = IOUtil.changeFileExt(openFile, "prj");
				if (prjFile.exists()) {
					try {
						prjCRS = GeoImportUtil.readProjectionFile(prjFile);
					} catch (Exception e) {
						prjCRS = null;
						if (!AVSwingUtil
								.askOKCancel(
										owner,
										AtlasStyler
												.R("AtlasStylerGUI.importShapePrjBrokenWillCreateDefaultFor",
														e.getMessage(),
														prjFile.getName(),
														GeoImportUtil
																.getDefaultCRS()
																.getName())))
							dataStore.dispose();
						return false;
					}
				} else {
					if (!AVSwingUtil
							.askOKCancel(
									owner,
									AtlasStyler
											.R("AtlasStylerGUI.importShapePrjNotFoundWillCreateDefaultFor",
													prjFile.getName(),
													GeoImportUtil
															.getDefaultCRS()
															.getName())))
						dataStore.dispose();
					return false;
				}

				if (prjCRS == null) {
					dataStore.forceSchemaCRS(GeoImportUtil.getDefaultCRS());
				}

				// After optionally forcing the CRS we get the FS
				FeatureSource<SimpleFeatureType, SimpleFeature> fs = dataStore
						.getFeatureSource(dataStore.getTypeNames()[0]);

				File sldFile = IOUtil.changeFileExt(openFile, "sld");

				// Handle if .SLD exists instead
				if (!sldFile.exists()
						&& IOUtil.changeFileExt(openFile, "SLD").exists()) {
					AVSwingUtil.showMessageDialog(owner,
							"Change the file ending to .sld and try again!"); // i8n
					return false;
				}

				StyledFS styledFS = new StyledFS(fs, sldFile,
						urlToShape.toString());

				asg.addOpenDatastore(styledFS.getId(), dataStore);

				return asg.addLayer(styledFS);

			} catch (Exception e) {
				dataStore.dispose();
				throw e;
			}

		} catch (Exception e2) {
			// LOGGER.info(e2);
			ExceptionDialog.show(owner, e2);
			return false;
		}

	}

}
