/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies, Polarion Software and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.discovery.core.util;

import java.io.IOException;

/**
 * An IO Exception that allows for {@link #getCause() a cause}.
 * 
 * @author David Green
 * @author Igor Burilo
 */
public class IOWithCauseException extends IOException {

	private static final long serialVersionUID = 1L;

	private final Throwable cause;

	public IOWithCauseException(String message, Throwable cause) {
		super(message);
		this.cause = cause;
	}

	public IOWithCauseException(Throwable cause) {
		this.cause = cause;
	}

	@Override
	public Throwable getCause() {
		return cause;
	}
}
