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

package org.eclipse.team.svn.core.extension.crashrecovery;

/**
 * Provides ability to solve the problem which is unrecoverable in the core module context
 * 
 * @author Alexander Gurov
 */
public interface IResolutionHelper {
	/**
	 * The method provides resolution for errors about it is known
	 * 
	 * @param description
	 *            structure which describes the error happened
	 */
	boolean acquireResolution(ErrorDescription description);
}
