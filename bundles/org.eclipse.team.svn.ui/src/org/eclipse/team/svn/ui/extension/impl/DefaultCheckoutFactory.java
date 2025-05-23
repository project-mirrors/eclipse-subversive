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

package org.eclipse.team.svn.ui.extension.impl;

import java.util.HashMap;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.LocateProjectsOperation;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.remote.CheckoutAction;
import org.eclipse.team.svn.ui.extension.factory.ICheckoutFactory;

/**
 * Default implementation
 * 
 * @author Alexander Gurov
 */
public class DefaultCheckoutFactory implements ICheckoutFactory {

	public DefaultCheckoutFactory() {

	}

	// for common reasons should be implemented as post-processing for checkout action, but while tracker is Eclipse project we should make it the best as we can...
	@Override
	public LocateProjectsOperation.ILocateFilter getLocateFilter() {
		return new EclipseProjectsFilter();
	}

	@Override
	public IActionOperation getCheckoutOperation(Shell shell, IRepositoryResource[] remote, HashMap checkoutMap,
			boolean respectHierarchy, String location, SVNDepth recurseDepth, boolean ignoreExternals) {
		return CheckoutAction.getCheckoutOperation(shell, remote, checkoutMap, respectHierarchy, location, recurseDepth,
				ignoreExternals);
	}

	protected class EclipseProjectsFilter implements LocateProjectsOperation.ILocateFilter {
		@Override
		public boolean isProject(IRepositoryResource remote, IRepositoryResource[] children) {
			for (IRepositoryResource child : children) {
				if (child instanceof IRepositoryFile && child.getName().equals(".project")) { //$NON-NLS-1$
					return true;
				}
			}
			return false;
		}

	}

	@Override
	public ITableLabelProvider getLabelProvider(HashMap resources2names) {
		return new LabelProvider(resources2names);
	}

	protected class LabelProvider implements ITableLabelProvider {
		protected HashMap resources2names;

		public LabelProvider(HashMap resources2names) {
			this.resources2names = resources2names;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void dispose() {
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			IRepositoryResource resource = (IRepositoryResource) element;
			switch (columnIndex) {
				case 1: {
					return resource.getUrl();
				}
				case 2: {
					return FileUtility.formatResourceName((String) resources2names.get(resource));
				}
				case 3: {
					return SVNUIMessages.DefaultCheckoutFactory_EclipseProject;
				}
			}
			return ""; //$NON-NLS-1$
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	@Override
	public IRepositoryResourceProvider additionalProcessing(CompositeOperation op,
			IRepositoryResourceProvider provider) {
		return provider;
	}

	@Override
	public HashMap prepareName2resources(HashMap name2resources) {
		return name2resources;
	}

	@Override
	public boolean findProjectsOptionEnabled() {
		return false;
	}

}
