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

package org.eclipse.team.svn.core.extension.factory;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.eclipse.team.svn.core.connector.ISVNAnnotationCallback;
import org.eclipse.team.svn.core.connector.ISVNCallListener;
import org.eclipse.team.svn.core.connector.ISVNChangeListCallback;
import org.eclipse.team.svn.core.connector.ISVNConflictResolutionCallback;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNCredentialsPrompt;
import org.eclipse.team.svn.core.connector.ISVNDiffStatusCallback;
import org.eclipse.team.svn.core.connector.ISVNEntryCallback;
import org.eclipse.team.svn.core.connector.ISVNEntryInfoCallback;
import org.eclipse.team.svn.core.connector.ISVNEntryStatusCallback;
import org.eclipse.team.svn.core.connector.ISVNImportFilterCallback;
import org.eclipse.team.svn.core.connector.ISVNLogEntryCallback;
import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.connector.ISVNPatchCallback;
import org.eclipse.team.svn.core.connector.ISVNProgressMonitor;
import org.eclipse.team.svn.core.connector.ISVNPropertyCallback;
import org.eclipse.team.svn.core.connector.SVNConflictResolution.Choice;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryReference;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNExternalReference;
import org.eclipse.team.svn.core.connector.SVNMergeInfo;
import org.eclipse.team.svn.core.connector.SVNMergeInfo.LogKind;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
import org.eclipse.team.svn.core.connector.configuration.ISVNConfigurationEventHandler;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.StringId;

/**
 * This class servers as facade for {@link ISVNConnector}. During operation, the thread name is modified in order to reflect the current
 * operation.
 * 
 * NIC misleading name. better: SvnConnectorFacade
 * 
 * Replace CRLF,CR to LF for svn:log property
 * 
 * @author Alexander Gurov
 */
public class ThreadNameModifier implements ISVNConnector {
	protected ISVNConnector connector;

	public ThreadNameModifier(ISVNConnector connector) {
		this.connector = connector;
	}

	@Override
	public void addCallListener(ISVNCallListener listener) {
		connector.addCallListener(listener);
	}

	@Override
	public void removeCallListener(ISVNCallListener listener) {
		connector.removeCallListener(listener);
	}

	@Override
	public void add(String path, SVNDepth depth, long options, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.add(FileUtility.normalizePathJavaHL(path), depth, options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void annotate(SVNEntryReference reference, SVNRevisionRange revisionRange, long options, long diffOptions,
			ISVNAnnotationCallback callback, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.annotate(reference, revisionRange, options, diffOptions, callback, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public long checkout(SVNEntryRevisionReference fromReference, String destPath, SVNDepth depth, long options,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			return connector.checkout(ThreadNameModifier.normalizePathJavaHL(fromReference),
					FileUtility.normalizePathJavaHL(destPath), depth, options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void cleanup(String path, long options, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.cleanup(FileUtility.normalizePathJavaHL(path), options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void commit(String[] path, String message, String[] changelistNames, SVNDepth depth, long options,
			Map revProps, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.commit(FileUtility.normalizePathsJavaHL(path), processSVNLogProperty(message), changelistNames,
					depth, options, revProps, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void copyLocal(SVNEntryRevisionReference[] srcPaths, String destPath, long options,
			Map<String, List<SVNExternalReference>> externalsToPin, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.copyLocal(ThreadNameModifier.normalizePathJavaHL(srcPaths),
					FileUtility.normalizePathJavaHL(destPath), options, externalsToPin, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void copyRemote(SVNEntryRevisionReference[] srcPaths, String destPath, String message, long options,
			Map revProps, Map<String, List<SVNExternalReference>> externalsToPin, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.copyRemote(ThreadNameModifier.normalizePathJavaHL(srcPaths),
					FileUtility.normalizePathJavaHL(destPath), processSVNLogProperty(message), options, revProps,
					externalsToPin, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void diffTwo(SVNEntryRevisionReference reference1, SVNEntryRevisionReference reference2,
			String relativeToDir, OutputStream stream, SVNDepth depth, long options, String[] changelistNames,
			long outputOptions, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.diffTwo(ThreadNameModifier.normalizePathJavaHL(reference1),
					ThreadNameModifier.normalizePathJavaHL(reference2), FileUtility.normalizePathJavaHL(relativeToDir),
					stream, depth, options, changelistNames, outputOptions, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void diff(SVNEntryReference reference, SVNRevisionRange revision1, String relativeToDir, String fileName,
			SVNDepth depth, long options, String[] changelistNames, long outputOptions, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.diff(ThreadNameModifier.normalizePathJavaHL(reference), revision1,
					FileUtility.normalizePathJavaHL(relativeToDir), fileName, depth, options, changelistNames,
					outputOptions, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void diffTwo(SVNEntryRevisionReference reference1, SVNEntryRevisionReference reference2,
			String relativeToDir, String fileName, SVNDepth depth, long options, String[] changelistNames,
			long outputOptions, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.diffTwo(ThreadNameModifier.normalizePathJavaHL(reference1),
					ThreadNameModifier.normalizePathJavaHL(reference2), FileUtility.normalizePathJavaHL(relativeToDir),
					fileName, depth, options, changelistNames, outputOptions, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void diff(SVNEntryReference reference, SVNRevisionRange revision1, String relativeToDir, OutputStream stream,
			SVNDepth depth, long options, String[] changelistNames, long outputOptions, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.diff(ThreadNameModifier.normalizePathJavaHL(reference), revision1,
					FileUtility.normalizePathJavaHL(relativeToDir), stream, depth, options, changelistNames,
					outputOptions, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void diffStatusTwo(SVNEntryRevisionReference reference1, SVNEntryRevisionReference reference2,
			SVNDepth depth, long options, String[] changelistNames, ISVNDiffStatusCallback cb,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.diffStatusTwo(ThreadNameModifier.normalizePathJavaHL(reference1),
					ThreadNameModifier.normalizePathJavaHL(reference2), depth, options, changelistNames, cb, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void diffStatus(SVNEntryReference reference, SVNRevisionRange revision1, SVNDepth depth, long options,
			String[] changelistNames, ISVNDiffStatusCallback cb, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.diffStatus(ThreadNameModifier.normalizePathJavaHL(reference), revision1, depth, options,
					changelistNames, cb, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void dispose() {
		String oldName = overrideThreadName();
		try {
			connector.dispose();
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public long exportTo(SVNEntryRevisionReference fromReference, String destPath, String nativeEOL, SVNDepth depth,
			long options, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			return connector.exportTo(ThreadNameModifier.normalizePathJavaHL(fromReference),
					FileUtility.normalizePathJavaHL(destPath), nativeEOL, depth, options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void importTo(String path, String url, String message, SVNDepth depth, long options, Map revProps,
			ISVNImportFilterCallback filter, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.importTo(FileUtility.normalizePathJavaHL(path), url, processSVNLogProperty(message), depth,
					options, revProps, filter, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public long switchTo(String path, SVNEntryRevisionReference toReference, SVNDepth depth, long options,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			return connector.switchTo(FileUtility.normalizePathJavaHL(path),
					ThreadNameModifier.normalizePathJavaHL(toReference), depth, options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void setConfigurationEventHandler(ISVNConfigurationEventHandler configHandler) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.setConfigurationEventHandler(configHandler);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public ISVNConfigurationEventHandler getConfigurationEventHandler() throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			return connector.getConfigurationEventHandler();
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public String getConfigDirectory() throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			return connector.getConfigDirectory();
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public ISVNNotificationCallback getNotificationCallback() {
		String oldName = overrideThreadName();
		try {
			return connector.getNotificationCallback();
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public ISVNCredentialsPrompt getPrompt() {
		String oldName = overrideThreadName();
		try {
			return connector.getPrompt();
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void listEntries(SVNEntryRevisionReference reference, SVNDepth depth, int direntFields, long options,
			ISVNEntryCallback cb, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.listEntries(ThreadNameModifier.normalizePathJavaHL(reference), depth, direntFields, options, cb,
					monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void lock(String[] path, String comment, long options, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.lock(FileUtility.normalizePathsJavaHL(path), comment, options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void listHistoryLog(SVNEntryReference reference, SVNRevisionRange[] revisionRanges, String[] revProps,
			long limit, long options, ISVNLogEntryCallback cb, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.listHistoryLog(ThreadNameModifier.normalizePathJavaHL(reference), revisionRanges, revProps, limit,
					options, cb, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void mergeTwo(SVNEntryRevisionReference reference1, SVNEntryRevisionReference reference2, String localPath,
			SVNDepth depth, long options, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.mergeTwo(ThreadNameModifier.normalizePathJavaHL(reference1),
					ThreadNameModifier.normalizePathJavaHL(reference2), FileUtility.normalizePathJavaHL(localPath),
					depth, options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void merge(SVNEntryReference reference, SVNRevisionRange[] revisions, String localPath, SVNDepth depth,
			long options, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.merge(ThreadNameModifier.normalizePathJavaHL(reference), revisions,
					FileUtility.normalizePathJavaHL(localPath), depth, options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void mergeReintegrate(SVNEntryReference reference, String localPath, long options,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.mergeReintegrate(ThreadNameModifier.normalizePathJavaHL(reference),
					FileUtility.normalizePathJavaHL(localPath), options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void mkdir(String[] path, String message, long options, Map revProps, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.mkdir(FileUtility.normalizePathsJavaHL(path), processSVNLogProperty(message), options, revProps,
					monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void moveLocal(String[] srcPaths, String dstPath, long options, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.moveLocal(FileUtility.normalizePathsJavaHL(srcPaths), FileUtility.normalizePathJavaHL(dstPath),
					options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void moveRemote(String[] srcPaths, String dstPath, String message, long options, Map revProps,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.moveRemote(FileUtility.normalizePathsJavaHL(srcPaths), FileUtility.normalizePathJavaHL(dstPath),
					processSVNLogProperty(message), options, revProps, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void setNotificationCallback(ISVNNotificationCallback notify) {
		String oldName = overrideThreadName();
		try {
			connector.setNotificationCallback(notify);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void setPassword(String password) {
		String oldName = overrideThreadName();
		try {
			connector.setPassword(password);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public SVNProperty getProperty(SVNEntryRevisionReference reference, String name, String[] changeLists,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			return connector.getProperty(ThreadNameModifier.normalizePathJavaHL(reference), name, changeLists, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void setPropertyLocal(String[] path, SVNProperty property, SVNDepth depth, long options,
			String[] changelistNames, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.setPropertyLocal(FileUtility.normalizePathsJavaHL(path), property, depth, options,
					changelistNames, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void setPropertyRemote(SVNEntryReference reference, SVNProperty property, String message, long options,
			Map revProps, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.setPropertyRemote(ThreadNameModifier.normalizePathJavaHL(reference), property, message, options,
					revProps, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void relocate(String from, String to, String path, SVNDepth depth, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.relocate(from, to, FileUtility.normalizePathJavaHL(path), depth, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void removeLocal(String[] path, long options, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.removeLocal(FileUtility.normalizePathsJavaHL(path), options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void removeRemote(String[] path, String message, long options, Map revProps, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.removeRemote(FileUtility.normalizePathsJavaHL(path), processSVNLogProperty(message), options,
					revProps, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void resolve(String path, Choice conflictResult, SVNDepth depth, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.resolve(FileUtility.normalizePathJavaHL(path), conflictResult, depth, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void revert(String[] paths, SVNDepth depth, String[] changeLists, long options, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.revert(FileUtility.normalizePathsJavaHL(paths), depth, changeLists, options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void setPrompt(ISVNCredentialsPrompt prompt) {
		String oldName = overrideThreadName();
		try {
			connector.setPrompt(prompt);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void status(String path, SVNDepth depth, long options, String[] changelistNames,
			ISVNEntryStatusCallback callback, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.status(FileUtility.normalizePathJavaHL(path), depth, options, changelistNames, callback, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public SVNProperty[] streamFileContent(SVNEntryRevisionReference reference, long options, OutputStream stream,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			return connector.streamFileContent(ThreadNameModifier.normalizePathJavaHL(reference), options, stream,
					monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void unlock(String[] path, long options, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.unlock(FileUtility.normalizePathsJavaHL(path), options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public long[] update(String[] path, SVNRevision revision, SVNDepth depth, long options, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			return connector.update(FileUtility.normalizePathsJavaHL(path), revision, depth, options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void setUsername(String username) {
		String oldName = overrideThreadName();
		try {
			connector.setUsername(username);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void addToChangeList(String[] paths, String changelist, SVNDepth depth, String[] changelistNames,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.addToChangeList(FileUtility.normalizePathsJavaHL(paths), changelist, depth, changelistNames,
					monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void listMergeInfoLog(LogKind logKind, SVNEntryReference reference, SVNEntryReference mergeSourceReference,
			SVNRevisionRange mergeSourceRange, String[] revProps, SVNDepth depth, long options, ISVNLogEntryCallback cb,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.listMergeInfoLog(logKind, ThreadNameModifier.normalizePathJavaHL(reference), mergeSourceReference,
					mergeSourceRange, revProps, depth, options, cb, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void dumpChangeLists(String[] changeLists, String rootPath, SVNDepth depth, ISVNChangeListCallback cb,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.dumpChangeLists(changeLists, FileUtility.normalizePathJavaHL(rootPath), depth, cb, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public SVNMergeInfo getMergeInfo(SVNEntryReference reference, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			return connector.getMergeInfo(ThreadNameModifier.normalizePathJavaHL(reference), monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void getInfo(SVNEntryRevisionReference reference, SVNDepth depth, long options, String[] changeLists,
			ISVNEntryInfoCallback cb, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.getInfo(ThreadNameModifier.normalizePathJavaHL(reference), depth, options, changeLists, cb,
					monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void listProperties(SVNEntryRevisionReference reference, SVNDepth depth, String[] changelistNames,
			long options, ISVNPropertyCallback callback, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.listProperties(ThreadNameModifier.normalizePathJavaHL(reference), depth, changelistNames, options,
					callback, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void removeFromChangeLists(String[] paths, SVNDepth depth, String[] changelistNames,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.removeFromChangeLists(FileUtility.normalizePathsJavaHL(paths), depth, changelistNames, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void setConfigDirectory(String configDir) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.setConfigDirectory(FileUtility.normalizePathJavaHL(configDir));
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void setConflictResolver(ISVNConflictResolutionCallback listener) {
		String oldName = overrideThreadName();
		try {
			connector.setConflictResolver(listener);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public ISVNConflictResolutionCallback getConflictResolver() {
		String oldName = overrideThreadName();
		try {
			return connector.getConflictResolver();
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public String[] suggestMergeSources(SVNEntryReference reference, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			return connector.suggestMergeSources(ThreadNameModifier.normalizePathJavaHL(reference), monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public SVNProperty[] listRevisionProperties(SVNEntryReference reference, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			return connector.listRevisionProperties(ThreadNameModifier.normalizePathJavaHL(reference), monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public SVNProperty getRevisionProperty(SVNEntryReference reference, String name, ISVNProgressMonitor monitor)
			throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			return connector.getRevisionProperty(ThreadNameModifier.normalizePathJavaHL(reference), name, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void setRevisionProperty(SVNEntryReference reference, SVNProperty property, String originalValue,
			long options, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.setRevisionProperty(ThreadNameModifier.normalizePathJavaHL(reference), property, originalValue,
					options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void upgrade(String path, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.upgrade(FileUtility.normalizePathJavaHL(path), monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void vacuum(String path, long options, ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.vacuum(FileUtility.normalizePathJavaHL(path), options, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	@Override
	public void patch(String patchPath, String targetPath, int stripCount, long options, ISVNPatchCallback callback,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		String oldName = overrideThreadName();
		try {
			connector.patch(FileUtility.normalizePathJavaHL(patchPath), FileUtility.normalizePathJavaHL(targetPath),
					stripCount, options, callback, monitor);
		} finally {
			restoreThreadName(oldName);
		}
	}

	protected String overrideThreadName() {
		Thread current = Thread.currentThread();
		String oldName = current.getName();
		current.setName(StringId.generateRandom("SVN", 5)); //$NON-NLS-1$
		return oldName;
	}

	protected void restoreThreadName(String oldName) {
		Thread.currentThread().setName(oldName);
	}

	/**
	 * Replace CRLF,CR to LF
	 * 
	 * @param str
	 * @return
	 */
	protected String processSVNLogProperty(String str) {
		return str != null ? str.replaceAll("\r\n|\r", "\n") : null;
	}

	protected static SVNEntryReference normalizePathJavaHL(SVNEntryReference reference) {
		if (reference instanceof SVNEntryRevisionReference) {
			return ThreadNameModifier.normalizePathJavaHL((SVNEntryRevisionReference) reference);
		}
		if (reference instanceof SVNExternalReference) {
			return ThreadNameModifier.normalizePathJavaHL((SVNExternalReference) reference);
		}
		if (reference.path.indexOf("://") == -1) {
			return new SVNEntryReference(FileUtility.normalizePathJavaHL(reference.path), reference.pegRevision);
		}
		return reference;
	}

	protected static SVNExternalReference normalizePathJavaHL(SVNExternalReference reference) {
		if (reference.path.indexOf("://") == -1) {
			return new SVNExternalReference(reference.target, FileUtility.normalizePathJavaHL(reference.path),
					reference.pegRevision, reference.revision);
		}
		return reference;
	}

	protected static SVNEntryRevisionReference[] normalizePathJavaHL(SVNEntryRevisionReference[] references) {
		SVNEntryRevisionReference[] retVal = new SVNEntryRevisionReference[references.length];
		for (int i = 0; i < references.length; i++) {
			retVal[i] = ThreadNameModifier.normalizePathJavaHL(references[i]);
		}
		return retVal;
	}

	protected static SVNEntryRevisionReference normalizePathJavaHL(SVNEntryRevisionReference reference) {
		if (reference.path.indexOf("://") == -1) {//reference.revision.getKind() == SVNRevision.Kind.WORKING &&
			return new SVNEntryRevisionReference(FileUtility.normalizePathJavaHL(reference.path), reference.pegRevision,
					reference.revision);
		}
		return reference;
	}
}
