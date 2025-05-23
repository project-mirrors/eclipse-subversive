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

package org.eclipse.team.svn.core.synchronize.variant;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.svn.core.resource.ILocalResource;

/**
 * Base WC folder revision variant
 * 
 * @author Alexander Gurov
 */
public class BaseFolderVariant extends ResourceVariant {

	public BaseFolderVariant(ILocalResource local) {
		super(local);
	}

	@Override
	protected void fetchContents(IProgressMonitor monitor) throws TeamException {

	}

	@Override
	public boolean isContainer() {
		return true;
	}

}
