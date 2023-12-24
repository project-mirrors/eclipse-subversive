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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.extension.factory;

/**
 * Mail settings provider
 * 
 * @author Sergiy Logvin
 */
public interface IReportingDescriptor {
	/**
	 * Returns report addressee
	 * 
	 * @return report addressee
	 */
	String getEmailTo();

	/**
	 * Returns report sender
	 * 
	 * @return report sender
	 */
	String getEmailFrom();

	/**
	 * Returns mail server host
	 * 
	 * @return mail server host
	 */
	String getHost();

	/**
	 * Returns mail server port
	 * 
	 * @return mail server port
	 */
	String getPort();

	/**
	 * Returns plug-in name
	 * 
	 * @return plug-in name
	 */
	String getProductName();

	/**
	 * Returns plug-in version
	 * 
	 * @return plug-in version
	 */
	String getProductVersion();

	/**
	 * Returns the product tracker URL.
	 * 
	 * @return the product tracker URL
	 */
	String getTrackerUrl();

	/**
	 * Returns <code>true</code> if a bug tracker supports HTML in reports
	 * 
	 * @return <code>true</code> if tracker supports HTML in reports
	 */
	boolean isTrackerSupportsHTML();
}
