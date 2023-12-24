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
 * Abstract error description. Contains error class code and context information. Context will be different in most cases and it depends on
 * error class.
 * 
 * @author Alexander Gurov
 */
public class ErrorDescription {
	private static int codeCounter = 0;

	public static final int SUCCESS = ErrorDescription.codeCounter++;

	public static final int CANNOT_READ_LOCATION_DATA = ErrorDescription.codeCounter++;

	public static final int REPOSITORY_LOCATION_IS_DISCARDED = ErrorDescription.codeCounter++;

	public static final int CANNOT_READ_PROJECT_METAINFORMATION = ErrorDescription.codeCounter++;

	public static final int PROJECT_IS_RELOCATED_OUTSIDE_PLUGIN = ErrorDescription.codeCounter++;

	public static final int WORKING_COPY_REQUIRES_UPGRADE = ErrorDescription.codeCounter++;

	public static final int WORKING_COPY_REQUIRES_CLEANUP = ErrorDescription.codeCounter++;

	public final int code;

	public final Object context;

	public ErrorDescription(int code, Object context) {
		this.code = code;
		this.context = context;
	}
}
