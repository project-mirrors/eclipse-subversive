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
 * Activity cancelled exception
 * 
 * @author Alexander Gurov
 */
public class ActivityCancelledException extends UnreportableException {
	private static final long serialVersionUID = 6390395981269341729L;
	
	public ActivityCancelledException() {
		super();
	}

	public ActivityCancelledException(String message) {
		super(message);
	}

	public ActivityCancelledException(Throwable cause) {
		super(cause);
	}

	public ActivityCancelledException(String message, Throwable cause) {
		super(message, cause);
	}

}
