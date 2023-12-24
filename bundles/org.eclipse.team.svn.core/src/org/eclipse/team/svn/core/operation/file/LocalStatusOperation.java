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

package org.eclipse.team.svn.core.operation.file;

import java.io.File;

import org.eclipse.team.svn.core.SVNMessages;

/**
 * Working copy resources status
 * 
 * @author Alexander Gurov
 */
public class LocalStatusOperation extends AbstractStatusOperation {
	public LocalStatusOperation(File[] files, boolean recursive) {
		super("Operation_LocalStatusFile", SVNMessages.class, files, recursive); //$NON-NLS-1$
	}

	public LocalStatusOperation(IFileProvider provider, boolean recursive) {
		super("Operation_LocalStatusFile", SVNMessages.class, provider, recursive); //$NON-NLS-1$
	}

	@Override
	protected boolean isRemote() {
		return false;
	}

}
