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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Neels Hofmeyr (elego.de) - Replace with Revision fails to notice trivial changes on locked files (Bug 353875)
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.remote;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Operation for replacement of local resources with remote ones. Deletes the resources, that are not got from remote source.
 * 
 * @author Alexei Goncharov
 */
public class ReplaceWithRemoteOperation extends AbstractActionOperation {

	protected IResource toReplace;

	protected IRepositoryResource remoteRoot;

	protected boolean ignoreExternals;

	public ReplaceWithRemoteOperation(IResource toReplace, IRepositoryResource remoteResource,
			boolean ignoreExternals) {
		super("Operation_ReplaceWithRemote", SVNMessages.class); //$NON-NLS-1$
		this.toReplace = toReplace;
		remoteRoot = remoteResource;
		this.ignoreExternals = ignoreExternals;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		//perform export into temp folder
		IRepositoryLocation location = remoteRoot.getRepositoryLocation();
		String toReplacePath = FileUtility.getWorkingCopyPath(toReplace);
		File f = File.createTempFile("svn", "", FileUtility.getResourcePath(toReplace.getParent()).toFile()); //$NON-NLS-1$ //$NON-NLS-2$
		f.delete();
		f.mkdir();
		String tempPath = toReplacePath.substring(0, toReplacePath.lastIndexOf("/") + 1) + f.getName() + "/"; //$NON-NLS-1$ //$NON-NLS-2$
		final ISVNConnector proxy = location.acquireSVNProxy();
		final String path = tempPath + remoteRoot.getName();
		final SVNEntryRevisionReference entryRef = SVNUtility.getEntryRevisionReference(remoteRoot);
		try {
			long options = ISVNConnector.Options.FORCE;
			if (ignoreExternals) {
				options |= ISVNConnector.Options.IGNORE_EXTERNALS;
			}
			proxy.exportTo(entryRef, path, null, SVNDepth.INFINITY, options,
					new SVNProgressMonitor(this, monitor, null));
			//perform replacement
			if (toReplace instanceof IFile) {
				FileUtility.copyFile(new File(toReplacePath), new File(path), monitor);
			} else {
				performReplacementRecursively(toReplacePath, path, proxy, monitor);
			}
		} finally {
			location.releaseSVNProxy(proxy);
			FileUtility.deleteRecursive(new File(tempPath));
		}
	}

	protected void performReplacementRecursively(String pathForReplacement, String sourcePath,
			ISVNConnector connectorProxy, IProgressMonitor monitor) throws Exception {
		File dirToReplace = new File(pathForReplacement);
		File sourceDir = new File(sourcePath);
		ArrayList<String> toReplaceChildren = new ArrayList<>();
		String[] children = dirToReplace.list();
		if (children != null) {
			toReplaceChildren.addAll(Arrays.asList(children));
		}
		children = sourceDir.list();
		ArrayList<String> sourceChildren = new ArrayList<>();
		if (children != null) {
			sourceChildren.addAll(Arrays.asList(children));
		}
		ArrayList<String> pathsToDelete = new ArrayList<>();
		for (String currentToReplace : toReplaceChildren) {
			if (!currentToReplace.equalsIgnoreCase(".svn") && !sourceChildren.contains(currentToReplace)) { //$NON-NLS-1$
				pathsToDelete.add(pathForReplacement + "/" + currentToReplace); //$NON-NLS-1$
			}
		}
		connectorProxy.removeRemote(pathsToDelete.toArray(new String[0]), "", ISVNConnector.Options.FORCE, null, //$NON-NLS-1$
				new SVNProgressMonitor(this, monitor, null));
		for (Iterator<String> it = sourceChildren.iterator(); it.hasNext() && !monitor.isCanceled();) {
			String currentFromSource = it.next();
			File toReplace = new File(pathForReplacement + "/" + currentFromSource); //$NON-NLS-1$
			File source = new File(sourcePath + "/" + currentFromSource); //$NON-NLS-1$
			if (source.isDirectory()) {
				if (!toReplace.exists()) {
					toReplace.mkdir();
				}
				performReplacementRecursively(pathForReplacement + "/" + currentFromSource, //$NON-NLS-1$
						sourcePath + "/" + currentFromSource, connectorProxy, monitor); //$NON-NLS-1$
			} else {
				// If a file is locked, this fails (a locked file has read-only permissions).
				// That's perfectly fine, but if the files are identical, ignore the error.
				if (toReplace.exists() && !toReplace.canWrite() && toReplace.isFile() && source.isFile()
						&& toReplace.length() == source.length()) {
					BufferedReader left = null;
					BufferedReader right = null;

					boolean identical = true;
					try {
						left = new BufferedReader(new FileReader(source));
						right = new BufferedReader(new FileReader(toReplace));
						while (identical) {
							if (left.ready() != right.ready()) {
								identical = false;
							} else if (!left.ready()) {
								break;
							} else if (left.read() != right.read()) {
								identical = false;
							}
						}
					} catch (IOException ioe) {
						identical = false;
					} finally {
						if (left != null) {
							try {
								left.close();
							} catch (IOException e1) {
							}
						}
						if (right != null) {
							try {
								right.close();
							} catch (IOException e1) {
							}
						}
					}
					if (identical) {
						continue;
					}
				}
				FileUtility.copyFile(toReplace, source, monitor);
			}
		}
	}
}
