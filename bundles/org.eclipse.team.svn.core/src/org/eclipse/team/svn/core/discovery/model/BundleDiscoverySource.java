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

import java.net.URL;

import org.osgi.framework.Bundle;

/**
 * @author David Green
 * @author Igor Burilo
 */
public class BundleDiscoverySource extends AbstractDiscoverySource {

	private final Bundle bundle;

	public BundleDiscoverySource(Bundle bundle) {
		if (bundle == null) {
			throw new IllegalArgumentException();
		}
		this.bundle = bundle;
	}

	@Override
	public Object getId() {
		return "bundle:" + bundle.getSymbolicName(); //$NON-NLS-1$
	}

	@Override
	public URL getResource(String relativeUrl) {
		return bundle.getEntry(relativeUrl);
	}

}
