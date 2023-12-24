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
 *    Alexander Gurov - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.synchronize.variant;

import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.resource.ILocalResource;

/**
 * Remote resource emulator variant. Allow us to fetch BASE resource info for the remote view without request to repository.
 * 
 * @author Alexander Gurov
 */
public abstract class VirtualRemoteResourceVariant extends RemoteResourceVariant {

	public VirtualRemoteResourceVariant(ILocalResource local) {
		super(local);
	}

	@Override
	public String getStatus() {
		return isNotOnRepository() ? IStateFilter.ST_NOTEXISTS : IStateFilter.ST_NORMAL;
	}

	@Override
	protected boolean isNotOnRepository() {
		return !IStateFilter.SF_ONREPOSITORY.accept(local);
	}

}
