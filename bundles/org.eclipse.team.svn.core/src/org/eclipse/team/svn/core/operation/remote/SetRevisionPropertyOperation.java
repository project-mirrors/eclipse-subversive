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
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.remote;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNConnector.Options;
import org.eclipse.team.svn.core.connector.SVNEntryReference;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.local.property.IRevisionPropertiesProvider;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;

/**
 * Set revision property operation implementation
 * 
 * @author Alexei Goncharov
 */
public class SetRevisionPropertyOperation extends AbstractActionOperation {

	protected IRepositoryLocation location;

	protected SVNRevision revision;

	protected IRevisionPropertiesProvider provider;

	/**
	 * Creates an instance of SetRevisionPropertyOperation depending on the repository location.
	 * 
	 * @param location
	 *            - repository location to set property to
	 * @param revision
	 *            - revision to apply property to
	 * @param revProp
	 *            - the revision property to set
	 * 
	 * @author Alexei Goncharov
	 */
	public SetRevisionPropertyOperation(IRepositoryLocation location, SVNRevision revision, final SVNProperty revProp) {
		this(location, revision, () -> new SVNProperty[] { revProp });
	}

	/**
	 * Creates an instance of SetRevisionPropertyOperation depending on the repository location.
	 * 
	 * @param location
	 *            - repository location to set property to
	 * @param revision
	 *            - revision to apply property to
	 * @param revPropProvider
	 *            - the revision property to set provider
	 * 
	 * @author Alexei Goncharov
	 */
	public SetRevisionPropertyOperation(IRepositoryLocation location, SVNRevision revision,
			IRevisionPropertiesProvider revPropProvider) {
		super("Operation_SetRevisionProperty", SVNMessages.class); //$NON-NLS-1$
		this.revision = revision;
		provider = revPropProvider;
		this.location = location;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		SVNProperty toSet = provider.getRevisionProperties()[0];
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			proxy.setRevisionProperty(new SVNEntryReference(location.getUrl(), revision), toSet, null, Options.FORCE,
					new SVNProgressMonitor(SetRevisionPropertyOperation.this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

}
