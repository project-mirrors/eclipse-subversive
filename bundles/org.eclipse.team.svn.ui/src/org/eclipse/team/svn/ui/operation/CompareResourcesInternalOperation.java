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
 *    Florent Angebault - Exclude from compare view files with identical content in case they're reported
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.operation;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNDiffStatus;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.ILocalFolder;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.compare.ComparePanel;
import org.eclipse.team.svn.ui.compare.ResourceCompareInput;
import org.eclipse.team.svn.ui.compare.ThreeWayResourceCompareInput;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * This operation calculate and show differences between WORKING and BASE revisions of a local resources
 * 
 * @author Alexander Gurov
 */
public class CompareResourcesInternalOperation extends AbstractActionOperation {
	protected ILocalResource local;

	protected IRepositoryResource ancestor;

	protected IRepositoryResource remote;

	protected boolean showInDialog;

	protected boolean forceReuse;

	protected String forceId;

	protected long options;

	public CompareResourcesInternalOperation(ILocalResource local, IRepositoryResource remote, boolean forceReuse,
			boolean showInDialog, long options) {
		super("Operation_CompareLocal", SVNUIMessages.class); //$NON-NLS-1$
		this.local = local;
		ancestor = local.isCopied()
				? SVNUtility.getCopiedFrom(local)
				: SVNRemoteStorage.instance().asRepositoryResource(local.getResource());
		ancestor.setSelectedRevision(local.getBaseRevision() != SVNRevision.INVALID_REVISION_NUMBER
				? SVNRevision.fromNumber(local.getBaseRevision())
				: SVNRevision.INVALID_REVISION);
		this.remote = remote;
		this.showInDialog = showInDialog;
		this.forceReuse = forceReuse;
		this.options = options;
	}

	public void setForceId(String forceId) {
		this.forceId = forceId;
	}

	public String getForceId() {
		return forceId;
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		final ArrayList<SVNDiffStatus> localChanges = new ArrayList<>();
		final ArrayList<SVNDiffStatus> remoteChanges = new ArrayList<>();

		IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(local.getResource());
		ISVNConnector proxy = location.acquireSVNProxy();

		try {
			if (CoreExtensionsManager.instance()
					.getSVNConnectorFactory()
					.getSVNAPIVersion() == ISVNConnectorFactory.APICompatibility.SVNAPI_1_7_x
					|| remote.getSelectedRevision() == SVNRevision.BASE) {
				fetchStatuses17(proxy, localChanges, remoteChanges, monitor);
			} else {
				fetchStatuses18(proxy, localChanges, remoteChanges, monitor);
			}
		} finally {
			location.releaseSVNProxy(proxy);
		}

		this.protectStep(monitor1 -> {
			CompareConfiguration cc = new CompareConfiguration();
			cc.setProperty(CompareEditor.CONFIRM_SAVE_PROPERTY, Boolean.TRUE);
			final ThreeWayResourceCompareInput compare = new ThreeWayResourceCompareInput(cc, local, ancestor,
					remote, localChanges, remoteChanges);
			compare.setForceId(forceId);
			compare.initialize(monitor1);
			if (!monitor1.isCanceled()) {
				UIMonitorUtility.getDisplay().syncExec(() -> {
					if (showInDialog) {
						if (CompareResourcesInternalOperation.this.compareResultOK(compare)) {
							ComparePanel panel = new ComparePanel(compare, local.getResource());
							DefaultDialog dialog = new DefaultDialog(UIMonitorUtility.getShell(), panel);
							dialog.open();
						}
					} else {
						ResourceCompareInput.openCompareEditor(compare, forceReuse);
					}
				});
			}
		}, monitor, 100, 50);
	}

	protected void fetchStatuses17(final ISVNConnector proxy, final ArrayList<SVNDiffStatus> localChanges,
			final ArrayList<SVNDiffStatus> remoteChanges, final IProgressMonitor monitor) throws Exception {
		final HashMap<IResource, Long> resourcesWithChanges = new HashMap<>();
		final IContainer compareRoot = local instanceof ILocalFolder
				? (IContainer) local.getResource()
				: local.getResource().getParent();
		IRepositoryResource resource = SVNRemoteStorage.instance()
				.asRepositoryResource(CompareResourcesInternalOperation.this.local.getResource());
		final long cmpTargetRevision = remote.getSelectedRevision().getKind() == SVNRevision.Kind.BASE
				? local.getRevision()
				: resource.exists() ? resource.getRevision() : SVNRevision.INVALID_REVISION_NUMBER;
		final LinkedHashSet<Long> revisions = new LinkedHashSet<>();

		if (cmpTargetRevision != SVNRevision.INVALID_REVISION_NUMBER) {
			revisions.add(cmpTargetRevision);
			this.protectStep(monitor1 -> {
				final String rootPath = FileUtility.getWorkingCopyPath(local.getResource());
				long options = ISVNConnector.Options.IGNORE_EXTERNALS | ISVNConnector.Options.LOCAL_SIDE;
				if (remote.getSelectedRevision() != SVNRevision.BASE) {
					options |= ISVNConnector.Options.SERVER_SIDE;
				}
				proxy.status(rootPath, SVNDepth.INFINITY, options, null, status -> {
					IPath tPath = new Path(status.path.substring(rootPath.length()));
					IResource resource1 = compareRoot.findMember(tPath);
					if (resource1 == null) {
						resource1 = status.nodeKind == SVNEntry.Kind.FILE
								? compareRoot.getFile(tPath)
								: compareRoot.getFolder(tPath);
					}
					String textStatus = SVNRemoteStorage.getTextStatusString(status.propStatus,
							status.textStatus, false);
					if (IStateFilter.SF_ANY_CHANGE.accept(resource1, textStatus, 0)
							|| status.propStatus == SVNEntryStatus.Kind.MODIFIED) {
						localChanges.add(new SVNDiffStatus(status.path, status.path, status.nodeKind,
								status.textStatus, status.propStatus));
					}
					textStatus = SVNRemoteStorage.getTextStatusString(status.repositoryPropStatus,
							status.repositoryTextStatus, true);
					if ((IStateFilter.SF_ANY_CHANGE.accept(resource1, textStatus, 0)
							|| status.repositoryTextStatus == SVNEntryStatus.Kind.MODIFIED)
							&& status.revision != cmpTargetRevision) {
						resourcesWithChanges.put(resource1, status.revision);
						revisions.add(status.revision);
					}
				}, new SVNProgressMonitor(CompareResourcesInternalOperation.this, monitor1, null, false));
			}, monitor, 100, 10);
		} else { // compare new unversioned files
			String path = FileUtility.getWorkingCopyPath(local.getResource());
			localChanges.add(new SVNDiffStatus(path, path,
					local.getResource().getType() == IResource.FILE ? SVNEntry.Kind.FILE : SVNEntry.Kind.DIR,
					SVNEntryStatus.Kind.UNVERSIONED, SVNEntryStatus.Kind.NORMAL));
		}

		if (remote.getSelectedRevision() != SVNRevision.BASE) {
			for (final long revision : revisions) {
				this.protectStep(monitor1 -> {
					IRepositoryResource resource2 = SVNRemoteStorage.instance()
							.asRepositoryResource(local.getResource());
					resource2.setSelectedRevision(SVNRevision.fromNumber(revision));
					final SVNEntryRevisionReference refPrev = SVNUtility.getEntryRevisionReference(resource2);
					final SVNEntryRevisionReference refNext = SVNUtility.getEntryRevisionReference(remote);
					final String prevRootURL = resource2.getUrl();
					proxy.diffStatusTwo(refPrev, refNext, SVNDepth.INFINITY, options, null,
							status -> {
								IPath tPath = new Path(status.pathPrev.substring(prevRootURL.length()));
								IResource resource1 = compareRoot.findMember(tPath);
								if (resource1 == null) {
									resource1 = status.nodeKind == SVNEntry.Kind.FILE
											? compareRoot.getFile(tPath)
											: compareRoot.getFolder(tPath);
								}
								Long rev = resourcesWithChanges.get(resource1);
								if (rev == null || rev == revision) {
									String pathPrev = ancestor.getUrl()
											+ status.pathNext.substring(refNext.path.length());
									remoteChanges.add(new SVNDiffStatus(pathPrev, status.pathNext,
											status.nodeKind, status.textStatus, status.propStatus));
								}
							},
							new SVNProgressMonitor(CompareResourcesInternalOperation.this, monitor1, null, false));
				}, monitor, 100, 40 / revisions.size());
			}
		}
	}

	protected void fetchStatuses18(final ISVNConnector proxy, final ArrayList<SVNDiffStatus> localChanges,
			final ArrayList<SVNDiffStatus> remoteChanges, final IProgressMonitor monitor) {
		final IContainer compareRoot = CompareResourcesInternalOperation.this.local instanceof ILocalFolder
				? (IContainer) CompareResourcesInternalOperation.this.local.getResource()
				: CompareResourcesInternalOperation.this.local.getResource().getParent();

		this.protectStep(monitor1 -> {
			String rootPath = FileUtility.getWorkingCopyPath(local.getResource());
			final String searchPath = local.getResource().getType() == IResource.FILE
					? FileUtility.getWorkingCopyPath(
							local.getResource().getParent())
					: rootPath;
			long options = ISVNConnector.Options.IGNORE_EXTERNALS | ISVNConnector.Options.LOCAL_SIDE;
			if (remote.getSelectedRevision() != SVNRevision.BASE) {
				options |= ISVNConnector.Options.SERVER_SIDE;
			}
			proxy.status(rootPath, SVNDepth.INFINITY, options, null, status -> {
				IPath tPath = new Path(status.path.substring(searchPath.length()));
				IResource resource = compareRoot.findMember(tPath);
				if (resource == null) {
					resource = status.nodeKind == SVNEntry.Kind.FILE
							? compareRoot.getFile(tPath)
							: compareRoot.getFolder(tPath);
				}
				String textStatus = SVNRemoteStorage.getTextStatusString(status.propStatus, status.textStatus,
						false);
				if (IStateFilter.SF_ANY_CHANGE.accept(resource, textStatus, 0)
						|| status.propStatus == SVNEntryStatus.Kind.MODIFIED) {
					localChanges.add(new SVNDiffStatus(status.path, status.path, status.nodeKind,
							status.textStatus, status.propStatus));
				}
			}, new SVNProgressMonitor(CompareResourcesInternalOperation.this, monitor1, null, false));
		}, monitor, 100, 10);

		this.protectStep(monitor1 -> {
			final IPath rootPath = FileUtility.getResourcePath(compareRoot);

			final boolean isFile = local.getResource().getType() == IResource.FILE;
			SVNEntryRevisionReference refPrev = new SVNEntryRevisionReference(
					FileUtility.getWorkingCopyPath(local.getResource()), null, SVNRevision.WORKING);
			final SVNEntryRevisionReference refNext = SVNUtility.getEntryRevisionReference(remote);
			// does not work with BASE working copy revision (not implemented yet exception)
			// NOTE SVN Kit 1.8.14 loses some statuses [when the folder was removed then added, for example]
			final FakeOutputStream diffPathCollector = new FakeOutputStream();
			proxy.diffTwo(refPrev, refNext, rootPath.toFile().getAbsolutePath(), diffPathCollector,
					SVNDepth.INFINITY, options, null, ISVNConnector.DiffOptions.NONE,
					new SVNProgressMonitor(CompareResourcesInternalOperation.this, monitor1, null, false));
			proxy.diffStatusTwo(refPrev, refNext, SVNDepth.INFINITY, options, null, status -> {
				IPath tPath = new Path(status.pathPrev);
				tPath = tPath.removeFirstSegments(rootPath.segmentCount());
				IResource resource = compareRoot.findMember(tPath);
				if (resource == null) {
					resource = status.nodeKind == SVNEntry.Kind.FILE
							? compareRoot.getFile(tPath)
							: compareRoot.getFolder(tPath);
				}
				ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
				if (!IStateFilter.SF_ANY_CHANGE.accept(local) || IStateFilter.SF_NOTEXISTS.accept(local)) {
					// it seems the status is calculated relatively to the working copy, so deletion and addition changes should actually be reversed
					SVNDiffStatus.Kind change = status.textStatus == SVNDiffStatus.Kind.ADDED
							? SVNDiffStatus.Kind.DELETED
							: status.textStatus == SVNDiffStatus.Kind.DELETED
									? SVNDiffStatus.Kind.ADDED
									: status.textStatus;
					// TODO could there be a case when relative paths are reported? If so - looks like a bug to me...
					String diffPathEntry = status.pathNext.startsWith(refNext.path)
							&& !status.pathNext.equals(refNext.path)
									? status.pathNext.substring(refNext.path.length())
									: "/" + resource.getName(); // isFile or wrong path issue?
					if (status.nodeKind == SVNEntry.Kind.DIR || diffPathCollector.contains(diffPathEntry)) {
						String relativePart = isFile ? "" : diffPathEntry;
						String pathPrev = ancestor.getUrl() + relativePart;
						remoteChanges.add(new SVNDiffStatus(pathPrev, status.pathNext, status.nodeKind, change,
								status.propStatus));
					}
				}
			}, new SVNProgressMonitor(CompareResourcesInternalOperation.this, monitor1, null, false));
		}, monitor, 100, 40);
	}

	protected boolean compareResultOK(CompareEditorInput input) {
		final Shell shell = UIMonitorUtility.getShell();

		try {
			SVNTeamUIPlugin.instance().getWorkbench().getProgressService().run(true, true, input);

			String message = input.getMessage();
			if (message != null) {
				MessageDialog.openError(shell, Utilities.getString("CompareUIPlugin.compareFailed"), message); //$NON-NLS-1$ Compare's property
			} else if (input.getCompareResult() == null) {
				MessageDialog.openInformation(shell, Utilities.getString("CompareUIPlugin.dialogTitle"), //$NON-NLS-1$
						Utilities.getString("CompareUIPlugin.noDifferences")); //$NON-NLS-1$ Compare's properties
			} else {
				return true;
			}
		} catch (InterruptedException x) {
			// cancelled by user
		} catch (InvocationTargetException x) {
			MessageDialog.openError(shell, Utilities.getString("CompareUIPlugin.compareFailed"), //$NON-NLS-1$
					x.getTargetException().getMessage()); //Compare's property
		}
		return false;
	}

	/**
	 * Workaround.
	 * 
	 * This {@link OutputStream} parses the output of "svn diff" and only catches paths from the lines beginning with "Index: ".
	 * 
	 * @author fangebault
	 */
	private static class FakeOutputStream extends OutputStream {

		boolean lineStart = true;

		boolean indexLine = false;

		Set<String> paths = new LinkedHashSet<>();

		StringBuilder w = new StringBuilder();

		@Override
		public void write(int b) throws IOException {
			char c = (char) b;
			if (c == 'I' && lineStart) {
				indexLine = true;
				w.append(c);
			} else if (c == '\n' || c == '\r') {
				if (indexLine) {
					paths.add("/" + w.toString().substring("Index: ".length()));
					w.replace(0, w.length(), "");
				}
				lineStart = true;
				indexLine = false;
			} else {
				lineStart = false;
				if (indexLine) {
					w.append(c);
				}
			}
		}

		private Set<String> getPaths() {
			return paths;
		}

		public boolean contains(String path) {
			return getPaths().contains(path);
		}

	}

}
