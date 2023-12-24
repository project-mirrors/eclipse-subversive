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

package org.eclipse.team.svn.core.operation.remote;

import org.eclipse.osgi.util.NLS;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;

/**
 * Abstract repository operation implementation
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractRepositoryOperation extends AbstractActionOperation {
	private IRepositoryResource[] resources;

	private IRepositoryResourceProvider provider;

	public AbstractRepositoryOperation(String operationName, Class<? extends NLS> messagesClass,
			IRepositoryResource[] resources) {
		super(operationName, messagesClass);
		this.resources = resources;
	}

	public AbstractRepositoryOperation(String operationName, Class<? extends NLS> messagesClass,
			IRepositoryResourceProvider provider) {
		super(operationName, messagesClass);
		this.provider = provider;
	}

	protected IRepositoryResource[] operableData() {
		return this.resources == null ? this.provider.getRepositoryResources() : this.resources;
	}

}
