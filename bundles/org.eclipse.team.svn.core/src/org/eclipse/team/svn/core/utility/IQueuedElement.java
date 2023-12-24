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
 *    Andrey Loskutov <loskutov@gmx.de> - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.utility;

/**
 * An element suitable for queuing into {@link AsynchronousActiveQueue}.
 * 
 * @param <Data>
 */
public interface IQueuedElement<Data extends IQueuedElement<Data>> {

	/**
	 * Helps {@link AsynchronousActiveQueue} to decide if the element can be skipped instead of being added to the queue.
	 * 
	 * @return {@code true} if this element supports skipping
	 */
	boolean canSkip();

	/**
	 * Helps {@link AsynchronousActiveQueue} to decide if two elements can be merged together
	 * 
	 * @param d
	 *            non null
	 * @return {@code true} if this element can be merged with given element
	 */
	boolean canMerge(Data d);

	/**
	 * Merges two elements together
	 * 
	 * @param d
	 *            non null
	 * @return a new instance containing data from this and given element
	 */
	Data merge(Data d);

}