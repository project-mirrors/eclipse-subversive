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

package org.eclipse.team.svn.core.resource;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNRevision;

/**
 * Allow us to generate ILocalResource-based changes independently from real information provider structure.
 * 
 * @author Alexander Gurov
 */
public interface IChangeStateProvider {
	public String getLocalPath();
	public SVNEntry.Kind getNodeKind();
	public SVNEntryStatus.Kind getPropertiesChangeType();
	public SVNEntryStatus.Kind getTextChangeType();
	public SVNRevision.Number getChangeRevision();
	public String getChangeAuthor();
	public String getComment();
	public long getChangeDate();
	public boolean isCopied();
	public boolean isSwitched();
	public IResource getExact(IResource []set);
	SVNConflictDescriptor getTreeConflictDescriptor();
}
