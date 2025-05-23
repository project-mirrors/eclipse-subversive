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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.file;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNEntryStatusCallback;
import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.connector.SVNNotification;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevision.Number;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * This operation fetch remote resource statuses
 * 
 * @author Alexander Gurov
 */
public class RemoteStatusOperation extends AbstractStatusOperation implements ISVNNotificationCallback {
	protected Map<String, Number> pegRevisions = new HashMap<>();

	public RemoteStatusOperation(File[] files, boolean recursive) {
		super("Operation_UpdateStatusFile", SVNMessages.class, files, recursive); //$NON-NLS-1$
	}

	public RemoteStatusOperation(IFileProvider provider, boolean recursive) {
		super("Operation_UpdateStatusFile", SVNMessages.class, provider, recursive); //$NON-NLS-1$
	}

	public SVNRevision getPegRevision(File change) {
		IPath resourcePath = new Path(change.getAbsolutePath());
		for (Iterator<?> it = pegRevisions.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			IPath rootPath = new Path((String) entry.getKey());
			if (rootPath.isPrefixOf(resourcePath)) {
				return (SVNRevision) entry.getValue();
			}
		}
		return null;
	}

	@Override
	public void notify(SVNNotification info) {
		if (info.revision != SVNRevision.INVALID_REVISION_NUMBER) {
			pegRevisions.put(info.path, SVNRevision.fromNumber(info.revision));
		}
	}

	@Override
	protected void reportStatuses(final ISVNConnector proxy, final ISVNEntryStatusCallback cb, final File current,
			IProgressMonitor monitor, int tasks) {
		SVNUtility.addSVNNotifyListener(proxy, this);
		super.reportStatuses(proxy, cb, current, monitor, tasks);
		SVNUtility.removeSVNNotifyListener(proxy, this);
	}

	@Override
	protected boolean isRemote() {
		return true;
	}

}
