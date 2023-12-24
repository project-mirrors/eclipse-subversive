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
 *    Igor Burilo - Bug 245509: Improve extract log
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;

/**
 * Performs finalization of extract operations log file.
 * 
 * @author Alexander Gurov
 */
public class FiniExtractLogOperation extends AbstractActionOperation {
	protected InitExtractLogOperation logger;

	public FiniExtractLogOperation(InitExtractLogOperation logger) {
		super("Operation_FiniExtractLog", SVNMessages.class); //$NON-NLS-1$
		this.logger = logger;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		logger.flushLog();
	}

}
