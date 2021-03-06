/*******************************************************************************
 * Copyright (c) 2010 Stefan A. Tzeggai.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan A. Tzeggai - initial API and implementation
 ******************************************************************************/
package org.geopublishing.atlasViewer.dp.layer;

import java.net.URL;

import org.geopublishing.atlasViewer.AtlasConfig;
import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import de.schmitzm.jfree.feature.style.FeatureChartStyle;

public class DpLayerVectorFeatureSourceWFS extends DpLayerVectorFeatureSource {

	public DpLayerVectorFeatureSourceWFS(AtlasConfig ac) {
		super(ac);
		setId("wfs test layer");
	}

	@Override
	public URL getInfoURL() {
		return null;
	}

	@Override
	public FeatureSource<SimpleFeatureType, SimpleFeature> getGeoObject() {
		throw new IllegalAccessError("Implementation for WFS pending!");
	}

	@Override
	public DpLayer<FeatureSource<SimpleFeatureType, SimpleFeature>, FeatureChartStyle> copy() {
		DpLayerVectorFeatureSourceWFS copy = new DpLayerVectorFeatureSourceWFS(
				ac);
		return copyTo(copy);
	}

}
