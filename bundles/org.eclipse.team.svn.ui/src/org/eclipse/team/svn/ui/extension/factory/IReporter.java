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
	IReportingDescriptor getReportingDescriptor();

	boolean isCustomEditorSupported();

	void setSummary(String summary);

	void setUserComment(String userComment);

	void setUserName(String userName);

	void setUserEMail(String userEMail);

	void setProblemStatus(IStatus problemStatus);

	String buildReport();

	String buildSubject();
}
