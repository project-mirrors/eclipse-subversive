/*******************************************************************************
 * Copyright (c) 2009, 2023 Tasktop Technologies, Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tasktop Technologies - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.discovery.util;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A default implementation of an error handler that throws exceptions on all errors.
 * 
 * @author David Green
 * @author Igor Burilo
 */
public class DefaultSaxErrorHandler implements ErrorHandler {
	@Override
	public void warning(SAXParseException exception) throws SAXException {
		// ignore
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		throw exception;
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		throw exception;
	}
}