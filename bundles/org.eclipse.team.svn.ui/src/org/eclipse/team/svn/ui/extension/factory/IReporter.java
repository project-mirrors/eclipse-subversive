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

package org.eclipse.team.svn.ui.extension.factory;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.svn.core.operation.IActionOperation;

/**
 * Generic issue reporter interface
 * 
 * @author Alexander Gurov
 */
public interface IReporter extends IActionOperation {
	public IReportingDescriptor getReportingDescriptor();
	
	public boolean isCustomEditorSupported();
	
	public void setSummary(String summary);

	public void setUserComment(String userComment);

	public void setUserName(String userName);

	public void setUserEMail(String userEMail);

	public void setProblemStatus(IStatus problemStatus);
	
	public String buildReport();
	
	public String buildSubject();
}
