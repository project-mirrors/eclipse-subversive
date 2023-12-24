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
 *    Gabor Liptak - Speedup Pattern's usage
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.PatternProvider;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * The operation saves project meta (.project and .classpath) in order to prevent project refresh problem when meta is deleted
 * 
 * @author Alexander Gurov
 */
public class SaveProjectMetaOperation extends AbstractWorkingCopyOperation implements IResourceProvider {
	// exclude .settings folder due large information amount contained in it
	protected static final String[] META_FILES = { ".project", ".classpath" }; //$NON-NLS-1$ //$NON-NLS-2$

	protected HashMap<String, File> savedMetas;

	protected String startsWith;

	public SaveProjectMetaOperation(IResource[] resources) {
		this(resources, null);
	}

	public SaveProjectMetaOperation(IResource[] resources, String startsWith) {
		super("Operation_SaveMeta", SVNMessages.class, resources); //$NON-NLS-1$
		savedMetas = new HashMap<>();
		this.startsWith = startsWith;
	}

	public SaveProjectMetaOperation(IResourceProvider provider) {
		this(provider, null);
	}

	public SaveProjectMetaOperation(IResourceProvider provider, String startsWith) {
		super("Operation_SaveMeta", SVNMessages.class, provider); //$NON-NLS-1$
		savedMetas = new HashMap<>();
		this.startsWith = startsWith;
	}

	@Override
	public IResource[] getResources() {
		return operableData();
	}

	public Map<String, File> getSavedMetas() {
		return savedMetas;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] changeSet = operableData();

		for (int i = 0; i < changeSet.length && !monitor.isCanceled(); i++) {
			final IResource change = changeSet[i];
			this.protectStep(monitor1 -> {
				IProject project = change.getProject();
				if (!project.isAccessible()) {
					return;
				}
				IResource[] members = project.members();
				for (int i1 = 0; i1 < members.length && !monitor1.isCanceled(); i1++) {
					if (SaveProjectMetaOperation.this.shouldBeSaved(members[i1])) {
						try {
							SaveProjectMetaOperation.this.saveMeta(members[i1], monitor1);
						} catch (Exception ex) {
							// disallow error reporting
						}
					}
				}
			}, monitor, changeSet.length);
		}
	}

	protected boolean shouldBeSaved(IResource resource) {
		String name = resource.getName();
		if (name.equals(SVNUtility.getSVNFolderName())) {
			return false;
		}
		if (startsWith != null) {
			return name.startsWith(startsWith);
		}
		for (String element : SaveProjectMetaOperation.META_FILES) {
			if (name.equalsIgnoreCase(element)) {
				return true;
			}
		}
		return false;
	}

	protected void saveMeta(IResource resource, IProgressMonitor monitor) throws Exception {
		if (resource != null) {
			String sourceLocation = PatternProvider.replaceAll(FileUtility.getWorkingCopyPath(resource), "\\\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
			File source = new File(sourceLocation);
			if (source.exists()) {
				File target = SVNTeamPlugin.instance().getTemporaryFile(null, resource.getName());
				target.deleteOnExit();
				if (source.isDirectory()) {
					target.delete();
					FileUtility.copyAll(target, source, FileUtility.COPY_NO_OPTIONS, pathname -> !pathname.getName().equals(SVNUtility.getSVNFolderName()), monitor);
				} else {
					FileUtility.copyFile(target, source, monitor);
				}
				savedMetas.put(sourceLocation, target);
			}
		}
	}

}
