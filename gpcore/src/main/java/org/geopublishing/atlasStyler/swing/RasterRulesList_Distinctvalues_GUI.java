package org.geopublishing.atlasStyler.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.WeakHashMap;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.geopublishing.atlasStyler.ASUtil;
import org.geopublishing.atlasStyler.AtlasStylerRaster;
import org.geopublishing.atlasStyler.AtlasStylerVector;
import org.geopublishing.atlasStyler.RasterRulesList_DistinctValues;
import org.geopublishing.atlasStyler.RuleChangeListener;
import org.geopublishing.atlasStyler.RuleChangedEvent;
import org.geopublishing.atlasViewer.swing.AVSwingUtil;

import de.schmitzm.i18n.Translation;
import de.schmitzm.lang.LangUtil;
import de.schmitzm.swing.JPanel;
import de.schmitzm.swing.ThinButton;
import de.schmitzm.swing.TranslationAskJDialog;
import de.schmitzm.swing.TranslationEditJPanel;

public class RasterRulesList_Distinctvalues_GUI extends
		AbstractRulesListGui<RasterRulesList_DistinctValues> {

	protected final static Logger LOGGER = LangUtil
			.createLogger(RasterRulesList_Distinctvalues_GUI.class);
	private JTable jTable;
	private DefaultTableModel tableModel;
	private ThinButton jButtonRemoveAll;
	private ThinButton jButtonRemove;

	public RasterRulesList_Distinctvalues_GUI(
			RasterRulesList_DistinctValues rulesList,
			AtlasStylerRaster atlasStyler) {
		super(rulesList);
		initialize();
		rulesList.fireEvents(new RuleChangedEvent("GUI created for "
				+ this.getClass().getSimpleName()
				+ ", possibly setting some default values", rulesList));
	}

	private void initialize() {
		JLabel jLabelHeading = new JLabel(
				AtlasStylerVector.R("UniqueValues.Heading"));
		jLabelHeading.setFont(jLabelHeading.getFont().deriveFont(
				AVSwingUtil.HEADING_FONT_SIZE));
		this.setLayout(new MigLayout("inset 1, gap 1, wrap 1, fillx"));

		this.add(jLabelHeading, "center");
		this.add(getJPanelColorAndOpacity(), "grow x");

		this.add(new JScrollPane(getJTable()), "grow x, height 50:150:600");

		JPanel jPanelButtons = new JPanel(new MigLayout(
				"ins n 0 n 0, gap 1, fillx"));
		{
			jPanelButtons.add(getJButtonAddAllValues());
			jPanelButtons.add(getJButtonAddValues(), "gapx rel unrel");
			jPanelButtons.add(getJButtonRemove());
			jPanelButtons.add(getJButtonRemoveAll(), "gapx rel unrel");
			jPanelButtons.add(getJButtonUp());
			jPanelButtons.add(getJButtonDown(), "gapx rel");
		}
		this.add(jPanelButtons, "");
	}

	private Component getJButtonDown() {
		return new JPanel();
	}

	private Component getJButtonUp() {
		return new JPanel();
	}

	/**
	 * This method initializes jButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonRemove() {
		if (jButtonRemove == null) {
			jButtonRemove = new ThinButton(new AbstractAction(
					AtlasStylerVector.R("UniqueValues.Button.RemoveValue")) {

				@Override
				public void actionPerformed(ActionEvent e) {
					int[] selectedRows = getJTable().getSelectedRows();

					// We remove from last to fist - so that indexes to not mix
					// up
					Arrays.sort(selectedRows);
					ArrayUtils.reverse(selectedRows);

					for (int rowIdx : selectedRows) {
						rulesList.removeIdx(rowIdx);
					}

					// De-select anything afterwards
					getJTable().getSelectionModel().clearSelection();
				}

			});

			getJTable().getSelectionModel().addListSelectionListener(
					new ListSelectionListener() {

						@Override
						public void valueChanged(ListSelectionEvent e) {
							if (getJTable().getSelectedRows().length == 0)
								jButtonRemove.setEnabled(false);
							else {
								jButtonRemove.setEnabled(true);
							}
						}

					});

			/** Initializing with disabled button * */
			jButtonRemove.setEnabled(false);

		}
		return jButtonRemove;
	}

	private Component getJButtonRemoveAll() {
		if (jButtonRemoveAll == null) {
			jButtonRemoveAll = new ThinButton(
					new AbstractAction(
							AtlasStylerVector
									.R("UniqueValues.Button.RemoveAllValues")) {

						@Override
						public void actionPerformed(ActionEvent e) {
							rulesList.removeAll();
						}

					});

			if (rulesList.getValues().size() < 1) {
				jButtonRemoveAll.setEnabled(false);
			} else {
				jButtonRemoveAll.setEnabled(true);
			}
		}
		return jButtonRemoveAll;

	}

	private Component getJButtonAddValues() {
		return new JPanel();
	}

	private Component getJButtonAddAllValues() {
		return new JPanel();
	}

	private Component getJPanelColorAndOpacity() {
		return new JPanel();
	}

	final static int COLIDX_COLOR = 0;
	final static int COLIDX_OPACITY = 1;
	final static int COLIDX_VALUE = 2;
	final static int COLIDX_LABEL = 3;

	/**
	 * Listen for changes in the RulesList. Must be kept as a reference in
	 * {@link UniqueValuesGUI} because the listeners are kept in a
	 * {@link WeakHashMap}
	 */
	final RuleChangeListener updateTableWhenRuleListChanges = new RuleChangeListener() {

		@Override
		public void changed(RuleChangedEvent e) {

			// Try to remember the selected item
			int selViewIdx = getJTable().getSelectedRow();

			getTableModel().fireTableDataChanged();

			getJTable().getSelectionModel().clearSelection();
			getJTable().getSelectionModel().addSelectionInterval(selViewIdx,
					selViewIdx);

			/** scroll */
			getJTable().scrollRectToVisible(
					getJTable().getCellRect(selViewIdx, 0, true));
		}

	};

	private DefaultTableModel getTableModel() {

		if (tableModel == null) {

			tableModel = new DefaultTableModel() {

				@Override
				public Class<?> getColumnClass(int columnIndex) {
					if (columnIndex == COLIDX_COLOR) // Color
						return Color.class;

					if (columnIndex == COLIDX_OPACITY) // Value
						return Double.class;

					if (columnIndex == COLIDX_VALUE) // Value
						return Double.class;

					if (columnIndex == COLIDX_LABEL) // Label
						return Translation.class;

					return null;
				}

				@Override
				public int getColumnCount() {
					return 4;
				}

				@Override
				public String getColumnName(int columnIndex) {
					if (columnIndex == COLIDX_COLOR)
						return AtlasStylerVector
								.R("RasterRulesList_Distinctvalues_GUI.classesTable.columnHeadersTitle.color");
					if (columnIndex == COLIDX_OPACITY)
						return AtlasStylerVector
								.R("RasterRulesList_Distinctvalues_GUI.classesTable.columnHeadersTitle.opacity");
					if (columnIndex == COLIDX_VALUE)
						return AtlasStylerVector
								.R("RasterRulesList_Distinctvalues_GUI.classesTable.columnHeadersTitle.value");
					if (columnIndex == COLIDX_LABEL)
						return AtlasStylerVector
								.R("RasterRulesList_Distinctvalues_GUI.classesTable.columnHeadersTitle.label");
					return super.getColumnName(columnIndex);
				}

				@Override
				public int getRowCount() {
					return getRulesList().getNumClasses();
				}

				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {

					if (columnIndex == COLIDX_COLOR) {
						return rulesList.getColors().get(rowIndex);
					} else if (columnIndex == COLIDX_OPACITY) {
						return rulesList.getOpacities().get(rowIndex);
					} else if (columnIndex == COLIDX_VALUE) {
						return rulesList.getValues().get(rowIndex);
					} else if (columnIndex == COLIDX_LABEL) {
						return rulesList.getLabels().get(rowIndex);
					}
					return super.getValueAt(rowIndex, columnIndex);
				}

				@Override
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
				}

				@Override
				public void setValueAt(Object aValue, int rowIndex,
						int columnIndex) {
				}

			};
		}

		return tableModel;
	}

	/**
	 * This method initializes jTable
	 * 
	 * @return javax.swing.JTable
	 */
	private JTable getJTable() {
		if (jTable == null) {
			jTable = new JTable(getTableModel());

			/** Render nicely COLOR */
			jTable.setDefaultRenderer(Color.class, new ColorTableCellRenderer());

			getRulesList().addListener(updateTableWhenRuleListChanges);

			/*******************************************************************
			 * Listening to clicks on the JTable.. e.g. for translation and
			 * symbol changes
			 */
			jTable.addMouseListener(new MouseAdapter() {

				private TranslationAskJDialog ask;

				@Override
				public void mouseClicked(MouseEvent e) {

					if (e.getClickCount() == COLIDX_LABEL) {
						int col = jTable.columnAtPoint(e.getPoint());
						final int row = jTable.rowAtPoint(e.getPoint());

						if (col == 2) {

							Object val = getRulesList().getValues().get(row);

							if (AtlasStylerVector.getLanguageMode() == AtlasStylerVector.LANGUAGE_MODE.ATLAS_MULTILANGUAGE) {
								LOGGER.debug(AtlasStylerVector.getLanguages());

								final Translation translation = getRulesList()
										.getLabels().get(row);

								if (ask == null) {
									TranslationEditJPanel transLabel;

									// The index depends on the whether the
									// "all others rule" is eneabled and
									// where it is positioned in the list!
									int index = row;

									transLabel = new TranslationEditJPanel(
											AtlasStylerVector
													.R("RasterRulesList_Distinctvalues_GUI.LabelForClass",
															getRulesList()
																	.getValues()
																	.get(index)),
											translation, AtlasStylerVector
													.getLanguages());

									ask = new TranslationAskJDialog(
											RasterRulesList_Distinctvalues_GUI.this,
											transLabel);
									ask.addPropertyChangeListener(new PropertyChangeListener() {

										@Override
										public void propertyChange(
												PropertyChangeEvent evt) {
											if (evt.getPropertyName()
													.equals(TranslationAskJDialog.PROPERTY_CANCEL_AND_CLOSE)) {
												ask = null;
											}
											if (evt.getPropertyName()
													.equals(TranslationAskJDialog.PROPERTY_APPLY_AND_CLOSE)) {
												LOGGER.debug("Saving new ranslation in rulelist and fire event ");

												getRulesList().getLabels().set(
														row, translation);

											}
											ask = null;
											//
											getRulesList()
													.fireEvents(
															new RuleChangedEvent(
																	"Legend Label changed",
																	getRulesList()));
										}

									});

								}
								ask.setVisible(true);

							} else {
								/***********************************************
								 * AtlasStyler.LANGUAGE_MODE.OGC
								 */
								String newTitle = ASUtil
										.askForString(
												RasterRulesList_Distinctvalues_GUI.this,
												getRulesList().getLabels()
														.get(row).toString(),
												null);
								if (newTitle != null) {
									getRulesList().getLabels().set(row,
											new Translation(newTitle));
									getRulesList().fireEvents(
											new RuleChangedEvent(
													"Legend Label changed",
													getRulesList()));
								}
							}

						}

						else
						/*******************************************************
						 * Changing the Symbol with a MouseClick
						 */
						if (col == COLIDX_COLOR) {
							// getRulesList().getSymbols().get(row).getListeners()
							// .clear();
							// final SingleRuleList<? extends Symbolizer>
							// editSymbol = getRulesList()
							// .getSymbols().get(row);
							// final SingleRuleList<? extends Symbolizer> backup
							// = editSymbol
							// .copy();
							//
							// SymbolSelectorGUI gui = new SymbolSelectorGUI(
							// SwingUtil
							// .getParentWindow(RasterRulesList_Distinctvalues_GUI.this),
							// "Change symbol for "
							// + getRulesList().getLabels().get(
							// row), editSymbol);
							//
							// /***************************************************
							// * Listen to a CANCEL to use the backup
							// */
							// gui.addPropertyChangeListener(new
							// PropertyChangeListener() {
							//
							// @Override
							// public void propertyChange(
							// PropertyChangeEvent evt) {
							//
							// if (evt.getPropertyName()
							// .equals(SymbolSelectorGUI.PROPERTY_CANCEL_CHANGES))
							// {
							//
							// backup.copyTo(editSymbol);
							// }
							//
							// if (evt.getPropertyName().equals(
							// SymbolSelectorGUI.PROPERTY_CLOSED)) {
							// }
							//
							// }
							//
							// });
							//
							// // we have a referenct to it!
							// listenToEditedSymbolAndPassOnTheEvent = new
							// RuleChangeListener() {
							//
							// @Override
							// public void changed(RuleChangedEvent e) {
							//
							// /** Exchanging the Symbol * */
							// getRulesList().getSymbols().set(row,
							// editSymbol);
							//
							// // Fire an event?! TODO
							// getRulesList().fireEvents(
							// new RuleChangedEvent(
							// "Editing a Symbol",
							// getRulesList()));
							//
							// }
							//
							// };
							// editSymbol
							// .addListener(listenToEditedSymbolAndPassOnTheEvent);
							//
							// gui.setModal(true);
							// gui.setVisible(true);
						}
					}

				}

			});

			jTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

			jTable.setRowHeight(AtlasStylerVector.DEFAULT_SYMBOL_PREVIEW_SIZE.height + 2);
			jTable.getColumnModel().getColumn(0).setMaxWidth(53);
		}
		return jTable;
	}

}