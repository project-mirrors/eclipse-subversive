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

package org.eclipse.team.svn.ui;

import org.eclipse.team.svn.core.resource.IRepositoryResource;

/**
 * Remote resource transferrable implementation
 * 
 * @author Alexander Gurov
 */
public class RemoteResourceTransferrable {
	public static final int OP_NONE = 0xff;

	public static final int OP_COPY = 0;

	public static final int OP_CUT = 1;

	public final IRepositoryResource[] resources;

	public final int operation;

	public RemoteResourceTransferrable(IRepositoryResource[] resources, int operation) {
		this.resources = resources;
		this.operation = operation;
	}

}
