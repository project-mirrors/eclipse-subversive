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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.remote;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Break repository resources lock operation
 *
 * @author Sergiy Logvin
 */
public class BreakLockOperation extends AbstractRepositoryOperation {
	public BreakLockOperation(IRepositoryResource[] resources) {
		super("Operation_BreakLock", SVNMessages.class, resources); //$NON-NLS-1$
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		IRepositoryResource[] resources = operableData();
		Map<?, ?> splittedSet = SVNUtility.splitRepositoryLocations(resources);

		for (Iterator<?> it = splittedSet.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();

			final IRepositoryLocation location = (IRepositoryLocation) entry.getKey();
			List<?> values = (List<?>) entry.getValue();
			final String[] paths = SVNUtility.asURLArray(values.toArray(new IRepositoryResource[values.size()]), true);

			complexWriteToConsole(() -> {
				BreakLockOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
						"svn unlock" + ISVNConnector.Options.asCommandLine(ISVNConnector.Options.FORCE)); //$NON-NLS-1$
				for (int i = 0; i < paths.length && !monitor.isCanceled(); i++) {
					BreakLockOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
							" \"" + SVNUtility.decodeURL(paths[i]) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				}
				BreakLockOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
						" --force" + FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			});

			final ISVNConnector proxy = location.acquireSVNProxy();
			this.protectStep(monitor1 -> proxy.unlock(paths, ISVNConnector.Options.FORCE,
					new SVNProgressMonitor(BreakLockOperation.this, monitor1, null)), monitor, splittedSet.size());
			location.releaseSVNProxy(proxy);
		}
	}

}
