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

import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNErrorCodes;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNProperty.BuiltIn;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Add to version control operation
 * 
 * @author Alexander Gurov
 */
public class AddToSVNOperation extends AbstractWorkingCopyOperation {
	protected boolean isRecursive;

	public AddToSVNOperation(IResource[] resources) {
		this(resources, false);
	}

	public AddToSVNOperation(IResource[] resources, boolean isRecursive) {
		super("Operation_AddToSVN", SVNMessages.class, resources); //$NON-NLS-1$
		this.isRecursive = isRecursive;
	}

	public AddToSVNOperation(IResourceProvider provider) {
		this(provider, false);
	}

	public AddToSVNOperation(IResourceProvider provider, boolean isRecursive) {
		super("Operation_AddToSVN", SVNMessages.class, provider); //$NON-NLS-1$
		this.isRecursive = isRecursive;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();

		if (isRecursive) {
			isRecursive = !FileUtility.checkForResourcesPresenceRecursive(resources, IStateFilter.SF_IGNORED);
		}
		if (isRecursive) {
			resources = FileUtility.shrinkChildNodesWithSwitched(resources);
		} else {
			FileUtility.reorder(resources, true);
		}

		final IRemoteStorage storage = SVNRemoteStorage.instance();
		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			final IResource current = resources[i];
			IRepositoryLocation location = storage.getRepositoryLocation(current);
			final ISVNConnector proxy = location.acquireSVNProxy();

			this.protectStep(monitor1 -> AddToSVNOperation.this.doAdd(current, proxy, monitor1), monitor, resources.length);
			location.releaseSVNProxy(proxy);
		}
	}

	public static void removeFromParentIgnore(ISVNConnector proxy, String parentPath, String name) throws Exception {
		try {
			SVNProperty data = proxy.getProperty(new SVNEntryRevisionReference(parentPath), BuiltIn.IGNORE, null,
					new SVNNullProgressMonitor());
			String ignoreValue = data == null ? "" : data.value; //$NON-NLS-1$

			StringTokenizer tok = new StringTokenizer(ignoreValue, "\n", true); //$NON-NLS-1$
			ignoreValue = ""; //$NON-NLS-1$
			boolean skipToken = false;
			while (tok.hasMoreTokens()) {
				String oneOf = tok.nextToken();

				if (!oneOf.equals(name) && !skipToken) {
					ignoreValue += oneOf;
				} else {
					skipToken = !skipToken;
				}
			}

			proxy.setPropertyLocal(new String[] { parentPath },
					new SVNProperty(BuiltIn.IGNORE, ignoreValue.length() > 0 ? ignoreValue : null), SVNDepth.EMPTY,
					ISVNConnector.Options.NONE, null, new SVNNullProgressMonitor());
		} catch (SVNConnectorException ex) {
			if (ex.getErrorId() != SVNErrorCodes.unversionedResource) { // if the parent is unversioned, then just ignore it
				throw ex;
			}
		}
	}

	protected void doAdd(IResource current, ISVNConnector proxy, IProgressMonitor monitor) throws Exception {
		String wcPath = FileUtility.getWorkingCopyPath(current);

		AddToSVNOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
				"svn add \"" + FileUtility.normalizePath(wcPath) + "\"" //$NON-NLS-1$//$NON-NLS-2$
						+ (AddToSVNOperation.this.isRecursive ? "" : " -N") + ISVNConnector.Options.asCommandLine( //$NON-NLS-1$//$NON-NLS-2$
								ISVNConnector.Options.FORCE | ISVNConnector.Options.INCLUDE_PARENTS)
						+ "\n"); //$NON-NLS-1$

		IResource parent = current.getParent();
		if (parent != null) {
			AddToSVNOperation.removeFromParentIgnore(proxy, FileUtility.getWorkingCopyPath(parent), current.getName());
		}

		proxy.add(wcPath, SVNDepth.infinityOrEmpty(AddToSVNOperation.this.isRecursive),
				ISVNConnector.Options.FORCE | ISVNConnector.Options.INCLUDE_PARENTS,
				new SVNProgressMonitor(AddToSVNOperation.this, monitor, null));
	}

}
