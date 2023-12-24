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

import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.resource.ILocalResource;

/**
 * Abstract remote resource variant
 * 
 * @author Alexander Gurov
 */
public abstract class RemoteResourceVariant extends ResourceVariant {
	protected static String svnAuthor;

	public RemoteResourceVariant(ILocalResource local) {
		super(local);
		RemoteResourceVariant.svnAuthor = SVNMessages.SVNInfo_Author;
	}

	@Override
	protected String getCacheId() {
		return "Remote: " + super.getCacheId();
	}

	@Override
	public String getContentIdentifier() {
		if (IStateFilter.SF_PREREPLACED.accept(local)) {
			return "";
		}
		String retVal = super.getContentIdentifier();
		if ((!isNotOnRepository() || local.isCopied()) && local.getAuthor() != null) {
			retVal += " " + BaseMessages.format(RemoteResourceVariant.svnAuthor, new Object[] { local.getAuthor() });
		}
		return retVal;
	}

}
