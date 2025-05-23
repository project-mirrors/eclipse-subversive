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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.operation.local.change.IActionOperationProcessor;
import org.eclipse.team.svn.core.operation.local.change.IResourceChangeVisitor;
import org.eclipse.team.svn.core.operation.local.change.ResourceChange;
import org.eclipse.team.svn.core.operation.local.property.GetPropertiesOperation;
import org.eclipse.team.svn.core.operation.local.property.RemovePropertiesOperation;
import org.eclipse.team.svn.core.operation.local.property.SetPropertiesOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;

/**
 * Restores properties
 * 
 * @author Alexander Gurov
 */
public class RestorePropertiesVisitor implements IResourceChangeVisitor {

	@Override
	public void preVisit(ResourceChange change, IActionOperationProcessor processor, IProgressMonitor monitor)
			throws Exception {
	}

	@Override
	public void postVisit(ResourceChange change, IActionOperationProcessor processor, IProgressMonitor monitor)
			throws Exception {
		ILocalResource local = change.getLocal();
		if (IStateFilter.SF_VERSIONED.accept(local)) {
			//remove remote properties
			GetPropertiesOperation getProp = new GetPropertiesOperation(local.getResource());
			processor.doOperation(getProp, monitor);
			SVNProperty[] remoteProperties = getProp.getProperties();
			if (remoteProperties != null && remoteProperties.length > 0) {
				RemovePropertiesOperation removeProp = new RemovePropertiesOperation(
						new IResource[] { local.getResource() }, remoteProperties, false);
				processor.doOperation(removeProp, monitor);
			}
			//add local properties
			if (change.getProperties() != null && change.getProperties().length > 0) {
				processor.doOperation(new SetPropertiesOperation(new IResource[] { local.getResource() },
						change.getProperties(), false), monitor);
			}
		}
	}

}
