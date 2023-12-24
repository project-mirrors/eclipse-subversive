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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;

/**
 * Used by operations from different hierarchies
 * 
 * @author Igor Burilo
 */
public class UnresolvedConflictDetectorHelper implements IUnresolvedConflictDetector {

	protected Set<IResource> processed;

	protected Set<IResource> unprocessed;

	protected boolean hasUnresolvedConflict;

	protected String conflictMessage;

	@Override
	public void setUnresolvedConflict(boolean hasUnresolvedConflict) {
		this.hasUnresolvedConflict = hasUnresolvedConflict;
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
	public IResource[] getUnprocessed() {
		return unprocessed == null ? new IResource[0] : unprocessed.toArray(new IResource[unprocessed.size()]);
	}

	@Override
	public IResource[] getProcessed() {
		return processed == null ? new IResource[0] : processed.toArray(new IResource[processed.size()]);
	}

	protected void defineInitialResourceSet(IResource[] resources) {
		hasUnresolvedConflict = false;
		unprocessed = new HashSet<>();
		processed = new HashSet<>();
		processed.addAll(Arrays.asList(resources));
	}

	@Override
	public void addUnprocessed(IResource unprocessed) {
		this.unprocessed.add(unprocessed);
	}

	@Override
	public void setConflictMessage(String message) {
		conflictMessage = message;

	}

	@Override
	public void removeProcessed(IResource resource) {
		unprocessed.remove(resource);
	}
}
