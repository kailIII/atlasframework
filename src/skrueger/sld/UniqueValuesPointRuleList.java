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
package skrueger.sld;

import org.apache.log4j.Logger;
import org.geotools.styling.FeatureTypeStyle;

import schmitzm.geotools.feature.FeatureUtil.GeometryForm;
import skrueger.geotools.StyledFeaturesInterface;

public class UniqueValuesPointRuleList extends UniqueValuesRuleList {

	public UniqueValuesPointRuleList(StyledFeaturesInterface<?> styledFeatures) {
		super(styledFeatures);
	}

	protected Logger LOGGER = ASUtil.createLogger(this);
//
//	@Override
//	public SingleRuleList getDefaultTemplate() {
//		return ASUtil.getDefaultPointTemplate();
//	}

	@Override
	public void importTemplate(FeatureTypeStyle importFTS) {
		setTemplate(ASUtil.importPointTemplateFromFirstRule(importFTS));
	}

	@Override
	public RulesListType getTypeID() {
		return RulesListType.UNIQUE_VALUE_POINT;
	}

	@Override
	public GeometryForm getGeometryForm() {
		return GeometryForm.POINT;
	}

}