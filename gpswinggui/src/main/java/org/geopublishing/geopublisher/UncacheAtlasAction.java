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
package org.geopublishing.geopublisher;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.geopublishing.atlasViewer.dp.DataPool.EventTypes;
import org.geopublishing.atlasViewer.swing.internal.AtlasTask;
import org.geopublishing.geopublisher.swing.GeopublisherGUI;
import org.geopublishing.geopublisher.swing.GpSwingUtil;



/**
 * When this action is performed, it shows a progress bar while the atlas is
 * uncached and reread.
 * 
 */
public class UncacheAtlasAction extends AbstractAction {

	private final Component parentGUI;
	private final AtlasConfigEditable ace;

	public UncacheAtlasAction(Component parentGUI, AtlasConfigEditable ace) {
		super(GeopublisherGUI.R("MenuBar.OptionsMenu.ClearCaches"), new ImageIcon(GPProps.class
				.getResource("/icons/uncache.png")));
		this.parentGUI = parentGUI;
		this.ace = ace;
	}

	/**
	 * All cached information will be dropped and the atlas will be reread (will
	 * it really ?)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		final AtlasTask<Void> uncacheTask = new AtlasTask<Void>(parentGUI,
				GeopublisherGUI.R("ClearCaches.process.WaitMsg")) {

			@Override
			protected void done() {
				super.done();
				ace.getDataPool().fireChangeEvents(EventTypes.changeDpe);
				GpSwingUtil.validate(ace, owner);
			}

			@Override
			protected Void doInBackground() throws Exception {
				if (ace != null) {
					ace.uncacheAndReread();
					
				}
				return null;
			}

		};
		uncacheTask.execute();

	}

}
