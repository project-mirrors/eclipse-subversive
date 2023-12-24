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
 *    Panagiotis Korros - [patch] optimization: reduces memory consumption by 15%
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.svnstorage;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;
import org.eclipse.team.svn.core.resource.ILocalResource;

/**
 * Working copy resource represenation
 * 
 * @author Alexander Gurov
 */
public abstract class SVNLocalResource implements ILocalResource {
	protected IResource resource;

	protected long revision;

	protected long baseRevision;

	protected String textStatus;

	protected String propStatus;

	protected int changeMask;

	protected String author;

	protected long lastCommitDate;

	protected SVNConflictDescriptor treeConflictDescriptor;

	protected SVNLocalResource(IResource resource, long revision, long baseRevision, String textStatus,
			String propStatus, int changeMask, String author, long lastCommitDate,
			SVNConflictDescriptor treeConflictDescriptor) {
		this.resource = resource;
		this.revision = revision;
		this.baseRevision = baseRevision;
		this.textStatus = textStatus;
		this.propStatus = propStatus;
		this.changeMask = changeMask;
		this.author = author != null ? author.intern() : null;
		this.lastCommitDate = lastCommitDate;
		this.treeConflictDescriptor = treeConflictDescriptor;
	}

	@Override
	public boolean isLocked() {
		return (changeMask & ILocalResource.IS_LOCKED) != 0;
	}

	@Override
	public IResource getResource() {
		return resource;
	}

	@Override
	public String getName() {
		return resource.getName();
	}

	@Override
	public long getRevision() {
		return revision;
	}

	@Override
	public long getBaseRevision() {
		return baseRevision;
	}

	@Override
	public String getTextStatus() {
		return textStatus;
	}

	@Override
	public String getPropStatus() {
		return propStatus;
	}

	@Override
	public String getStatus() {
		return SVNRemoteStorage.getCompoundStatusString(textStatus, propStatus);
	}

	@Override
	public int getChangeMask() {
		return changeMask;
	}

	@Override
	public boolean isCopied() {
		return (changeMask & ILocalResource.IS_COPIED) != 0;
	}

	@Override
	public String getAuthor() {
		return author;
	}

	@Override
	public long getLastCommitDate() {
		return lastCommitDate;
	}

	@Override
	public boolean hasTreeConflict() {
		return treeConflictDescriptor != null && treeConflictDescriptor.conflictKind == SVNConflictDescriptor.Kind.TREE;
	}

	@Override
	public SVNConflictDescriptor getTreeConflictDescriptor() {
		return treeConflictDescriptor;
	}

	@Override
	public String toString() {
		return resource.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ILocalResource) {
			return resource.equals(((ILocalResource) obj).getResource());
		}
		return false;
	}

}
