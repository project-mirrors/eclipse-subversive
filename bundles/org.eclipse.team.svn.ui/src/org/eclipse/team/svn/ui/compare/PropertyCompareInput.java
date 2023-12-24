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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.compare;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.DiffTreeViewer;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Compare editor input for SVN properties comparison.
 * 
 * @author Alexei Goncharov
 */
public abstract class PropertyCompareInput extends CompareEditorInput {

	protected DiffTreeViewer viewer;

	protected IRepositoryLocation location;

	protected SVNEntryRevisionReference left;

	protected SVNEntryRevisionReference right;

	protected SVNEntryRevisionReference ancestor;

	protected HashMap<String, String> leftProps;

	protected HashMap<String, String> ancestorProps;

	protected HashMap<String, String> rightProps;

	protected HashSet<String> propSet;

	public PropertyCompareInput(CompareConfiguration configuration, SVNEntryRevisionReference left,
			SVNEntryRevisionReference right, SVNEntryRevisionReference ancestor, IRepositoryLocation location) {
		super(configuration);
		this.left = left;
		this.right = right;
		this.ancestor = ancestor;
		this.location = location;
	}

	@Override
	public Viewer createDiffViewer(Composite parent) {
		viewer = (DiffTreeViewer) super.createDiffViewer(parent);
		viewer.addOpenListener(event -> {
			PropertyCompareNode selected = (PropertyCompareNode) ((TreeSelection) event.getSelection())
					.getPaths()[0].getFirstSegment();
			CompareConfiguration conf = PropertyCompareInput.this.getCompareConfiguration();
			if (ancestor != null) {
				conf.setAncestorLabel(selected.getName() + " [" //$NON-NLS-1$
						+ PropertyCompareInput.this.getRevisionPart(ancestor) + "]"); //$NON-NLS-1$
			}
			conf.setLeftLabel(selected.getName() + " [" //$NON-NLS-1$
					+ PropertyCompareInput.this.getRevisionPart(left) + "]"); //$NON-NLS-1$
			conf.setRightLabel(selected.getName() + " [" //$NON-NLS-1$
					+ PropertyCompareInput.this.getRevisionPart(right) + "]"); //$NON-NLS-1$
		});

		MenuManager menuMgr = new MenuManager();
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		menuMgr.addMenuListener(manager -> {
			manager.removeAll();
			TreeSelection selection = (TreeSelection) viewer.getSelection();
			if (selection.size() == 0) {
				return;
			}
			PropertyCompareInput.this.fillMenu(manager, selection);
		});
		viewer.getControl().setMenu(menu);

		return viewer;
	}

	protected abstract void fillMenu(IMenuManager manager, TreeSelection selection);

	protected String getRevisionPart(SVNEntryRevisionReference reference) {
		if (reference == null) {
			return SVNUIMessages.ResourceCompareInput_PrejFile;
		}
		return BaseMessages.format(SVNUIMessages.ResourceCompareInput_RevisionSign,
				new String[] { String.valueOf(reference.revision) });
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		leftProps = new HashMap<>();
		ancestorProps = new HashMap<>();
		rightProps = new HashMap<>();
		propSet = new HashSet<>();

		//read the properties
		GetPropertiesOperation leftPropOperation = new GetPropertiesOperation(left, location, false);
		GetPropertiesOperation rightPropOperation = new GetPropertiesOperation(
				right == null ? left : right, location, right == null);
		GetPropertiesOperation ancestorPropOperation = null;
		final CompositeOperation op = new CompositeOperation(leftPropOperation.getOperationName(), SVNMessages.class);
		op.add(leftPropOperation);
		op.add(rightPropOperation);
		if (ancestor != null) {
			ancestorPropOperation = new GetPropertiesOperation(ancestor, location, false);
			op.add(ancestorPropOperation);
		}
		UIMonitorUtility.getDisplay().syncExec(() -> UIMonitorUtility.doTaskNowDefault(op, true));

		//gather found properties
		SVNProperty[] properties = leftPropOperation.getProperties();
		for (SVNProperty current : properties) {
			propSet.add(current.name);
			leftProps.put(current.name, current.value);
		}
		properties = null;
		properties = rightPropOperation.getProperties();
		for (SVNProperty current : properties) {
			propSet.add(current.name);
			rightProps.put(current.name, current.value);
		}
		if (ancestor != null) {
			properties = null;
			properties = ancestorPropOperation.getProperties();
			for (SVNProperty current : properties) {
				propSet.add(current.name);
				ancestorProps.put(current.name, current.value);
			}
		}

		//prepare input
		RootCompareNode root = new RootCompareNode(Differencer.NO_CHANGE);
		for (String current : propSet) {
			String leftValue = leftProps.get(current);
			String rightValue = rightProps.get(current);
			String ancestorValue = ancestorProps.get(current);
			int diffKind = calculateDifference(leftValue, rightValue, ancestorValue);
			if (rightPropOperation.isConflicting(current)) {
				int tDiffKind = calculateDifference(leftValue, ancestorValue, ancestorValue);
				diffKind = rightPropOperation.isAdded(current)
						? Differencer.ADDITION
						: rightPropOperation.isChanged(current) ? Differencer.CHANGE : Differencer.DELETION;
				diffKind = Differencer.CONFLICTING | (diffKind == Differencer.CHANGE ? tDiffKind : diffKind);
			}
			if (diffKind != Differencer.NO_CHANGE) {
				new PropertyCompareNode(
						root, diffKind, new PropertyElement(current, ancestorValue, false),
						new PropertyElement(current, leftValue, ancestor == null ? false : true),
						new PropertyElement(current, rightValue, false));
			}
		}

		if (root.getChildren().length > 0) {
			return root;
		}
		return null;
	}

	protected int calculateDifference(String leftValue, String rightValue, String ancestorValue) {
		int diffKind = Differencer.NO_CHANGE;
		if (ancestor == null) {
			if (leftValue != null && rightValue != null) {
				diffKind = rightValue.equals(leftValue) ? Differencer.NO_CHANGE : Differencer.CHANGE;
			} else if (leftValue == null) {
				diffKind = Differencer.ADDITION;
			} else {
				diffKind = Differencer.DELETION;
			}
		} else if (ancestorValue == null) {
			if (rightValue != null && leftValue != null) {
				diffKind = Differencer.ADDITION | Differencer.CONFLICTING;
			} else if (rightValue != null) {
				diffKind = Differencer.RIGHT | Differencer.ADDITION;
			} else if (leftValue != null) {
				diffKind = Differencer.LEFT | Differencer.ADDITION;
			}
		} else {
			if (rightValue != null && leftValue != null) {
				if (!rightValue.equals(ancestorValue) && !leftValue.equals(ancestorValue)) {
					diffKind = Differencer.CHANGE | Differencer.CONFLICTING;
				} else if (!rightValue.equals(ancestorValue)) {
					diffKind = Differencer.RIGHT | Differencer.CHANGE;
				} else if (!leftValue.equals(ancestorValue)) {
					diffKind = Differencer.LEFT | Differencer.CHANGE;
				}
			} else if (leftValue == null && rightValue == null) {
				diffKind = Differencer.DELETION | Differencer.CONFLICTING;
			} else if (leftValue == null) {
				diffKind = Differencer.LEFT | Differencer.DELETION;
				if (!rightValue.equals(ancestorValue)) {
					diffKind |= Differencer.CONFLICTING;
				}
			} else if (rightValue == null) {
				diffKind = Differencer.RIGHT | Differencer.DELETION;
				if (!leftValue.equals(ancestorValue)) {
					diffKind |= Differencer.CONFLICTING;
				}
			}
		}
		return diffKind;
	}

	@Override
	public void saveChanges(IProgressMonitor monitor) throws CoreException {
		super.saveChanges(monitor);
		PropertyCompareNode currentNode = (PropertyCompareNode) getSelectedEdition();
		PropertyElement left = (PropertyElement) currentNode.getLeft();
		PropertyElement right = (PropertyElement) currentNode.getRight();
		PropertyElement ancestor = (PropertyElement) currentNode.getAncestor();
		left.commit(monitor);
		currentNode.setKind(calculateDifference(left.getValue(), right.getValue(), ancestor.getValue()));
		currentNode.fireChange();
		viewer.refresh();
	}

	protected class RootCompareNode extends DiffNode {
		public RootCompareNode(int kind) {
			super(kind);
		}
	}

	protected class PropertyCompareNode extends DiffNode {

		public PropertyCompareNode(IDiffContainer parent, int kind, ITypedElement ancestor, ITypedElement left,
				ITypedElement right) {
			super(parent, kind, ancestor, left, right);
		}

		@Override
		public void fireChange() {
			super.fireChange();
		}

	}

	protected class PropertyElement
			implements ITypedElement, IEditableContent, IStreamContentAccessor, IContentChangeNotifier {

		protected String basedOnName;

		protected String basedOnValue;

		protected String currentInput;

		protected boolean isEditable;

		protected ArrayList<IContentChangeListener> listenersList;

		public PropertyElement(String name, String value, boolean isEditable) {
			basedOnName = name;
			basedOnValue = value;
			this.isEditable = isEditable;
			listenersList = new ArrayList<>();
		}

		@Override
		public Image getImage() {
			return CompareUI.getImage(""); //$NON-NLS-1$
		}

		@Override
		public String getName() {
			return basedOnName;
		}

		public String getValue() {
			return basedOnValue;
		}

		public void setValue(String value) {
			basedOnValue = value;
		}

		@Override
		public String getType() {
			return ITypedElement.TEXT_TYPE;
		}

		@Override
		public boolean isEditable() {
			return isEditable;
		}

		@Override
		public ITypedElement replace(ITypedElement dest, ITypedElement src) {
			return dest;
		}

		@Override
		public void setContent(byte[] newContent) {
			currentInput = new String(newContent);
		}

		public void commit(IProgressMonitor pm) throws CoreException {
			basedOnValue = currentInput;
			new SavePropChangesOperation(left, new SVNProperty(basedOnName, basedOnValue), location).run(pm);
			fireContentChanged();
		}

		@Override
		public InputStream getContents() throws CoreException {
			return new ByteArrayInputStream(basedOnValue == null ? "".getBytes() : basedOnValue.getBytes()); //$NON-NLS-1$
		}

		@Override
		public void addContentChangeListener(IContentChangeListener listener) {
			listenersList.add(listener);
		}

		@Override
		public void removeContentChangeListener(IContentChangeListener listener) {
			listenersList.remove(listener);
		}

		protected void fireContentChanged() {
			IContentChangeListener[] listeners = listenersList.toArray(new IContentChangeListener[0]);
			for (IContentChangeListener listener : listeners) {
				listener.contentChanged(this);
			}
		}

	}

	protected class SavePropChangesOperation extends AbstractActionOperation {
		protected SVNEntryRevisionReference reference;

		protected SVNProperty propToSet;

		protected IRepositoryLocation location;

		public SavePropChangesOperation(SVNEntryRevisionReference reference, SVNProperty propToSet,
				IRepositoryLocation location) {
			super("Operation_SetProperties", SVNMessages.class); //$NON-NLS-1$
			this.propToSet = propToSet;
			this.reference = reference;
			this.location = location;
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			ISVNConnector proxy = location.acquireSVNProxy();
			try {
				proxy.setPropertyLocal(new String[] { reference.path },
						new SVNProperty(propToSet.name, propToSet.value), SVNDepth.EMPTY, ISVNConnector.Options.FORCE,
						null, new SVNProgressMonitor(this, monitor, null));
			} finally {
				location.releaseSVNProxy(proxy);
			}
		}
	}

	protected class GetPropertiesOperation extends AbstractActionOperation {
		protected SVNEntryRevisionReference reference;

		protected IRepositoryLocation location;

		protected SVNProperty[] properties;

		protected ArrayList<SVNProperty> propsAdd = new ArrayList<>();

		protected ArrayList<SVNProperty> propsChange = new ArrayList<>();

		protected ArrayList<SVNProperty> propsDel = new ArrayList<>();

		protected boolean usePropsRej;

		public GetPropertiesOperation(SVNEntryRevisionReference reference, IRepositoryLocation location,
				boolean usePropsRej) {
			super("Operation_GetRevisionProperties", SVNMessages.class); //$NON-NLS-1$
			this.reference = reference;
			this.location = location;
			this.usePropsRej = usePropsRej;
		}

		public boolean isConflicting(String name) {
			return usePropsRej && (findProperty(name, propsAdd) != null || findProperty(name, propsChange) != null
					|| findProperty(name, propsDel) != null);
		}

		public boolean isAdded(String name) {
			return usePropsRej && findProperty(name, propsAdd) != null;
		}

		public boolean isChanged(String name) {
			return usePropsRej && findProperty(name, propsChange) != null;
		}

		public boolean isDeleted(String name) {
			return usePropsRej && findProperty(name, propsDel) != null;
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			ISVNConnector proxy = location.acquireSVNProxy();
			try {
				properties = SVNUtility.properties(proxy, reference, ISVNConnector.Options.NONE,
						new SVNProgressMonitor(this, monitor, null));
				if (usePropsRej) {
					SVNChangeStatus[] status = SVNUtility.status(proxy, reference.path, SVNDepth.EMPTY,
							ISVNConnector.Options.NONE, new SVNNullProgressMonitor());
					if (status.length > 0 && status[0].propStatus == SVNEntryStatus.Kind.CONFLICTED
							&& status[0].treeConflicts != null && status[0].treeConflicts[0].remotePath != null) {
						File rejFile = new File(status[0].treeConflicts[0].remotePath);
						if (rejFile.exists()) {
							BufferedInputStream is = null;
							BufferedReader reader = null;
							try {
								is = new BufferedInputStream(new FileInputStream(rejFile));
								reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
								String line = null, pName = null, pValue = null;
								int state = 0;
								while ((line = reader.readLine()) != null) {
									if ((state == 0 || state == 2) && line.startsWith("Trying to add new property '")) {
										if (state == 2 || state == 8) {
											propsAdd.add(new SVNProperty(pName, pValue));
										}
										pName = line.substring("Trying to add new property '".length(),
												line.length() - 1);
										pValue = null;
										state = 1;
									}
									if ((state == 0 || state == 2) && line.startsWith("Trying to change property '")) {
										if (state == 2 || state == 8) {
											propsAdd.add(new SVNProperty(pName, pValue));
										}
										pName = line.substring("Trying to change property '".length(),
												line.length() - 1);
										pValue = null;
										state = 3;
									}
									if ((state == 0 || state == 2) && line.startsWith("Trying to delete property '")) {
										if (state == 2 || state == 8) {
											propsAdd.add(new SVNProperty(pName, pValue));
										}
										pName = line.substring("Trying to change property '".length(),
												line.length() - 1);
										propsDel.add(new SVNProperty(pName, ""));
										pName = null;
										pValue = null;
										state = 6;
									}
									if (state == 1 && line.equals("Incoming property value:")) {
										state = 2;
										continue;
									}
									if (state == 1 && line.equals("<<<<<<< (local property value)")) {
										state = 7;
										continue;
									}
									if (state == 2) {
										pValue = pValue != null ? pValue + "\n" + line : line;
									}
									if (state == 3 && line.equals("<<<<<<< (local property value)")) {
										state = 4;
										continue;
									}
									if (state == 4 && line.endsWith("=======")) {
										state = 5;
										continue;
									}
									if (state == 5) {
										if (line.endsWith(">>>>>>> (incoming property value)")) {
											line = line.substring(0,
													line.length() - ">>>>>>> (incoming property value)".length());
											pValue = pValue != null ? pValue + "\n" + line : line;
											propsChange.add(new SVNProperty(pName, pValue));
											state = 0;
										} else {
											pValue = pValue != null ? pValue + "\n" + line : line;
										}
									}
									if (state == 6 && line.endsWith(">>>>>>> (incoming property value)")) {
										state = 0;
									}
									if (state == 7 && line.endsWith("=======")) {
										state = 8;
										continue;
									}
									if (state == 8) {
										if (line.endsWith(">>>>>>> (incoming property value)")) {
											line = line.substring(0,
													line.length() - ">>>>>>> (incoming property value)".length());
											pValue = pValue != null ? pValue + "\n" + line : line;
											propsAdd.add(new SVNProperty(pName, pValue));
											state = 0;
										} else {
											pValue = pValue != null ? pValue + "\n" + line : line;
										}
									}
								}
								if (state == 2 || state == 8) {
									propsAdd.add(new SVNProperty(pName, pValue));
								}
								ArrayList<SVNProperty> props = new ArrayList<>(propsAdd);
								props.addAll(propsChange);
								if (properties != null) {
									for (SVNProperty property : properties) {
										if (findProperty(property.name, propsDel) == null
												&& findProperty(property.name, propsChange) == null
												&& findProperty(property.name, propsAdd) == null) {
											props.add(property);
										}
									}
								}
								properties = props.toArray(new SVNProperty[props.size()]);
							} catch (IOException ex) {
								// uninterested
							} finally {
								if (reader != null) {
									try {
										reader.close();
									} catch (IOException ex) {
									}
								}
								if (is != null) {
									try {
										is.close();
									} catch (IOException ex) {
									}
								}
							}
						}
					}
				}
			} finally {
				location.releaseSVNProxy(proxy);
			}
		}

		private SVNProperty findProperty(String name, ArrayList<SVNProperty> props) {
			for (SVNProperty p : props) {
				if (name.equals(p.name)) {
					return p;
				}
			}
			return null;
		}

		public SVNProperty[] getProperties() {
			return properties == null ? new SVNProperty[0] : properties;
		}
	}
}
