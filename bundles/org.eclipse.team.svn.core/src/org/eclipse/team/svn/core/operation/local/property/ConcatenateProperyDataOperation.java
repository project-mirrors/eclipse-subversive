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
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Concatenate new property value with already existing value (by retrieving it from SVN)
 * 
 * @author Igor Burilo
 */
public class ConcatenateProperyDataOperation extends AbstractActionOperation implements IResourcePropertyProvider {

	protected IResource resource;

	protected String propertyName;

	protected boolean isStringValue;

	protected String stringValuesSeparator;

	protected String newStringValue;

	protected byte[] newByteValue;

	protected IResourcePropertyProvider propertyProvider;

	protected SVNProperty property;

	public ConcatenateProperyDataOperation(IResource resource, String propertyName,
			IResourcePropertyProvider propertyProvider) {
		this(resource, propertyName, true);
		this.propertyProvider = propertyProvider;
	}

	public ConcatenateProperyDataOperation(IResource resource, String propertyName, String newStringValue) {
		this(resource, propertyName, true);
		this.newStringValue = newStringValue;
	}

	public ConcatenateProperyDataOperation(IResource resource, String propertyName, byte[] newByteValue) {
		this(resource, propertyName, false);
		this.newByteValue = newByteValue;
	}

	private ConcatenateProperyDataOperation(IResource resource, String propertyName, boolean isStringValue) {
		super("Operation_ConcatenatePropertyData", SVNMessages.class); //$NON-NLS-1$
		this.resource = resource;
		this.propertyName = propertyName;
		this.isStringValue = isStringValue;
		stringValuesSeparator = "\r\n"; //$NON-NLS-1$
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		final String wcPath = FileUtility.getWorkingCopyPath(resource);
		IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(resource);
		final ISVNConnector proxy = location.acquireSVNProxy();
		SVNProperty existingProperty;
		try {
			existingProperty = proxy.getProperty(new SVNEntryRevisionReference(wcPath), propertyName, null,
					new SVNProgressMonitor(this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
		}
		if (existingProperty != null && existingProperty.value != null) {
			if (isStringValue) {
				String value = existingProperty.value;
				value += stringValuesSeparator + getNewStringValue();
				property = new SVNProperty(propertyName, value);
			} else {
				byte[] existingData = existingProperty.value.getBytes();
				byte[] newData = new byte[existingData.length + getNewByteValue().length];
				System.arraycopy(existingData, 0, newData, 0, existingData.length);
				System.arraycopy(getNewByteValue(), 0, newData, existingData.length, getNewByteValue().length);
				property = new SVNProperty(propertyName, new String(newData));
			}
		} else if (isStringValue) {
			property = new SVNProperty(propertyName, getNewStringValue());
		} else {
			property = new SVNProperty(propertyName, new String(getNewByteValue()));
		}
	}

	protected String getNewStringValue() {
		if (!isStringValue) {
			return null;
		}
		return propertyProvider != null ? propertyProvider.getProperties()[0].value : newStringValue;
	}

	protected byte[] getNewByteValue() {
		return newByteValue;
	}

	public void setStringValuesSeparator(String stringValuesSeparator) {
		this.stringValuesSeparator = stringValuesSeparator;
	}

	@Override
	public IResource getLocal() {
		return resource;
	}

	@Override
	public SVNProperty[] getProperties() {
		return new SVNProperty[] { property };
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

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] { propertyName, resource.getName() });
	}
}
