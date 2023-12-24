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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.extension.crashrecovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.team.svn.core.extension.CoreExtensionsManager;

/**
 * Default error handling facility implementation.
 * 
 * @author Alexander Gurov
 */
public class DefaultErrorHandlingFacility implements IErrorHandlingFacility {
	protected List<IResolutionHelper> helpers;

	private boolean extensionsFetched;

	public DefaultErrorHandlingFacility() {
		this.helpers = new ArrayList<IResolutionHelper>();
		this.extensionsFetched = false;
	}

	public synchronized void addResolutionHelper(IResolutionHelper helper) {
		if (helper != this && !this.helpers.contains(helper)) {
			this.helpers.add(helper);
		}
	}

	public synchronized void removeResolutionHelper(IResolutionHelper helper) {
		this.helpers.remove(helper);
	}

	public boolean acquireResolution(ErrorDescription description) {
		IResolutionHelper[] helpers = this.getHelpers();
		for (int i = 0; i < helpers.length; i++) {
			if (helpers[i].acquireResolution(description)) {
				return true;
			}
		}
		return false;
	}

	protected synchronized IResolutionHelper[] getHelpers() {
		if (!this.extensionsFetched) {
			this.helpers.addAll(Arrays.asList(CoreExtensionsManager.instance().getResolutionHelpers()));
			this.extensionsFetched = true;
		}
		return this.helpers.toArray(new IResolutionHelper[this.helpers.size()]);
	}

}
