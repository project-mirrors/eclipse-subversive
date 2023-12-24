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

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNProperty.BuiltIn;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.property.IPropertyProvider;
import org.eclipse.team.svn.core.operation.local.property.SetMultiPropertiesOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.StringMatcher;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.ArrayStructuredContentProvider;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.utility.UserInputHistory;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;

/**
 * Keyword property edit panel
 * 
 * @author Sergiy Logvin
 */
public class PropertyKeywordEditPanel extends AbstractDialogPanel {

	protected CheckboxTableViewer checkboxViewer;

	protected Button setRecursivelyCheckbox;

	protected Button useMaskCheckbox;

	protected Combo maskText;

	protected IResource[] selectedResources;

	protected IResource[] alreadyWithProperties;

	protected IPropertyProvider properties;

	protected boolean recursionEnabled;

	protected boolean setRecursively;

	protected String mask;

	protected boolean useMask;

	protected boolean computeStates;

	protected KeywordTableElement dateElement;

	protected KeywordTableElement revisionElement;

	protected KeywordTableElement lastChangedByElement;

	protected KeywordTableElement headUrlElement;

	protected KeywordTableElement idElement;

	protected KeywordTableElement headerElement;

	protected UserInputHistory maskHistory;

	public PropertyKeywordEditPanel(IResource[] selection, IResourceProvider alreadyWithProperties,
			IPropertyProvider properties) {
		selectedResources = selection;
		this.properties = properties;
		this.alreadyWithProperties = alreadyWithProperties == null
				? new IResource[0]
				: alreadyWithProperties.getResources();
		recursionEnabled = FileUtility.checkForResourcesPresence(selection, new IStateFilter.AbstractStateFilter() {
			@Override
			protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
				return false;
			}

			@Override
			protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
				return resource instanceof IContainer;
			}
		}, IResource.DEPTH_ZERO);
		dialogTitle = SVNUIMessages.PropertyKeywordEditPanel_Title;
		dialogDescription = SVNUIMessages.PropertyKeywordEditPanel_Description;
		defaultMessage = this.alreadyWithProperties.length > 1
				? SVNUIMessages.PropertyKeywordEditPanel_Message_Single
				: SVNUIMessages.PropertyKeywordEditPanel_Message_Multi;

		initializeKeywordElements();
	}

	@Override
	public void createControlsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		checkboxViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);
		GridData data = new GridData(GridData.FILL_BOTH);
		checkboxViewer.getTable().setLayoutData(data);

		TableLayout tlayout = new TableLayout();
		checkboxViewer.getTable().setLayout(tlayout);

		TableColumn column = new TableColumn(checkboxViewer.getTable(), SWT.LEFT);
		column.setText(SVNUIMessages.PropertyKeywordEditPanel_Keyword);
		tlayout.addColumnData(new ColumnWeightData(20, true));

		column = new TableColumn(checkboxViewer.getTable(), SWT.LEFT);
		column.setText(SVNUIMessages.PropertyKeywordEditPanel_Description1);
		tlayout.addColumnData(new ColumnWeightData(50, true));

		column = new TableColumn(checkboxViewer.getTable(), SWT.LEFT);
		column.setText(SVNUIMessages.PropertyKeywordEditPanel_Sample);
		tlayout.addColumnData(new ColumnWeightData(30, true));

		KeywordTableElement[] elements = { dateElement, revisionElement, lastChangedByElement, headUrlElement,
				idElement, headerElement };

		checkboxViewer.setContentProvider(new ArrayStructuredContentProvider());

		checkboxViewer.addCheckStateListener(event -> {
			KeywordTableElement element = (KeywordTableElement) event.getElement();

			if (element.getCurrentState() == KeywordTableElement.DESELECTED) {
				element.setCurrentState(KeywordTableElement.SELECTED);
			} else if (element.getCurrentState() == KeywordTableElement.SELECTED
					&& element.getInitialState() == KeywordTableElement.MIXED) {
				element.setCurrentState(KeywordTableElement.MIXED);
			} else {
				element.setCurrentState(KeywordTableElement.DESELECTED);
			}
			PropertyKeywordEditPanel.this.refreshCheckboxState(element);
		});

		checkboxViewer.setLabelProvider(new ITableLabelProvider() {
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}

			@Override
			public String getColumnText(Object element, int columnIndex) {
				KeywordTableElement keyElement = (KeywordTableElement) element;
				switch (columnIndex) {
					case 0: {
						return keyElement.getName();
					}
					case 1: {
						return keyElement.getDescription();
					}
					case 2: {
						return keyElement.getSample();
					}
					default: {
						return ""; //$NON-NLS-1$
					}
				}
			}

			@Override
			public void addListener(ILabelProviderListener listener) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			@Override
			public void removeListener(ILabelProviderListener listener) {
			}
		});

		checkboxViewer.setInput(elements);
		checkboxViewer.getTable().setHeaderVisible(true);

		addSelectionButtons(composite);

		if (recursionEnabled || selectedResources.length > 1) {
			Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
			separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
			separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			separator.setVisible(false);

			Composite subComposite = new Composite(composite, SWT.NONE);
			layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = 0;
			layout.numColumns = 2;
			subComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			subComposite.setLayout(layout);

			Composite maskComposite = new Composite(subComposite, SWT.NONE);
			layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = 0;
			layout.numColumns = 2;
			maskComposite.setLayout(layout);
			maskComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			useMaskCheckbox = new Button(maskComposite, SWT.CHECK);
			useMaskCheckbox.setText(SVNUIMessages.PropertyKeywordEditPanel_UseMask);
			maskHistory = new UserInputHistory("keywordsEditPanel"); //$NON-NLS-1$
			maskText = new Combo(maskComposite, SWT.BORDER);
			maskText.setItems(maskHistory.getHistory());
			if (maskText.getItemCount() == 0) {
				maskText.setText("*"); //$NON-NLS-1$
			} else {
				maskText.select(0);
			}
			Listener maskTextListener = event -> {
				checkboxViewer.setAllGrayed(false);
				PropertyKeywordEditPanel.this.changeMixedElementsToChecked();
			};
			maskText.addListener(SWT.Selection, maskTextListener);
			maskText.addListener(SWT.Modify, maskTextListener);

			attachTo(maskText, new AbstractVerifierProxy(
					new NonEmptyFieldVerifier(SVNUIMessages.PropertyKeywordEditPanel_Mask_Verifier)) {
				@Override
				protected boolean isVerificationEnabled(Control input) {
					return useMaskCheckbox.getSelection();
				}
			});
			data = new GridData();
			data.widthHint = 170;
			maskText.setLayoutData(data);

			maskText.setEnabled(false);

			useMaskCheckbox.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					maskText.setEnabled(useMaskCheckbox.getSelection());
					checkboxViewer.setAllGrayed(false);
					PropertyKeywordEditPanel.this.changeMixedElementsToChecked();
					PropertyKeywordEditPanel.this.validateContent();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});

			if (recursionEnabled) {
				setRecursivelyCheckbox = new Button(subComposite, SWT.CHECK);
				setRecursivelyCheckbox.setText(SVNUIMessages.PropertyKeywordEditPanel_Recursively);
				setRecursivelyCheckbox
						.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
				setRecursivelyCheckbox.setSelection(true);
				setRecursivelyCheckbox.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						checkboxViewer.setAllGrayed(false);
						PropertyKeywordEditPanel.this.changeMixedElementsToChecked();
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
			}
		}

		for (KeywordTableElement element : elements) {
			refreshCheckboxState(element);
		}
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.setKeysDialogContext"; //$NON-NLS-1$
	}

	public void performKeywordChanges() {
		//if filtration by mask is enabled - remove all non-matching resources from the operable map
		IStateFilter filter = IStateFilter.SF_EXCLUDE_PREREPLACED_AND_DELETED_FILES;
		if (useMask) {
			filter = new IStateFilter.AbstractStateFilter() {
				private StringMatcher fileNameMatcher = new StringMatcher(mask);

				@Override
				protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state,
						int mask) {
					return IStateFilter.SF_EXCLUDE_PREREPLACED_AND_DELETED.allowsRecursion(resource, state, mask);
				}

				@Override
				protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
					if (!IStateFilter.SF_EXCLUDE_PREREPLACED_AND_DELETED_FILES.accept(resource, state, mask)) {
						return false;
					}
					return fileNameMatcher.match(resource.getName());
				}
			};
		}

		IResourceProvider resourceProvider = () -> selectedResources;

		IPropertyProvider propertyProvider = resource -> {
			SVNProperty[] retVal = properties == null ? null : properties.getProperties(resource);
			if (retVal == null) {
				retVal = new SVNProperty[1];
			}
			SVNKeywordProperty keyProperty = new SVNKeywordProperty(retVal[0] == null ? null : retVal[0].value);
			PropertyKeywordEditPanel.this.configureProperty(keyProperty);
			retVal[0] = new SVNProperty(BuiltIn.KEYWORDS, keyProperty.toString());
			return retVal;
		};

		CompositeOperation composite = new CompositeOperation("Operation_SetKeywords", SVNUIMessages.class); //$NON-NLS-1$
		composite.add(new SetMultiPropertiesOperation(resourceProvider, propertyProvider, filter,
				recursionEnabled && setRecursively ? IResource.DEPTH_INFINITE : IResource.DEPTH_ONE));
		composite.add(new RefreshResourcesOperation(resourceProvider));
		UIMonitorUtility.doTaskScheduledActive(composite);
	}

	protected void configureProperty(SVNKeywordProperty keyProperty) {
		keyProperty.setDateEnabled(dateElement.getCurrentState() == KeywordTableElement.SELECTED
				? true
				: dateElement.getCurrentState() == KeywordTableElement.DESELECTED
						? false
						: keyProperty.isDateEnabled());

		keyProperty.setRevisionEnabled(revisionElement.getCurrentState() == KeywordTableElement.SELECTED
				? true
				: revisionElement.getCurrentState() == KeywordTableElement.DESELECTED
						? false
						: keyProperty.isLastChangedByEnabled());

		keyProperty.setLastChangedByEnabled(lastChangedByElement.getCurrentState() == KeywordTableElement.SELECTED
				? true
				: lastChangedByElement.getCurrentState() == KeywordTableElement.DESELECTED
						? false
						: keyProperty.isLastChangedByEnabled());

		keyProperty.setHeadUrlEnabled(headUrlElement.getCurrentState() == KeywordTableElement.SELECTED
				? true
				: headUrlElement.getCurrentState() == KeywordTableElement.DESELECTED
						? false
						: keyProperty.isHeadUrlEnabled());

		keyProperty.setIdEnabled(idElement.getCurrentState() == KeywordTableElement.SELECTED
				? true
				: idElement.getCurrentState() == KeywordTableElement.DESELECTED ? false : keyProperty.isIdEnabled());

		keyProperty.setHeaderEnabled(headerElement.getCurrentState() == KeywordTableElement.SELECTED
				? true
				: headerElement.getCurrentState() == KeywordTableElement.DESELECTED
						? false
						: keyProperty.isHeaderEnabled());
	}

	protected void applyCurrentKeywordValuesOnTableElement(KeywordTableElement tableElement, boolean propertyPresent) {
		tableElement.setInitialState(tableElement.getInitialState() == KeywordTableElement.INITIAL
				? propertyPresent ? KeywordTableElement.SELECTED : KeywordTableElement.DESELECTED
				: propertyPresent && tableElement.getInitialState() == KeywordTableElement.DESELECTED
						|| !propertyPresent && tableElement.getInitialState() == KeywordTableElement.SELECTED
								? KeywordTableElement.MIXED
								: tableElement.getInitialState());

		tableElement.setCurrentState(tableElement.getInitialState());
	}

	protected void initializeKeywordElements() {
		dateElement = new KeywordTableElement(SVNKeywordProperty.DATE_NAMES[0], SVNKeywordProperty.DATE_DESCR(),
				SVNKeywordProperty.DATE_SAMPLE, KeywordTableElement.INITIAL);
		revisionElement = new KeywordTableElement(SVNKeywordProperty.REVISION_NAMES[0],
				SVNKeywordProperty.REVISION_DESCR(), SVNKeywordProperty.REVISION_SAMPLE, KeywordTableElement.INITIAL);
		lastChangedByElement = new KeywordTableElement(SVNKeywordProperty.AUTHOR_NAMES[0],
				SVNKeywordProperty.AUTHOR_DESCR(), SVNKeywordProperty.AUTHOR_SAMPLE, KeywordTableElement.INITIAL);
		headUrlElement = new KeywordTableElement(SVNKeywordProperty.HEAD_URL_NAMES[0],
				SVNKeywordProperty.HEAD_URL_DESCR(), SVNKeywordProperty.HEAD_URL_SAMPLE, KeywordTableElement.INITIAL);
		idElement = new KeywordTableElement(SVNKeywordProperty.ID_NAMES[0], SVNKeywordProperty.ID_DESCR(),
				SVNKeywordProperty.ID_SAMPLE, KeywordTableElement.INITIAL);
		headerElement = new KeywordTableElement(SVNKeywordProperty.HEADER_NAMES[0], SVNKeywordProperty.HEADER_DESCR(),
				SVNKeywordProperty.HEADER_SAMPLE, KeywordTableElement.INITIAL);

		List<IResource> alreadyWithPropertiesList = Arrays.asList(alreadyWithProperties);
		for (IResource element : selectedResources) {
			SVNProperty[] data;
			SVNKeywordProperty keywordPropertyValue = new SVNKeywordProperty(null);
			if (alreadyWithPropertiesList.contains(element) && properties != null
					&& (data = properties.getProperties(element)) != null) {
				keywordPropertyValue = new SVNKeywordProperty(data[0].value);
			}
			applyCurrentKeywordValuesOnTableElement(dateElement, keywordPropertyValue.isDateEnabled());
			applyCurrentKeywordValuesOnTableElement(revisionElement, keywordPropertyValue.isRevisionEnabled());
			applyCurrentKeywordValuesOnTableElement(lastChangedByElement,
					keywordPropertyValue.isLastChangedByEnabled());
			applyCurrentKeywordValuesOnTableElement(headUrlElement, keywordPropertyValue.isHeadUrlEnabled());
			applyCurrentKeywordValuesOnTableElement(idElement, keywordPropertyValue.isIdEnabled());
			applyCurrentKeywordValuesOnTableElement(headerElement, keywordPropertyValue.isHeaderEnabled());
		}
	}

	protected void refreshCheckboxState(KeywordTableElement element) {
		checkboxViewer.setChecked(element, element.getCurrentState() == KeywordTableElement.MIXED
				|| element.getCurrentState() == KeywordTableElement.SELECTED);
		checkboxViewer.setGrayed(element, element.getCurrentState() == KeywordTableElement.MIXED);
	}

	protected void changeMixedElementsToChecked() {
		Object[] elements = checkboxViewer.getCheckedElements();
		for (Object element : elements) {
			((KeywordTableElement) element).setCurrentState(KeywordTableElement.SELECTED);
		}
	}

	protected void addSelectionButtons(Composite composite) {

		Composite tComposite = new Composite(composite, SWT.RIGHT);
		GridLayout gLayout = new GridLayout();
		gLayout.numColumns = 2;
		gLayout.marginWidth = 0;
		tComposite.setLayout(gLayout);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		tComposite.setData(data);

		Button selectButton = new Button(tComposite, SWT.PUSH);
		selectButton.setText(SVNUIMessages.Button_SelectAll);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(selectButton);
		selectButton.setLayoutData(data);
		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PropertyKeywordEditPanel.this.refreshKeywordElements(true);
			}
		};
		selectButton.addSelectionListener(listener);

		Button deselectButton = new Button(tComposite, SWT.PUSH);
		deselectButton.setText(SVNUIMessages.Button_ClearSelection);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(deselectButton);
		deselectButton.setLayoutData(data);
		listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PropertyKeywordEditPanel.this.refreshKeywordElements(false);
			}
		};
		deselectButton.addSelectionListener(listener);
	}

	protected void refreshKeywordElements(boolean selected) {
		int state = selected ? KeywordTableElement.SELECTED : KeywordTableElement.DESELECTED;
		dateElement.setCurrentState(state);
		revisionElement.setCurrentState(state);
		lastChangedByElement.setCurrentState(state);
		headUrlElement.setCurrentState(state);
		idElement.setCurrentState(state);
		headerElement.setCurrentState(state);
		checkboxViewer.setAllChecked(selected);
		checkboxViewer.setAllGrayed(false);
	}

	@Override
	protected void cancelChangesImpl() {
	}

	@Override
	protected void saveChangesImpl() {
		useMask = useMaskCheckbox == null ? false : useMaskCheckbox.getSelection();
		mask = maskText == null ? "*" : maskText.getText().trim(); //$NON-NLS-1$
		setRecursively = setRecursivelyCheckbox == null ? false : setRecursivelyCheckbox.getSelection();
		if (useMask) {
			maskHistory.addLine(maskText.getText());
		}
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(670, SWT.DEFAULT);
	}

}
