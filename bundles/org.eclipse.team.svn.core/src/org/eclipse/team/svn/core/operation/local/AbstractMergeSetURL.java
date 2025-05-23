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

/**
 * Abstract URL-based merge set
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractMergeSetURL extends AbstractMergeSet {
	public final boolean ignoreAncestry;

	public final SVNDepth depth;

	public final boolean recordOnly;

	public AbstractMergeSetURL(IResource[] to, boolean ignoreAncestry, boolean recordOnly, SVNDepth depth) {
		super(to);

		this.ignoreAncestry = ignoreAncestry;
		this.recordOnly = recordOnly;
		this.depth = depth;
	}

}
