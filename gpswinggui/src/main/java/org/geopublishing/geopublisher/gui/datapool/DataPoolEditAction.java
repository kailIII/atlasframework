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
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.geopublishing.atlasViewer.dp.DpEntry;
import org.geopublishing.atlasViewer.swing.Icons;
import org.geopublishing.geopublisher.gui.internal.GPDialogManager;
import org.geopublishing.geopublisher.swing.GeopublisherGUI;

import de.schmitzm.jfree.chart.style.ChartStyle;

/**
 * An {@link Action} that will open a dialog the configure DpEntry specific settings. 
 */
public class DataPoolEditAction extends AbstractAction {

	private Component owner;
	DpEntry<? extends ChartStyle> dpe;
	DataPoolJTable dpTable;

	public DataPoolEditAction(DataPoolJTable dpTable, Component owner) {
		super(GeopublisherGUI.R("DataPoolWindow_Action_EditDPE_label"),
				Icons.ICON_TOOL);
		this.dpTable = dpTable;
		this.owner = owner;

	}

	public DataPoolEditAction(Component owner, DpEntry<? extends ChartStyle> dpe) {
		super(GeopublisherGUI.R("DataPoolWindow_Action_EditDPE_label"),
				Icons.ICON_TOOL);
		this.owner = owner;
		this.dpe = dpe;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (dpe == null) {
			dpe = dpTable.getDataPool().get(
					dpTable.convertRowIndexToModel(dpTable.getSelectedRow()));
		}

		GPDialogManager.dm_EditDpEntry.getInstanceFor(dpe, owner, dpe);
	}

}
