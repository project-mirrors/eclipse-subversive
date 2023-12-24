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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.osgi.util.NLS;

/**
 * Abstract operation that implement functionality to detect unresolved conflicts in time of WC operations
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractFileConflictDetectionOperation extends AbstractFileOperation
		implements IUnresolvedConflictDetector {
	protected Set processed;

	protected Set unprocessed;

	protected boolean hasUnresolvedConflict;

	protected String conflictMessage;

	public AbstractFileConflictDetectionOperation(String operationName, Class<? extends NLS> messagesClass,
			File[] files) {
		super(operationName, messagesClass, files);
	}

	public AbstractFileConflictDetectionOperation(String operationName, Class<? extends NLS> messagesClass,
			IFileProvider provider) {
		super(operationName, messagesClass, provider);
	}

	@Override
	public boolean hasUnresolvedConflicts() {
		return hasUnresolvedConflict;
	}

	@Override
	public String getMessage() {
		return conflictMessage;
	}

	@Override
	public File[] getProcessed() {
		return processed == null ? null : (File[]) processed.toArray(new File[processed.size()]);
	}

	@Override
	public File[] getUnprocessed() {
		return unprocessed == null ? null : (File[]) unprocessed.toArray(new File[unprocessed.size()]);
	}

	protected void defineInitialResourceSet(File[] resources) {
		hasUnresolvedConflict = false;
		unprocessed = new HashSet();
		processed = new HashSet();
		processed.addAll(Arrays.asList(resources));
	}

}
