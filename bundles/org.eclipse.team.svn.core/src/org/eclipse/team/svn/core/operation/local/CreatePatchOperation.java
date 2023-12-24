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

package org.eclipse.team.svn.core.operation.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.Team;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Create patch operation implementation
 * 
 * @author Alexander Gurov
 */
public class CreatePatchOperation extends AbstractActionOperation {
	public static final int SELECTION = 0;

	public static final int PROJECT = 1;

	public static final int WORKSPACE = 2;

	protected IResource[] resources;

	protected IResource[] selection;

	protected String fileName;

	protected boolean recurse;

	protected boolean processUnversioned;

	protected long options;

	protected long diffOptions;

	protected int rootPoint;

	protected String lineFeed = System.lineSeparator();

	protected String contentSeparatorLine = "==================================================================="; //$NON-NLS-1$

	protected String contentSeparator = lineFeed + contentSeparatorLine + lineFeed;

	protected String indexEntry = "Index: "; //$NON-NLS-1$

	protected String removeSign = "--- "; //$NON-NLS-1$

	protected String addSign = "+++ "; //$NON-NLS-1$

	protected String revisionMark = "\t(revision 0)" + lineFeed; //$NON-NLS-1$

	protected String noLF = lineFeed + "\\ No newline at end of file" + lineFeed; //$NON-NLS-1$

	protected String rangeStart = "@@ -0,0 +1"; //$NON-NLS-1$

	protected String rangeEnd = " @@" + lineFeed; //$NON-NLS-1$

	public CreatePatchOperation(IResource[] resources, String fileName, boolean recurse, boolean ignoreDeleted,
			boolean processBinary, boolean processUnversioned) {
		this(resources, fileName, recurse, ignoreDeleted, processBinary, processUnversioned,
				CreatePatchOperation.PROJECT);
	}

	public CreatePatchOperation(IResource[] resources, String fileName, boolean recurse, boolean ignoreDeleted,
			boolean processBinary, boolean processUnversioned, int rootPoint) {
		this(resources, fileName, recurse, processUnversioned,
				ISVNConnector.Options.IGNORE_ANCESTRY
						| (ignoreDeleted ? ISVNConnector.Options.SKIP_DELETED : ISVNConnector.Options.NONE)
						| (processBinary ? ISVNConnector.Options.FORCE : ISVNConnector.Options.NONE),
				rootPoint, ISVNConnector.DiffOptions.NONE);
	}

	public CreatePatchOperation(IResource[] resources, String fileName, boolean recurse, boolean processUnversioned,
			long options, int rootPoint, long diffOptions) {
		super("Operation_CreatePatchLocal", SVNMessages.class); //$NON-NLS-1$
		this.resources = resources;
		this.fileName = fileName;
		this.recurse = recurse;
		this.processUnversioned = processUnversioned;
		this.options = options & ISVNConnector.CommandMasks.DIFF;
		this.rootPoint = rootPoint;
		this.diffOptions = diffOptions;
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		Map<IProject, List<IResource>> workingCopies = new HashMap<>();//SVNUtility.splitWorkingCopies(this.resources);
		for (IResource res : resources) {
			List<IResource> list = workingCopies.get(res.getProject());
			if (list == null) {
				workingCopies.put(res.getProject(), list = new ArrayList<>());
			}
			list.add(res);
		}
		final FileOutputStream stream = new FileOutputStream(fileName);
		try {
//			this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn diff " + (this.recurse ? "" : " -N") + (this.ignoreDeleted ? " --no-diff-deleted" : "") + "\n");
			if (workingCopies.size() > 1 || rootPoint == CreatePatchOperation.WORKSPACE) {
				rootPoint = CreatePatchOperation.WORKSPACE;
				stream.write("### Eclipse Workspace Patch 1.0".getBytes());
				stream.write(lineFeed.getBytes());
			} else if (rootPoint == CreatePatchOperation.SELECTION) {
				selection = FileUtility.shrinkChildNodes(resources);
			}
			for (Iterator<?> it = workingCopies.entrySet().iterator(); it.hasNext() && !monitor.isCanceled();) {
				Map.Entry entry = (Map.Entry) it.next();
				IProject project = (IProject) entry.getKey();
				if (rootPoint == CreatePatchOperation.WORKSPACE) {
					stream.write("#P ".getBytes());
					stream.write(project.getName().getBytes());
					stream.write(lineFeed.getBytes());
				}
				IResource[] resources = ((List<?>) entry.getValue()).toArray(new IResource[0]);
				FileUtility.reorder(resources, true);
				for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
					if (resources[i] instanceof IFile) {
						addFileDiff(stream, (IFile) resources[i], monitor);
					} else if (recurse) {
						FileUtility.visitNodes(resources[i], resource -> {
							if (monitor.isCanceled() || FileUtility.isNotSupervised(resource)) {
								return false;
							}
							if (resource instanceof IFile) {
								CreatePatchOperation.this.addFileDiff(stream, (IFile) resource, monitor);
							}
							return true;
						}, IResource.DEPTH_INFINITE, true, true);
					}
				}
			}
		} finally {
			try {
				stream.close();
			} catch (Exception ex) {
			}
		}
	}

	protected void addFileDiff(OutputStream stream, IFile resource, IProgressMonitor monitor) {
		try {
			String charset = null;
			if (resource instanceof IEncodedStorage) {
				charset = ((IEncodedStorage) resource).getCharset();
			}
			String wcPath = FileUtility.getWorkingCopyPath(resource);
			String projectPath = FileUtility.getWorkingCopyPath(resource.getProject());
			String fileName = wcPath.substring(projectPath.length() + 1);
			if (rootPoint == CreatePatchOperation.SELECTION) {
				IPath resourcePath = resource.getFullPath();
				for (IResource element : selection) {
					IPath selectionPath = element.getFullPath();
					if (selectionPath.isPrefixOf(resourcePath)) {
						fileName = element.getType() == IResource.FILE
								? fileName = resource.getName()
								: resourcePath.toString().substring(selectionPath.toString().length() + 1);
						break;
					}
				}
			}

			ILocalResource local = SVNRemoteStorage.instance().asLocalResourceAccessible(resource);
			if (IStateFilter.SF_VERSIONED.accept(local)) {
				File tmp = SVNTeamPlugin.instance().getTemporaryFile(null, "patch.tmp"); //$NON-NLS-1$

				IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(resource);
				ISVNConnector proxy = location.acquireSVNProxy();
				try {
					proxy.diffTwo(new SVNEntryRevisionReference(wcPath, null, SVNRevision.BASE),
							new SVNEntryRevisionReference(wcPath, null, SVNRevision.WORKING), projectPath,
							tmp.getAbsolutePath(), SVNDepth.EMPTY, options, null, diffOptions,
							new SVNProgressMonitor(this, monitor, null));

					int len = (int) tmp.length();
					if (len > 0) {
						byte[] data = new byte[len];
						InputStream input = new FileInputStream(tmp);
						try {
							input.read(data);
						} finally {
							try {
								input.close();
							} catch (Exception ex) {
							}
						}

						int idx = CreatePatchOperation.findOffset(data, contentSeparatorLine.getBytes(), 0);
						if (idx != -1) {
							byte[] rs = removeSign.getBytes();
							byte[] as = addSign.getBytes();
							byte[] fn = fileName.getBytes();
							stream.write(indexEntry.getBytes());
							stream.write(fn);
							stream.write(lineFeed.getBytes());
							int idx0 = CreatePatchOperation.findOffset(data, rs, idx);
							int idx1 = CreatePatchOperation.findOffset(data, "\t(".getBytes(), idx0);
							if (idx0 != -1 && idx1 != -1) {
								stream.write(data, idx, idx0 - idx + rs.length);
								stream.write(fn);
								idx = idx1;
							}
							idx0 = CreatePatchOperation.findOffset(data, as, idx);
							idx1 = CreatePatchOperation.findOffset(data, "\t(".getBytes(), idx0);
							if (idx0 != -1 && idx1 != -1) {
								stream.write(data, idx, idx0 - idx + as.length);
								stream.write(fn);
								idx = idx1;
							}
							stream.write(data, idx, data.length - idx);
						} else {
							stream.write(data);
						}
					}
				} finally {
					location.releaseSVNProxy(proxy);
					tmp.delete();
				}
			} else if (processUnversioned && !IStateFilter.SF_IGNORED.accept(local)) {
				int type = FileUtility.getMIMEType(resource);
				if ((options & ISVNConnector.Options.FORCE) != 0 || type != Team.BINARY) {
					stream.write(getNewFileDiff(wcPath, fileName, charset).getBytes(charset));
				}
			}
		} catch (IOException | SVNConnectorException | CoreException ex) {
			throw new UnreportableException(ex);
		}
	}

	protected String getNewFileDiff(String path, String fileName, String charset) throws IOException {
		File file = new File(path);
		byte[] data = new byte[(int) file.length()];
		InputStream stream = new FileInputStream(file);
		try {
			stream.read(data);
			return getNewContentDiff(fileName, new String(data, charset));
		} finally {
			try {
				stream.close();
			} catch (Exception ex) {
			}
		}
	}

	protected String getNewContentDiff(String fileName, String content) {
		if (content.length() == 0) {
			return getEmptyNewContentDiff(fileName);
		}
		ArrayList<String> tLines = new ArrayList<>();
		for (int i = 0, lineStart = 0, m = content.length(); i < m;) {
			if (content.charAt(i) == '\n') {
				tLines.add(content.substring(lineStart, ++i));
				lineStart = i;
			} else if (content.charAt(i) == '\r') {
				++i;
				if (i < m && content.charAt(i) == '\n') {
					++i;
				}
				tLines.add(content.substring(lineStart, i));
				lineStart = i;
			} else {
				i++;
				if (i == m) {
					tLines.add(content.substring(lineStart, i));
				}
			}
		}
		String[] lines = tLines.toArray(new String[tLines.size()]);
		String retVal = getFilledNewContentDiff(fileName, lines);
		if (!content.endsWith("\r\n") && !content.endsWith("\r") && !content.endsWith("\n")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			retVal += noLF;
		}
		return retVal;
	}

	protected String getFilledNewContentDiff(String fileName, String[] lines) {
		String retVal = getEmptyNewContentDiff(fileName) + removeSign + fileName + revisionMark + addSign + fileName
				+ revisionMark + rangeStart + (lines.length == 1 ? "" : "," + lines.length) + rangeEnd; //$NON-NLS-1$ //$NON-NLS-2$
		for (String line : lines) {
			retVal += "+" + line; //$NON-NLS-1$
		}
		return retVal;
	}

	protected String getEmptyNewContentDiff(String fileName) {
		return indexEntry + fileName + contentSeparator;
	}

	protected static int findOffset(byte[] where, byte[] what, int offset) {
		for (int i = offset, m = where.length - what.length; i < m; i++) {
			if (CreatePatchOperation.match(where, what, i)) {
				return i;
			}
		}
		return -1;
	}

	protected static boolean match(byte[] where, byte[] what, int offset) {
		if (where.length - offset < what.length) {
			return false;
		}
		for (int i = offset + what.length - 1, j = what.length - 1; i >= offset; i--, j--) {
			if (where[i] != what[j]) {
				return false;
			}
		}
		return true;
	}

}
