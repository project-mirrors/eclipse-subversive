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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local.refactor;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.SVNResourceRuleFactory;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Copy only work files without SVN meta information operation implementation
 * 
 * We can't call IResource API to make copying because of restrictions of caller context so we use file system methods.
 * 
 * @author Igor Burilo
 */
public class CopyResourceFromHookOperation extends AbstractActionOperation {
	protected IResource source;

	protected IResource destination;

	protected int options;

	public CopyResourceFromHookOperation(IResource source, IResource destination, int options) {
		super("Operation_CopyResourceFromHook", SVNMessages.class); //$NON-NLS-1$
		this.source = source;
		this.destination = destination;
		this.options = options;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return SVNResourceRuleFactory.INSTANCE.copyRule(source, destination);
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t),
				new Object[] { source.getName(), destination.toString() });
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		//If we copy folder, then copy it to its parent
		IResource toResource = destination;
		if (toResource instanceof IContainer) {
			toResource = toResource.getParent();
			if (toResource == null) {
				String errMessage = SVNMessages.formatErrorString("Error_NoParent", //$NON-NLS-1$
						new String[] { destination.getFullPath().toString() });
				throw new UnreportableException(errMessage);
			}
		}
		FileUtility.copyAll(new File(FileUtility.getWorkingCopyPath(toResource)),
				new File(FileUtility.getWorkingCopyPath(source)), options, pathname -> !pathname.getName().equals(SVNUtility.getSVNFolderName()), monitor);
	}
}
