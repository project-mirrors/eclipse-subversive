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

import org.eclipse.team.core.variants.CachedResourceVariant;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNEntryInfo;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Abstract resource variant implementation
 * 
 * @author Alexander Gurov
 */
public abstract class ResourceVariant extends CachedResourceVariant {

	protected ILocalResource local;

	public ResourceVariant(ILocalResource local) {
		this.local = local;
	}

	public ILocalResource getResource() {
		return local;
	}

	@Override
	protected String getCachePath() {
		IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(local.getResource());
		return location.getId() + local.getResource().getFullPath().toString() + " " + getContentIdentifier();
	}

	@Override
	protected String getCacheId() {
		return IRemoteStorage.class.getName();
	}

	@Override
	public String getName() {
		return local.getName();
	}

	@Override
	public byte[] asBytes() {
		return getContentIdentifier().getBytes();
	}

	public String getStatus() {
		return local.getStatus();
	}

	@Override
	public String getContentIdentifier() {
		long revision = local.getRevision();
		if (revision == SVNRevision.INVALID_REVISION_NUMBER
				&& (IStateFilter.SF_ONREPOSITORY.accept(local) || local.isCopied())) {
			SVNEntryInfo[] st = SVNUtility
					.info(new SVNEntryRevisionReference(FileUtility.getWorkingCopyPath(local.getResource())));
			if (st != null && st.length > 0) {
				revision = local.isCopied() ? st[0].copyFromRevision : st[0].lastChangedRevision;
			}
		}
		if (revision == SVNRevision.INVALID_REVISION_NUMBER) {
			if (isNotOnRepository()) {
				return SVNMessages.ResourceVariant_unversioned;
			}
			if (IStateFilter.SF_DELETED.accept(local)) {
				return SVNMessages.ResourceVariant_deleted;
			}
		}
		return String.valueOf(revision);
	}

	protected boolean isNotOnRepository() {
		return IStateFilter.SF_UNVERSIONED.accept(local);
	}

}
