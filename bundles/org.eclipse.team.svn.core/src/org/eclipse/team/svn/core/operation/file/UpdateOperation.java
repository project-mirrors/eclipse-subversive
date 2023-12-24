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

package org.eclipse.team.svn.core.operation.file;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNConflictDetectionProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Update resources operation
 * 
 * @author Alexander Gurov
 */
public class UpdateOperation extends AbstractFileConflictDetectionOperation implements IFileProvider {
	protected SVNRevision selectedRevision;

	protected long options;

	protected SVNDepth depth;

	protected String updateDepthPath;

	public UpdateOperation(File[] files, SVNRevision selectedRevision, boolean ignoreExternals) {
		this(files, selectedRevision, SVNDepth.INFINITY,
				ignoreExternals ? ISVNConnector.Options.IGNORE_EXTERNALS : ISVNConnector.Options.NONE, null);
	}

	public UpdateOperation(IFileProvider provider, SVNRevision selectedRevision, boolean ignoreExternals) {
		this(provider, selectedRevision, SVNDepth.INFINITY,
				ignoreExternals ? ISVNConnector.Options.IGNORE_EXTERNALS : ISVNConnector.Options.NONE, null);
	}

	public UpdateOperation(File[] files, SVNRevision selectedRevision, SVNDepth depth, long options,
			String updateDepthPath) {
		super("Operation_UpdateFile", SVNMessages.class, files); //$NON-NLS-1$
		this.selectedRevision = selectedRevision;
		this.options = options & ISVNConnector.CommandMasks.UPDATE;
		this.depth = depth;
		this.updateDepthPath = updateDepthPath;
	}

	public UpdateOperation(IFileProvider provider, SVNRevision selectedRevision, SVNDepth depth, long options,
			String updateDepthPath) {
		super("Operation_UpdateFile", SVNMessages.class, provider); //$NON-NLS-1$
		this.selectedRevision = selectedRevision;
		this.options = options & ISVNConnector.CommandMasks.UPDATE;
		this.depth = depth;
		this.updateDepthPath = updateDepthPath;
	}

	public void setDepthOptions(SVNDepth depth, boolean isStickyDepth, String updateDepthPath) {
		this.depth = depth;
		options &= ~ISVNConnector.Options.DEPTH_IS_STICKY;
		options |= isStickyDepth ? ISVNConnector.Options.DEPTH_IS_STICKY : ISVNConnector.Options.NONE;
		this.updateDepthPath = updateDepthPath;
	}

	@Override
	public File[] getFiles() {
		return getProcessed();
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		File[] files = operableData();

		// container with resources that is really updated
		defineInitialResourceSet(files);

		Map wc2Resources = SVNUtility.splitWorkingCopies(files);
		for (Iterator it = wc2Resources.entrySet().iterator(); it.hasNext() && !monitor.isCanceled();) {
			Map.Entry entry = (Map.Entry) it.next();

			IRepositoryResource wcRoot = SVNFileStorage.instance().asRepositoryResource((File) entry.getKey(), false);
			final IRepositoryLocation location = wcRoot.getRepositoryLocation();

			final String[] paths = FileUtility.asPathArray((File[]) ((List) entry.getValue()).toArray(new File[0]));

			//append update depth path
			if ((UpdateOperation.this.options & ISVNConnector.Options.DEPTH_IS_STICKY) != 0 && updateDepthPath != null
					&& paths.length == 1) {
				String newPath = paths[0] + "/" + updateDepthPath;
				newPath = FileUtility.normalizePath(newPath);
				paths[0] = newPath;
			}

			complexWriteToConsole(() -> {
				UpdateOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
						"svn update" + ISVNConnector.Options.asCommandLine(options)); //$NON-NLS-1$
				for (int i = 0; i < paths.length && !monitor.isCanceled(); i++) {
					UpdateOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, " \"" + paths[i] + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				}
				UpdateOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, " -r " + selectedRevision //$NON-NLS-1$
						+ SVNUtility.getDepthArg(depth, options)
						+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
			});

			final ISVNConnector proxy = location.acquireSVNProxy();
			this.protectStep(monitor1 -> proxy.update(paths, selectedRevision, depth, options,
					new ConflictDetectionProgressMonitor(UpdateOperation.this, monitor1, null)), monitor, wc2Resources.size());

			location.releaseSVNProxy(proxy);
		}
	}

	protected class ConflictDetectionProgressMonitor extends SVNConflictDetectionProgressMonitor {
		public ConflictDetectionProgressMonitor(IActionOperation parent, IProgressMonitor monitor, IPath root) {
			super(parent, monitor, root);
		}

		@Override
		protected void processConflict(ItemState state) {
			UpdateOperation.this.hasUnresolvedConflict = true;
			UpdateOperation.this.unprocessed.add(new File(state.path));
			IPath conflictPath = new Path(state.path);
			for (Iterator it = UpdateOperation.this.processed.iterator(); it.hasNext();) {
				File res = (File) it.next();
				if (new Path(res.getAbsolutePath()).equals(conflictPath)) {
					it.remove();
					break;
				}
			}
		}
	}

}
