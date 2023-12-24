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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Revision information container
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public class SVNRevision {
	/**
	 * The revision definition ways
	 */
	public enum Kind {
		/**
		 * First existing revision
		 */
		START(0, "START"), //$NON-NLS-1$ /* "UNSPECIFIED" text in SVN 1.9 API instead of "START"*/
		/**
		 * Number-based revision
		 */
		NUMBER(1, ""), //$NON-NLS-1$
		/**
		 * Date-based revision
		 */
		DATE(2, ""), //$NON-NLS-1$
		/**
		 * Last committed revision
		 */
		COMMITTED(3, "COMMITTED"), //$NON-NLS-1$
		/**
		 * The revision before last committed
		 */
		PREVIOUS(4, "PREV"), //$NON-NLS-1$
		/**
		 * The working copy base revision
		 */
		BASE(5, "BASE"), //$NON-NLS-1$
		/**
		 * The working copy working revision
		 */
		WORKING(6, "WORKING"), //$NON-NLS-1$
		/**
		 * The latest repository revision
		 */
		HEAD(7, "HEAD"); //$NON-NLS-1$

		public final int id;

		private final String name;

		@Override
		public String toString() {
			// and there is no need for named representations of numeric/date kinds. So, they're empty.
			return name;
		}

		public static Kind fromId(int id) {
			for (Kind kind : values()) {
				if (kind.id == id) {
					return kind;
				}
			}
			throw new IllegalArgumentException("Invalid revision kind: " + id); //$NON-NLS-1$
		}

		Kind(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	/**
	 * Last committed revision
	 */
	public static final SVNRevision HEAD = new SVNRevision(Kind.HEAD);

	/**
	 * First existing revision
	 */
	public static final SVNRevision START = new SVNRevision(Kind.START);
	// + UNSPECIFIED = START in SVN 1.9

	/**
	 * Last committed revision, needs working copy
	 */
	public static final SVNRevision COMMITTED = new SVNRevision(Kind.COMMITTED);

	/**
	 * Previous committed revision, needs working copy
	 */
	public static final SVNRevision PREVIOUS = new SVNRevision(Kind.PREVIOUS);

	/**
	 * Base revision of working copy
	 */
	public static final SVNRevision BASE = new SVNRevision(Kind.BASE);

	/**
	 * Working version in working copy
	 */
	public static final SVNRevision WORKING = new SVNRevision(Kind.WORKING);

	/**
	 * Invalid revision number
	 */
	public static final int INVALID_REVISION_NUMBER = -1;

	/**
	 * Invalid revision object
	 */
	public static final SVNRevision.Number INVALID_REVISION = new SVNRevision.Number(INVALID_REVISION_NUMBER);

	/**
	 * Number-based revision
	 */
	public static class Number extends SVNRevision {
		protected long revNumber;

		/**
		 * Returns the revision number
		 * 
		 * @return number
		 */
		public long getNumber() {
			return revNumber;
		}

		@Override
		public String toString() {
			return String.valueOf(revNumber);
		}

		@Override
		public int hashCode() {
			int result = 31;
			result += revKind.id;
			result = 31 * result + (int) revNumber;
			result = 31 * result + (int) (revNumber >> 32);
			return result;
		}

		@Override
		public boolean equals(Object target) {
			return super.equals(target) && ((SVNRevision.Number) target).revNumber == revNumber;
		}

		protected Number(long number) {
			super(Kind.NUMBER);
			revNumber = number;
		}

	}

	/**
	 * Date-based revision
	 */
	public static class Date extends SVNRevision {
		protected long revDate;

		/**
		 * Returns the date of the revision
		 * 
		 * @return the date
		 */
		public long getDate() {
			return revDate;
		}

		@Override
		public String toString() {
			DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT,
					Locale.getDefault());
			return dateTimeFormat.format(new java.util.Date(revDate));
		}

		@Override
		public int hashCode() {
			int result = 31;
			result += revKind.id;
			result = 31 * result + (int) revDate;
			result = 31 * result + (int) (revDate >> 32);
			return result;
		}

		@Override
		public boolean equals(Object target) {
			return super.equals(target) && ((SVNRevision.Date) target).revDate == revDate;
		}

		protected Date(long date) {
			super(Kind.DATE);
			revDate = date;
		}

	}

	protected Kind revKind;

	/**
	 * Creates revision object by revision kind
	 * 
	 * @param kind
	 *            revision kind
	 * @return revision object
	 * @throws IllegalArgumentException
	 *             if kind is {@link Kind#DATE}, {@link Kind#NUMBER} or exceeds kind limits
	 */
	public static SVNRevision fromKind(Kind kind) {
		switch (kind) {
			case BASE: {
				return SVNRevision.BASE;
			}
			case WORKING: {
				return SVNRevision.WORKING;
			}
			case HEAD: {
				return SVNRevision.HEAD;
			}
			case PREVIOUS: {
				return SVNRevision.PREVIOUS;
			}
			case START: {
				return SVNRevision.START;
			}
			case COMMITTED: {
				return SVNRevision.COMMITTED;
			}
			case DATE: {
				throw new IllegalArgumentException("Use fromDate() method instead"); //$NON-NLS-1$
			}
			case NUMBER: {
				throw new IllegalArgumentException("Use fromNumber() method instead"); //$NON-NLS-1$
			}
		}
		throw new IllegalArgumentException("Invalid revision kind: " + kind); //$NON-NLS-1$
	}

	/**
	 * Creates revision object by revision number
	 * 
	 * @param revisionNumber
	 *            revision number
	 * @return revision object
	 * @throws IllegalArgumentException
	 *             if revisionNumber is negative
	 */
	public static SVNRevision.Number fromNumber(long revisionNumber) {
		if (revisionNumber < 0) {
			throw new IllegalArgumentException("Negative revision numbers are not allowed: " + revisionNumber); //$NON-NLS-1$
		}
		return new SVNRevision.Number(revisionNumber);
	}

	/**
	 * Creates revision object by revision date
	 * 
	 * @param revisionDate
	 *            revision date in milliseconds
	 * @return revision object
	 * @throws IllegalArgumentException
	 *             if revisionDate is zero
	 */
	public static SVNRevision.Date fromDate(long revisionDate) {
		if (revisionDate == -1) {
			throw new IllegalArgumentException("A date must be specified"); //$NON-NLS-1$
		}
		return new SVNRevision.Date(revisionDate);
	}

	/**
	 * Creates revision object by revision string
	 * 
	 * @param revisionString
	 *            string representation of one of valid revisions or revision kinds
	 * @return revision object
	 * @throws IllegalArgumentException
	 *             if invalid revision kind or revision value specified
	 */
	public static SVNRevision fromString(String revisionString) {
		revisionString = revisionString.toUpperCase();
		if (START.toString().equals(revisionString)) {
			return SVNRevision.START;
		}
		if (BASE.toString().equals(revisionString)) {
			return SVNRevision.BASE;
		}
		if (WORKING.toString().equals(revisionString)) {
			return SVNRevision.WORKING;
		}
		if (COMMITTED.toString().equals(revisionString)) {
			return SVNRevision.COMMITTED;
		}
		if (HEAD.toString().equals(revisionString)) {
			return SVNRevision.HEAD;
		}
		if (PREVIOUS.toString().equals(revisionString)) {
			return SVNRevision.PREVIOUS;
		}
		try {
			return SVNRevision.fromNumber(Long.parseLong(revisionString));
		} catch (NumberFormatException ex) {
			// check if revision specified as date (always locale-specific)
			DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT,
					Locale.getDefault());
			try {
				return SVNRevision.fromDate(dateTimeFormat.parse(revisionString).getTime());
			} catch (ParseException e) {
				// do nothing
			}
		}
		throw new IllegalArgumentException("Invalid revision string: " + revisionString); //$NON-NLS-1$
	}

	/**
	 * Returns the kind of the revision object
	 * 
	 * @return the revision kind
	 */
	public Kind getKind() {
		return revKind;
	}

	@Override
	public String toString() {
		return revKind.toString();
	}

	@Override
	public int hashCode() {
		return revKind.id;
	}

	@Override
	public boolean equals(Object target) {
		if (this == target) {
			return true;
		}
		if (!this.getClass().equals(target.getClass())) {
			return false;
		}

		return ((SVNRevision) target).revKind == revKind;
	}

	protected SVNRevision(Kind kind) {
		revKind = kind;
	}

}
