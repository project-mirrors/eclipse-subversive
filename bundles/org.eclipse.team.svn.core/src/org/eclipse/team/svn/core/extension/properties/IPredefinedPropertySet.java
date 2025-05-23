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

import java.util.List;
import java.util.Map;

/**
 * Predefined properties provider interface
 *
 * @author Sergiy Logvin
 */
public interface IPredefinedPropertySet {
	List<PredefinedProperty> getPredefinedProperties();

	PredefinedProperty getPredefinedProperty(String name);

	/**
	 * @deprecated
	 */
	@Deprecated
	Map<String, String> getPredefinedPropertiesRegexps();
}
