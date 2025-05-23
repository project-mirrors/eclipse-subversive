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
import org.eclipse.team.svn.core.connector.SVNEntryReference;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IRevisionProvider;
import org.eclipse.team.svn.core.operation.IRevisionProvider.RevisionPair;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;

/**
 * Set revision author name operation implementation
 * 
 * @author Alexei Goncharov
 */
public class SetRevisionAuthorNameOperation extends AbstractActionOperation {

	protected IRevisionProvider provider;

	protected long options;

	public SetRevisionAuthorNameOperation(final RevisionPair[] revisions, long options) {
		this(() -> revisions, options);

	}

	public SetRevisionAuthorNameOperation(IRevisionProvider provider, long options) {
		super("Operation_SetRevisionAuthorName", SVNMessages.class); //$NON-NLS-1$
		this.provider = provider;
		this.options = options;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		final RevisionPair[] revisions = provider.getRevisions();
		if (revisions == null) {
			return;
		}
		for (int i = 0; i < revisions.length && !monitor.isCanceled(); i++) {
			if (revisions[i] == null || revisions[i].revision == SVNRevision.INVALID_REVISION_NUMBER) {
				continue;
			}
			final IRepositoryLocation location = revisions[i].location;
			if (!location.isAuthorNameEnabled()) {
				continue;
			}
			final ISVNConnector proxy = location.acquireSVNProxy();
			final SVNRevision rev = SVNRevision.fromNumber(revisions[i].revision);
			this.protectStep(monitor1 -> proxy.setRevisionProperty(new SVNEntryReference(location.getUrl(), rev),
					new SVNProperty(SVNProperty.BuiltIn.REV_AUTHOR, location.getAuthorName()), null, options,
					new SVNProgressMonitor(SetRevisionAuthorNameOperation.this, monitor1, null)), monitor, 1);
			location.releaseSVNProxy(proxy);
		}
	}

}
