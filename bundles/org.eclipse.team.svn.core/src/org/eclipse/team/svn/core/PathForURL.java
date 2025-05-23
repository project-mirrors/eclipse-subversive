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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core;

import java.io.File;
import java.util.Arrays;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * The implementation of the <code>IPath</code> interface adapted for working with SVN URLs. It should be used instead of {@link Path} when
 * you need to manipulate with URLs.
 * 
 * {@link Path} implementation will not work for particular cases when it's used not on Windows, the reason is that not on Windows it
 * collapses slashes in such a way that URLs become invalid. E.g. after calling toString method for http://localhost/repos it will produce
 * http:/localhost/repos, note to one slash after http. Current implementation fixes this problem when you work with URL, see special
 * parameter in constructor which tells whether to treat provided path string as URL or not.
 * 
 * This class is copied from {@link Path}.
 * 
 * @author Igor Burilo
 */
public class PathForURL implements IPath, Cloneable {
	/** masks for separator values */
	private static final int HAS_LEADING = 1;

	private static final int IS_UNC = 2;

	private static final int HAS_TRAILING = 4;

	private static final int ALL_SEPARATORS = HAS_LEADING | IS_UNC | HAS_TRAILING;

	/** Constant empty string value. */
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	/** Constant value indicating no segments */
	private static final String[] NO_SEGMENTS = {};

	/** Constant value containing the empty path with no device. */
	public static final PathForURL EMPTY = new PathForURL(EMPTY_STRING, false);

	/** Mask for all bits that are involved in the hash code */
	private static final int HASH_MASK = ~HAS_TRAILING;

	/** Constant root path string (<code>"/"</code>). */
	private static final String ROOT_STRING = "/"; //$NON-NLS-1$

	/** Constant value containing the root path with no device. */
	public static final PathForURL ROOT = new PathForURL(ROOT_STRING, false);

	/** Constant value indicating if the current platform is Windows */
	private static final boolean WINDOWS = java.io.File.separatorChar == '\\';

	/** The device id string. May be null if there is no device. */
	private String device = null;

	//Private implementation note: the segments and separators
	//arrays are never modified, so that they can be shared between
	//path instances

	/** The path segments */
	private String[] segments;

	/** flags indicating separators (has leading, is UNC, has trailing) */
	private int separators;

	/**
	 * Constructs a new path from the given string path. The string path must represent a valid file system path on the local file system.
	 * The path is canonicalized and double slashes are removed except at the beginning. (to handle UNC paths). All forward slashes ('/')
	 * are treated as segment delimiters, and any segment and device delimiters for the local file system are also respected.
	 *
	 * @param pathString
	 *            the portable string path
	 * @see IPath#toPortableString()
	 * @since 3.1
	 */
	public static IPath fromOSString(String pathString) {
		return new PathForURL(pathString, false);
	}

	/**
	 * Constructs a new path from the given path string. The path string must have been produced by a previous call to
	 * <code>IPath.toPortableString</code>.
	 *
	 * @param pathString
	 *            the portable path string
	 * @see IPath#toPortableString()
	 * @since 3.1
	 */
	public static IPath fromPortableString(String pathString) {
		int firstMatch = pathString.indexOf(DEVICE_SEPARATOR) + 1;
		//no extra work required if no device characters
		if (firstMatch <= 0) {
			return new PathForURL().initialize(null, pathString);
		}
		//if we find a single colon, then the path has a device
		String devicePart = null;
		int pathLength = pathString.length();
		if (firstMatch == pathLength || pathString.charAt(firstMatch) != DEVICE_SEPARATOR) {
			devicePart = pathString.substring(0, firstMatch);
			pathString = pathString.substring(firstMatch, pathLength);
		}
		//optimize for no colon literals
		if (pathString.indexOf(DEVICE_SEPARATOR) == -1) {
			return new PathForURL().initialize(devicePart, pathString);
		}
		//contract colon literals
		char[] chars = pathString.toCharArray();
		int readOffset = 0, writeOffset = 0, length = chars.length;
		while (readOffset < length) {
			if (chars[readOffset] == DEVICE_SEPARATOR) {
				if (++readOffset >= length) {
					break;
				}
			}
			chars[writeOffset++] = chars[readOffset++];
		}
		return new PathForURL().initialize(devicePart, new String(chars, 0, writeOffset));
	}

	/* (Intentionally not included in javadoc)
	 * Private constructor.
	 */
	private PathForURL() {
		// not allowed
	}

	/**
	 * Constructs a new path from the given string path. The string path must represent a valid file system path on the local file system.
	 * The path is canonicalized and double slashes are removed except at the beginning. (to handle UNC paths). All forward slashes ('/')
	 * are treated as segment delimiters, and any segment and device delimiters for the local file system are also respected (such as colon
	 * (':') and backslash ('\') on some file systems).
	 *
	 * @param fullPath
	 *            the string path
	 * @param isSVNUrl
	 *            flag which tells whether to treat provided path as URL or not
	 * @see #isValidPath(String)
	 */
	public PathForURL(String fullPath, boolean isSVNUrl) {
		String devicePart = null;
		if (isSVNUrl) {
			//convert backslash to forward slash
			fullPath = fullPath.indexOf('\\') == -1 ? fullPath : fullPath.replace('\\', SEPARATOR);
			//extract device
			int i = fullPath.indexOf("://");
			if (i != -1) {
				//remove leading slash from device part to handle output of URL.getFile()
				devicePart = fullPath.substring(0, i + 3);
				fullPath = fullPath.substring(i + 3);
			}
		} else if (WINDOWS) {
			//convert backslash to forward slash
			fullPath = fullPath.indexOf('\\') == -1 ? fullPath : fullPath.replace('\\', SEPARATOR);
			//extract device
			int i = fullPath.indexOf(DEVICE_SEPARATOR);
			if (i != -1) {
				//remove leading slash from device part to handle output of URL.getFile()
				int start = fullPath.charAt(0) == SEPARATOR ? 1 : 0;
				devicePart = fullPath.substring(start, i + 1);
				fullPath = fullPath.substring(i + 1);
			}
		}
		initialize(devicePart, fullPath);
	}

	/**
	 * Constructs a new path from the given device id and string path. The given string path must be valid. The path is canonicalized and
	 * double slashes are removed except at the beginning (to handle UNC paths). All forward slashes ('/') are treated as segment
	 * delimiters, and any segment delimiters for the local file system are also respected (such as backslash ('\') on some file systems).
	 *
	 * @param device
	 *            the device id
	 * @param path
	 *            the string path
	 * @see #isValidPath(String)
	 * @see #setDevice(String)
	 */
	public PathForURL(String device, String path) {
		if (WINDOWS) {
			//convert backslash to forward slash
			path = path.indexOf('\\') == -1 ? path : path.replace('\\', SEPARATOR);
		}
		initialize(device, path);
	}

	/* (Intentionally not included in javadoc)
	 * Private constructor.
	 */
	private PathForURL(String device, String[] segments, int _separators) {
		// no segment validations are done for performance reasons
		this.segments = segments;
		this.device = device;
		//hash code is cached in all but the bottom three bits of the separators field
		separators = computeHashCode() << 3 | _separators & ALL_SEPARATORS;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#addFileExtension
	 */
	@Override
	public IPath addFileExtension(String extension) {
		if (isRoot() || isEmpty() || hasTrailingSeparator()) {
			return this;
		}
		int len = segments.length;
		String[] newSegments = new String[len];
		System.arraycopy(segments, 0, newSegments, 0, len - 1);
		newSegments[len - 1] = segments[len - 1] + '.' + extension;
		return new PathForURL(device, newSegments, separators);
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#addTrailingSeparator
	 */
	@Override
	public IPath addTrailingSeparator() {
		if (hasTrailingSeparator() || isRoot()) {
			return this;
		}
		//XXX workaround, see 1GIGQ9V
		if (isEmpty()) {
			return new PathForURL(device, segments, HAS_LEADING);
		}
		return new PathForURL(device, segments, separators | HAS_TRAILING);
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#append(IPath)
	 */
	@Override
	public IPath append(IPath tail) {
		//optimize some easy cases
		if (tail == null || tail.segmentCount() == 0) {
			return this;
		}
		//these call chains look expensive, but in most cases they are no-ops
		if (isEmpty()) {
			return tail.setDevice(device).makeRelative().makeUNC(isUNC());
		}
		if (isRoot()) {
			return tail.setDevice(device).makeAbsolute().makeUNC(isUNC());
		}

		//concatenate the two segment arrays
		int myLen = segments.length;
		int tailLen = tail.segmentCount();
		String[] newSegments = new String[myLen + tailLen];
		System.arraycopy(segments, 0, newSegments, 0, myLen);
		for (int i = 0; i < tailLen; i++) {
			newSegments[myLen + i] = tail.segment(i);
		}
		//use my leading separators and the tail's trailing separator
		PathForURL result = new PathForURL(device, newSegments,
				separators & (HAS_LEADING | IS_UNC) | (tail.hasTrailingSeparator() ? HAS_TRAILING : 0));
		String tailFirstSegment = newSegments[myLen];
		if (tailFirstSegment.equals("..") || tailFirstSegment.equals(".")) { //$NON-NLS-1$ //$NON-NLS-2$
			result.canonicalize();
		}
		return result;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#append(java.lang.String)
	 */
	@Override
	public IPath append(String tail) {
		//optimize addition of a single segment
		if (tail.indexOf(SEPARATOR) == -1 && tail.indexOf("\\") == -1 && tail.indexOf(DEVICE_SEPARATOR) == -1) { //$NON-NLS-1$
			int tailLength = tail.length();
			if (tailLength < 3) {
				//some special cases
				if (tailLength == 0 || ".".equals(tail)) { //$NON-NLS-1$
					return this;
				}
				if ("..".equals(tail)) { //$NON-NLS-1$
					return removeLastSegments(1);
				}
			}
			//just add the segment
			int myLen = segments.length;
			String[] newSegments = new String[myLen + 1];
			System.arraycopy(segments, 0, newSegments, 0, myLen);
			newSegments[myLen] = tail;
			return new PathForURL(device, newSegments, separators & ~HAS_TRAILING);
		}
		//go with easy implementation
		return append(new PathForURL(tail, false));
	}

	/**
	 * Destructively converts this path to its canonical form.
	 * <p>
	 * In its canonical form, a path does not have any "." segments, and parent references ("..") are collapsed where possible.
	 * </p>
	 * 
	 * @return true if the path was modified, and false otherwise.
	 */
	private boolean canonicalize() {
		//look for segments that need canonicalizing
		for (String segment : segments) {
			if (segment.charAt(0) == '.' && (segment.equals("..") || segment.equals("."))) { //$NON-NLS-1$ //$NON-NLS-2$
				//path needs to be canonicalized
				collapseParentReferences();
				//paths of length 0 have no trailing separator
				if (segments.length == 0) {
					separators &= HAS_LEADING | IS_UNC;
				}
				//recompute hash because canonicalize affects hash
				separators = separators & ALL_SEPARATORS | computeHashCode() << 3;
				return true;
			}
		}
		return false;
	}

	/* (Intentionally not included in javadoc)
	 * Clones this object.
	 */
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	/**
	 * Destructively removes all occurrences of ".." segments from this path.
	 */
	private void collapseParentReferences() {
		int segmentCount = segments.length;
		String[] stack = new String[segmentCount];
		int stackPointer = 0;
		for (int i = 0; i < segmentCount; i++) {
			String segment = segments[i];
			if (segment.equals("..")) { //$NON-NLS-1$
				if (stackPointer == 0) {
					// if the stack is empty we are going out of our scope
					// so we need to accumulate segments.  But only if the original
					// path is relative.  If it is absolute then we can't go any higher than
					// root so simply toss the .. references.
					if (!isAbsolute()) {
						stack[stackPointer++] = segment; //stack push
					}
				} else // if the top is '..' then we are accumulating segments so don't pop
				if ("..".equals(stack[stackPointer - 1])) { //$NON-NLS-1$
					stack[stackPointer++] = ".."; //$NON-NLS-1$
				} else { //$NON-NLS-1$
					stackPointer--;
					//stack pop
				}
			} else if (!segment.equals(".") || segmentCount == 1) { //$NON-NLS-1$
				stack[stackPointer++] = segment; //stack push
			}
		}
		//if the number of segments hasn't changed, then no modification needed
		if (stackPointer == segmentCount) {
			return;
		}
		//build the new segment array backwards by popping the stack
		String[] newSegments = new String[stackPointer];
		System.arraycopy(stack, 0, newSegments, 0, stackPointer);
		segments = newSegments;
	}

	/**
	 * Removes duplicate slashes from the given path, with the exception of leading double slash which represents a UNC path.
	 */
	private String collapseSlashes(String path) {
		int length = path.length();
		// if the path is only 0, 1 or 2 chars long then it could not possibly have illegal
		// duplicate slashes.
		// check for an occurrence of // in the path.  Start at index 1 to ensure we skip leading UNC //
		// If there are no // then there is nothing to collapse so just return.
		if ((length < 3) || (path.indexOf("//", 1) == -1)) { //$NON-NLS-1$
			return path;
		}
		// We found an occurrence of // in the path so do the slow collapse.
		char[] result = new char[path.length()];
		int count = 0;
		boolean hasPrevious = false;
		char[] characters = path.toCharArray();
		for (int index = 0; index < characters.length; index++) {
			char c = characters[index];
			if (c == SEPARATOR) {
				if (hasPrevious) {
					// skip double slashes, except for beginning of UNC.
					// note that a UNC path can't have a device.
					if (device == null && index == 1) {
						result[count] = c;
						count++;
					}
				} else {
					hasPrevious = true;
					result[count] = c;
					count++;
				}
			} else {
				hasPrevious = false;
				result[count] = c;
				count++;
			}
		}
		return new String(result, 0, count);
	}

	/* (Intentionally not included in javadoc)
	 * Computes the hash code for this object.
	 */
	private int computeHashCode() {
		int hash = device == null ? 17 : device.hashCode();
		int segmentCount = segments.length;
		for (int i = 0; i < segmentCount; i++) {
			//this function tends to given a fairly even distribution
			hash = hash * 37 + segments[i].hashCode();
		}
		return hash;
	}

	/* (Intentionally not included in javadoc)
	 * Returns the size of the string that will be created by toString or toOSString.
	 */
	private int computeLength() {
		int length = 0;
		if (device != null) {
			length += device.length();
		}
		if ((separators & HAS_LEADING) != 0) {
			length++;
		}
		if ((separators & IS_UNC) != 0) {
			length++;
		}
		//add the segment lengths
		int max = segments.length;
		if (max > 0) {
			for (int i = 0; i < max; i++) {
				length += segments[i].length();
			}
			//add the separator lengths
			length += max - 1;
		}
		if ((separators & HAS_TRAILING) != 0) {
			length++;
		}
		return length;
	}

	/* (Intentionally not included in javadoc)
	 * Returns the number of segments in the given path
	 */
	private int computeSegmentCount(String path) {
		int len = path.length();
		if (len == 0 || len == 1 && path.charAt(0) == SEPARATOR) {
			return 0;
		}
		int count = 1;
		int prev = -1;
		int i;
		while ((i = path.indexOf(SEPARATOR, prev + 1)) != -1) {
			if (i != prev + 1 && i != len) {
				++count;
			}
			prev = i;
		}
		if (path.charAt(len - 1) == SEPARATOR) {
			--count;
		}
		return count;
	}

	/**
	 * Computes the segment array for the given canonicalized path.
	 */
	private String[] computeSegments(String path) {
		// performance sensitive --- avoid creating garbage
		int segmentCount = computeSegmentCount(path);
		if (segmentCount == 0) {
			return NO_SEGMENTS;
		}
		String[] newSegments = new String[segmentCount];
		int len = path.length();
		// check for initial slash
		int firstPosition = path.charAt(0) == SEPARATOR ? 1 : 0;
		// check for UNC
		if (firstPosition == 1 && len > 1 && path.charAt(1) == SEPARATOR) {
			firstPosition = 2;
		}
		int lastPosition = path.charAt(len - 1) != SEPARATOR ? len - 1 : len - 2;
		// for non-empty paths, the number of segments is
		// the number of slashes plus 1, ignoring any leading
		// and trailing slashes
		int next = firstPosition;
		for (int i = 0; i < segmentCount; i++) {
			int start = next;
			int end = path.indexOf(SEPARATOR, next);
			if (end == -1) {
				newSegments[i] = path.substring(start, lastPosition + 1);
			} else {
				newSegments[i] = path.substring(start, end);
			}
			next = end + 1;
		}
		return newSegments;
	}

	/**
	 * Returns the platform-neutral encoding of the given segment onto the given string buffer. This escapes literal colon characters with
	 * double colons.
	 */
	private void encodeSegment(String string, StringBuffer buf) {
		int len = string.length();
		for (int i = 0; i < len; i++) {
			char c = string.charAt(i);
			buf.append(c);
			if (c == DEVICE_SEPARATOR) {
				buf.append(DEVICE_SEPARATOR);
			}
		}
	}

	/* (Intentionally not included in javadoc)
	 * Compares objects for equality.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PathForURL)) {
			return false;
		}
		PathForURL target = (PathForURL) obj;
		//check leading separators and hash code
		if ((separators & HASH_MASK) != (target.separators & HASH_MASK)) {
			return false;
		}
		String[] targetSegments = target.segments;
		int i = segments.length;
		//check segment count
		if (i != targetSegments.length) {
			return false;
		}
		//check segments in reverse order - later segments more likely to differ
		while (--i >= 0) {
			if (!segments[i].equals(targetSegments[i])) {
				return false;
			}
		}
		//check device last (least likely to differ)
		return device == target.device || device != null && device.equals(target.device);
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#getDevice
	 */
	@Override
	public String getDevice() {
		return device;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#getFileExtension
	 */
	@Override
	public String getFileExtension() {
		if (hasTrailingSeparator()) {
			return null;
		}
		String lastSegment = lastSegment();
		if (lastSegment == null) {
			return null;
		}
		int index = lastSegment.lastIndexOf('.');
		if (index == -1) {
			return null;
		}
		return lastSegment.substring(index + 1);
	}

	/* (Intentionally not included in javadoc)
	 * Computes the hash code for this object.
	 */
	@Override
	public int hashCode() {
		return separators & HASH_MASK;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#hasTrailingSeparator2
	 */
	@Override
	public boolean hasTrailingSeparator() {
		return (separators & HAS_TRAILING) != 0;
	}

	/*
	 * Initialize the current path with the given string.
	 */
	private IPath initialize(String deviceString, String path) {
		Assert.isNotNull(path);
		device = deviceString;

		path = collapseSlashes(path);
		int len = path.length();

		//compute the separators array
		if (len < 2) {
			if (len == 1 && path.charAt(0) == SEPARATOR) {
				separators = HAS_LEADING;
			} else {
				separators = 0;
			}
		} else {
			boolean hasLeading = path.charAt(0) == SEPARATOR;
			boolean isUNC = hasLeading && path.charAt(1) == SEPARATOR;
			//UNC path of length two has no trailing separator
			boolean hasTrailing = !(isUNC && len == 2) && path.charAt(len - 1) == SEPARATOR;
			separators = hasLeading ? HAS_LEADING : 0;
			if (isUNC) {
				separators |= IS_UNC;
			}
			if (hasTrailing) {
				separators |= HAS_TRAILING;
			}
		}
		//compute segments and ensure canonical form
		segments = computeSegments(path);
		if (!canonicalize()) {
			//compute hash now because canonicalize didn't need to do it
			separators = separators & ALL_SEPARATORS | computeHashCode() << 3;
		}
		return this;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#isAbsolute
	 */
	@Override
	public boolean isAbsolute() {
		//it's absolute if it has a leading separator
		return (separators & HAS_LEADING) != 0;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#isEmpty
	 */
	@Override
	public boolean isEmpty() {
		//true if no segments and no leading prefix
		return segments.length == 0 && (separators & ALL_SEPARATORS) != HAS_LEADING;

	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#isPrefixOf
	 */
	@Override
	public boolean isPrefixOf(IPath anotherPath) {
		if (device == null) {
			if (anotherPath.getDevice() != null) {
				return false;
			}
		} else if (!device.equalsIgnoreCase(anotherPath.getDevice())) {
			return false;
		}
		if (isEmpty() || isRoot() && anotherPath.isAbsolute()) {
			return true;
		}
		int len = segments.length;
		if (len > anotherPath.segmentCount()) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			if (!segments[i].equals(anotherPath.segment(i))) {
				return false;
			}
		}
		return true;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#isRoot
	 */
	@Override
	public boolean isRoot() {
		//must have no segments, a leading separator, and not be a UNC path.
		return this == ROOT || segments.length == 0 && (separators & ALL_SEPARATORS) == HAS_LEADING;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#isUNC
	 */
	@Override
	public boolean isUNC() {
		if (device != null) {
			return false;
		}
		return (separators & IS_UNC) != 0;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#isValidPath(String)
	 * 
	 * Note that SVN URL is not always valid, so this method
	 * should not be used for URLs
	 */
	@Override
	public boolean isValidPath(String path) {
		PathForURL test = new PathForURL(path, false);
		for (int i = 0, max = test.segmentCount(); i < max; i++) {
			if (!isValidSegment(test.segment(i))) {
				return false;
			}
		}
		return true;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#isValidSegment(String)
	 * 
	 * Note that SVN URL is not always valid, e.g. if url contains port number,
	 * so this method should not be used for URLs
	 */
	@Override
	public boolean isValidSegment(String segment) {
		int size = segment.length();
		if (size == 0) {
			return false;
		}
		for (int i = 0; i < size; i++) {
			char c = segment.charAt(i);
			if ((c == '/') || (WINDOWS && (c == '\\' || c == ':'))) {
				return false;
			}
		}
		return true;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#lastSegment()
	 */
	@Override
	public String lastSegment() {
		int len = segments.length;
		return len == 0 ? null : segments[len - 1];
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#makeAbsolute()
	 */
	@Override
	public IPath makeAbsolute() {
		if (isAbsolute()) {
			return this;
		}
		PathForURL result = new PathForURL(device, segments, separators | HAS_LEADING);
		//may need canonicalizing if it has leading ".." or "." segments
		if (result.segmentCount() > 0) {
			String first = result.segment(0);
			if (first.equals("..") || first.equals(".")) { //$NON-NLS-1$ //$NON-NLS-2$
				result.canonicalize();
			}
		}
		return result;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#makeRelative()
	 */
	@Override
	public IPath makeRelative() {
		if (!isAbsolute()) {
			return this;
		}
		return new PathForURL(device, segments, separators & HAS_TRAILING);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since org.eclipse.equinox.common 3.5
	 */
	@Override
	public IPath makeRelativeTo(IPath base) {
		//can't make relative if devices are not equal
		if (device != base.getDevice() && (device == null || !device.equalsIgnoreCase(base.getDevice()))) {
			return this;
		}
		int commonLength = matchingFirstSegments(base);
		final int differenceLength = base.segmentCount() - commonLength;
		final int newSegmentLength = differenceLength + segmentCount() - commonLength;
		if (newSegmentLength == 0) {
			return PathForURL.EMPTY;
		}
		String[] newSegments = new String[newSegmentLength];
		//add parent references for each segment different from the base
		Arrays.fill(newSegments, 0, differenceLength, ".."); //$NON-NLS-1$
		//append the segments of this path not in common with the base
		System.arraycopy(segments, commonLength, newSegments, differenceLength, newSegmentLength - differenceLength);
		return new PathForURL(null, newSegments, separators & HAS_TRAILING);
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#makeUNC(boolean)
	 */
	@Override
	public IPath makeUNC(boolean toUNC) {
		// if we are already in the right form then just return
		if (!(toUNC ^ isUNC())) {
			return this;
		}

		int newSeparators = separators;
		if (toUNC) {
			newSeparators |= HAS_LEADING | IS_UNC;
		} else {
			//mask out the UNC bit
			newSeparators &= HAS_LEADING | HAS_TRAILING;
		}
		return new PathForURL(toUNC ? null : device, segments, newSeparators);
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#matchingFirstSegments(IPath)
	 */
	@Override
	public int matchingFirstSegments(IPath anotherPath) {
		Assert.isNotNull(anotherPath);
		int anotherPathLen = anotherPath.segmentCount();
		int max = Math.min(segments.length, anotherPathLen);
		int count = 0;
		for (int i = 0; i < max; i++) {
			if (!segments[i].equals(anotherPath.segment(i))) {
				return count;
			}
			count++;
		}
		return count;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#removeFileExtension()
	 */
	@Override
	public IPath removeFileExtension() {
		String extension = getFileExtension();
		if (extension == null || extension.equals("")) { //$NON-NLS-1$
			return this;
		}
		String lastSegment = lastSegment();
		int index = lastSegment.lastIndexOf(extension) - 1;
		return removeLastSegments(1).append(lastSegment.substring(0, index));
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#removeFirstSegments(int)
	 */
	@Override
	public IPath removeFirstSegments(int count) {
		if (count == 0) {
			return this;
		}
		if (count >= segments.length) {
			return new PathForURL(device, NO_SEGMENTS, 0);
		}
		Assert.isLegal(count > 0);
		int newSize = segments.length - count;
		String[] newSegments = new String[newSize];
		System.arraycopy(segments, count, newSegments, 0, newSize);

		//result is always a relative path
		return new PathForURL(device, newSegments, separators & HAS_TRAILING);
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#removeLastSegments(int)
	 */
	@Override
	public IPath removeLastSegments(int count) {
		if (count == 0) {
			return this;
		}
		if (count >= segments.length) {
			//result will have no trailing separator
			return new PathForURL(device, NO_SEGMENTS, separators & (HAS_LEADING | IS_UNC));
		}
		Assert.isLegal(count > 0);
		int newSize = segments.length - count;
		String[] newSegments = new String[newSize];
		System.arraycopy(segments, 0, newSegments, 0, newSize);
		return new PathForURL(device, newSegments, separators);
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#removeTrailingSeparator()
	 */
	@Override
	public IPath removeTrailingSeparator() {
		if (!hasTrailingSeparator()) {
			return this;
		}
		return new PathForURL(device, segments, separators & (HAS_LEADING | IS_UNC));
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#segment(int)
	 */
	@Override
	public String segment(int index) {
		if (index >= segments.length) {
			return null;
		}
		return segments[index];
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#segmentCount()
	 */
	@Override
	public int segmentCount() {
		return segments.length;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#segments()
	 */
	@Override
	public String[] segments() {
		String[] segmentCopy = new String[segments.length];
		System.arraycopy(segments, 0, segmentCopy, 0, segments.length);
		return segmentCopy;
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#setDevice(String)
	 */
	@Override
	public IPath setDevice(String value) {
		if (value != null) {
			Assert.isTrue(value.indexOf(IPath.DEVICE_SEPARATOR) == value.length() - 1,
					"Last character should be the device separator"); //$NON-NLS-1$
		}
		//return the receiver if the device is the same
		if (value == device || value != null && value.equals(device)) {
			return this;
		}

		return new PathForURL(value, segments, separators);
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#toFile()
	 */
	@Override
	public File toFile() {
		return new File(toOSString());
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#toOSString()
	 */
	@Override
	public String toOSString() {
		//Note that this method is identical to toString except
		//it uses the OS file separator instead of the path separator
		int resultSize = computeLength();
		if (resultSize <= 0) {
			return EMPTY_STRING;
		}
		char FILE_SEPARATOR = File.separatorChar;
		char[] result = new char[resultSize];
		int offset = 0;
		if (device != null) {
			int size = device.length();
			device.getChars(0, size, result, offset);
			offset += size;
		}
		if ((separators & HAS_LEADING) != 0) {
			result[offset++] = FILE_SEPARATOR;
		}
		if ((separators & IS_UNC) != 0) {
			result[offset++] = FILE_SEPARATOR;
		}
		int len = segments.length - 1;
		if (len >= 0) {
			//append all but the last segment, with separators
			for (int i = 0; i < len; i++) {
				int size = segments[i].length();
				segments[i].getChars(0, size, result, offset);
				offset += size;
				result[offset++] = FILE_SEPARATOR;
			}
			//append the last segment
			int size = segments[len].length();
			segments[len].getChars(0, size, result, offset);
			offset += size;
		}
		if ((separators & HAS_TRAILING) != 0) {
			result[offset++] = FILE_SEPARATOR;
		}
		return new String(result);
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#toPortableString()
	 */
	@Override
	public String toPortableString() {
		int resultSize = computeLength();
		if (resultSize <= 0) {
			return EMPTY_STRING;
		}
		StringBuffer result = new StringBuffer(resultSize);
		if (device != null) {
			result.append(device);
		}
		if ((separators & HAS_LEADING) != 0) {
			result.append(SEPARATOR);
		}
		if ((separators & IS_UNC) != 0) {
			result.append(SEPARATOR);
		}
		int len = segments.length;
		//append all segments with separators
		for (int i = 0; i < len; i++) {
			if (segments[i].indexOf(DEVICE_SEPARATOR) >= 0) {
				encodeSegment(segments[i], result);
			} else {
				result.append(segments[i]);
			}
			if (i < len - 1 || (separators & HAS_TRAILING) != 0) {
				result.append(SEPARATOR);
			}
		}
		return result.toString();
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#toString()
	 */
	@Override
	public String toString() {
		int resultSize = computeLength();
		if (resultSize <= 0) {
			return EMPTY_STRING;
		}
		char[] result = new char[resultSize];
		int offset = 0;
		if (device != null) {
			int size = device.length();
			device.getChars(0, size, result, offset);
			offset += size;
		}
		if ((separators & HAS_LEADING) != 0) {
			result[offset++] = SEPARATOR;
		}
		if ((separators & IS_UNC) != 0) {
			result[offset++] = SEPARATOR;
		}
		int len = segments.length - 1;
		if (len >= 0) {
			//append all but the last segment, with separators
			for (int i = 0; i < len; i++) {
				int size = segments[i].length();
				segments[i].getChars(0, size, result, offset);
				offset += size;
				result[offset++] = SEPARATOR;
			}
			//append the last segment
			int size = segments[len].length();
			segments[len].getChars(0, size, result, offset);
			offset += size;
		}
		if ((separators & HAS_TRAILING) != 0) {
			result[offset++] = SEPARATOR;
		}
		return new String(result);
	}

	/* (Intentionally not included in javadoc)
	 * @see IPath#uptoSegment(int)
	 */
	@Override
	public IPath uptoSegment(int count) {
		if (count == 0) {
			return new PathForURL(device, NO_SEGMENTS, separators & (HAS_LEADING | IS_UNC));
		}
		if (count >= segments.length) {
			return this;
		}
		Assert.isTrue(count > 0, "Invalid parameter to Path.uptoSegment"); //$NON-NLS-1$
		String[] newSegments = new String[count];
		System.arraycopy(segments, 0, newSegments, 0, count);
		return new PathForURL(device, newSegments, separators);
	}
}
