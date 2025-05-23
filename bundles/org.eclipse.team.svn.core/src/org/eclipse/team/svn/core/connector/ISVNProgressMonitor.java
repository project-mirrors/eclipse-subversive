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

package org.eclipse.team.svn.core.connector;

/**
 * Progress monitor interface
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public interface ISVNProgressMonitor {
	int TOTAL_UNKNOWN = -1;

	public static class ItemState {
		public final String path;

		/**
		 * It could be of either kind: SVNNotification.PerformedAction or SVNRepositoryNotification.Action, so leaving as int
		 */
		public final int action;

		public final SVNEntry.Kind kind;

		public final String mimeType;

		/**
		 * It could be of either kind: SVNNotification.NodeStatus or SVNEntryStatus.Kind, so leaving as int
		 */
		public final int contentState;

		/**
		 * It could be of either kind: SVNNotification.NodeStatus or SVNEntryStatus.Kind, so leaving as int
		 */
		public final int propState;

		public final long revision;

		public final String error;

		public ItemState(String path, int action, SVNEntry.Kind kind, String mimeType, int contentState, int propState,
				long revision, String error) {
			this.path = path;
			this.action = action;
			this.kind = kind;
			this.mimeType = mimeType;
			this.contentState = contentState;
			this.propState = propState;
			this.revision = revision;
			this.error = error;
		}
	}

	void progress(int current, int total, ItemState state);

	void commitStatus(SVNCommitStatus status);

	boolean isActivityCancelled();
}
