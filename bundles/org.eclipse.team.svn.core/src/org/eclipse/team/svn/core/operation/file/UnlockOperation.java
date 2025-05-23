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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.connector.SVNNotification;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Unlock resources
 * 
 * @author Alexander Gurov
 */
public class UnlockOperation extends AbstractFileOperation {
	public UnlockOperation(File[] files) {
		super("Operation_UnlockFile", SVNMessages.class, files); //$NON-NLS-1$
	}

	public UnlockOperation(IFileProvider provider) {
		super("Operation_UnlockFile", SVNMessages.class, provider); //$NON-NLS-1$
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		File[] files = operableData();

		final List<SVNNotification> problems = new ArrayList<>();
		Map<?, ?> wc2Resources = SVNUtility.splitWorkingCopies(files);
		for (Iterator<?> it = wc2Resources.entrySet().iterator(); it.hasNext() && !monitor.isCanceled();) {
			Map.Entry entry = (Map.Entry) it.next();

			IRepositoryResource wcRoot = SVNFileStorage.instance().asRepositoryResource((File) entry.getKey(), false);
			final IRepositoryLocation location = wcRoot.getRepositoryLocation();

			final String[] paths = FileUtility.asPathArray(((List<?>) entry.getValue()).toArray(new File[0]));

			complexWriteToConsole(() -> {
				UnlockOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
						"svn unlock" + ISVNConnector.Options.asCommandLine(ISVNConnector.Options.FORCE)); //$NON-NLS-1$
				for (int i = 0; i < paths.length && !monitor.isCanceled(); i++) {
					UnlockOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, " \"" + paths[i] + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				}
				UnlockOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
						" --force" + FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			});

			final ISVNConnector proxy = location.acquireSVNProxy();
			this.protectStep(monitor1 -> {
				ISVNNotificationCallback listener = info -> {
					if (SVNNotification.PerformedAction.FAILED_UNLOCK == info.action) {
						problems.add(info);
					}
				};

				SVNUtility.addSVNNotifyListener(proxy, listener);
				try {
					proxy.unlock(paths, ISVNConnector.Options.FORCE,
							new SVNProgressMonitor(UnlockOperation.this, monitor1, null));
				} finally {
					SVNUtility.removeSVNNotifyListener(proxy, listener);
				}
			}, monitor, wc2Resources.size());

			location.releaseSVNProxy(proxy);
		}

		//check problems
		if (!problems.isEmpty()) {
			StringBuilder res = new StringBuilder();
			Iterator<SVNNotification> iter = problems.iterator();
			while (iter.hasNext()) {
				SVNNotification problem = iter.next();
				res.append(problem.errMsg);
				if (iter.hasNext()) {
					res.append("\n\n");
				}
			}
			throw new UnreportableException(res.toString());
		}
	}

}
