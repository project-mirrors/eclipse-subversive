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

package org.eclipse.team.svn.core.resource;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.connector.SVNRevision;

/**
 * Deffered in time comment acquisition: SVN does not provide comment while synchronizing with repository
 * 
 * @author Alexander Gurov
 */
public interface ICommentProvider {
	String getComment(IResource resource, SVNRevision rev, SVNRevision peg);
}
