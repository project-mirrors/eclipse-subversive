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

package org.eclipse.team.svn.core.operation.file.refactor;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.operation.file.AbstractFileOperation;
import org.eclipse.team.svn.core.operation.file.IFileProvider;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Copy files between working copies
 * 
 * @author Alexander Gurov
 */
public class CopyOperation extends AbstractFileOperation {
	protected File localTo;

	protected boolean forceNonSVN;

	public CopyOperation(File[] files, File localTo, boolean forceNonSVN) {
		super("Operation_CopyFile", SVNMessages.class, files); //$NON-NLS-1$
		this.localTo = localTo;
		this.forceNonSVN = forceNonSVN;
	}

	public CopyOperation(IFileProvider provider, File localTo, boolean forceNonSVN) {
		super("Operation_CopyFile", SVNMessages.class, provider); //$NON-NLS-1$
		this.localTo = localTo;
		this.forceNonSVN = forceNonSVN;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		ISchedulingRule parentRule = super.getSchedulingRule();
		return MultiRule.combine(new LockingRule(localTo), parentRule);
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File[] files = operableData();

		IRepositoryResource remoteTo = SVNFileStorage.instance().asRepositoryResource(localTo, true);
		IRepositoryLocation location = remoteTo == null ? null : remoteTo.getRepositoryLocation();
		final ISVNConnector proxy = location == null ? null : location.acquireSVNProxy();

		for (int i = 0; i < files.length && !monitor.isCanceled(); i++) {
			final File current = files[i];
			this.protectStep(monitor1 -> {
				IRepositoryResource remote = SVNFileStorage.instance().asRepositoryResource(current, true);
				File checked = CopyOperation.this.getCopyTo(current);
				if (remote == null || proxy == null || forceNonSVN) {
					CopyOperation.this.nonSVNCopy(current, monitor1);
				} else {
					proxy.copyLocal(
							new SVNEntryRevisionReference[] { new SVNEntryRevisionReference(
									current.getAbsolutePath(), null, SVNRevision.WORKING) },
							checked.getAbsolutePath(), ISVNConnector.Options.NONE,
							ISVNConnector.NO_EXTERNALS_TO_PIN,
							new SVNProgressMonitor(CopyOperation.this, monitor1, null));
				}
			}, monitor, files.length);
		}

		if (location != null) {
			location.releaseSVNProxy(proxy);
		}
	}

	protected File getCopyTo(File what) {
		File checked = new File(localTo.getAbsolutePath() + "/" + what.getName()); //$NON-NLS-1$
		if (checked.exists()) {
			String message = getNationalizedString("Error_AlreadyExists"); //$NON-NLS-1$
			throw new UnreportableException(BaseMessages.format(message, new Object[] { checked.getAbsolutePath() }));
		}
		return checked;
	}

	protected void nonSVNCopy(File what, IProgressMonitor monitor) throws Exception {
		FileUtility.copyAll(localTo, what, FileUtility.COPY_NO_OPTIONS, pathname -> !pathname.getName().equals(SVNUtility.getSVNFolderName()), monitor);
	}

}
