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
	String getLocalPath();

	SVNEntry.Kind getNodeKind();

	SVNEntryStatus.Kind getPropertiesChangeType();

	SVNEntryStatus.Kind getTextChangeType();

	SVNRevision.Number getChangeRevision();

	String getChangeAuthor();

	String getComment();

	long getChangeDate();

	boolean isCopied();

	boolean isSwitched();

	IResource getExact(IResource[] set);

	SVNConflictDescriptor getTreeConflictDescriptor();
}
