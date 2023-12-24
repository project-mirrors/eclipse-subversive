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

package org.eclipse.team.svn.core.synchronize.variant;

import java.io.ByteArrayInputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.GetLocalFileContentOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Remote file emulator variant
 * 
 * @author Alexander Gurov
 */
public class VirtualRemoteFileVariant extends VirtualRemoteResourceVariant {

	public VirtualRemoteFileVariant(ILocalResource local) {
		super(local);
	}

	@Override
	protected void fetchContents(IProgressMonitor monitor) throws TeamException {
		if (!IStateFilter.SF_VERSIONED.accept(local) || local.isCopied()) {
			setContents(new ByteArrayInputStream(new byte[0]), monitor);
			return;
		}
		GetLocalFileContentOperation op = new GetLocalFileContentOperation(local.getResource(), Kind.BASE);
		ProgressMonitorUtility.doTaskExternal(op, monitor);
		if (op.getExecutionState() == IActionOperation.OK) {
			setContents(op.getContent(), monitor);
		}
	}

	@Override
	public boolean isContainer() {
		return false;
	}

}
