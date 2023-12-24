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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNProgressMonitor;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryReference;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * JavaHL-mode merge implementation
 * 
 * @author Alexander Gurov
 */
public class JavaHLMergeOperation extends AbstractWorkingCopyOperation {
	protected IRepositoryResourceProvider from;

	protected IRepositoryResourceProvider fromEnd;

	protected SVNRevisionRange[] revisions;

	protected SVNDepth depth;

	protected long options;

	protected ISVNProgressMonitor externalMonitor;

	public JavaHLMergeOperation(IResource[] localTo, IRepositoryResourceProvider from, SVNRevisionRange[] revisions,
			boolean dryRun, boolean ignoreAncestry, SVNDepth depth) {
		this(localTo, from, revisions, depth,
				(dryRun ? ISVNConnector.Options.SIMULATE : ISVNConnector.Options.NONE)
						| (ignoreAncestry ? ISVNConnector.Options.IGNORE_ANCESTRY : ISVNConnector.Options.NONE)
						| ISVNConnector.Options.ALLOW_MIXED_REVISIONS);
	}

	public JavaHLMergeOperation(IResource[] localTo, IRepositoryResourceProvider fromStart,
			IRepositoryResourceProvider fromEnd, boolean dryRun, boolean ignoreAncestry, SVNDepth depth) {
		this(localTo, fromStart, fromEnd, depth,
				(dryRun ? ISVNConnector.Options.SIMULATE : ISVNConnector.Options.NONE)
						| (ignoreAncestry ? ISVNConnector.Options.IGNORE_ANCESTRY : ISVNConnector.Options.NONE)
						| ISVNConnector.Options.ALLOW_MIXED_REVISIONS);
	}

	public JavaHLMergeOperation(IResource[] localTo, IRepositoryResourceProvider from, boolean dryRun) {
		this(localTo, from, ISVNConnector.Options.SIMULATE | ISVNConnector.Options.ALLOW_MIXED_REVISIONS);
	}

	public JavaHLMergeOperation(IResource[] localTo, IRepositoryResourceProvider from, SVNRevisionRange[] revisions,
			SVNDepth depth, long options) {
		super("Operation_JavaHLMerge", SVNMessages.class, localTo); //$NON-NLS-1$
		this.from = from;
		this.revisions = revisions;
		this.options = options & ISVNConnector.CommandMasks.MERGE;
		this.depth = depth;
	}

	public JavaHLMergeOperation(IResource[] localTo, IRepositoryResourceProvider fromStart,
			IRepositoryResourceProvider fromEnd, SVNDepth depth, long options) {
		super("Operation_JavaHLMerge", SVNMessages.class, localTo); //$NON-NLS-1$
		from = fromStart;
		this.fromEnd = fromEnd;
		this.options = options & ISVNConnector.CommandMasks.MERGE;
		this.depth = depth;
	}

	public JavaHLMergeOperation(IResource[] localTo, IRepositoryResourceProvider from, long options) {
		super("Operation_JavaHLMerge", SVNMessages.class, localTo); //$NON-NLS-1$
		this.from = from;
		this.options = options & ISVNConnector.CommandMasks.MERGE;
	}

	public void setRecordOnly(boolean recordOnly) {
		options &= ~ISVNConnector.Options.RECORD_ONLY;
		options |= recordOnly ? ISVNConnector.Options.RECORD_ONLY : ISVNConnector.Options.NONE;
	}

	@Override
	public int getOperationWeight() {
		return 19;
	}

	public void setExternalMonitor(ISVNProgressMonitor externalMonitor) {
		this.externalMonitor = externalMonitor;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();
		IRepositoryResource[] fromStart = from.getRepositoryResources();
		IRepositoryResource[] fromEnd = this.fromEnd != null ? this.fromEnd.getRepositoryResources() : null;

		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			final IResource resource = resources[i];
			final IRepositoryResource from1 = fromStart[i];
			final IRepositoryResource from2 = fromEnd == null ? null : fromEnd[i];
			this.protectStep(monitor1 -> {
				if (from2 != null) {
					JavaHLMergeOperation.this.doMerge2URL(resource, from1, from2, monitor1);
				} else if (revisions != null) {
					JavaHLMergeOperation.this.doMerge1URL(resource, from1, monitor1);
				} else {
					JavaHLMergeOperation.this.doMergeReintegrate(resource, from1, monitor1);
				}
			}, monitor, resources.length);
		}
	}

	protected void doMerge1URL(IResource resource, IRepositoryResource from, IProgressMonitor monitor)
			throws Exception {
		IRepositoryLocation location = from.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();

		try {
			String wcPath = FileUtility.getWorkingCopyPath(resource);
			SVNEntryReference ref1 = SVNUtility.getEntryReference(from);
			String changes = ""; //$NON-NLS-1$
			String ranges = ""; //$NON-NLS-1$
			for (SVNRevisionRange range : revisions) {
				if (range.from.equals(range.to)) {
					changes += changes.length() > 0 ? "," + range.from.toString() : range.from.toString(); //$NON-NLS-1$
				} else {
					ranges += " -r " + range.from.toString() + ":" + range.to.toString(); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			if (changes.length() > 0) {
				changes = " -c " + changes; //$NON-NLS-1$
			}
			writeToConsole(IConsoleStream.LEVEL_CMD,
					"svn merge" + changes + ranges + " \"" + from.getUrl() + "@" + from.getPegRevision() + "\" \"" //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
							+ FileUtility.normalizePath(wcPath) + "\"" //$NON-NLS-1$
							+ SVNUtility.getDepthArg(depth, ISVNConnector.Options.NONE)
							+ ISVNConnector.Options.asCommandLine(options)
							+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
			proxy.merge(ref1, revisions, wcPath, depth, options, new MergeProgressMonitor(this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	protected void doMerge2URL(IResource resource, IRepositoryResource from1, IRepositoryResource from2,
			IProgressMonitor monitor) throws Exception {
		IRepositoryLocation location = from1.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();

		try {
			String wcPath = FileUtility.getWorkingCopyPath(resource);
			SVNEntryRevisionReference ref1 = SVNUtility.getEntryRevisionReference(from1);
			SVNEntryRevisionReference ref2 = SVNUtility.getEntryRevisionReference(from2);
			writeToConsole(IConsoleStream.LEVEL_CMD,
					"svn merge \"" + from1.getUrl() + "@" + from1.getSelectedRevision() + "\" \"" + from2.getUrl() + "@" //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
							+ from2.getSelectedRevision() + "\" \"" + FileUtility.normalizePath(wcPath) + "\"" //$NON-NLS-1$//$NON-NLS-2$
							+ SVNUtility.getDepthArg(depth, ISVNConnector.Options.NONE)
							+ ISVNConnector.Options.asCommandLine(options)
							+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
			proxy.mergeTwo(ref1, ref2, wcPath, depth, options, new MergeProgressMonitor(this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	protected void doMergeReintegrate(IResource resource, IRepositoryResource from1, IProgressMonitor monitor)
			throws Exception {
		IRepositoryLocation location = from1.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();

		try {
			String wcPath = FileUtility.getWorkingCopyPath(resource);
			SVNEntryReference ref1 = SVNUtility.getEntryReference(from1);
			writeToConsole(IConsoleStream.LEVEL_CMD,
					"svn merge --reintegrate \"" + from1.getUrl() + "@" + from1.getPegRevision() + "\" \"" //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
							+ FileUtility.normalizePath(wcPath) + "\"" //$NON-NLS-1$
							+ ISVNConnector.Options.asCommandLine(options)
							+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
			proxy.mergeReintegrate(ref1, wcPath, options, new MergeProgressMonitor(this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	protected class MergeProgressMonitor extends SVNProgressMonitor {
		public MergeProgressMonitor(IActionOperation parent, IProgressMonitor monitor, IPath root) {
			super(parent, monitor, root, (options & ISVNConnector.Options.SIMULATE) == 0);
		}

		@Override
		public void progress(int current, int total, ItemState state) {
			super.progress(current, total, state);
			if (externalMonitor != null) {
				externalMonitor.progress(current, total, state);
			}
		}
	}

}
