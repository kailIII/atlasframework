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
import java.io.IOException;

import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.geopublishing.atlasViewer.dp.DpEntryType;
import org.geopublishing.atlasViewer.dp.layer.DpLayerRaster;
import org.geopublishing.atlasViewer.exceptions.AtlasImportException;
import org.geopublishing.geopublisher.AtlasConfigEditable;
import org.geotools.data.DataUtilities;

import schmitzm.geotools.io.GeoImportUtil;
import schmitzm.geotools.io.GeoImportUtil.ARCASCII_POSTFIXES;
import schmitzm.swing.ExceptionDialog;

/**
 * A tester class that can determine, if a file is importable as a
 * {@link DpLayerRaster}
 * 
 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
 */
public class DpLayerRasterArcASCIITester implements DpEntryTesterInterface {
	Logger log = Logger.getLogger(DpLayerRasterArcASCIITester.class);

	public static final FileFilter FILEFILTER = new FileFilter() {

		@Override
		public boolean accept(File f) {

			if (f.isDirectory())
				return true;

			String filename = f.getName();

		
			// ****************************************************************************
			// Check if the ending suggests an Arc/Info ASCII Grid
			// ****************************************************************************
			for (ARCASCII_POSTFIXES ending : GeoImportUtil.ARCASCII_POSTFIXES
					.values()) {
				if (filename.endsWith(ending.toString())) {
					return true;
				}
			}

			return false;
		}

		@Override
		public String getDescription() {
			return DpEntryType.RASTER_ARCASCII.getDesc();
		}

	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see skrueger.creator.DatapoolEntryTester#test(java.io.File)
	 */
	public boolean test(Component owner, File file) {
		String filename = file.getName().toLowerCase();

		// ****************************************************************************
		// Check if the ending suggests an Arc/Info ASCII Grid
		// ****************************************************************************
		for (ARCASCII_POSTFIXES ending : GeoImportUtil.ARCASCII_POSTFIXES
				.values()) {
			if (filename.endsWith(ending.toString())) {
				try {
					GeoImportUtil.readGridFromArcInfoASCII(DataUtilities
							.fileToURL(file));
					return true;
				} catch (IOException e) {

					try {
						// Here we test, whether the header of the file
						// contains the
						// comma "," char. This happens if ArcGIS exported
						// the
						// ArcASCII file on a German computer and is not
						// compatible
						// with GT ArcASCII importer! GP will create a
						// corrected copy of the file in /tmp and import it
						// from there.
						// Copies src file to dst file. // If the dst file
						// does not exist, it is created
						if (e.getMessage()
								.equals("Expected new line, not null")) {

							File tempFile = DpeImportUtil
									.copyFileReplaceCommata(file);

							// Try again
							GeoImportUtil
									.readGridFromArcInfoASCII(DataUtilities
											.fileToURL(tempFile));
							return true;
						}

					} catch (Exception ee) {
						ExceptionDialog.show(owner, ee);
						return false;
					}
				} catch (Exception e) {
					ExceptionDialog.show(owner, e);
					return false;
				}
			}
		}

		return false;
	}

	public DpLayerRasterEd_ArcASCII create(AtlasConfigEditable ac, File f,
			Component owner) throws AtlasImportException {
		// TODO Here we need to create an ArcASCIIEd instance
		return new DpLayerRasterEd_ArcASCII(owner, ac, f);
	}

}
