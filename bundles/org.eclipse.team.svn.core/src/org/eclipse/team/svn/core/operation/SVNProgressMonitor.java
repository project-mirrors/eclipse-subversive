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

package org.eclipse.team.svn.core.operation;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNConnectorUnresolvedConflictException;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNNotification.NodeStatus;
import org.eclipse.team.svn.core.connector.SVNNotification.PerformedAction;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Default progress monitor implementation
 * 
 * @author Alexander Gurov
 */
public class SVNProgressMonitor extends SVNNullProgressMonitor {
	protected org.eclipse.core.runtime.IProgressMonitor monitor;

	protected IActionOperation parent;

	protected IConsoleStream stream;

	protected IPath root;

	protected boolean enableConsoleOutput;

	public SVNProgressMonitor(IActionOperation parent, org.eclipse.core.runtime.IProgressMonitor monitor, IPath root) {
		this(parent, monitor, root, true);
	}

	public SVNProgressMonitor(IActionOperation parent, org.eclipse.core.runtime.IProgressMonitor monitor, IPath root,
			boolean enableConsoleOutput) {
		this.monitor = monitor;
		this.parent = parent;
		this.root = root;
		stream = parent.getConsoleStream();
		this.enableConsoleOutput = enableConsoleOutput;
	}

	@Override
	public void progress(int current, int total, ItemState state) {
		if (state.error != null) {
			SVNConnectorException ex = state.path != null && state.path.length() > 0
					? new SVNConnectorUnresolvedConflictException(state.error)
					: new SVNConnectorException(state.error);
			parent.reportStatus(IStatus.ERROR, null, ex);
		}
		if (state != null && state.path != null) {
//			TODO rework display path...
//			String info = root.toString();
//			info = state.path.length() > info.length() ? state.path.substring(info.length()) : state.path;
			ProgressMonitorUtility.setTaskInfo(monitor, parent, state.path);
			if (enableConsoleOutput) {
				SVNProgressMonitor.writeToConsole(stream, state.contentState, state.propState, state.action, state.path,
						state.revision);
			}
		}
		ProgressMonitorUtility.progress(monitor, current, total);
	}

	@Override
	public boolean isActivityCancelled() {
		return monitor.isCanceled();
	}

	public static void writeToConsole(IConsoleStream stream, int contentState, int propState, int action, String path,
			long revision) {
		if (stream != null && path != null && path.length() > 0) {
			PerformedAction pAction = PerformedAction.fromId(action);
			if (pAction == PerformedAction.UPDATE_COMPLETED || pAction == PerformedAction.STATUS_COMPLETED) {
				String message = BaseMessages.format(SVNMessages.Console_AtRevision,
						new String[] { String.valueOf(revision) });
				stream.write(IConsoleStream.LEVEL_OK, message);
			} else if (pAction == PerformedAction.UPDATE_EXTERNAL) {
				String message = BaseMessages.format(SVNMessages.Console_UpdateExternal, new String[] { path });
				stream.write(IConsoleStream.LEVEL_OK, message);
			} else {
				int severity = IConsoleStream.LEVEL_OK;
				String status = null;
				switch (pAction) {
					case ADD:
					case UPDATE_ADD:
					case COMMIT_ADDED: {
						status = SVNMessages.Console_Action_Added;
						break;
					}
					case DELETE:
					case UPDATE_DELETE:
					case COMMIT_DELETED: {
						status = SVNMessages.Console_Action_Deleted;
						break;
					}
					case UPDATE_UPDATE: {
						int resourceState = contentState == NodeStatus.INAPPLICABLE.id
								|| contentState == NodeStatus.UNCHANGED.id ? propState : contentState;
						severity = contentState == NodeStatus.CONFLICTED.id || contentState == NodeStatus.OBSTRUCTED.id
								|| propState == NodeStatus.CONFLICTED.id
										? IConsoleStream.LEVEL_WARNING
										: IConsoleStream.LEVEL_OK;
						if (resourceState >= 0 && resourceState < NodeStatus.shortStatusNames.length) {
							status = SVNMessages
									.getString("Console_Update_Status_" + NodeStatus.statusNames[resourceState]); //$NON-NLS-1$
							if (status.length() > 0) {
								break;
							}
						}
						status = " "; //$NON-NLS-1$
						break;
					}
					case COMMIT_MODIFIED: {
						status = SVNMessages.Console_Action_Modified;
						break;
					}
					case COMMIT_REPLACED: {
						status = SVNMessages.Console_Action_Replaced;
						break;
					}
					case REVERT: {
						status = SVNMessages.Console_Action_Reverted;
						break;
					}
					case RESTORE: {
						status = SVNMessages.Console_Action_Restored;
						break;
					}
					case LOCKED: {
						status = SVNMessages.Console_Action_Locked;
						break;
					}
					case UNLOCKED: {
						status = SVNMessages.Console_Action_Unlocked;
						break;
					}
					case TREE_CONFLICT:
						status = SVNMessages.Console_Status_Conflicted;
						severity = IConsoleStream.LEVEL_WARNING;
						break;
					default: {
						int resourceState = contentState == SVNEntryStatus.Kind.NORMAL.id ? propState : contentState;
						status = SVNProgressMonitor.getStatus(resourceState);
						severity = resourceState == SVNEntryStatus.Kind.CONFLICTED.id
								|| resourceState == SVNEntryStatus.Kind.OBSTRUCTED.id
										? IConsoleStream.LEVEL_WARNING
										: IConsoleStream.LEVEL_OK;
						break;
					}
				}
				if (pAction == PerformedAction.COMMIT_POSTFIX_TXDELTA) {
					String message = BaseMessages.format(SVNMessages.Console_TransmittingData, new String[] { path });
					stream.write(severity, message);
				} else if (status != null) {
					String message = BaseMessages.format(SVNMessages.Console_Status, new String[] { status, path });
					stream.write(severity, message);
				}
			}
		}
	}

	protected static String getStatus(int resourceState) {
		switch (SVNEntryStatus.Kind.fromId(resourceState)) {
			case ADDED: {
				return SVNMessages.Console_Status_Added;
			}
			case MODIFIED: {
				return SVNMessages.Console_Status_Modified;
			}
			case DELETED: {
				return SVNMessages.Console_Status_Deleted;
			}
			case MISSING: {
				return SVNMessages.Console_Status_Missing;
			}
			case REPLACED: {
				return SVNMessages.Console_Status_Replaced;
			}
			case MERGED: {
				return SVNMessages.Console_Status_Merged;
			}
			case CONFLICTED: {
				return SVNMessages.Console_Status_Conflicted;
			}
			case OBSTRUCTED: {
				return SVNMessages.Console_Status_Obstructed;
			}
			default: {
				return null;
			}
		}
	}

}
