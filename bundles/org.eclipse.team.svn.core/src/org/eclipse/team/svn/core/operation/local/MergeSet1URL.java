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

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
import org.eclipse.team.svn.core.resource.IRepositoryResource;

/**
 * 1URL-based merge set
 * 
 * @author Alexander Gurov
 */
public class MergeSet1URL extends AbstractMergeSetURL {
	public final IRepositoryResource[] from;

	public final SVNRevisionRange[] revisions;

	public MergeSet1URL(IResource[] to, IRepositoryResource[] from, SVNRevisionRange[] revisions,
			boolean ignoreAncestry, boolean recordOnly, SVNDepth depth) {
		super(to, ignoreAncestry, recordOnly, depth);
		this.from = from;
		this.revisions = revisions;
	}

}
