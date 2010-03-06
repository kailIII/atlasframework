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
package skrueger.atlas.map;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.log4j.Logger;

import skrueger.atlas.AtlasRefInterface;

/**
 * A {@link MapRef} is a reference to a {@link Map} in {@link MapPool} A
 * {@link MapRef} can be added to a {@link JTree}, because it extends
 * {@link DefaultMutableTreeNode}
 * 
 * @author Stefan Alfons Krüger
 * 
 */
public class MapRef extends DefaultMutableTreeNode implements
		AtlasRefInterface<Map> {
	final static private Logger LOGGER = Logger.getLogger(MapRef.class);

	private final String targetId;

	private final MapPool mapPool;

	/**
	 * Creates a {@link MapRef} to the targetMap in maps
	 * 
	 * @param mapPool
	 *            {@link MapPool} where the target {@link Map} exists in
	 */
	public MapRef(Map targetMap, MapPool mapPool) {
		this.mapPool = mapPool;
		this.targetId = targetMap.getId();
	}

	/**
	 * Return the translated Title of the referenced Map
	 */
	@Override
	public String toString() {
		if (getTarget() != null)
			return getTarget().getTitle().toString();
		else return super.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.tree.DefaultMutableTreeNode#getAllowsChildren()
	 */
	@Override
	public boolean getAllowsChildren() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.tree.DefaultMutableTreeNode#isLeaf()
	 */
	@Override
	public boolean isLeaf() {
		return true;
	}

	/**
	 * @return the referenced {@link Map} directly
	 */
	public Map getTarget() {
		return getMapPool().get(targetId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see skrueger.atlas.AtlasRefInterface#isTargetLayer()
	 */
	public boolean isTargetLayer() {
		return false;
	}

	/**
	 * @return The ID of the {@link Map} referenced
	 */
	public final String getTargetId() {
		return targetId;
	}

	public MapPool getMapPool() {
		return mapPool;
	}
}
