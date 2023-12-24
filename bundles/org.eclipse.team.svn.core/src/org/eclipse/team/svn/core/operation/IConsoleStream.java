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
 * Operation output console stream
 * 
 * @author Alexander Gurov
 */
public interface IConsoleStream {
	int LEVEL_CMD = 0;

	int LEVEL_OK = 1;

	int LEVEL_WARNING = 2;

	int LEVEL_ERROR = 3;

	void markStart(String data);

	void write(int severity, String data);

	void markEnd();

	void markCancelled();

	void doComplexWrite(Runnable runnable);
}
