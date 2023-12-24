/*******************************************************************************
 * Copyright (c) 2008, 2023 Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Eugene Kuleshov - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.m2e;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.m2e.scm.ScmUrl;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.repository.model.IResourceTreeNode;

/**
 * @author Eugene Kuleshov
 */
public class SvnUrlAdapterFactory implements IAdapterFactory {

	@SuppressWarnings("rawtypes")
	private static final Class[] ADAPTER_TYPES = new Class[] { ScmUrl.class };

	@SuppressWarnings("rawtypes")
	public Class[] getAdapterList() {
		return SvnUrlAdapterFactory.ADAPTER_TYPES;
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Object adaptable, Class adapterType) {
		// IResourceTreeNode is common interface for both UI objects: RepositoryFolder and RepositoryLocation
		if (ScmUrl.class.equals(adapterType) && (adaptable instanceof IResourceTreeNode)) {
			IRepositoryResource repositoryResource = ((IResourceTreeNode)adaptable).getRepositoryResource();

			String scmUrl = SVNScmHandler.SVN_SCM_ID + repositoryResource.getUrl();
			String scmParentUrl = null;

			IRepositoryResource parent = repositoryResource.getParent();
			if (parent != null) {
				scmParentUrl = SVNScmHandler.SVN_SCM_ID + parent.getUrl();
			}

			return new ScmUrl(scmUrl, scmParentUrl);
		}
		return null;
	}

}
