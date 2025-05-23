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

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Checkout content into the project which have a name equals to the remote resource name
 * 
 * @author Alexander Gurov
 */
public class CheckoutOperation extends AbstractActionOperation implements IResourceProvider {
	protected IProject[] projects;

	protected CheckoutAsOperation[] operations;

	protected ISchedulingRule rule;

	public CheckoutOperation(Map<String, IRepositoryResource> checkoutMap, boolean respectHierarchy, String location,
			SVNDepth recureDepth, boolean ignoreExternals) {
		super("Operation_CheckOut", SVNMessages.class); //$NON-NLS-1$

		ArrayList<IProject> projects = new ArrayList<>();
		ArrayList<CheckoutAsOperation> operations = new ArrayList<>();
		ArrayList<ISchedulingRule> rules = new ArrayList<>();
		for (Map.Entry<String, IRepositoryResource> entry : checkoutMap.entrySet()) {
			CheckoutAsOperation coOp = CheckoutOperation.getCheckoutAsOperation(entry.getKey(), entry.getValue(),
					respectHierarchy, location, recureDepth, ignoreExternals);
			operations.add(coOp);
			projects.add(coOp.getProject());
			rules.add(coOp.getSchedulingRule());
		}
		rule = new MultiRule(rules.toArray(new ISchedulingRule[rules.size()]));
		this.projects = projects.toArray(new IProject[projects.size()]);
		this.operations = operations.toArray(new CheckoutAsOperation[operations.size()]);
	}

	@Override
	public int getOperationWeight() {
		return 19;
	}

	@Override
	public IResource[] getResources() {
		return projects;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return rule;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		for (int i = 0; i < operations.length && !monitor.isCanceled(); i++) {
			operations[i].setConsoleStream(getConsoleStream());
			ProgressMonitorUtility.doTask(operations[i], monitor, IActionOperation.DEFAULT_WEIGHT * projects.length,
					IActionOperation.DEFAULT_WEIGHT);
			this.reportStatus(operations[i].getStatus());
		}
	}

	public static CheckoutAsOperation getCheckoutAsOperation(String name, IRepositoryResource currentResource,
			boolean respectHierarchy, String location, SVNDepth recureDepth, boolean ignoreExternals) {
		if (location != null && location.trim().length() > 0) {
			return new CheckoutAsOperation(name, currentResource, respectHierarchy, location, recureDepth,
					ignoreExternals);
		}
		return new CheckoutAsOperation(name, currentResource, recureDepth, ignoreExternals);
	}

}
