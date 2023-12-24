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

package org.eclipse.team.svn.core.operation.local.management;

import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.SVNResourceRuleFactory;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Disconnect operation implementation
 * 
 * @author Alexander Gurov
 */
public class DisconnectOperation extends AbstractActionOperation {
	protected IProject[] projects;

	protected boolean dropSVNFolders;

	public DisconnectOperation(IProject[] projects, boolean dropSVNFolders) {
		super("Operation_Disconnect", SVNMessages.class); //$NON-NLS-1$
		this.projects = projects;
		this.dropSVNFolders = dropSVNFolders;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		HashSet<ISchedulingRule> rules = new HashSet<>();
		for (IProject project : projects) {
			rules.add(SVNResourceRuleFactory.INSTANCE.modifyRule(project));
		}
		return new MultiRule(rules.toArray(new ISchedulingRule[rules.size()]));
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		for (final IProject current : projects) {
			this.protectStep(monitor1 -> {
				if (RepositoryProvider.isShared(current)) { // it seems sometimes projects could be unmapped prior to running this code, for example by an outside activity (see bug #403385)
					RepositoryProvider.unmap(current);
				}
				if (dropSVNFolders) {
					FileUtility.removeSVNMetaInformation(current, null);
				}
			}, monitor, projects.length);
		}
	}

}
