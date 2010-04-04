/*******************************************************************************
 * Copyright (c) 2010 Stefan A. Krüger (soon changing to Stefan A. Tzeggai).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan A. Krüger (soon changing to Stefan A. Tzeggai) - initial API and implementation
 ******************************************************************************/
package org.geopublishing.geopublisher.dp;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.geopublishing.atlasStyler.ASUtil;
import org.geopublishing.atlasViewer.AVUtil;
import org.geopublishing.atlasViewer.AtlasConfig;
import org.geopublishing.atlasViewer.AtlasStatusDialogInterface;
import org.geopublishing.atlasViewer.dp.layer.DpLayerVectorFeatureSource;
import org.geopublishing.atlasViewer.dp.layer.DpLayerVectorFeatureSourceShapefile;
import org.geopublishing.atlasViewer.exceptions.AtlasImportException;
import org.geopublishing.geopublisher.AtlasConfigEditable;
import org.geopublishing.geopublisher.DpEditableInterface;
import org.geopublishing.geopublisher.GpUtil;
import org.geopublishing.geopublisher.swing.GpSwingUtil;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.styling.Style;
import org.opengis.feature.type.GeometryDescriptor;

import schmitzm.geotools.io.GeoImportUtil;
import schmitzm.geotools.styling.StylingUtil;
import schmitzm.io.IOUtil;
import skrueger.geotools.StyledLayerUtil;

public class DpLayerVectorFeatureSourceShapefileEd extends
		DpLayerVectorFeatureSourceShapefile implements DpEditableInterface {

	static private final Logger LOGGER = Logger
			.getLogger(DpLayerVectorFeatureSourceShapefileEd.class);

	/**
	 * Constructs a new {@link DpLayerVectorFeatureSource} and copies the given
	 * file and its associates to the ad/data/id - dir
	 * 
	 * @param guiInteraction
	 *            Use GUI to ask user or continue with default values
	 * @throws Exception
	 */
	public DpLayerVectorFeatureSourceShapefileEd(AtlasConfig ac, URL url,
			Component owner) throws AtlasImportException {
		super(ac);

		/**
		 * The target directory where all the files go to.
		 */
		File dataDir = null;

		try {

			// The file that has been selected for import will be the
			// "filename".. IF the user accepts any changes to clean the name!
			// Otherwise an AtlasImportException is thrown
			final String name = GpSwingUtil.cleanFilenameWithUI(owner, new File(url
					.toURI()).getName());

			setFilename(name);

			/**
			 * Base name is cities if URL points to cities.gml or cities.shp
			 */
			// final String basename = name.substring(0, name.lastIndexOf("."));
			// LOGGER.debug("Basename is " + basename);

			// Set a directory
			setId(GpUtil.getRandomID("vector"));

			String dirname = getId()
					+ "_"
					+ getFilename()
							.substring(0, getFilename().lastIndexOf('.'));

			// setTitle(new Translation(getAc().getLanguages(), getFilename()));
			// setDesc(new Translation());

			setDataDirname(dirname);

			// Create sub directory to hold data, called the dataDirectory
			dataDir = new File(getAce().getDataDir(), dirname);
			dataDir.mkdirs();
			if (!dataDir.exists())
				throw new IOException("Couldn't create "
						+ dataDir.getAbsolutePath());

			DpeImportUtil.copyFilesWithOrWithoutGUI(this, url, owner, dataDir);

		} catch (Exception e) {
			// In case of any Exception, lets delete the datadir we just
			// created.
			if (dataDir != null) {
				try {
					FileUtils.deleteDirectory(dataDir);
				} catch (IOException e1) {
					LOGGER.error("Deleting the dataDir " + dataDir
							+ " (beacause the import failed) failed:", e);
				}
			}

			if (!(e instanceof AtlasImportException)) {
				LOGGER.error(e);
				throw new AtlasImportException(e);
			} else
				throw (AtlasImportException) e;

		}
	}

	@Override
	public void copyFiles(URL urlToShape, Component owner, File targetDir,
			AtlasStatusDialogInterface status) throws URISyntaxException, IOException,
			TransformerException {

		if (urlToShape.getFile().toLowerCase().endsWith("zip")) {
			// Falls es sich um eine .ZIP datei handelt, wird sie entpackt. 
			urlToShape = GeoImportUtil.uncompressShapeZip(urlToShape.openStream());
		} 
		
		/**
		 * Getting a DataStore for the VectorLayer
		 */
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("url", urlToShape);
		DataStore dataStore = DataStoreFinder.getDataStore(params);

		try {

			if (dataStore instanceof ShapefileDataStore) {
//				ShapefileDataStore shapefileDS = (ShapefileDataStore) dataStore;

				/*******************************************************************
				 * Now copy all the files that belong to ESRI Shapefile
				 ******************************************************************/

				/**
				 * Copy projection-file and deal with upper-case/lower-case
				 * extensions
				 */
				URL prjURL = IOUtil.changeUrlExt(urlToShape, "prj");

				// final File prjFile = new File(prjFilename);
				try {
					AVUtil.copyUrl(prjURL, targetDir, true);
				} catch (FileNotFoundException e) {
					LOGGER.debug(prjURL
							+ " not found, trying with capital '.PRJ'");

					try {
						prjURL = IOUtil.changeUrlExt(urlToShape, "PRJ");

						// Creating a destination File with small ending!
						final String basename = getFilename().substring(0,
								getFilename().lastIndexOf("."));
						AVUtil.copyUrl(prjURL, new File(targetDir, basename
								+ ".prj"), true);
					} catch (FileNotFoundException e2) {
						LOGGER
								.debug("No .prj or .PRJ file for Shapefile found.");

						// Ask the user what to do, unless we run in
						// automatic
						// mode
						// int importAsDefaultCRS;

						if (status != null) {
							// We have a modal atlas status dialog open. Just
							// write a warning to the status dialog.
							status.warningOccurred(getFilename(), "",
									GpUtil.R(
											"DpVector.Import.NoCRS.WarningMsg",
											GeoImportUtil.getDefaultCRS()
													.getName()));
							// importAsDefaultCRS = JOptionPane.YES_OPTION;
						}
						// else if (owner != null && status == null) {
						//
						// importAsDefaultCRS = JOptionPane
						// .showConfirmDialog(
						// owner,
						// AtlasCreator
						// .R(
						// "DpVector.Import.NoCRS.QuestionUseDefaultOrCancel",
						// GeoImportUtil
						// .getDefaultCRS()
						// .getName()),
						// AtlasCreator
						// .R("DpVector.Import.NoCRS.Title"),
						// JOptionPane.YES_NO_OPTION);
						// } else
						// importAsDefaultCRS = JOptionPane.YES_OPTION;
						//
						// if (importAsDefaultCRS == JOptionPane.YES_OPTION) {
						// Force CRS (which creates a .prj and copy it
						// shapefileDS.forceSchemaCRS(GeoImportUtil
						// .getDefaultCRS());
						// prjURL = IOUtil.changeUrlExt(fromUrl, "prj");

						// AVUtil.copyUrl(prjURL, targetDir, true);

						/*
						 * Force Schema created a .prj file in the source
						 * folder. If shall be deleted after copy. TODO There
						 * will be a problem if the source is read-only
						 */
						// IOUtil.urlToFile(prjURL).delete();
						// } else
						// throw (new AtlasException(AtlasCreator
						// .R("DpVector.Import.NoCRS.CanceledMsg")));
					}

				}

				/**
				 * Copy main SHP file!
				 */
				final URL shpURL = IOUtil.changeUrlExt(urlToShape, "shp");
				AVUtil.copyUrl(shpURL, targetDir, true);

				final URL shxURL = IOUtil.changeUrlExt(urlToShape, "shx");
				AVUtil.copyURLNoException(shxURL, targetDir, true);

				final URL grxURL = IOUtil.changeUrlExt(urlToShape, "grx");
				AVUtil.copyURLNoException(grxURL, targetDir, true);

				final URL fixURL = IOUtil.changeUrlExt(urlToShape, "fix");
				AVUtil.copyURLNoException(fixURL, targetDir, true);

				final URL qixURL = IOUtil.changeUrlExt(urlToShape, "qix");
				AVUtil.copyURLNoException(qixURL, targetDir, true);

				final URL xmlURL = IOUtil.changeUrlExt(urlToShape, "shp.xml");
				AVUtil.copyURLNoException(xmlURL, targetDir, true);

				final URL dbfURL = IOUtil.changeUrlExt(urlToShape, "dbf");
				AVUtil.copyURLNoException(dbfURL, targetDir, true);

				/**
				 * Optionally copy a .cpg file that describes the
				 */
				final URL cpgURL = IOUtil.changeUrlExt(urlToShape, "cpg");
				AVUtil.copyURLNoException(cpgURL, targetDir, true);

				/**
				 * Try to copy an attached .sld / .SLD files or create a default
				 * Style.
				 * 
				 * 1. Copy the SLD. Check the SLD. If no SLD is provided, then
				 * create a dummy SLD.
				 */
				try {
					try {

						AVUtil.copyUrl(IOUtil.changeUrlExt(urlToShape, "sld"),
								targetDir, true);
					} catch (Exception e) {
						AVUtil.copyUrl(IOUtil.changeUrlExt(urlToShape, "SLD"),
								targetDir, true);
					}
				} catch (Exception e) {

					GeometryDescriptor geometryType;

					geometryType = getDefaultGeometry();
					Style defaultStyle = ASUtil.createDefaultStyle(this);
					File changeFileExt = IOUtil.changeFileExt(new File(
							targetDir + "/" + getFilename()), "sld");
					StylingUtil.saveStyleToSLD(defaultStyle, changeFileExt);

				}
				//
				// // // TODO is that a good idea? What if we do not have a .prj
				// // // file?mmm
				// // // then the froced CRS should be returned..
				// crs = shapefileDS.getFeatureSource().getSchema()
				// .getGeometryDescriptor().getCoordinateReferenceSystem();

				// Add the empty string as a default NODATA-Value to all textual
				// layers
				StyledLayerUtil.addEmptyStringToAllTextualAttributes(
						getAttributeMetaDataMap(), getSchema());

			} else {
				throw new AtlasImportException(
						"dataStore was not of type Shapefile, ignoring it for now...");
			}

		} finally {
			if (dataStore != null)
				dataStore.dispose();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see skrueger.creator.dp.DatapoolEditableInterface#getAce()
	 */
	public AtlasConfigEditable getAce() {
		return (AtlasConfigEditable) getAtlasConfig();
	}

}