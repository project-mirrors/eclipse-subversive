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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local.property;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Generate external property value by provided info
 * 
 * @author Igor Burilo
 */
public class GenerateExternalsPropertyOperation extends AbstractActionOperation implements IResourcePropertyProvider {

	protected IResource resource;

	protected String url;

	protected SVNRevision revision;

	protected String localPath;

	protected boolean isPriorToSVN15Format;

	protected SVNProperty property;

	public GenerateExternalsPropertyOperation(IResource resource, String url, SVNRevision revision, String localPath,
			boolean isPriorToSVN15Format) {
		super("Operation_GenerateExternalsProperty", SVNMessages.class); //$NON-NLS-1$
		this.resource = resource;
		this.url = url;
		this.revision = revision;
		this.localPath = localPath;
		this.isPriorToSVN15Format = isPriorToSVN15Format;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		preprocessInputParameters();

		StringBuilder value = new StringBuilder();
		if (isPriorToSVN15Format) {
			//local -r 10 url
			value.append(localPath);
			if (getStrRevision() != null) {
				value.append(" -r ").append(getStrRevision()); //$NON-NLS-1$
			}
			value.append(" ").append(url); //$NON-NLS-1$
		} else {
			//-r 10 url local
			if (getStrRevision() != null) {
				value.append("-r ").append(getStrRevision()).append(" "); //$NON-NLS-1$ //$NON-NLS-2$
			}
			value.append(url);
			value.append(" ").append(localPath); //$NON-NLS-1$
		}
		property = new SVNProperty(SVNProperty.BuiltIn.EXTERNALS, value.toString());
	}

	protected void preprocessInputParameters() {
		url = SVNUtility.encodeURL(url);

		if (localPath.contains(" ")) { //$NON-NLS-1$
			localPath = "\"" + localPath + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	protected String getStrRevision() {
//		if (this.revision.getKind() == SVNRevision.Kind.DATE) {
//			//Example: 2006-02-17 15:30 +0230
//			SimpleDateFormat formatter = new SimpleDateFormat("{yyyy-dd-MM}");
//			return formatter.format(((SVNRevision.Date) this.revision).getDate());
//		} else
		if (revision.getKind() == SVNRevision.Kind.NUMBER) {
			long number = ((SVNRevision.Number) revision).getNumber();
			if (number != -1) {
				return String.valueOf(number);
			}
		}
		return null;
	}

	@Override
	public SVNProperty[] getProperties() {
		return new SVNProperty[] { property };
	}

	@Override
	public IResource getLocal() {
		return resource;
	}

	@Override
	public IRepositoryResource getRemote() {
		return SVNRemoteStorage.instance().asRepositoryResource(resource);
	}

	@Override
	public boolean isEditAllowed() {
		return false;
	}

	@Override
	public void refresh() {
	}
}
