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

import org.eclipse.team.svn.core.connector.SVNRevision;

/**
 * Resource change descriptor interface
 * 
 * @author Alexander Gurov
 */
public interface IResourceChange extends ILocalResource {
	SVNRevision getPegRevision();

	void setPegRevision(SVNRevision pegRevision);

	String getComment();

	IRepositoryResource getOriginator();

	void setOriginator(IRepositoryResource originator);

	void setCommentProvider(ICommentProvider provider);

	void treatAsReplacement();
}
