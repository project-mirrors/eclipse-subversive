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

package org.eclipse.team.svn.core.connector;

/**
 * The revision range container
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public class SVNRevisionRange {
	/**
	 * The "from" revision object
	 */
	public final SVNRevision from;

	/**
	 * The "to" revision object
	 */
	public final SVNRevision to;

	/**
	 * @since 1.9
	 */
	public final boolean inheritable;

	/**
	 * The {@link SVNRevisionRange} instance could be initialized only once because all fields are final
	 * 
	 * @param from
	 *            the "from" revision object. Greater or equals to zero.
	 * @param to
	 *            the "to" revision object. Greater or equals to zero.
	 * @throws IllegalArgumentException
	 *             if from or to contains negative value
	 */
	public SVNRevisionRange(long from, long to) {
		this(from, to, true);
	}

	/**
	 * The {@link SVNRevisionRange} instance could be initialized only once because all fields are final
	 * 
	 * @param from
	 *            the "from" revision object. Greater or equals to zero.
	 * @param to
	 *            the "to" revision object. Greater or equals to zero.
	 * @param inheritable
	 * @throws IllegalArgumentException
	 *             if from or to contains negative value
	 * @since 1.9
	 */
	public SVNRevisionRange(long from, long to, boolean inheritable) {
		this.from = SVNRevision.fromNumber(from);
		this.to = SVNRevision.fromNumber(to);
		this.inheritable = inheritable;
	}

	/**
	 * The {@link SVNRevisionRange} instance could be initialized only once because all fields are final
	 * 
	 * @param from
	 *            the "from" revision object. Cannot be <code>null</code>.
	 * @param to
	 *            the "to" revision object Cannot be <code>null</code>.
	 * @throws NullPointerException
	 *             if one of arguments (or both) is null
	 */
	public SVNRevisionRange(SVNRevision from, SVNRevision to) {
		this(from, to, true);
	}

	/**
	 * The {@link SVNRevisionRange} instance could be initialized only once because all fields are final
	 * 
	 * @param from
	 *            the "from" revision object. Cannot be <code>null</code>.
	 * @param to
	 *            the "to" revision object Cannot be <code>null</code>.
	 * @param inheritable
	 * @throws NullPointerException
	 *             if one of arguments (or both) is null
	 * @since 1.9
	 */
	public SVNRevisionRange(SVNRevision from, SVNRevision to, boolean inheritable) {
		if (from == null) {
			throw new NullPointerException("The \"from\" field cannot be initialized with null");
		}
		if (to == null) {
			throw new NullPointerException("The \"to\" field cannot be initialized with null");
		}
		this.from = from;
		this.to = to;
		this.inheritable = inheritable;
	}

	/**
	 * The {@link SVNRevisionRange} instance could be initialized only once because all fields are final
	 * 
	 * Accepts a string in one of these forms:
	 * 
	 * {revision} the "from" and "to" fields will be initialized with the same value
	 * 
	 * {revision}-{revision} the first revision will be set into "from" object and the second into the "to" object
	 * 
	 * @param revisionElement
	 *            revision range or single revision
	 * @throws NumberFormatException
	 *             if the string does not contain a parsable <code>long</code>.
	 */
	public SVNRevisionRange(String revisionElement) {
		inheritable = !revisionElement.endsWith("*");
		if (!inheritable) {
			revisionElement = revisionElement.substring(0, revisionElement.length() - 1);
		}

		int hyphen = revisionElement.indexOf('-');
		if (hyphen > 0) {
			from = SVNRevision.fromNumber(Long.parseLong(revisionElement.substring(0, hyphen)));
			to = SVNRevision.fromNumber(Long.parseLong(revisionElement.substring(hyphen + 1)));
		} else {
			long rev = Long.parseLong(revisionElement.trim());
			to = SVNRevision.fromNumber(rev);
			from = SVNRevision.fromNumber(rev - 1);
		}
	}

	@Override
	public String toString() {
		if (from.equals(to) || from.getKind() == SVNRevision.Kind.NUMBER && from.getKind() == to.getKind()
				&& ((SVNRevision.Number) from).getNumber() == ((SVNRevision.Number) to).getNumber() - 1) {
			return from.toString() + (inheritable ? "" : "*");
		}
		return from.toString() + '-' + to.toString() + (inheritable ? "" : "*");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = inheritable ? 1 : 2;
		result = prime * result + from.hashCode();
		result = prime * result + to.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object range) {
		if (this == range) {
			return true;
		}
		if (!(range instanceof SVNRevisionRange)) {
			return false;
		}

		SVNRevisionRange other = (SVNRevisionRange) range;
		return from.equals(other.from) && to.equals(other.to) && inheritable == other.inheritable;
	}

}
