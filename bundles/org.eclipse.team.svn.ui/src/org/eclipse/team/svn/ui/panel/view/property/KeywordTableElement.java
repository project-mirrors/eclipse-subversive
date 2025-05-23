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
 *    Sergiy Logvin (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.view.property;

/**
 * Table element representing 'svn:keywords' property value
 * 
 * @author Sergiy Logvin
 */
public class KeywordTableElement {
	public static final int INITIAL = -1;

	public static final int SELECTED = 0;

	public static final int DESELECTED = 1;

	public static final int MIXED = 2;

	private String name;

	private String description;

	private String sample;

	private int initialState;

	private int currentState;

	public KeywordTableElement(String name, String description, String sample, int initialState) {
		this.name = name;
		this.description = description;
		this.sample = sample;
		this.initialState = initialState;
		currentState = initialState;
	}

	public int getCurrentState() {
		return currentState;
	}

	public void setCurrentState(int currentState) {
		this.currentState = currentState;
	}

	public String getDescription() {
		return description;
	}

	public int getInitialState() {
		return initialState;
	}

	public void setInitialState(int initialState) {
		this.initialState = initialState;
	}

	public String getName() {
		return name;
	}

	public String getSample() {
		return sample;
	}

}
