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
		helpers = new ArrayList<>();
		extensionsFetched = false;
	}

	@Override
	public synchronized void addResolutionHelper(IResolutionHelper helper) {
		if (helper != this && !helpers.contains(helper)) {
			helpers.add(helper);
		}
	}

	@Override
	public synchronized void removeResolutionHelper(IResolutionHelper helper) {
		helpers.remove(helper);
	}

	@Override
	public boolean acquireResolution(ErrorDescription description) {
		IResolutionHelper[] helpers = getHelpers();
		for (IResolutionHelper helper : helpers) {
			if (helper.acquireResolution(description)) {
				return true;
			}
		}
		return false;
	}

	protected synchronized IResolutionHelper[] getHelpers() {
		if (!extensionsFetched) {
			helpers.addAll(Arrays.asList(CoreExtensionsManager.instance().getResolutionHelpers()));
			extensionsFetched = true;
		}
		return helpers.toArray(new IResolutionHelper[helpers.size()]);
	}

}
