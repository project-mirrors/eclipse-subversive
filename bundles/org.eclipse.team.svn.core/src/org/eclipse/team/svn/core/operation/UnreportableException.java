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

package org.eclipse.team.svn.core.operation;

/**
 * The exception should be ignored by mail reporter
 * 
 * @author Alexander Gurov
 */
public class UnreportableException extends RuntimeException {
	private static final long serialVersionUID = 1428755738425428674L;

	public UnreportableException() {
	}

	public UnreportableException(String message) {
		super(message);
	}

	public UnreportableException(Throwable cause) {
		super(cause);
	}

	public UnreportableException(String message, Throwable cause) {
		super(message, cause);
	}

}
