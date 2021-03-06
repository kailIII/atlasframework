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
package org.geopublishing.atlasViewer.swing;

import java.awt.Dialog.ModalityType;
import java.awt.Frame;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.apache.log4j.Logger;
import org.geopublishing.atlasStyler.swing.StylerDialog;
import org.geopublishing.atlasViewer.AtlasConfig;
import org.geopublishing.atlasViewer.ExportableLayer;
import org.geopublishing.atlasViewer.GpCoreUtil;
import org.geopublishing.atlasViewer.dp.layer.DpLayer;
import org.geopublishing.atlasViewer.dp.layer.DpLayerRaster;
import org.geopublishing.atlasViewer.dp.layer.DpLayerVectorFeatureSource;
import org.geopublishing.atlasViewer.dp.layer.LayerStyle;
import org.geopublishing.atlasViewer.map.Map;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.map.MapContext;
import org.geotools.map.MapLayer;
import org.geotools.styling.Style;
import org.opengis.filter.Filter;

import de.schmitzm.geotools.gui.FeatureLayerFilterDialog;
import de.schmitzm.geotools.styling.StyledFeatureCollectionInterface;
import de.schmitzm.geotools.styling.StyledFeaturesInterface;
import de.schmitzm.geotools.styling.StyledLayerInterface;
import de.schmitzm.geotools.styling.StyledLayerUtil;
import de.schmitzm.jfree.chart.style.ChartStyle;
import de.schmitzm.lang.LangUtil;
import de.schmitzm.swing.SwingUtil;

/**
 * An extension to an ordinary {@link MapLayerLegend}. The difference is, that
 * {@link MapLayerLegend} works on {@link StyledLayerInterface} objects, and
 * {@link AtlasMapLayerLegend} works on {@link DpLayer DpLayers} and has a
 * reference to a {@link Map}. {@link DpLayer}. The class also extends the
 * {@link JPopupMenu} with Atlas specific {@link JMenuItem}s.
 * 
 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
 */
public class AtlasMapLayerLegend extends MapLayerLegend {

	final private Logger LOGGER = LangUtil.createLogger(this);

	protected final DpLayer<?, ? extends ChartStyle> dpLayer;

	/**
	 * A reference to the {@link AtlasMapLegend} panel that contains
	 * {@link AtlasMapLayerLegend AtlasMapLayerLegends} for all {@link MapLayer}
	 * s in the {@link MapContext}.
	 */
	protected final AtlasMapLegend atlasMapLegend;

	/**
	 * A reference to the {@link Map} in which this {@link DpLayer} is
	 * represented at the moment.
	 */
	protected final Map map;

	/**
	 * Returns true, is the layer has visible attributes defined
	 * 
	 * @return
	 */
	protected boolean hasVisibleAttributes() {
		return (dpLayer instanceof StyledFeatureCollectionInterface && StyledLayerUtil
				.getVisibleAttributeMetaData(
						((StyledFeatureCollectionInterface) styledLayer)
								.getAttributeMetaDataMap(),
						true).size() > 0)
				|| (dpLayer instanceof DpLayerVectorFeatureSource && ((DpLayerVectorFeatureSource) styledLayer)
						.getAttributeMetaDataMap().sortedValuesVisibleOnly()
						.size() > 0);
	}

	/**
	 * Returns true, is the layer has visible attributes defined or is of atlastype
	 * grid. When this method returns <code>true</code>, it makes sense to show
	 * clickInfo for this layer.
	 */
	protected boolean hasVisibleAttributesOrIsGrid() {
		return dpLayer instanceof DpLayerRaster || hasVisibleAttributes();
	}

	/**
	 * Extends the {@link JPopupMenu} with Atlas specific {@link JMenuItem}s. In
	 * extension to an ordinary {@link MapLayerLegend}, this holds a link to a
	 * {@link DpLayer}
	 * 
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons
	 *         Tzeggai</a>e
	 * 
	 * @param mapLayer
	 *            {@link MapLayer}
	 * @param exportable
	 *            {@link ExportableLayer} may be <code>null</code>
	 *            // * @param styleObj {@link StyledLayerInterface}
	 * @param atlasMapLegend
	 *            {@link AtlasMapLegend} that contains this
	 *            {@link AtlasMapLayerLegend}
	 * @param dpLayer
	 *            {@link DpLayer}
	 * @param map
	 *            {@link Map}
	 */
	public AtlasMapLayerLegend(MapLayer mapLayer, ExportableLayer exportable,
			AtlasMapLegend atlasMapLegend, DpLayer<?, ChartStyle> dpLayer,
			Map map) {

		super(mapLayer, exportable, dpLayer, atlasMapLegend);

		this.map = map;

		this.dpLayer = dpLayer;

		this.atlasMapLegend = atlasMapLegend;

		/**
		 * Check if the LayerPaneGroup should come up minimized
		 */
		Boolean minimized = map.getMinimizedInLegendMap().get(dpLayer.getId());
		setCollapsed(minimized != null ? minimized : false);

		// See whether we have to replace the existing legend panel..
		java.util.Map<String, ArrayList<String>> additionalStyles = map
				.getAdditionalStyles();
		final ArrayList<String> availableStyles = additionalStyles
				.get(styledLayer.getId());
		if (availableStyles == null || availableStyles.size() == 0) {
			// All is good.. the default style/legend panel is right
			return;
		}

		/**
		 * Once we are here, we can be sure that we do have at least one
		 * additional style!<br/>
		 * If there is only one additional (active) style for this layer (in
		 * this map), we do not render a JTabbedPane. The Title of the only
		 * additional style is not displayed. If there are more than 5
		 * additional styles, we render a DropDown component.
		 */
		removeAll();

		add(SldLegendUtil.createAdditionalStylesPane(mapLayer, availableStyles,
				(DpLayer<?, ? extends ChartStyle>) styledLayer, map,
				atlasMapLegend, mapLegend.getGeoMapPane().getMapPane()
						.getScaleDenominator()));

		// Update the style in the MapLayer if needed and keep any selection FTS
		LayerStyle selectedLayerStyle = dpLayer.getLayerStyleByID(map
				.getSelectedStyleID(dpLayer.getId()));

		if (selectedLayerStyle == null) {
			// The selected layer has probably been removed completely.
			// Select the first one instead
			map.setSelectedStyleID(dpLayer.getId(), availableStyles.get(0));
			selectedLayerStyle = dpLayer.getLayerStyleByID(map
					.getSelectedStyleID(dpLayer.getId()));
		}

		StyledLayerUtil.updateMapLayerStyleIfChangedAndKeepSelection(mapLayer,
				selectedLayerStyle.getStyle());
	}

	@Override
	public JPopupMenu getToolMenu() {
		JPopupMenu menu = super.getToolMenu();
		//
		// //
		// ****************************************************************************
		// // Create AtlasStylerRaster Button
		// //
		// ****************************************************************************
		// if (styledLayer instanceof StyledRasterInterface<?>
		// && isStyleEditable()) {
		//
		// menu.add(new JMenuItem(new AbstractAction(AtlasViewerGUI
		// .R("LayerToolMenu.style"),
		// Icons.ICON_STYLE) {
		//
		// @Override
		// public void actionPerformed(ActionEvent e) {
		//
		// AVDialogManager.dm_AtlasStyler.getInstanceFor(
		// styledLayer, AtlasMapLayerLegend.this, styledLayer,
		// AtlasMapLayerLegend.this);
		// }
		// }));
		// }

		return menu;
	}

	/**
	 * Allows access to the {@link Map} this layer is part of.
	 */
	public Map getMap() {
		return map;
	}

	/**
	 * Remembers the given "minimized" state in the map.
	 */
	public void setMinimized(boolean b) {
		map.getMinimizedInLegendMap().put(dpLayer.getId(), b);
	}

	/**
	 * Allows access to the {@link AtlasMapLegend} containing this
	 * {@link AtlasMapLayerLegend}.
	 */
	public AtlasMapLegend getAtlasMapLegend() {
		return atlasMapLegend;
	}

	/**
	 * Opens an {@link StylerDialog} which allows to change the selected
	 * (Additional-)Style of the layer.
	 * 
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
	 */
	@Override
	public StylerDialog openStylerDialog() {
		LayerStyle layerStyle = map.getSelectedStyle(dpLayer.getId());
		Object key = layerStyle == null ? dpLayer : layerStyle;
		StylerDialog asDialog = AVDialogManager.dm_AtlasStyler.getInstanceFor(
				key, mapLegend, styledLayer, mapLegend, getMapLayer(),
				layerStyle);

		return asDialog;
	}

	/**
	 * @return <code>true</code> if the Datatable for this layer may be viewed
	 *         from within the AV.
	 */
	@Override
	public boolean isTableViewable() {
		return super.isTableViewable() && dpLayer.isTableVisibleInLegend();
	}

	/**
	 * @return <code>true</code> if the {@link Style} is possible to be edited
	 *         with the AS from the legend's tool menu.
	 */
	@Override
	public boolean isStyleEditable() {
		return super.isStyleEditable() && dpLayer.isStylerInLegend();
	}

	/**
	 * @return <code>true</code> if the {@link MapLayer} is possible to be
	 *         filtered with {@link FeatureLayerFilterDialog}
	 */
	@Override
	public boolean isFilterable() {
		return super.isFilterable() & dpLayer.isFilterInLegend();
	}

	/**
	 * Removes the {@link Query} that filters this {@link FeatureCollection} by
	 * setting the {@link Query} to {@link Query}.ALL
	 */
	@Override
	public void removeFilter() {
		super.removeFilter();
		((StyledFeaturesInterface) styledLayer).setFilter(Filter.INCLUDE);
	}

	/**
	 * The HTMLBrowserWindow has to be initialized with an {@link AtlasConfig}
	 * not <code>null</code> to support map://mapID links.
	 */
	@Override
	protected HTMLBrowserWindow getHTMLBrowserWindow() {
		final String titleText = GpCoreUtil.R(
				"LayerPaneGroup.ClickedInfoButton.information_about",
				getTitle());

		return new HTMLBrowserWindow(AtlasMapLayerLegend.this, getInfoURL(),
				titleText, map.getAc());
	}

	@Override
	public void updateStyle(Style style) {
		super.updateStyle(style);
		atlasMapLegend.showOrHideSearchButton();
	}
	
	/**
	 * Override to implement terms of use
	 */
	@Override
	public void export() {
		if (exportable == null)
			return;
		final Frame owner = SwingUtil.getParentFrame(this);
		try {
			if (map.getAc().getTermsOfUseHTMLURL() != null) {
				AtlasTermsOfUseDialog aboutWindow = new AtlasTermsOfUseDialog(
						owner, map.getAc(), ModalityType.APPLICATION_MODAL);
				if(!aboutWindow.isAccepted()){
					AVSwingUtil.showMessageDialog(owner,
							AvUtil.R("AcceptTermsOfUse"));
					return;
				}
			}
			exportable.exportWithGUI(owner);
		} catch (final IOException e) {
			final String msg = GpCoreUtil
					.R("LayerPaneGroup.JOptionPane.ShowMessageDialog.export_failed");
			JOptionPane.showMessageDialog(owner, msg); // i8ndone
		}
	}

	/**
	 * If returns <code>true</code>, the layer will not be part of the legend if
	 * displayed within the atlas.
	 */
	public boolean isHiddenInLegend() {
		if (atlasMapLegend != null && atlasMapLegend.getMap() != null
				&& atlasMapLegend.getMap().getHideInLegendMap() != null) {
			Boolean hidden = atlasMapLegend.getMap().getHideInLegendMap()
					.get(dpLayer.getId());
			if (hidden == null)
				return false;
			return hidden;
		} else
			return false;
	}

	/**
	 * Change the visibility of the associated {@link MapLayer} (on/off)
	 */
	@Override
	public boolean toggleVisibility() {
		final boolean b = super.toggleVisibility();
		atlasMapLegend.showOrHideSearchButton();
		return b;
	}
}
