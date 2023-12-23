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

package org.eclipse.team.svn.core.operation;

/**
 * The exception will be never shown to user
 * 
 * @author Alexander Gurov
 */
public class HiddenException extends UnreportableException {
	private static final long serialVersionUID = -7093439079259787375L;

	public HiddenException() {
		super();
	}

	public HiddenException(String message) {
		super(message);
	}

	public HiddenException(Throwable cause) {
		super(cause);
	}

	public HiddenException(String message, Throwable cause) {
		super(message, cause);
	}

}
