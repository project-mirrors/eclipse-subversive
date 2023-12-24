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

package org.eclipse.team.svn.core.operation;

import org.eclipse.team.svn.core.resource.IRepositoryLocation;

/**
 * Interface which will allow to return produced revision number for the operations which modify repository
 * 
 * @author Alexander Gurov
 */
public interface IRevisionProvider {
	public static class RevisionPair {
		public final long revision;

		public final String[] paths;

		public final IRepositoryLocation location;

		public RevisionPair(long revision, String[] paths, IRepositoryLocation location) {
			this.revision = revision;
			this.paths = paths;
			this.location = location;
		}
	}

	public RevisionPair[] getRevisions();

}
