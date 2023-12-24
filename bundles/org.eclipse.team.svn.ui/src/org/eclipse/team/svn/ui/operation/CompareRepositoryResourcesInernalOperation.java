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

package org.eclipse.team.svn.ui.operation;

import java.util.ArrayList;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNDiffStatus;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.compare.ResourceCompareInput;
import org.eclipse.team.svn.ui.compare.TwoWayResourceCompareInput;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Two way compare for repository resources operation implementation
 * 
 * @author Alexander Gurov
 */
public class CompareRepositoryResourcesInernalOperation extends AbstractActionOperation {
	protected IRepositoryResource next;

	protected IRepositoryResource prev;

	protected IRepositoryResourceProvider provider;

	protected boolean forceReuse;

	protected String forceId;

	protected long options;

	public CompareRepositoryResourcesInernalOperation(IRepositoryResource prev, IRepositoryResource next,
			boolean forceReuse, long options) {
		super("Operation_CompareRepository", SVNUIMessages.class); //$NON-NLS-1$
		this.prev = prev;
		this.next = next;
		this.forceReuse = forceReuse;
		this.options = options;
	}

	public CompareRepositoryResourcesInernalOperation(IRepositoryResourceProvider provider, boolean forceReuse,
			long options) {
		this(null, null, forceReuse, options);
		this.provider = provider;
	}

	public void setForceId(String forceId) {
		this.forceId = forceId;
	}

	public String getForceId() {
		return forceId;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		if (provider != null) {
			IRepositoryResource[] toCompare = provider.getRepositoryResources();
			prev = toCompare[0];
			next = toCompare[1];
		}

		IRepositoryLocation location = prev.getRepositoryLocation();
		final ISVNConnector proxy = location.acquireSVNProxy();
		final ArrayList<SVNDiffStatus> statuses = new ArrayList<>();

		ProgressMonitorUtility.setTaskInfo(monitor, this, SVNMessages.Progress_Running);
		this.protectStep(monitor1 -> {
			SVNEntryRevisionReference refPrev = SVNUtility.getEntryRevisionReference(prev);
			SVNEntryRevisionReference refNext = SVNUtility.getEntryRevisionReference(next);
			if (SVNUtility.useSingleReferenceSignature(refPrev, refNext)) {
				SVNUtility.diffStatus(proxy, statuses, refPrev,
						new SVNRevisionRange(refPrev.revision, refNext.revision), SVNDepth.INFINITY, options,
						new SVNProgressMonitor(
								CompareRepositoryResourcesInernalOperation.this, monitor1, null, false));
			} else {
				SVNUtility.diffStatus(proxy, statuses, refPrev, refNext, SVNDepth.INFINITY, options,
						new SVNProgressMonitor(
								CompareRepositoryResourcesInernalOperation.this, monitor1, null, false));
			}
		}, monitor, 100, 20);

		location.releaseSVNProxy(proxy);

		if (!monitor.isCanceled()) {
			this.protectStep(monitor1 -> {
				CompareConfiguration cc = new CompareConfiguration();
				cc.setProperty(CompareEditor.CONFIRM_SAVE_PROPERTY, Boolean.FALSE);
				final TwoWayResourceCompareInput compare = new TwoWayResourceCompareInput(cc, next, prev, statuses);
				compare.setForceId(forceId);
				compare.initialize(monitor1);
				UIMonitorUtility.getDisplay().syncExec(() -> ResourceCompareInput.openCompareEditor(compare, forceReuse));
			}, monitor, 100, 20);
		}
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] { next.getName(), prev.getName() });
	}

}
