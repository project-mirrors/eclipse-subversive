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

package org.eclipse.team.svn.core.operation.remote;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.AbstractGetFileContentOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ExternalProgramParameters;
import org.eclipse.team.svn.core.operation.local.IExecutable;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.DetectExternalCompareOperation;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.ExternalCompareOperationHelper;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.IExternalProgramParametersProvider;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider.DefaultRepositoryResourceProvider;

/**
 * Run external repository compare editor operation implementation
 *
 * @author Igor Burilo
 */
public class RunExternalRepositoryCompareOperation extends CompositeOperation implements IExecutable {

	protected ExternalCompareRepositoryOperation externalCompareOperation;

	public RunExternalRepositoryCompareOperation(final IRepositoryResourceProvider provider,
			DiffViewerSettings diffSettings) {
		super("Operation_ExternalRepositoryCompare", SVNMessages.class); //$NON-NLS-1$

		final DetectExternalCompareOperation detectOperation = new DetectExternalCompareOperation(provider,
				diffSettings);
		this.add(detectOperation);

		externalCompareOperation = new ExternalCompareRepositoryOperation(provider, detectOperation);
		this.add(externalCompareOperation, new IActionOperation[] { detectOperation });
	}

	@Override
	public boolean isExecuted() {
		return externalCompareOperation.isExecuted();
	}

	/**
	 * Prepare files and run external compare editor
	 *
	 * @author Igor Burilo
	 */
	public static class ExternalCompareRepositoryOperation extends CompositeOperation {

		protected IExternalProgramParametersProvider parametersProvider;

		protected ExternalProgramParameters externalProgramParams;

		protected boolean isExecuted;

		public ExternalCompareRepositoryOperation(IRepositoryResource left, IRepositoryResource right,
				IExternalProgramParametersProvider parametersProvider) {
			this(new DefaultRepositoryResourceProvider(new IRepositoryResource[] { right, left }), parametersProvider);
		}

		public ExternalCompareRepositoryOperation(final IRepositoryResourceProvider resourcesProvider,
				IExternalProgramParametersProvider parametersProvider) {
			super("Operation_ExternalRepositoryCompare", SVNMessages.class); //$NON-NLS-1$
			this.parametersProvider = parametersProvider;

			//get files operations
			final AbstractGetFileContentOperation nextFileGetOp = new GetFileContentOperation(
					() -> new IRepositoryResource[] { resourcesProvider.getRepositoryResources()[1] });
			this.add(nextFileGetOp);

			final AbstractGetFileContentOperation prevFileGetOp = new GetFileContentOperation(
					() -> new IRepositoryResource[] { resourcesProvider.getRepositoryResources()[0] });
			this.add(prevFileGetOp, new IActionOperation[] { nextFileGetOp });

			//Run external program operation
			this.add(new AbstractActionOperation("Operation_ExternalRepositoryCompare", SVNMessages.class) { //$NON-NLS-1$
				@Override
				protected void runImpl(IProgressMonitor monitor) throws Exception {
					ExternalCompareOperationHelper externalRunHelper = new ExternalCompareOperationHelper(
							prevFileGetOp.getTemporaryPath(), nextFileGetOp.getTemporaryPath(), null, null,
							externalProgramParams);
					externalRunHelper.execute(monitor);
				}
			}, new IActionOperation[] { nextFileGetOp, prevFileGetOp });
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			externalProgramParams = parametersProvider.getExternalProgramParameters();
			if (externalProgramParams != null) {
				isExecuted = true;
				super.runImpl(monitor);
			} else {
				isExecuted = false;
			}
		}

		public boolean isExecuted() {
			return isExecuted;
		}
	}

}