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

package org.eclipse.team.svn.core.operation.local.change;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.resource.ILocalFile;
import org.eclipse.team.svn.core.resource.ILocalFolder;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Local resource change store
 * 
 * @author Alexander Gurov
 */
public abstract class ResourceChange {
	protected ILocalResource local;

	protected File tmp;

	protected SVNProperty[] properties;

	public ResourceChange(ResourceChange parent, ILocalResource local, boolean needsTemporary) {
		this.local = local;
		if (needsTemporary) {
			tmp = SVNTeamPlugin.instance()
					.getTemporaryFile(parent == null ? null : parent.getTemporary(), local.getName());
		}
		properties = null;
	}

	public ILocalResource getLocal() {
		return local;
	}

	public SVNProperty[] getProperties() {
		return properties;
	}

	public void setProperties(SVNProperty[] properties) {
		this.properties = properties;
	}

	public File getTemporary() {
		return tmp;
	}

	public void disposeChangeModel(IProgressMonitor monitor) throws Exception {
		if (tmp != null) {
			FileUtility.deleteRecursive(tmp, monitor);
		}
	}

	public void traverse(IResourceChangeVisitor visitor, int depth, IActionOperationProcessor processor,
			IProgressMonitor monitor) throws Exception {
		preTraverse(visitor, depth, processor, monitor);
		postTraverse(visitor, depth, processor, monitor);
	}

	protected abstract void preTraverse(IResourceChangeVisitor visitor, int depth, IActionOperationProcessor processor,
			IProgressMonitor monitor) throws Exception;

	protected abstract void postTraverse(IResourceChangeVisitor visitor, int depth, IActionOperationProcessor processor,
			IProgressMonitor monitor) throws Exception;

	public static ResourceChange wrapLocalResource(ResourceChange parent, ILocalResource local,
			boolean needsTemporary) {
		if (local == null) {
			return null;
		}
		return local instanceof ILocalFile
				? new FileChange(parent, (ILocalFile) local, needsTemporary)
				: (ResourceChange) new FolderChange(parent, (ILocalFolder) local, needsTemporary);
	}

}
