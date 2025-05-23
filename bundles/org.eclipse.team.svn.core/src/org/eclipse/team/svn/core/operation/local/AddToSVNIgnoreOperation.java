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
import org.eclipse.core.runtime.Path;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNProperty.BuiltIn;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.resource.ISVNStorage;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * "Add to svn::ignore" operation implementation
 * 
 * @author Alexander Gurov
 */
public class AddToSVNIgnoreOperation extends AbstractWorkingCopyOperation {
	protected int ignoreType;

	protected String pattern;

	public AddToSVNIgnoreOperation(IResource[] resources, int ignoreType, String pattern) {
		super("Operation_AddToSVNIgnore", SVNMessages.class, resources); //$NON-NLS-1$

		this.ignoreType = ignoreType;
		this.pattern = pattern;
	}

	public AddToSVNIgnoreOperation(IResourceProvider provider, int ignoreType, String pattern) {
		super("Operation_AddToSVNIgnore", SVNMessages.class, provider); //$NON-NLS-1$

		this.ignoreType = ignoreType;
		this.pattern = pattern;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();

		final IRemoteStorage storage = SVNRemoteStorage.instance();

		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			final IResource current = resources[i];

			this.protectStep(monitor1 -> AddToSVNIgnoreOperation.this.handleResource(storage, current), monitor, resources.length);
		}
	}

	protected void handleResource(IRemoteStorage storage, IResource current) throws Exception {
		IResource parent = current.getParent();
		IRepositoryLocation location = storage.getRepositoryLocation(parent);
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			AddToSVNIgnoreOperation.changeIgnoreProperty(proxy, ignoreType, pattern,
					FileUtility.getWorkingCopyPath(parent), current.getName());
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	public static void changeIgnoreProperty(ISVNConnector proxy, int ignoreType, String pattern, String path,
			String name) throws Exception {
		SVNProperty data = proxy.getProperty(new SVNEntryRevisionReference(path), BuiltIn.IGNORE, null,
				new SVNNullProgressMonitor());
		String ignoreValue = data == null ? "" : data.value; //$NON-NLS-1$
		String mask = null;
		switch (ignoreType) {
			case ISVNStorage.IGNORE_NAME: {
				mask = name;
				break;
			}
			case ISVNStorage.IGNORE_EXTENSION: {
				String extension = new Path(path + "/" + name).getFileExtension(); //$NON-NLS-1$
				if (extension != null) {
					mask = "*." + extension; //$NON-NLS-1$
				}
				break;
			}
			case ISVNStorage.IGNORE_PATTERN: {
				mask = pattern;
				break;
			}
		}
		ignoreValue = AddToSVNIgnoreOperation.addMask(ignoreValue, mask);
		proxy.setPropertyLocal(new String[] { path }, new SVNProperty(BuiltIn.IGNORE, ignoreValue), SVNDepth.EMPTY,
				ISVNConnector.Options.NONE, null, new SVNNullProgressMonitor());
	}

	protected static String addMask(String ignore, String mask) {
		if (mask == null || mask.length() == 0) {
			return ignore;
		}
		StringTokenizer tok = new StringTokenizer(ignore, "\n", false); //$NON-NLS-1$
		boolean found = false;
		while (tok.hasMoreTokens()) {
			if (tok.nextToken().equals(mask)) {
				found = true;
				break;
			}
		}

		return found ? ignore : ignore + (ignore.length() > 0 ? "\n" : "") + mask; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
