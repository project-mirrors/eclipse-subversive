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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.properties.PropertiesView;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Show properties operation
 *
 * @author Sergiy Logvin
 */
public class ShowPropertiesOperation extends AbstractActionOperation {
	protected IAdaptable resource;

	protected IRepositoryResourceProvider provider;

	protected IWorkbenchPage page;

	protected IResourcePropertyProvider propertyProvider;

	public ShowPropertiesOperation(IWorkbenchPage page, IRepositoryResourceProvider provider,
			IResourcePropertyProvider propertyProvider) {
		this(page, (IRepositoryResource) null, propertyProvider);
		this.provider = provider;
	}

	public ShowPropertiesOperation(IWorkbenchPage page, IAdaptable resource,
			IResourcePropertyProvider propertyProvider) {
		super("Operation_ShowProperties", SVNUIMessages.class); //$NON-NLS-1$
		this.resource = resource;
		this.page = page;
		this.propertyProvider = propertyProvider;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		if (provider != null) {
			resource = provider.getRepositoryResources()[0];
		}

		UIMonitorUtility.getDisplay().syncExec(() -> ShowPropertiesOperation.this.protectStep(monitor1 -> {
			PropertiesView view = (PropertiesView) page.showView(PropertiesView.VIEW_ID);
			view.setResource(resource, propertyProvider, false);
		}, monitor, 1));
	}

}