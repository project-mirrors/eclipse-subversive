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
	
	public static final int KIND_ROOT = 4;
	
	public static final int KIND_LOCATION_ROOT = 0;
	
	public static final int KIND_TRUNK = 1;
	
	public static final int KIND_BRANCHES = 2;
	
	public static final int KIND_TAGS = 3;
	
	public int getKind();
	
}
