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

package org.eclipse.team.svn.core.operation.local.refactor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.ActivityCancelledException;
import org.eclipse.team.svn.core.operation.SVNResourceRuleFactory;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Copy only work files without SVN metainformation operation implementation
 * 
 * @author Alexander Gurov
 */
public class CopyResourceOperation extends AbstractActionOperation {
	protected IResource source;

	protected IResource destination;

	protected boolean skipSVNMeta;

	public CopyResourceOperation(IResource source, IResource destination) {
		this(source, destination, true);
	}

	public CopyResourceOperation(IResource source, IResource destination, boolean skipSVNMeta) {
		super("Operation_CopyLocal", SVNMessages.class); //$NON-NLS-1$
		this.source = source;
		this.destination = destination;
		this.skipSVNMeta = skipSVNMeta;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return SVNResourceRuleFactory.INSTANCE.copyRule(source, destination);
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		try {
			source.copy(destination.getFullPath(), true, monitor);
			if (skipSVNMeta) {
				FileUtility.removeSVNMetaInformation(destination, monitor);
			}
		} catch (CoreException ex) {
			if (!destination.isSynchronized(IResource.DEPTH_ZERO)) { // resource exists on disk, but not in sync with eclipse: exception shouldn't be reported
				throw new ActivityCancelledException(ex);
			}
			throw ex;
		}
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t),
				new Object[] { source.getName(), destination.toString() });
	}

}
