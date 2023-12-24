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

import org.eclipse.core.resources.IResource;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.svn.core.resource.IResourceProvider;

/**
 * Abstract operation that implement functionality to detect unresolved conflicts in time of WC operations
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractConflictDetectionOperation extends AbstractWorkingCopyOperation
		implements IUnresolvedConflictDetector {

	protected UnresolvedConflictDetectorHelper conflictDetectorHelper;

	public AbstractConflictDetectionOperation(String operationName, Class<? extends NLS> messagesClass,
			IResource[] resources) {
		super(operationName, messagesClass, resources);
		conflictDetectorHelper = new UnresolvedConflictDetectorHelper();
	}

	public AbstractConflictDetectionOperation(String operationName, Class<? extends NLS> messagesClass,
			IResourceProvider provider) {
		super(operationName, messagesClass, provider);
		conflictDetectorHelper = new UnresolvedConflictDetectorHelper();
	}

	@Override
	public void setUnresolvedConflict(boolean hasUnresolvedConflict) {
		conflictDetectorHelper.setUnresolvedConflict(hasUnresolvedConflict);
	}

	@Override
	public boolean hasUnresolvedConflicts() {
		return conflictDetectorHelper.hasUnresolvedConflicts();
	}

	@Override
	public String getMessage() {
		return conflictDetectorHelper.getMessage();
	}

	@Override
	public IResource[] getUnprocessed() {
		return conflictDetectorHelper.getUnprocessed();
	}

	@Override
	public IResource[] getProcessed() {
		return conflictDetectorHelper.getProcessed();
	}

	public void defineInitialResourceSet(IResource[] resources) {
		conflictDetectorHelper.defineInitialResourceSet(resources);
	}

	@Override
	public void addUnprocessed(IResource unprocessed) {
		conflictDetectorHelper.addUnprocessed(unprocessed);
	}

	@Override
	public void setConflictMessage(String message) {
		conflictDetectorHelper.setConflictMessage(message);
	}

	@Override
	public void removeProcessed(IResource resource) {
		conflictDetectorHelper.removeProcessed(resource);
	}

}
