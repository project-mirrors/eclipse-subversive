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

package org.eclipse.team.svn.ui.action;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.options.IOptionProvider;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Abstract team action implementation with the recursive resources selection
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractRecursiveTeamAction extends AbstractNonRecursiveTeamAction implements IResourceSelector {

	public AbstractRecursiveTeamAction() {
	}

	@Override
	public IResource[] getSelectedResourcesRecursive(IStateFilter filter) {
		return this.getSelectedResourcesRecursive(filter, IResource.DEPTH_INFINITE);
	}

	@Override
	public IResource[] getSelectedResourcesRecursive(final IStateFilter filter, final int depth) {
		final IResource[][] retVal = { new IResource[0] };
		IActionOperation op = new AbstractActionOperation("Operation_CollectingResources", SVNUIMessages.class) { //$NON-NLS-1$
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				retVal[0] = FileUtility.getResourcesRecursive(AbstractRecursiveTeamAction.this.getSelectedResources(),
						filter, depth, this, monitor);
			}
		};
		if (CoreExtensionsManager.instance().getOptionProvider().is(IOptionProvider.SVN_CACHE_ENABLED)) {
			UIMonitorUtility.doTaskBusyDefault(op);
		} else {
			UIMonitorUtility.doTaskNowDefault(getShell(), op, true);
		}
		return retVal[0];
	}

}
