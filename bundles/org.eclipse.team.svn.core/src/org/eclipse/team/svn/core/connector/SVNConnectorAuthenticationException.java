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

package org.eclipse.team.svn.core.connector;

/**
 * SVN connector wrapper authentication exception
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public class SVNConnectorAuthenticationException extends SVNConnectorException {
	private static final long serialVersionUID = 8879809662661620066L;

	public SVNConnectorAuthenticationException(String message) {
		super(message, SVNErrorCodes.raNotAuthorized, null);
	}

	public SVNConnectorAuthenticationException(Throwable cause) {
		super(cause == null ? null : cause.getMessage(), SVNErrorCodes.raNotAuthorized, cause);
	}

	public SVNConnectorAuthenticationException(String message, Throwable cause) {
		super(message, SVNErrorCodes.raNotAuthorized, cause);
	}

}
