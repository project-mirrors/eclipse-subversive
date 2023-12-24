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

/**
 * Any repository root (branches, tags, repository location)
 * 
 * @author Alexander Gurov
 */
public interface IRepositoryRoot extends IRepositoryContainer {

	int KIND_ROOT = 4;

	int KIND_LOCATION_ROOT = 0;

	int KIND_TRUNK = 1;

	int KIND_BRANCHES = 2;

	int KIND_TAGS = 3;

	int getKind();

}
