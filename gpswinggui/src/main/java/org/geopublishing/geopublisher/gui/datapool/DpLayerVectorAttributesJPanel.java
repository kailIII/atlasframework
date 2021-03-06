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
package org.geopublishing.geopublisher.gui.datapool;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.geopublishing.atlasViewer.GpCoreUtil;
import org.geopublishing.atlasViewer.dp.layer.DpLayerVectorFeatureSource;
import org.geopublishing.atlasViewer.dp.layer.DpLayerVectorFeatureSourceShapefile;
import org.geopublishing.atlasViewer.swing.AVDialogManager;
import org.geopublishing.atlasViewer.swing.AVSwingUtil;
import org.geopublishing.atlasViewer.swing.Icons;
import org.geopublishing.geopublisher.AtlasConfigEditable;
import org.geopublishing.geopublisher.EditAttributesJDialog;
import org.geopublishing.geopublisher.gui.internal.GPDialogManager;
import org.geopublishing.geopublisher.swing.GeopublisherGUI;
import org.opengis.feature.simple.SimpleFeatureType;

import de.schmitzm.geotools.data.amd.AttributeMetadataMap;
import de.schmitzm.geotools.feature.FeatureUtil;
import de.schmitzm.io.IOUtil;
import de.schmitzm.lang.LangUtil;
import de.schmitzm.swing.Cancellable;
import de.schmitzm.swing.ExceptionDialog;
import de.schmitzm.swing.JPanel;
import de.schmitzm.swing.SmallButton;

public class DpLayerVectorAttributesJPanel extends JPanel implements
		Cancellable {

	private static final long serialVersionUID = 3815347428081593615L;

	protected final static Logger LOGGER = LangUtil
	.createLogger(DpLayerVectorAttributesJPanel.class);

	private Charset backupCRS;
	private final DpLayerVectorFeatureSource dplv;
	private Integer atts;
	private Integer numAtts;
	private Integer visNumAtts;
	private Integer visAtts;
	private Integer txtAtts;
	private Integer visTxtAtts;

	final JLabel visTxtAttsLabel = new JLabel();
	final JLabel txtAttsLabel = new JLabel();
	private final JLabel visNumAttsLabel = new JLabel();
	private final JLabel numAttsLabel = new JLabel();
	private final JLabel attsLabel = new JLabel();
	private final JLabel visAttsLabel = new JLabel();

	/**
	 * Update the JLabels which contain the overwiew of visible columns.
	 */
	private void updateAttributeStats() {
		/**
		 * Calculate the overview numbers
		 */
		final SimpleFeatureType schema = dplv.getSchema();

		if (schema == null)
			return;

		atts = FeatureUtil.getValueFieldNames(schema).size();

		final List<String> numericalFieldNames = FeatureUtil
				.getNumericalFieldNames(schema);
		numAtts = numericalFieldNames.size();
		visNumAtts = 0;

		for (final String numAttName : numericalFieldNames) {
			if (dplv.getAttributeMetaDataMap().get(numAttName) != null
					&& dplv.getAttributeMetaDataMap().get(numAttName)
							.isVisible())
				visNumAtts++;
		}
		visAtts = dplv.getAttributeMetaDataMap().sortedValuesVisibleOnly()
				.size();

		txtAtts = atts - numAtts;
		visTxtAtts = visAtts - visNumAtts;

		/**
		 * Update the JLabels
		 */
		visAttsLabel.setText(visAtts.toString());
		attsLabel.setText(atts.toString());
		visTxtAttsLabel.setText(visTxtAtts.toString());
		txtAttsLabel.setText(txtAtts.toString());
		visNumAttsLabel.setText(visNumAtts.toString());
		numAttsLabel.setText(numAtts.toString());

		// numAttsLabel.setText(new Random().nextInt()+"sd");
	}

	final WindowAdapter updateStatsWindowListener = new WindowAdapter() {
		@Override
		public void windowClosed(final WindowEvent e) {
			updateAttributeStats();
		}

	};
	private AttributeMetadataMap<?> backupAttributeMetadataMap;

	public DpLayerVectorAttributesJPanel(final DpLayerVectorFeatureSource dplv) {
		super(new MigLayout("width 100%, wrap 1", "[grow]"));
		this.dplv = dplv;

		backup();

		/**
		 * A Panel giving an overview of all available attributes
		 */
		{
			final JPanel attPanel = new JPanel(new MigLayout(
					"width 100%, wrap 3", "[grow][grow][grow]"));
			attPanel.setBorder(BorderFactory
					.createTitledBorder(R("EditDpEntryGUI.attributes.border")));
			add(attPanel, "growx");

			attPanel.add(new JLabel());
			attPanel.add(new JLabel(
					R("DpLayerVectorAttributesJPanel.attOverview.inTable")),
					"center");
			attPanel.add(new JLabel(
					R("DpLayerVectorAttributesJPanel.attOverview.visible")),
					"center");

			attPanel.add(new JLabel(
					R("DpLayerVectorAttributesJPanel.attOverview.totalAtts")
							+ ":"), "right");

			attPanel.add(attsLabel, "center");
			attPanel.add(visAttsLabel, "center");

			attPanel.add(
					new JLabel(
							R("DpLayerVectorAttributesJPanel.attOverview.numericalAtts")
									+ ":"), "right");
			attPanel.add(numAttsLabel, "center");
			attPanel.add(visNumAttsLabel, "center");

			attPanel.add(new JLabel(
					R("DpLayerVectorAttributesJPanel.attOverview.textualAtts")
							+ ":"), "right");
			attPanel.add(txtAttsLabel, "center");

			attPanel.add(visTxtAttsLabel, "center,wrap");

			//
			// A JButton to edit the Columns...
			//
			{
				final JButton editColumns = new SmallButton(new AbstractAction(
						R("DataPoolWindow_Action_EditColumns_label")) {

					@Override
					public void actionPerformed(final ActionEvent e) {
						final EditAttributesJDialog d = GPDialogManager.dm_EditAttribute
								.getInstanceFor(dplv,
										DpLayerVectorAttributesJPanel.this,
										dplv);

						if (!Arrays.asList(d.getWindowListeners()).contains(
								updateStatsWindowListener)) {
							d.addWindowListener(updateStatsWindowListener);
						}

					}
				});
				attPanel.add(
						editColumns,
						"span 3, split "
								+ (dplv instanceof DpLayerVectorFeatureSourceShapefile ? "3"
										: "2") + ", align right");
			}

			//
			// A JButton to open the attribute table
			//
			{
				final JButton openTable = new SmallButton(new AbstractAction(
						GpCoreUtil.R("LayerToolMenu.table"),
						Icons.ICON_TABLE) {

					@Override
					public void actionPerformed(final ActionEvent e) {
						AVDialogManager.dm_AttributeTable.getInstanceFor(dplv,
								DpLayerVectorAttributesJPanel.this, dplv, null);
					}
				});
				attPanel.add(openTable, "align right");
			}

			// A button to open the DBF directly:
			if (dplv instanceof DpLayerVectorFeatureSourceShapefile) {

				File shpFile = new File(
						new File(
								((AtlasConfigEditable) dplv.getAtlasConfig())
										.getDataDir(),
								dplv.getDataDirname()), dplv.getFilename());
				final File dbfFile = IOUtil.changeFileExt(shpFile, "dbf");

				if (dbfFile.exists() && Desktop.isDesktopSupported()) {
					final JButton openDbfJButton = new SmallButton(
							new AbstractAction(R("EditDPEDialog.OpenDBFButton")) {

								public void actionPerformed(final ActionEvent e) {
									if (!Desktop.isDesktopSupported())
										return;
									try {
										AVSwingUtil
												.showMessageDialog(
														DpLayerVectorAttributesJPanel.this,
														R("EditDPEDialog.OpenDBFButton.TT",
																dplv.getCharset()
																		.toString()));

										if (GPDialogManager
												.closeAllMapComposerDialogsUsing(dplv))

											Desktop.getDesktop().open(dbfFile);
									} catch (IOException ee) {
										ExceptionDialog
												.show(DpLayerVectorAttributesJPanel.this,
														ee);
									}
								}

							}, R("EditDPEDialog.OpenDBFButton.TT"));
					attPanel.add(openDbfJButton, "align right");
				}

			}

			updateAttributeStats();

		}

		/**
		 * A Panel to define the charset.
		 */
		{
			final JPanel charsetPanel = new JPanel(new MigLayout(
					"width 100%, wrap 2", "[grow]"));
			charsetPanel.setBorder(BorderFactory
					.createTitledBorder(R("EditDpEntryGUI.charset.border")));

			charsetPanel.add(
					new JLabel(R("EditDpEntryGUI.charset.explanation", dplv
							.getType().getDesc())),
					"span 2, right, width 100%, growx");

			charsetPanel.setToolTipText(R("EditDpEntryGUI.ChartsetLabel.TT"));
			charsetPanel.add(new JLabel(R("EditDpEntryGUI.ChartsetLabel")),
					"right");

			final SortedMap<String, Charset> availableCharsets = Charset
					.availableCharsets();
			final String[] charsetNames = availableCharsets.keySet().toArray(
					new String[] {});

			final JComboBox charSetJCombobox = new JComboBox(charsetNames);

			charSetJCombobox.setSelectedItem(dplv.getCharset().name());
			charSetJCombobox.addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(final ItemEvent e) {
					if (e.getStateChange() == ItemEvent.DESELECTED)
						return;

					final String item = (String) e.getItem();

					Charset newCharset = Charset.forName(item);

					LOGGER.debug("new charset = " + newCharset);

					dplv.setCharset(newCharset);
				}

			});

			/** A renderer that display a human readable form of the charset */
			charSetJCombobox.setRenderer(new DefaultListCellRenderer() {

				@Override
				public Component getListCellRendererComponent(final JList list,
						final Object value, final int index,
						final boolean isSelected, final boolean cellHasFocus) {
					final JLabel proto = (JLabel) super
							.getListCellRendererComponent(list, value, index,
									isSelected, cellHasFocus);
					final Charset cs = Charset.forName((String) value);
					proto.setText(cs.displayName());
					proto.setToolTipText(cs.aliases().toString());
					return proto;
				}

			});

			charsetPanel.add(charSetJCombobox, "left");

			add(charsetPanel, "growx");
		}
	}

	private void backup() {
		backupCRS = dplv.getCharset();

		backupAttributeMetadataMap = dplv.getAttributeMetaDataMap().copy();
	}

	@Override
	public void cancel() {
		dplv.setCharset(backupCRS);

		backupAttributeMetadataMap.copyTo(dplv.getAttributeMetaDataMap());
	}

	protected String R(final String string, final Object... obj) {
		return GeopublisherGUI.R(string, obj);
	}

}
