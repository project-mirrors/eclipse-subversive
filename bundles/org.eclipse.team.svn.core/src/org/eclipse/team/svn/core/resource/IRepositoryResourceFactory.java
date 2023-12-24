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

package org.eclipse.team.svn.core.resource;

/**
 * Repository resource factory interface
 * 
 * @author Alexander Gurov
 */
public interface IRepositoryResourceFactory {
	IRepositoryContainer asRepositoryContainer(String url, boolean allowsNull) throws IllegalArgumentException;

	IRepositoryFile asRepositoryFile(String url, boolean allowsNull) throws IllegalArgumentException;

}
