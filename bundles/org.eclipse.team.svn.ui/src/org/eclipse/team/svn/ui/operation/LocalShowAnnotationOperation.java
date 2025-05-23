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

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.annotate.BuiltInAnnotate;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IWorkbenchPage;

/**
 * The operation shows annotation for the local resource.
 * 
 * @author Alexander Gurov
 * @deprecated use BuiltInAnnotate.open() instead
 */
@Deprecated
public class LocalShowAnnotationOperation extends AbstractWorkingCopyOperation {

	public LocalShowAnnotationOperation(IResource resource) {
		super("Operation_ShowAnnotation", SVNUIMessages.class, new IResource[] { resource }); //$NON-NLS-1$
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IWorkbenchPage page = UIMonitorUtility.getActivePage();
		if (page != null) {
			new BuiltInAnnotate().open(UIMonitorUtility.getActivePage(), (IFile) operableData()[0],
					UIMonitorUtility.getShell());
		}
	}

}
