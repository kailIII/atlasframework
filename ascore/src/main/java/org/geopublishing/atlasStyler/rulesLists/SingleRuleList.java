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
package org.geopublishing.atlasStyler.rulesLists;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.geopublishing.atlasStyler.ASUtil;
import org.geopublishing.atlasStyler.AtlasStylerVector;
import org.geopublishing.atlasStyler.RuleChangeListener;
import org.geopublishing.atlasStyler.RuleChangedEvent;
import org.geotools.styling.Description;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Function;
import org.opengis.util.InternationalString;

import de.schmitzm.data.Copyable;
import de.schmitzm.geotools.FilterUtil;
import de.schmitzm.geotools.LegendIconFeatureRenderer;
import de.schmitzm.geotools.feature.FeatureUtil;
import de.schmitzm.geotools.feature.FeatureUtil.GeometryForm;
import de.schmitzm.geotools.styling.StylingUtil;
import de.schmitzm.geotools.styling.chartsymbols.ChartGraphicPreviewFixStyleVisitor;
import de.schmitzm.i18n.Translation;
import de.schmitzm.lang.LangUtil;

public abstract class SingleRuleList<SymbolizerType extends Symbolizer> extends
		AbstractRulesList implements Copyable<SingleRuleList<SymbolizerType>> {
	final static protected Logger LOGGER = LangUtil
			.createLogger(SingleRuleList.class);

	private String label = "title missing";

	/**
	 * Intended use: Set true if you want a point symbolizer on a multi-polygon
	 * geometry to paint only the centroid, not the center of each polygon.
	 */
	private boolean useCentroidFunction = false;

	/**
	 * This {@link Vector} represents a list of all {@link Symbolizer}s that
	 * will be used to paint the symbols
	 */
	protected Vector<SymbolizerType> layers = new Vector<SymbolizerType>();

	private String styleAbstract;

	private String styleName;

	private String styleTitle;

	/**
	 * This boolean defines whether the entry shall be shown the legend. <b>This
	 * is only interpreted in GP/Atlas context.</b>
	 */
	private boolean visibleInLegend = true;

	/**
	 * @param label
	 *            label for the rule
	 */
	public SingleRuleList(RulesListType rulesListType, String label,
			GeometryForm geometryForm) {
		super(rulesListType, geometryForm);
		pushQuite();
		setLabel(label);
		popQuite();
	}

	/**
	 * @param label
	 *            label for the rule
	 */
	public SingleRuleList(RulesListType rulesListType, Translation label,
			GeometryForm geometryForm) {
		super(rulesListType, geometryForm);
		setRuleTitle(label);
	}

	private void addFilters(Rule rule) {
		Filter filter = FilterUtil.ALLWAYS_TRUE_FILTER;

		// The order is important! This is parsed the reverse way. The last
		// thing added to the filter equals the first level in the XML.
		filter = addAbstractRlSettings(filter);

		rule.setFilter(filter);

	}

	/**
	 * Creates a new Symbolizer and adds it to the layers. This fires an event
	 * to all {@link RuleChangeListener}s.
	 */
	public abstract void addNewDefaultLayer();

	/**
	 * Adds a symbolizer to the {@link SingleRuleList}
	 * 
	 * @param symbolizer
	 *            The symbolizer to add.
	 */
	public boolean addSymbolizer(Symbolizer symbolizer) {
		boolean add = layers.add((SymbolizerType) symbolizer);
		if (add)
			fireEvents(new RuleChangedEvent("Added a Symbolizer", this));
		return add;
	}

	/**
	 * Adds a {@link List} of symbolizers to the {@link SingleRuleList}
	 * 
	 * @param symbolizer
	 *            The symbolizers to add.
	 */
	public boolean addSymbolizers(List<? extends Symbolizer> symbolizers) {

		// Wenn einer der Symbolizer die Geometrie mit einer
		// "centroid"-Funktion versehen hat, dann wird das bemerkt.
		// https://trac.wikisquare.de/gp/ticket/81
		for (Symbolizer s : symbolizers) {
			if (s.getGeometry() != null && s.getGeometry() instanceof Function) {
				Function f = (Function) s.getGeometry();
				if (f.getName() != null
						&& f.getName().equalsIgnoreCase("centroid")) {
					setUseCentroidFunction(true);
					continue;
				}
			}
		}

		boolean add = layers
				.addAll((Collection<? extends SymbolizerType>) symbolizers);
		
		if (add)
			fireEvents(new RuleChangedEvent("Added " + symbolizers.size()
					+ " symbolizers", this));
		return add;
	}

	/**
	 * Implementing the {@link Cloneable} interface, this method overwrites the
	 * {@link Object}'s clone method.
	 * 
	 * @param copyListeners
	 *            If <code>true</code> the listeners are copied, too. They are
	 *            not cloned. If <code>false</code> they are ignored (e.g. left
	 *            behind)
	 * 
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
	 */
	public abstract SingleRuleList<? extends SymbolizerType> clone(
			boolean copyListeners);

	/**
	 * Copies all values from the first {@link SinglePointSymbolRuleList} to the
	 * second {@link SinglePointSymbolRuleList}. The {@link RuleChangeListener}s
	 * are not changed. The {@link RuleChangeListener}s of the second
	 * {@link SinglePointSymbolRuleList} are fired afterwards.
	 * 
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
	 */
	@Override
	public SingleRuleList<SymbolizerType> copyTo(SingleRuleList to) {

		to.pushQuite();

		to.getSymbolizers().clear();

		to.setVisibleInLegend(isVisibleInLegend());

		// Wrong: This did't make a deep copy!
		// for (SymbolizerType ps : getSymbolizers()) {
		// to.addSymbolizer(ps);
		// }

		// Thats better:
		for (SymbolizerType ps : getSymbolizers()) {
			final DuplicatingStyleVisitor duplicatingStyleVisitor = new DuplicatingStyleVisitor();
			duplicatingStyleVisitor.visit(ps);
			SymbolizerType ps2 = (SymbolizerType) duplicatingStyleVisitor
					.getCopy();
			to.addSymbolizer(ps2);
		}

		to.setStyleAbstract(getStyleAbstract());
		to.setStyleName(getStyleName());
		to.setStyleTitle(getStyleTitle());

		to.setTitle(getTitle());
		to.setLabel(getLabel());

		to.setMinScaleDenominator(getMinScaleDenominator());
		to.setMaxScaleDenominator(getMaxScaleDenominator());

		to.popQuite();
		return to;
	}

	/**
	 * The {@link Color} returned by {@link #getColor()} is replaced against the
	 * given color parameter. Any other occurrence of the original color will
	 * also be replaced.
	 * 
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
	 */
	abstract public Color getColor();

	/**
	 * @return The {@link GeometryDescriptor} that this symbolization rules
	 *         works on.
	 */
	public abstract GeometryDescriptor getGeometryDescriptor();

	/**
	 * Return a {@link BufferedImage} that represent the symbol with default
	 * dimensions.
	 * 
	 * @return {@link BufferedImage} representing this SinglesRulesList
	 */
	public BufferedImage getImage() {
		return getImage(AtlasStylerVector.DEFAULT_SYMBOL_PREVIEW_SIZE);
	}

	/**
	 * Return a {@link BufferedImage} that represent the symbol.
	 * 
	 * @param size
	 *            Width and Height of the {@link BufferedImage} to create
	 */
	public BufferedImage getImage(Dimension size) {

		Rule rule = getRule();

		// Since this rule might well contain any ChartSymbols (which can not be
		// previewed without modification) we have to check all Symbolizers and
		// change any Chart-Symbolizers for proper preview.
		DuplicatingStyleVisitor sv = new ChartGraphicPreviewFixStyleVisitor();
		sv.visit(rule);
		rule = (Rule) sv.getCopy();

		BufferedImage image = LegendIconFeatureRenderer
				.getInstance()
				.createImageForRule(rule,
						ASUtil.createFeatureType(getGeometryDescriptor()), size);

		return image;
	}

	/**
	 * @return The title of the first and only {@link Rule}. This is used as the
	 *         label for this rule in the legend. This max return a
	 *         "oneLineCoded" {@link Translation} if running in GP.
	 */
	public String getLabel() {
		return label;
	}

	/** Returns a description or the type of the {@link Symbolizer} */
	public abstract String getLayerTypeDesc(int idx);

	/** returns the Rotation if it makes sense or null* */
	abstract public Double getRotation();

	/**
	 * Because a {@link SingleRuleList} only contains one {@link Rule}, this is
	 * a convenience method to get it.
	 */
	public Rule getRule() {
		return getRules().get(0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Rule> getRules() {

		// Reversing the order of symbols. Is also done in
		// AtlasStyler.importStyle when importing a SingleRulesList
		List<Symbolizer> symbolizers = new ArrayList<Symbolizer>();
		for (Symbolizer ps : getSymbolizers()) {

			// Set or remove Any centroid function

			if (isUseCentroidFunction()) {
				String geoColumn = "default";
				if (ps.getGeometryPropertyName() != null)
					geoColumn = ps.getGeometryPropertyName();

				ps.setGeometry(FeatureUtil.FILTER_FACTORY2.function("centroid",
						StylingUtil.STYLE_BUILDER
								.attributeExpression(geoColumn)));
			} else
				ps.setGeometryPropertyName(null);

			symbolizers.add(ps);
		}
		Collections.reverse(symbolizers);

		// TODO Add support for NODATA

		Rule rule = ASUtil.SB.createRule(symbolizers
				.toArray(new Symbolizer[symbolizers.size()]));

		applyScaleDominators(rule);

		/** Saving the legend label * */
		rule.setTitle(getLabel());

		addFilters(rule);

		ArrayList<Rule> rList = new ArrayList<Rule>();
		rList.add(rule);

		return rList;
	}

	/**
	 * @return Returns the biggest Size used if any Size is used. If no size
	 *         used returns 0.
	 */
	public abstract Float getSizeBiggest();

	public String getStyleAbstract() {
		return styleAbstract;
	}

	/**
	 * Used for the filename without .sld
	 */
	public String getStyleName() {
		return styleName;
	}

	/**
	 * The Title is used for the author of the Style.
	 */
	public String getStyleTitle() {
		return styleTitle;
	}

	/**
	 * @return A {@link Vector} of {@link PointSymbolizer}s that paint the
	 *         symbols. The are painted in reverse order.
	 */
	public Vector<SymbolizerType> getSymbolizers() {
		return layers;
	}

	/**
	 * @return <code>True</code> if any item is used that has a changeable
	 *         {@link Color} TODO think again .. do we need that?
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
	 */
	public boolean hasColor() {
		return getColor() != null;
	}

	abstract public boolean hasRotation();

	public boolean hasSize() {
		return getSizeBiggest() >= 0.;
	}

	/**
	 * This stuff is the same for all three SINGLE_RULES types.
	 */
	@Override
	public void importRules(List<Rule> rules) {
		pushQuite();

		if (rules.size() > 1) {
			LOGGER.warn("Importing a " + this.getClass().getSimpleName()
					+ " with " + rules.size() + " rules, strange!");
		}

		Rule rule = rules.get(0);

		try {

			final List<? extends Symbolizer> symbs = rule.symbolizers();

			addSymbolizers(symbs);
			reverseSymbolizers();

			// We had some stupid AbstractMethodException here...
			try {
				final Description description = rule.getDescription();
				final InternationalString title2 = description.getTitle();
				setLabel(title2.toString());
			} catch (final NullPointerException e) {
				LOGGER.warn("The title style to import has been null!");
				setLabel("");
			} catch (final Exception e) {
				LOGGER.error("The title style to import could not been set!", e);
				setLabel("");
			}

			// Analyse the filters...
			Filter filter = rule.getFilter();
			filter = parseAbstractRlSettings(filter);

		} finally {
			popQuite();
		}
	}

	/**
	 * This boolean defines whether the entry shall be shown the legend. <b>This
	 * is only interpreted in GP/Atlas context.</b>
	 */
	public boolean isVisibleInLegend() {
		return visibleInLegend;
	}

	/**
	 * This fires an event to all {@link RuleChangeListener}s.
	 */
	public boolean loadURL(URL url) {
		pushQuite();
		try {
			Style[] styles = StylingUtil.loadSLD(url);
			// LOGGER.debug("Anzahl Styles in URL " + styles.length);

			if (styles == null || styles.length == 0 || styles[0] == null)
				throw new RuntimeException("Symbol von " + url
						+ " konnte nicht geladen werden.");

			setStyleTitle(styles[0].getTitle());
			setStyleAbstract(styles[0].getAbstract());

			// if (StylingUtil.sldToString(styles[0]).contains("the_geom")) {
			// LOGGER.warn("The imported symbol contains a ref to the_geom!");
			// }

			// Transforming
			// http://freemapsymbols.org/point/Circle.sld to
			// Circle
			String fileName = new File(url.getFile()).getName();
			String fileNameWithoutSLD = fileName.substring(0,
					fileName.lastIndexOf('.'));

			setStyleName(fileNameWithoutSLD);

			try {
				FeatureTypeStyle featureTypeStyle = styles[0]
						.featureTypeStyles().get(0);
				Rule rule = featureTypeStyle.rules().get(0);
				Symbolizer[] symbolizers = rule.getSymbolizers();
				for (Symbolizer s : symbolizers) {
					addSymbolizer(s);
				}
				// log.debug("SingleRuleList loaded "+symbolizers.length+"
				// symbolizers from URL "+url.getFile());
			} catch (Exception e) {
				LOGGER.warn("Error loading " + url + ": "
						+ e.getLocalizedMessage());
				return false;
			}

			fireEvents(new RuleChangedEvent("Loaded from URL " + url, this));

			return true;

		} catch (RuntimeException e) {
			LOGGER.error("Error reading URL " + url, e);
			throw e;
			// } catch (TransformerException e) {
			// LOGGER.error("Error reading URL " + url, e);
			// throw new RuntimeException("Error reading URL " + url, e);
		} finally {
			pushQuite();
		}
	}

	/**
	 * @param row
	 * @param delta
	 *            -1 to move the row one up
	 */
	public void move(int row, int delta) {

		if (row + delta < 0 || row + delta > getSymbolizers().size())
			throw new IllegalArgumentException("Can't move Symbolizer idx "
					+ row + " by " + delta);

		// Something is selected
		getSymbolizers().add(row + delta, getSymbolizers().remove(row));

		fireEvents(new RuleChangedEvent("Index " + row + " moved up/down to "
				+ (row + delta), this));
	}

	@Override
	public void parseMetaInfoString(String metaInfoString, FeatureTypeStyle fts) {
	}

	public SymbolizerType removeSymbolizer(int index) {
		SymbolizerType rmvd = layers.remove(index);
		if (rmvd != null)
			fireEvents(new RuleChangedEvent("Removed a Symbolizer", this));
		return rmvd;
	}

	/**
	 * Removes a symbolizer from the list of symbolizers.
	 * 
	 * @return <code>true</code> if the symbolizer has actually been removed.
	 */
	public boolean removeSymbolizer(SymbolizerType ps) {
		boolean rmvd = layers.remove(ps);
		if (rmvd)
			fireEvents(new RuleChangedEvent("Removed a Symbolizer", this));
		return rmvd;
	}

	/**
	 * Constantly reverses the order of symbolizers. Fires an event if the order
	 * of symbolizers has changed.
	 * 
	 */
	public void reverseSymbolizers() {
		if (layers.size() > 1) {
			Collections.reverse(layers);
			fireEvents(new RuleChangedEvent("Changed the order of symbolizers",
					this));
		}
	}

	/**
	 * Wraps the Symbol in a {@link Style} and saves it as an SLD to the
	 * {@link File}
	 * 
	 * @param file
	 *            Where to save the SLD
	 * @throws TransformerException
	 * @throws IOException
	 */
	public void saveSymbolToFile(File file) throws TransformerException,
			IOException {
		if (layers.size() == 0)
			return;
		Style style = ASUtil.SB.createStyle(layers.get(0));
		style.featureTypeStyles().get(0).rules().get(0).symbolizers().clear();
		style.featureTypeStyles().get(0).rules().get(0).symbolizers()
				.addAll(layers);

		style.getDescription().setTitle(getStyleTitle());
		style.getDescription().setAbstract(getStyleAbstract());
		style.setName(getStyleName()); // Not really needed... we evaluate the
		// filename

		StylingUtil.saveStyleToSld(style, file);

		// TODO Override and call super from the specific implementations ??????
		// really?
	}

	/**
	 * The {@link Color} returned by {@link #getColor()} is replaced against the
	 * given color parameter. Any other occurrence of the original color will
	 * also be replaced. This fires an event to all {@link RuleChangeListener}s.
	 * 
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
	 */
	abstract public void setColor(Color newColor);

	/**
	 * Set the title of the first and only {@link Rule}. This is used as the
	 * label for this rule in the legend.
	 * 
	 */
	public void setLabel(String label) {

		if (label == null || label.equals("")) {
			// LOGGER.warn("rule title may not be empty");
			label = ""; // i8n
		}

		// Is the new title really different from the old one?
		boolean change = true;
		if (this.label != null && label != null && this.label.equals(label))
			change = false;

		if (change) {
			// Update the title and fire an event
			this.label = label;
			fireEvents(new RuleChangedEvent("Single Legend Label changed to "
					+ label, this));
		}
	}

	/**
	 * Sets the rotation of any subelement where it makes sense. This fires an
	 * event to all {@link RuleChangeListener}s.
	 */
	public abstract void setRotation(Double size);

	public void setRuleTitle(Translation translation) {
		setLabel(translation.toOneLine());
	}

	/**
	 * Sets the size of any sub-element where it makes sense. This fires an
	 * event to all {@link RuleChangeListener}s.
	 */
	public abstract void setSizeBiggest(Float size);

	/**
	 * The Abstract is used as a description line for a Symbol
	 * 
	 * @param styleAbstract
	 *            Description
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
	 */
	public void setStyleAbstract(final String styleAbstract) {

		// Is the new title really different from the old one?
		boolean change = true;
		if (this.styleAbstract == null && styleAbstract == null)
			change = false;
		if (this.styleAbstract != null && styleAbstract != null
				&& this.styleAbstract.equals(styleAbstract))
			change = false;

		if (change) {
			// Update the title and fire an event
			this.styleAbstract = styleAbstract;
			fireEvents(new RuleChangedEvent("Single Legend Label changed to "
					+ label, this));
		}

	}

	/**
	 * Used for the filename without .sld
	 * 
	 * @param styleName
	 */
	public void setStyleName(String styleName) {
		this.styleName = styleName;
	}

	/**
	 * Use for Author
	 * 
	 * @param styleTitle
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
	 */
	public void setStyleTitle(String styleTitle) {
		this.styleTitle = styleTitle;

	}

	/**
	 * @return A {@link Vector} of {@link PointSymbolizer}s that paint the
	 *         symbols. The are painted in reverse order.
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
	 */
	public void setSymbolizers(Vector<SymbolizerType> newlayers) {
		layers = newlayers;
		fireEvents(new RuleChangedEvent("All symbolizers have been replaced",
				this));
	}

	/**
	 * /** This boolean defines whether the entry shall be shown the legend.
	 * <b>This is only interpreted in GP/Atlas context.</b> Changing this
	 * property will automatically fire a {@link RuleChangedEvent}
	 */
	public void setVisibleInLegend(boolean visibleInLegend) {
		if (visibleInLegend != this.visibleInLegend) {
			this.visibleInLegend = visibleInLegend;
			fireEvents(new RuleChangedEvent("visiblility in legend changed",
					this));
		}
	}

	/**
	 * Intended use: Set true if you want a point symbolizer on a multi-polygon
	 * geometry to paint only the centroid, not the center of each polygon.
	 */
	public boolean isUseCentroidFunction() {
		return useCentroidFunction;
	}

	/**
	 * Intended use: Set true if you want a point symbolizer on a multi-polygon
	 * geometry to paint only the centroid, not the center of each polygon.
	 */
	public void setUseCentroidFunction(boolean useCentroidFunction) {
		if (useCentroidFunction != this.useCentroidFunction) {
			this.useCentroidFunction = useCentroidFunction;
			fireEvents(new RuleChangedEvent("setUseCentroidFunction to "
					+ useCentroidFunction, this));
		}
	}

}
