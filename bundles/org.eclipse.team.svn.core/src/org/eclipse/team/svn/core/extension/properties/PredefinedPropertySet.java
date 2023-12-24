/*******************************************************************************
 * Copyright (c) 2005, 2023 Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.extension.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default IPredefinedPropertySet implementation
 *
 * @author Sergiy Logvin
 */
public class PredefinedPropertySet implements IPredefinedPropertySet {
	private Map<String, PredefinedProperty> properties = new LinkedHashMap<>();

	@Override
	public List<PredefinedProperty> getPredefinedProperties() {
		return new ArrayList<>(getPropertiesMap().values());
	}

	@Override
	public PredefinedProperty getPredefinedProperty(String name) {
		return getPropertiesMap().get(name);
	}

	@Override
	public Map<String, String> getPredefinedPropertiesRegexps() {
		HashMap<String, String> regexpmap = new HashMap<>();
		for (PredefinedProperty property : getPredefinedProperties()) {
			regexpmap.put(property.name, property.validationRegexp);
		}
		return regexpmap;
	}

	public void registerProperties(List<PredefinedProperty> properties) {
		for (PredefinedProperty property : properties) {
			registerProperty(property);
		}
	}

	public void registerProperties(PredefinedProperty[] properties) {
		for (PredefinedProperty property : properties) {
			registerProperty(property);
		}
	}

	public void registerProperty(PredefinedProperty property) {
		properties.put(property.name, property);
	}

	protected synchronized Map<String, PredefinedProperty> getPropertiesMap() {
		init();
		return properties;
	}

	protected void init() {
	}

}
