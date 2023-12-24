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

package org.eclipse.team.svn.core.svnstorage;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.ICommentProvider;
import org.eclipse.team.svn.core.resource.IFolderChange;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;

/**
 * Folder change descriptor
 * 
 * @author Alexander Gurov
 */
public class SVNFolderChange extends SVNLocalFolder implements IFolderChange {
	protected SVNRevision pegRevision;

	protected IRepositoryResource originator;

	protected String comment;

	protected ICommentProvider provider;

	public SVNFolderChange(IResource resource, long revision, String textStatus, String propStatus, int changeMask,
			String author, long lastCommitDate, SVNConflictDescriptor treeConflictDescriptor, SVNRevision pegRevision,
			String comment) {
		super(resource, revision, revision, textStatus, propStatus, changeMask, author, lastCommitDate,
				treeConflictDescriptor);
		this.comment = comment;
		this.pegRevision = pegRevision;
	}

	@Override
	public void treatAsReplacement() {
		textStatus = IStateFilter.ST_REPLACED;
	}

	@Override
	public SVNRevision getPegRevision() {
		return pegRevision == null
				? revision != SVNRevision.INVALID_REVISION_NUMBER
						? SVNRevision.fromNumber(revision)
						: SVNRevision.INVALID_REVISION
				: pegRevision;
	}

	@Override
	public void setPegRevision(SVNRevision pegRevision) {
		this.pegRevision = pegRevision;
	}

	@Override
	public ILocalResource[] getChildren() {
		return new ILocalResource[0];
	}

	@Override
	public IRepositoryResource getOriginator() {
		if (originator == null && getRevision() != SVNRevision.INVALID_REVISION_NUMBER) {
			IRepositoryResource remote = SVNRemoteStorage.instance().asRepositoryResource(resource);
			remote.setPegRevision(getPegRevision());
			remote.setSelectedRevision(SVNRevision.fromNumber(getRevision()));
			return remote;
		}
		return originator;
	}

	@Override
	public void setOriginator(IRepositoryResource originator) {
		this.originator = originator;
	}

	@Override
	public synchronized String getComment() {
		if (comment == null && provider != null) {
			long rev = getRevision();
			comment = provider.getComment(getResource(),
					rev == SVNRevision.INVALID_REVISION_NUMBER
							? SVNRevision.INVALID_REVISION
							: SVNRevision.fromNumber(rev),
					getPegRevision());
			provider = null;
		}
		return comment;
	}

	@Override
	public void setCommentProvider(ICommentProvider provider) {
		this.provider = provider;
	}

}
