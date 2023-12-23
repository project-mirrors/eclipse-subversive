/*******************************************************************************
 * Copyright (c) 2009, 2023 Tasktop Technologies, Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tasktop Technologies - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/
package org.eclipse.team.svn.core.discovery.model;

/**
 * @author David Green
 * @author Igor Burilo
 */
public enum ConnectorDescriptorKind {

	DOCUMENT("document"), //$NON-NLS-1$
	TASK("task"), //$NON-NLS-1$
	VCS("vcs"); //$NON-NLS-1$

	private final String value;

	private ConnectorDescriptorKind(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	/**
	 * return the enum constant whose {@link #getValue() value} is the same as the given value.
	 * 
	 * @param value
	 *            the string value, or null
	 * 
	 * @return the corresponding enum constant or null if the given value was null
	 * 
	 * @throws IllegalArgumentException
	 *             if the given value does not correspond to any enum constant
	 */
	public static ConnectorDescriptorKind fromValue(String value) throws IllegalArgumentException {
		if (value == null) {
			return null;
		}
		for (ConnectorDescriptorKind e : values()) {
			if (e.getValue().equals(value)) {
				return e;
			}
		}
		throw new IllegalArgumentException(value);
	}

}
