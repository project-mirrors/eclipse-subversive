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

package org.eclipse.team.svn.core.operation.local.change.visitors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.local.change.IActionOperationProcessor;
import org.eclipse.team.svn.core.operation.local.change.IResourceChangeVisitor;
import org.eclipse.team.svn.core.operation.local.change.ResourceChange;
import org.eclipse.team.svn.core.operation.local.property.GetPropertiesOperation;
import org.eclipse.team.svn.core.resource.ILocalFolder;
import org.eclipse.team.svn.core.resource.ILocalResource;

/**
 * Saves properties into change model
 * 
 * @author Alexander Gurov
 */
public class SavePropertiesVisitor implements IResourceChangeVisitor {
	protected boolean foldersOnly;

	public SavePropertiesVisitor() {
		this(false);
	}

	public SavePropertiesVisitor(boolean foldersOnly) {
		this.foldersOnly = foldersOnly;
	}

	@Override
	public void preVisit(ResourceChange change, IActionOperationProcessor processor, IProgressMonitor monitor)
			throws Exception {
		if (foldersOnly && !(change.getLocal() instanceof ILocalFolder)) {
			return;
		}
		ILocalResource local = change.getLocal();
		if (IStateFilter.SF_VERSIONED.accept(local)) {
			GetPropertiesOperation getProp = new GetPropertiesOperation(local.getResource());
			processor.doOperation(getProp, monitor);
			change.setProperties(getProp.getProperties());
		}
	}

	@Override
	public void postVisit(ResourceChange change, IActionOperationProcessor processor, IProgressMonitor monitor)
			throws Exception {
	}

}
