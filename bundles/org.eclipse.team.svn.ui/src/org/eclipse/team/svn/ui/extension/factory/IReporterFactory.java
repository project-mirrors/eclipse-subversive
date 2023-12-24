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

/**
 * The reporter implements concrete issue reporting mechanism: mail, web services or something else.
 * 
 * @author Alexander Gurov
 */
public interface IReporterFactory {
	/**
	 * Enumeration of available report types
	 */
	public enum ReportType {
		BUG, TIP
	}

	/**
	 * Returns <code>true</code> if report could be edited in the custom feature-reach editor
	 * 
	 * @return <code>true</code> if the custom editor is supported by reporter
	 */
	boolean isCustomEditorSupported();

	/**
	 * Create new issue reporter instance based on {@link IReportingDescriptor} descriptor
	 * 
	 * @param settings
	 *            tracker settings descriptor
	 * @return new issue reporter instance or <code>null</code>
	 */
	IReporter newReporter(IReportingDescriptor settings, ReportType type);
}
