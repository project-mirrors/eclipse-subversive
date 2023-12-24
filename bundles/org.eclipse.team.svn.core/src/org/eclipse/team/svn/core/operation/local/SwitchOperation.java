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

package org.eclipse.team.svn.core.operation.local;

import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamProvider;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNConflictDetectionProgressMonitor;
import org.eclipse.team.svn.core.operation.remote.AbstractRepositoryOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Switch working copy base url operation implementation
 * 
 * @author Alexander Gurov
 */
public class SwitchOperation extends AbstractRepositoryOperation implements IUnresolvedConflictDetector {
	protected IResource[] resources;

	protected SVNDepth depth;

	protected long options;

	protected UnresolvedConflictDetectorHelper conflictDetectorHelper;

	public SwitchOperation(IResource[] resources, IRepositoryResourceProvider destination, SVNDepth depth,
			boolean isStickyDepth, boolean ignoreExternals) {
		this(resources, destination, depth,
				(ignoreExternals ? ISVNConnector.Options.IGNORE_EXTERNALS : ISVNConnector.Options.NONE)
						| (isStickyDepth ? ISVNConnector.Options.DEPTH_IS_STICKY : ISVNConnector.Options.NONE));
	}

	public SwitchOperation(IResource[] resources, IRepositoryResource[] destination, SVNDepth depth,
			boolean isStickyDepth, boolean ignoreExternals) {
		this(resources, destination, depth,
				(ignoreExternals ? ISVNConnector.Options.IGNORE_EXTERNALS : ISVNConnector.Options.NONE)
						| (isStickyDepth ? ISVNConnector.Options.DEPTH_IS_STICKY : ISVNConnector.Options.NONE));
	}

	public SwitchOperation(IResource[] resources, IRepositoryResourceProvider destination, SVNDepth depth,
			long options) {
		super("Operation_Switch", SVNMessages.class, destination); //$NON-NLS-1$
		this.resources = resources;
		this.depth = depth;
		this.options = options & ISVNConnector.CommandMasks.SWITCH;
		conflictDetectorHelper = new UnresolvedConflictDetectorHelper();
	}

	public SwitchOperation(IResource[] resources, IRepositoryResource[] destination, SVNDepth depth, long options) {
		super("Operation_Switch", SVNMessages.class, destination); //$NON-NLS-1$
		this.resources = resources;
		this.depth = depth;
		this.options = options & ISVNConnector.CommandMasks.SWITCH;
		conflictDetectorHelper = new UnresolvedConflictDetectorHelper();
	}

	@Override
	public int getOperationWeight() {
		return 19;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		HashSet<IResource> ruleSet = new HashSet<>();
		for (IResource element : resources) {
			ruleSet.add(element instanceof IProject ? element : element.getParent());
		}
		return new MultiRule(ruleSet.toArray(new IResource[ruleSet.size()]));
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource[] destinations = operableData();
		for (int i = 0; i < resources.length; i++) {
			final IResource resource = resources[i];
			final IRepositoryResource destination = destinations[i];
			final IRepositoryLocation location = destination.getRepositoryLocation();
			final ISVNConnector proxy = location.acquireSVNProxy();

			this.protectStep(monitor1 -> {
				String wcPath = FileUtility.getWorkingCopyPath(resource);
				SwitchOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
						"svn switch \"" + destination.getUrl() + "\" \"" + FileUtility.normalizePath(wcPath) //$NON-NLS-1$//$NON-NLS-2$
								+ "\" -r " + destination.getSelectedRevision() //$NON-NLS-1$
								+ ISVNConnector.Options.asCommandLine(options)
								+ SVNUtility.getDepthArg(depth, options)
								+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
				proxy.switchTo(wcPath, SVNUtility.getEntryRevisionReference(destination), depth, options,
						new ConflictDetectionProgressMonitor(SwitchOperation.this, monitor1, null));

				if (resource instanceof IProject) {
					SVNTeamProvider provider = (SVNTeamProvider) RepositoryProvider
							.getProvider((IProject) resource);
					provider.switchResource(destination);
				}
			}, monitor, resources.length);

			location.releaseSVNProxy(proxy);
		}
	}

	@Override
	public void setUnresolvedConflict(boolean hasUnresolvedConflict) {
		conflictDetectorHelper.setUnresolvedConflict(hasUnresolvedConflict);
	}

	@Override
	public boolean hasUnresolvedConflicts() {
		return conflictDetectorHelper.hasUnresolvedConflicts();
	}

	@Override
	public String getMessage() {
		return conflictDetectorHelper.getMessage();
	}

	@Override
	public IResource[] getUnprocessed() {
		return conflictDetectorHelper.getUnprocessed();
	}

	@Override
	public IResource[] getProcessed() {
		return conflictDetectorHelper.getProcessed();
	}

	protected void defineInitialResourceSet(IResource[] resources) {
		conflictDetectorHelper.defineInitialResourceSet(resources);
	}

	@Override
	public void addUnprocessed(IResource unprocessed) {
		conflictDetectorHelper.addUnprocessed(unprocessed);
	}

	@Override
	public void setConflictMessage(String message) {
		conflictDetectorHelper.setConflictMessage(message);
	}

	@Override
	public void removeProcessed(IResource resource) {
		conflictDetectorHelper.removeProcessed(resource);
	}

	protected class ConflictDetectionProgressMonitor extends SVNConflictDetectionProgressMonitor {
		public ConflictDetectionProgressMonitor(IActionOperation parent, IProgressMonitor monitor, IPath root) {
			super(parent, monitor, root);
		}

		@Override
		protected void processConflict(ItemState state) {
			setUnresolvedConflict(true);
		}
	}
}
