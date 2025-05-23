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

package org.eclipse.team.svn.core.operation.local.change;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.resource.ILocalFile;

/**
 * Local file change store
 * 
 * @author Alexander Gurov
 */
public class FileChange extends ResourceChange {
	public FileChange(ResourceChange parent, ILocalFile local, boolean needsTemporary) {
		super(parent, local, needsTemporary);
	}

	@Override
	protected void preTraverse(IResourceChangeVisitor visitor, int depth, IActionOperationProcessor processor,
			IProgressMonitor monitor) throws Exception {
		visitor.preVisit(this, processor, monitor);
	}

	@Override
	protected void postTraverse(IResourceChangeVisitor visitor, int depth, IActionOperationProcessor processor,
			IProgressMonitor monitor) throws Exception {
		visitor.postVisit(this, processor, monitor);
	}

}
