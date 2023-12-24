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
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * The operation restores project meta (.project and .classpath) in order to prevent project refresh problem when meta is deleted
 * 
 * @author Alexander Gurov
 */
public class RestoreProjectMetaOperation extends AbstractActionOperation {
	protected SaveProjectMetaOperation saveOp;

	protected boolean force;

	public RestoreProjectMetaOperation(SaveProjectMetaOperation saveOp) {
		this(saveOp, false);
	}

	public RestoreProjectMetaOperation(SaveProjectMetaOperation saveOp, boolean force) {
		super("Operation_RestoreMeta", SVNMessages.class); //$NON-NLS-1$
		this.saveOp = saveOp;
		this.force = force;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return saveOp.getSchedulingRule();
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		Map<?, ?> container = saveOp.getSavedMetas();
		for (Iterator<?> it = container.entrySet().iterator(); it.hasNext() && !monitor.isCanceled();) {
			Map.Entry entry = (Map.Entry) it.next();
			final File targetFile = new File((String) entry.getKey());
			final File savedCopy = (File) entry.getValue();
			if (targetFile.getParentFile().exists()) {
				if (savedCopy.isDirectory()) {
					this.protectStep(monitor1 -> {
						try {
							if (force) {
								FileUtility.copyAll(targetFile.getParentFile(), savedCopy.listFiles()[0],
										FileUtility.COPY_IGNORE_EXISTING_FOLDERS
												| FileUtility.COPY_OVERRIDE_EXISTING_FILES,
										null, monitor1);
							} else {
								FileUtility.copyAll(targetFile.getParentFile(), savedCopy.listFiles()[0],
										FileUtility.COPY_IGNORE_EXISTING_FOLDERS, null, monitor1);
							}
						} catch (Exception ex) {
							// Any Exception must be ignored in this context
						}
					}, monitor, container.size());
				} else if (!targetFile.exists() || force) {
					this.protectStep(monitor1 -> {
						try {
							FileUtility.copyFile(targetFile, savedCopy, monitor1);
						} catch (Exception ex) {
							// Any Exception must be ignored in this context
						}
					}, monitor, container.size());
				}
			}
			FileUtility.deleteRecursive(savedCopy);
		}
	}

}
