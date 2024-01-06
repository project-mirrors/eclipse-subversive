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

package org.eclipse.team.svn.tests.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.team.svn.core.IStateFilter;
import org.junit.Test;

/**
 * IStateFilter behaviour test
 * 
 * @author Alexander Gurov
 */
public class StateFilterTest {

	private final IResource resource = ResourcesPlugin.getWorkspace().getRoot();

	@Test
	public void testSF_ADDED() {
		if (!IStateFilter.SF_ADDED.accept(resource, IStateFilter.ST_ADDED, 0)
				|| IStateFilter.SF_ADDED.accept(resource, IStateFilter.ST_DELETED, 0)
				|| IStateFilter.SF_ADDED.accept(resource, IStateFilter.ST_MISSING, 0)
				|| IStateFilter.SF_ADDED.accept(resource, IStateFilter.ST_MODIFIED, 0)
				|| !IStateFilter.SF_ADDED.accept(resource, IStateFilter.ST_NEW, 0)
				|| IStateFilter.SF_ADDED.accept(resource, IStateFilter.ST_IGNORED, 0)
				|| IStateFilter.SF_ADDED.accept(resource, IStateFilter.ST_NORMAL, 0)
				|| IStateFilter.SF_ADDED.accept(resource, IStateFilter.ST_NOTEXISTS, 0)) {
			throw new RuntimeException(IStateFilter.SF_ADDED.getClass().getName() + " failed");
		}
	}

	@Test
	public void testSF_ALL() {
		if (!IStateFilter.SF_ALL.accept(resource, IStateFilter.ST_ADDED, 0)
				|| !IStateFilter.SF_ALL.accept(resource, IStateFilter.ST_DELETED, 0)
				|| !IStateFilter.SF_ALL.accept(resource, IStateFilter.ST_MISSING, 0)
				|| !IStateFilter.SF_ALL.accept(resource, IStateFilter.ST_MODIFIED, 0)
				|| !IStateFilter.SF_ALL.accept(resource, IStateFilter.ST_NEW, 0)
				|| !IStateFilter.SF_ALL.accept(resource, IStateFilter.ST_IGNORED, 0)
				|| !IStateFilter.SF_ALL.accept(resource, IStateFilter.ST_NORMAL, 0)
				|| !IStateFilter.SF_ALL.accept(resource, IStateFilter.ST_NOTEXISTS, 0)) {
			throw new RuntimeException(IStateFilter.SF_ALL.getClass().getName() + " failed");
		}
	}

	@Test
	public void testSF_ANY_CHANGE() {
		assertTrue(IStateFilter.SF_ANY_CHANGE.accept(resource, IStateFilter.ST_ADDED, 0));
		assertTrue(IStateFilter.SF_ANY_CHANGE.accept(resource, IStateFilter.ST_DELETED, 0));
		assertTrue(IStateFilter.SF_ANY_CHANGE.accept(resource, IStateFilter.ST_MISSING, 0));
		assertTrue(IStateFilter.SF_ANY_CHANGE.accept(resource, IStateFilter.ST_MODIFIED, 0));
		assertFalse(IStateFilter.SF_ANY_CHANGE.accept(resource, IStateFilter.ST_NEW, 0));
		assertFalse(IStateFilter.SF_ANY_CHANGE.accept(resource, IStateFilter.ST_IGNORED, 0));
		assertFalse(IStateFilter.SF_ANY_CHANGE.accept(resource, IStateFilter.ST_NORMAL, 0));
		assertFalse(IStateFilter.SF_ANY_CHANGE.accept(resource, IStateFilter.ST_NOTEXISTS, 0));
	}

	@Test
	public void testSF_COMMITABLE() {
		if (!IStateFilter.SF_COMMITABLE.accept(resource, IStateFilter.ST_ADDED, 0)
				|| !IStateFilter.SF_COMMITABLE.accept(resource, IStateFilter.ST_DELETED, 0)
				|| !IStateFilter.SF_COMMITABLE.accept(resource, IStateFilter.ST_MISSING, 0)
				|| !IStateFilter.SF_COMMITABLE.accept(resource, IStateFilter.ST_MODIFIED, 0)
				|| IStateFilter.SF_COMMITABLE.accept(resource, IStateFilter.ST_NEW, 0)
				|| IStateFilter.SF_COMMITABLE.accept(resource, IStateFilter.ST_IGNORED, 0)
				|| IStateFilter.SF_COMMITABLE.accept(resource, IStateFilter.ST_NORMAL, 0)
				|| IStateFilter.SF_COMMITABLE.accept(resource, IStateFilter.ST_NOTEXISTS, 0)) {
			throw new RuntimeException(IStateFilter.SF_COMMITABLE.getClass().getName() + " failed");
		}
	}

	@Test
	public void testSF_DELETED() {
		if (IStateFilter.SF_DELETED.accept(resource, IStateFilter.ST_ADDED, 0)
				|| !IStateFilter.SF_DELETED.accept(resource, IStateFilter.ST_DELETED, 0)
				|| !IStateFilter.SF_DELETED.accept(resource, IStateFilter.ST_MISSING, 0)
				|| IStateFilter.SF_DELETED.accept(resource, IStateFilter.ST_MODIFIED, 0)
				|| IStateFilter.SF_DELETED.accept(resource, IStateFilter.ST_NEW, 0)
				|| IStateFilter.SF_DELETED.accept(resource, IStateFilter.ST_IGNORED, 0)
				|| IStateFilter.SF_DELETED.accept(resource, IStateFilter.ST_NORMAL, 0)
				|| IStateFilter.SF_DELETED.accept(resource, IStateFilter.ST_NOTEXISTS, 0)) {
			throw new RuntimeException(IStateFilter.SF_DELETED.getClass().getName() + " failed");
		}
	}

	@Test
	public void testSF_IGNORED() {
		assertFalse(IStateFilter.SF_IGNORED.accept(resource, IStateFilter.ST_ADDED, 0));
		assertFalse(IStateFilter.SF_IGNORED.accept(resource, IStateFilter.ST_DELETED, 0));
		assertFalse(IStateFilter.SF_IGNORED.accept(resource, IStateFilter.ST_MISSING, 0));
		assertFalse(IStateFilter.SF_IGNORED.accept(resource, IStateFilter.ST_MODIFIED, 0));
		assertTrue(IStateFilter.SF_IGNORED.accept(resource, IStateFilter.ST_NEW, 0));
		assertTrue(IStateFilter.SF_IGNORED.accept(resource, IStateFilter.ST_IGNORED, 0));
		assertFalse(IStateFilter.SF_IGNORED.accept(resource, IStateFilter.ST_NORMAL, 0));
		assertTrue(IStateFilter.SF_IGNORED.accept(resource, IStateFilter.ST_NOTEXISTS, 0));
	}

	@Test
	public void testSF_MODIFIED() {
		if (IStateFilter.SF_MODIFIED.accept(resource, IStateFilter.ST_ADDED, 0)
				|| IStateFilter.SF_MODIFIED.accept(resource, IStateFilter.ST_DELETED, 0)
				|| IStateFilter.SF_MODIFIED.accept(resource, IStateFilter.ST_MISSING, 0)
				|| !IStateFilter.SF_MODIFIED.accept(resource, IStateFilter.ST_MODIFIED, 0)
				|| IStateFilter.SF_MODIFIED.accept(resource, IStateFilter.ST_NEW, 0)
				|| IStateFilter.SF_MODIFIED.accept(resource, IStateFilter.ST_IGNORED, 0)
				|| IStateFilter.SF_MODIFIED.accept(resource, IStateFilter.ST_NORMAL, 0)
				|| IStateFilter.SF_MODIFIED.accept(resource, IStateFilter.ST_NOTEXISTS, 0)) {
			throw new RuntimeException(IStateFilter.SF_MODIFIED.getClass().getName() + " failed");
		}
	}

	@Test
	public void testSF_NEW() {
		assertFalse(IStateFilter.SF_NEW.accept(resource, IStateFilter.ST_ADDED, 0));
		assertFalse(IStateFilter.SF_NEW.accept(resource, IStateFilter.ST_DELETED, 0));
		assertFalse(IStateFilter.SF_NEW.accept(resource, IStateFilter.ST_MISSING, 0));
		assertFalse(IStateFilter.SF_NEW.accept(resource, IStateFilter.ST_MODIFIED, 0));
		assertTrue(IStateFilter.SF_NEW.accept(resource, IStateFilter.ST_NEW, 0));
		assertFalse(IStateFilter.SF_NEW.accept(resource, IStateFilter.ST_IGNORED, 0));
		assertFalse(IStateFilter.SF_NEW.accept(resource, IStateFilter.ST_NORMAL, 0));
		assertFalse(IStateFilter.SF_NEW.accept(resource, IStateFilter.ST_NOTEXISTS, 0));
	}

	@Test
	public void testSF_NONVERSIONED() {
		if (IStateFilter.SF_UNVERSIONED.accept(resource, IStateFilter.ST_ADDED, 0)
				|| IStateFilter.SF_UNVERSIONED.accept(resource, IStateFilter.ST_DELETED, 0)
				|| IStateFilter.SF_UNVERSIONED.accept(resource, IStateFilter.ST_MISSING, 0)
				|| IStateFilter.SF_UNVERSIONED.accept(resource, IStateFilter.ST_MODIFIED, 0)
				|| !IStateFilter.SF_UNVERSIONED.accept(resource, IStateFilter.ST_NEW, 0)
				|| !IStateFilter.SF_UNVERSIONED.accept(resource, IStateFilter.ST_IGNORED, 0)
				|| IStateFilter.SF_UNVERSIONED.accept(resource, IStateFilter.ST_NORMAL, 0)
				|| !IStateFilter.SF_UNVERSIONED.accept(resource, IStateFilter.ST_NOTEXISTS, 0)) {
			throw new RuntimeException(IStateFilter.SF_UNVERSIONED.getClass().getName() + " failed");
		}
	}

	@Test
	public void testSF_NOTEXISTS() {
		if (IStateFilter.SF_NOTEXISTS.accept(resource, IStateFilter.ST_ADDED, 0)
				|| IStateFilter.SF_NOTEXISTS.accept(resource, IStateFilter.ST_DELETED, 0)
				|| IStateFilter.SF_NOTEXISTS.accept(resource, IStateFilter.ST_MISSING, 0)
				|| IStateFilter.SF_NOTEXISTS.accept(resource, IStateFilter.ST_MODIFIED, 0)
				|| IStateFilter.SF_NOTEXISTS.accept(resource, IStateFilter.ST_NEW, 0)
				|| IStateFilter.SF_NOTEXISTS.accept(resource, IStateFilter.ST_IGNORED, 0)
				|| IStateFilter.SF_NOTEXISTS.accept(resource, IStateFilter.ST_NORMAL, 0)
				|| !IStateFilter.SF_NOTEXISTS.accept(resource, IStateFilter.ST_NOTEXISTS, 0)) {
			throw new RuntimeException(IStateFilter.SF_NOTEXISTS.getClass().getName() + " failed");
		}
	}

	@Test
	public void testSF_NOTMODIFIED() {
		if (IStateFilter.SF_NOTMODIFIED.accept(resource, IStateFilter.ST_ADDED, 0)
				|| IStateFilter.SF_NOTMODIFIED.accept(resource, IStateFilter.ST_DELETED, 0)
				|| IStateFilter.SF_NOTMODIFIED.accept(resource, IStateFilter.ST_MISSING, 0)
				|| IStateFilter.SF_NOTMODIFIED.accept(resource, IStateFilter.ST_MODIFIED, 0)
				|| IStateFilter.SF_NOTMODIFIED.accept(resource, IStateFilter.ST_NEW, 0)
				|| IStateFilter.SF_NOTMODIFIED.accept(resource, IStateFilter.ST_IGNORED, 0)
				|| !IStateFilter.SF_NOTMODIFIED.accept(resource, IStateFilter.ST_NORMAL, 0)
				|| !IStateFilter.SF_NOTMODIFIED.accept(resource, IStateFilter.ST_NOTEXISTS, 0)) {
			throw new RuntimeException(IStateFilter.SF_NOTMODIFIED.getClass().getName() + " failed");
		}
	}

	@Test
	public void testSF_ONREPOSITORY() {
		if (IStateFilter.SF_ONREPOSITORY.accept(resource, IStateFilter.ST_ADDED, 0)
				|| !IStateFilter.SF_ONREPOSITORY.accept(resource, IStateFilter.ST_DELETED, 0)
				|| !IStateFilter.SF_ONREPOSITORY.accept(resource, IStateFilter.ST_MISSING, 0)
				|| !IStateFilter.SF_ONREPOSITORY.accept(resource, IStateFilter.ST_MODIFIED, 0)
				|| IStateFilter.SF_ONREPOSITORY.accept(resource, IStateFilter.ST_NEW, 0)
				|| IStateFilter.SF_ONREPOSITORY.accept(resource, IStateFilter.ST_IGNORED, 0)
				|| !IStateFilter.SF_ONREPOSITORY.accept(resource, IStateFilter.ST_NORMAL, 0)
				|| IStateFilter.SF_ONREPOSITORY.accept(resource, IStateFilter.ST_NOTEXISTS, 0)) {
			throw new RuntimeException(IStateFilter.SF_ONREPOSITORY.getClass().getName() + " failed");
		}
	}

	@Test
	public void testSF_VERSIONED() {
		if (!IStateFilter.SF_VERSIONED.accept(resource, IStateFilter.ST_ADDED, 0)
				|| !IStateFilter.SF_VERSIONED.accept(resource, IStateFilter.ST_DELETED, 0)
				|| !IStateFilter.SF_VERSIONED.accept(resource, IStateFilter.ST_MISSING, 0)
				|| !IStateFilter.SF_VERSIONED.accept(resource, IStateFilter.ST_MODIFIED, 0)
				|| IStateFilter.SF_VERSIONED.accept(resource, IStateFilter.ST_NEW, 0)
				|| IStateFilter.SF_VERSIONED.accept(resource, IStateFilter.ST_IGNORED, 0)
				|| !IStateFilter.SF_VERSIONED.accept(resource, IStateFilter.ST_NORMAL, 0)
				|| IStateFilter.SF_VERSIONED.accept(resource, IStateFilter.ST_NOTEXISTS, 0)) {
			throw new RuntimeException(IStateFilter.SF_VERSIONED.getClass().getName() + " failed");
		}
	}

}
