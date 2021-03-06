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
package org.geopublishing.atlasStyler.swing;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

import org.apache.log4j.Logger;
import org.geopublishing.atlasStyler.AtlasStylerVector;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import de.schmitzm.geotools.data.amd.AttributeMetadataImpl;
import de.schmitzm.geotools.data.amd.AttributeMetadataInterface;
import de.schmitzm.geotools.data.amd.AttributeMetadataMap;
import de.schmitzm.geotools.feature.FeatureUtil;
import de.schmitzm.i18n.I18NUtil;
import de.schmitzm.i18n.Translation;
import de.schmitzm.lang.LangUtil;
import de.schmitzm.swing.SwingUtil;

/**
 * This extension of a {@link JComboBox} is specialized on the visualization of
 * a selection of attribute. If {@link AttributeMetadataImpl} is stored in the
 * {@link AtlasStylerVector}, it's used for lables and tooltips.<br/>
 * {@link AttributesJComboBox} only sends {@link ItemEvent} of type SELECTED.
 * UNSELETED is ignored.<br/>
 * 
 * @author Stefan A. Tzeggai
 */
public class AttributesJComboBox extends JComboBox {
	static final private Logger LOGGER = LangUtil
			.createLogger(AttributesJComboBox.class);

	private List<String> numericalAttribs;

	public AttributesJComboBox(final AtlasStylerVector atlasStyler,
			List<String> attributes) {
		this(atlasStyler, new DefaultComboBoxModel(
				attributes.toArray(new String[] {})));
	}
//
//	static List<String> cleanProblematicAttribute(List<String> attributes) {
//		ArrayList<String> newList = new ArrayList<String>();
//
//		for (String an : attributes) {
//			if (FeatureUtil.checkAttributeNameRestrictions(an))
//				newList.add(an);
//			else
//				LOGGER.info("An illegal attribute name " + an
//						+ " has been hidden in the AttributesJComboBox");
//		}
//
//		return newList;
//	}

	public AttributesJComboBox(final SimpleFeatureType schema,
			final AttributeMetadataMap attributeMetaDataMap,
			List<String> attributes) {
		this(schema, attributeMetaDataMap, new DefaultComboBoxModel(
				attributes.toArray(new String[] {})));
	}

	public AttributesJComboBox(final AtlasStylerVector atlasStyler,
			ComboBoxModel comboBoxModel) {
		this(atlasStyler.getStyledFeatures().getSchema(), atlasStyler
				.getAttributeMetaDataMap(), comboBoxModel);
	}

	public AttributesJComboBox(final SimpleFeatureType schema_,
			final AttributeMetadataMap attributeMetaDataMap_,
			ComboBoxModel comboBoxModel_) {
		setValues(schema_, attributeMetaDataMap_, comboBoxModel_);
		SwingUtil.setMaximumWidth(this, 350);
	}

	/**
	 * This {@link JComboBox} is only sending {@link ItemEvent} of thype
	 * SELECTED. UNSELETED is omittet.
	 */
	@Override
	protected void fireItemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.DESELECTED)
			return;
		super.fireItemStateChanged(e);
	}

	/**
	 * Should only be called once! Either by the constructor, or when the
	 * default constructor was used by the API user.
	 * 
	 * @param schema
	 * @param attributeMetaDataMap
	 * @param comboBoxModel
	 */
	public void setValues(final SimpleFeatureType schema,
			final AttributeMetadataMap attributeMetaDataMap,
			ComboBoxModel comboBoxModel) {
		setModel(comboBoxModel);

		/*
		 * Caching the list of numerical attributes, so we can quickly determine
		 * the type of a selected attribute without accessing the schema.
		 */
		numericalAttribs = FeatureUtil.getNumericalFieldNames(schema, false,
				true);

		SwingUtil.addMouseWheelForCombobox(this, false);

		/**
		 * Use the AttributeMetaData (if available) for label+tooltip
		 */
		setRenderer(new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {

				JLabel prototype = (JLabel) super.getListCellRendererComponent(
						list, value, index, isSelected, cellHasFocus);

				// This list may contain null or ""
				if (value == null || value instanceof String
						&& ((String) value).trim().isEmpty()
						|| value instanceof String
						&& ((String) value).equalsIgnoreCase("-")) {
					prototype.setText("-");
					return prototype;
				}

				AttributeMetadataInterface attributeMetadataFor = attributeMetaDataMap != null ? attributeMetaDataMap
						.get(prototype.getText()) : null;

				prototype.setToolTipText(null);
				if (attributeMetadataFor != null) {

					final String metaTitle = attributeMetadataFor.getTitle()
							.toString();
					if (!I18NUtil.isEmpty(metaTitle))
						prototype.setText("<html>" + metaTitle);
					else
						prototype.setText("<html>" + prototype.getText());

					final Translation metaDesc = attributeMetadataFor.getDesc();
					if (!I18NUtil.isEmpty(metaDesc))
						prototype.setToolTipText(metaDesc.toString());
				} else
					prototype.setText("<html>" + prototype.getText());

				/**
				 * Adding the Attribute-Type to the Label
				 */
				if (attributeMetadataFor != null) {
					AttributeDescriptor attributeDesc = schema
							.getDescriptor(attributeMetadataFor.getName());
					if (attributeDesc != null) {
						prototype.setText(prototype.getText()
								+ " <i>("
								+ attributeDesc.getType().getBinding()
										.getSimpleName() + ")</i>");
					} else {
						LOGGER.warn("No attributedesc for " + attributeDesc
								+ " found.");
					}
				}

				prototype.setText(prototype.getText() + "</html>");

				return prototype;
			}
		});
	}

	/**
	 * @return <code>true</code> if an attribute is selected, and it is a
	 *         numerical attribute type. Calling this method is cheap and
	 *         doesn't access the schema.
	 */
	public boolean isNumericalAttribSelected() {
		final String selectedItem = (String) getSelectedItem();
		return (selectedItem != null && numericalAttribs.contains(selectedItem));
	}
}
