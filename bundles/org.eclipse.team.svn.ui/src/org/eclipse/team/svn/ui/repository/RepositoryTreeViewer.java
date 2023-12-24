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

package org.eclipse.team.svn.ui.repository;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.AbstractCopyMoveResourcesOperation;
import org.eclipse.team.svn.core.operation.remote.CopyResourcesOperation;
import org.eclipse.team.svn.core.operation.remote.MoveResourcesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.RemoteResourceTransfer;
import org.eclipse.team.svn.ui.RemoteResourceTransferrable;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;
import org.eclipse.team.svn.ui.panel.common.CommentPanel;
import org.eclipse.team.svn.ui.repository.model.IDataTreeNode;
import org.eclipse.team.svn.ui.repository.model.IParentTreeNode;
import org.eclipse.team.svn.ui.repository.model.IToolTipProvider;
import org.eclipse.team.svn.ui.repository.model.RepositoryBranches;
import org.eclipse.team.svn.ui.repository.model.RepositoryFile;
import org.eclipse.team.svn.ui.repository.model.RepositoryFolder;
import org.eclipse.team.svn.ui.repository.model.RepositoryResource;
import org.eclipse.team.svn.ui.repository.model.RepositoryRevision;
import org.eclipse.team.svn.ui.repository.model.RepositoryRoot;
import org.eclipse.team.svn.ui.repository.model.RepositoryTags;
import org.eclipse.team.svn.ui.repository.model.RepositoryTrunk;
import org.eclipse.team.svn.ui.repository.model.ToolTipVariableSetProvider;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Repository TreeViewer implementation
 * 
 * @author Alexander Gurov
 */
public class RepositoryTreeViewer extends TreeViewer {

	public static final String FMT_REPOSITORY_RESOURCE = "{" + ToolTipVariableSetProvider.NAME_OF_NAME + "}" + //$NON-NLS-1$ //$NON-NLS-2$
			"{" + ToolTipVariableSetProvider.NAME_OF_LAST_CHANGE_DATE + "}" + //$NON-NLS-1$ //$NON-NLS-2$
			"{" + ToolTipVariableSetProvider.NAME_OF_LAST_AUTHOR + "}"; //$NON-NLS-1$ //$NON-NLS-2$

	public static final String FMT_REPOSITORY_FILE = RepositoryTreeViewer.FMT_REPOSITORY_RESOURCE + "{" //$NON-NLS-1$
			+ ToolTipVariableSetProvider.NAME_OF_SIZE + "}" + //$NON-NLS-1$
			"{" + ToolTipVariableSetProvider.NAME_OF_LOCK_OWNER + "}" + //$NON-NLS-1$ //$NON-NLS-2$
			"{" + ToolTipVariableSetProvider.NAME_OF_LOCK_CREATION_DATE + "}" + //$NON-NLS-1$ //$NON-NLS-2$
			"{" + ToolTipVariableSetProvider.NAME_OF_LOCK_EXPIRATION_DATE + "}" + //$NON-NLS-1$ //$NON-NLS-2$
			"{" + ToolTipVariableSetProvider.NAME_OF_LOCK_COMMENT + "}"; //$NON-NLS-1$ //$NON-NLS-2$

	public static final String FMT_REPOSITORY_FOLDER = RepositoryTreeViewer.FMT_REPOSITORY_RESOURCE;

	public static final String FMT_REPOSITORY_BRANCHES = RepositoryTreeViewer.FMT_REPOSITORY_FOLDER;

	public static final String FMT_REPOSITORY_ROOT = RepositoryTreeViewer.FMT_REPOSITORY_FOLDER;

	public static final String FMT_REPOSITORY_TAGS = RepositoryTreeViewer.FMT_REPOSITORY_FOLDER;

	public static final String FMT_REPOSITORY_TRUNK = RepositoryTreeViewer.FMT_REPOSITORY_FOLDER;

	private static final Map<Class<?>, String> class2Format = new HashMap<>();

	static {
		RepositoryTreeViewer.class2Format.put(RepositoryResource.class, RepositoryTreeViewer.FMT_REPOSITORY_RESOURCE);
		RepositoryTreeViewer.class2Format.put(RepositoryFile.class, RepositoryTreeViewer.FMT_REPOSITORY_FILE);
		RepositoryTreeViewer.class2Format.put(RepositoryFolder.class, RepositoryTreeViewer.FMT_REPOSITORY_FOLDER);
		RepositoryTreeViewer.class2Format.put(RepositoryBranches.class, RepositoryTreeViewer.FMT_REPOSITORY_BRANCHES);
		RepositoryTreeViewer.class2Format.put(RepositoryRoot.class, RepositoryTreeViewer.FMT_REPOSITORY_ROOT);
		RepositoryTreeViewer.class2Format.put(RepositoryTags.class, RepositoryTreeViewer.FMT_REPOSITORY_TAGS);
		RepositoryTreeViewer.class2Format.put(RepositoryTrunk.class, RepositoryTreeViewer.FMT_REPOSITORY_TRUNK);
		RepositoryTreeViewer.class2Format.put(RepositoryRevision.class, "");
	}

	public interface IRefreshVisitor {
		void visit(Object data);
	}

	public interface IRefreshListener {
		void refreshed(Object element);
	}

	protected List<IRefreshListener> refreshListeners = new ArrayList<>();

	public RepositoryTreeViewer(Composite parent) {
		super(parent);
		initialize();
	}

	public RepositoryTreeViewer(Composite parent, int style) {
		super(parent, style);
		initialize();
	}

	public RepositoryTreeViewer(Tree tree) {
		super(tree);
		initialize();
	}

	public synchronized void addRefreshListener(IRefreshListener listener) {
		if (!refreshListeners.contains(listener)) {
			refreshListeners.add(listener);
		}
	}

	public synchronized void removeRefreshListener(IRefreshListener listener) {
		refreshListeners.remove(listener);
	}

	@Override
	public void setExpandedState(Object element, boolean expanded) {
		TreeItem[] items = getIdenticalNodes(element, true);
		if (items != null && items.length > 0) {
			if (expanded) {
				createChildren(items[0]);
			}
			setExpanded(items[0], expanded);
		} else {
			TreeItem[] nodes = getIdenticalNodes(element, false);
			if (nodes != null && nodes.length > 0) {
				super.setExpandedState(nodes[0].getData(), expanded);
			}
		}
	}

	@Override
	public void setSelection(ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			IStructuredSelection tmp = (IStructuredSelection) selection;
			TreeItem[] nodes = getIdenticalNodes(tmp.getFirstElement(), false);
			if (nodes != null && nodes.length > 0) {
				selection = new StructuredSelection(nodes[0].getData());
			}
		}
		super.setSelection(selection);
	}

	public void refresh(final Object element, final IRefreshVisitor visitor, final boolean exact) {
		getControl().getDisplay().syncExec(() -> {
			TreeItem[] nodes = RepositoryTreeViewer.this.getIdenticalNodes(element, exact);
			if (nodes != null && nodes.length != 0) {
				for (TreeItem node : nodes) {
					Object data = node.getData();
					if (visitor != null) {
						visitor.visit(data);
					}
					RepositoryTreeViewer.this.internalRefresh(node, data, true, true);
					RepositoryTreeViewer.this.fireRefresh(data);
				}
			} else {
				Object input = RepositoryTreeViewer.this.getInput();
				if (input instanceof IDataTreeNode) {
					Object data = ((IDataTreeNode) input).getData();
					if (data != null && data.equals(element) && visitor != null) {
						visitor.visit(input);
					}
				}
				RepositoryTreeViewer.super.refresh(null);
			}
		});
	}

	public void fireEmptySelectionEvent() {
		fireSelectionChanged(new SelectionChangedEvent(this, StructuredSelection.EMPTY));
	}

	// fix the problem with refresh of identical nodes in the tree
	@Override
	protected void internalRefresh(Widget widget, Object element, boolean doStruct, boolean updateLabels) {
		if (widget instanceof Item) {
			if (doStruct) {
				updatePlus((Item) widget, element);
			}
			if (updateLabels || !this.equals(element, widget.getData())) {
				this.doUpdateItem(widget, element, true);
			} else {
				associate(element, (Item) widget);
			}
		}

		if (doStruct) {
			internalRefreshStruct(widget, element, updateLabels);
		} else {
			Item[] children = this.getChildren(widget);
			if (children != null) {
				for (Item item : children) {
					Object data = item.getData();
					if (data != null) {
						this.internalRefresh(item, data, doStruct, updateLabels);
					}
				}
			}
		}
	}

	@Override
	protected void internalRefreshStruct(Widget widget, Object element, boolean updateLabels) {
//		this.updateChildren(widget, element, null);
		try {
			//updateChildren(Widget widget, Object parent, Object[] elementChildren, boolean updateLabels)
			Method m = AbstractTreeViewer.class.getDeclaredMethod("updateChildren", //$NON-NLS-1$
					Widget.class, Object.class, Object[].class, boolean.class);
			m.setAccessible(true);
			m.invoke(this, widget, element, null, Boolean.valueOf(updateLabels));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Item[] children = getChildren(widget);
		if (children != null) {
			for (Item item : children) {
				Object data = item.getData();
				if (data != null) {
					internalRefreshStruct(item, data, updateLabels);
				}
			}
		}
	}

	@Override
	public void refresh() {
		getControl().getDisplay().syncExec(super::refresh);
	}

	public TreeItem[] getIdenticalNodes(Object sample, boolean exact) {
		if (sample != null) {
			return this.findUnfreshNodes(getTree().getItems(), sample, exact);
		}
		return null;
	}

	protected synchronized void fireRefresh(Object data) {
		Object[] listeners = refreshListeners.toArray();
		for (Object listener : listeners) {
			((IRefreshListener) listener).refreshed(data);
		}
	}

	protected TreeItem[] findUnfreshNodes(TreeItem[] items, Object obj, boolean exact) {
		List<TreeItem> retVal = findUnfreshNodesImpl(items, obj, exact);
		return retVal == null ? null : (TreeItem[]) retVal.toArray(new TreeItem[retVal.size()]);
	}

	protected List<TreeItem> findUnfreshNodes(TreeItem item, Object obj, boolean exact) {
		Object data = item.getData();
		if (obj == data || !exact && obj.equals(data)) {
			return Arrays.asList(item);
		}
		if (data instanceof IDataTreeNode) {
			IDataTreeNode dataNode = (IDataTreeNode) data;
			if (obj == dataNode.getData() || !exact && obj.equals(dataNode.getData())) {
				return Arrays.asList(item);
			}
		}
		return findUnfreshNodesImpl(item.getItems(), obj, exact);
	}

	protected List<TreeItem> findUnfreshNodesImpl(TreeItem[] items, Object obj, boolean exact) {
		if (items != null) {
			List<TreeItem> retVal = new ArrayList<>();
			for (TreeItem item : items) {
				List<TreeItem> tmp = this.findUnfreshNodes(item, obj, exact);
				if (tmp != null) {
					retVal.addAll(tmp);
				}
			}
			return retVal;
		}
		return null;
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		Object node = selection.getFirstElement();
		if (node instanceof IParentTreeNode) {
			TreeItem[] items = getIdenticalNodes(node, true);
			if (items != null && items.length > 0) {
				boolean expanded = !getExpanded(items[0]);
				if (expanded) {
					createChildren(items[0]);
				}
				setExpanded(items[0], expanded);
			}
		}
	}

	private void initialize() {
		addDoubleClickListener(e -> {
			ISelection selection = e.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structured = (IStructuredSelection) selection;
				if (structured.size() == 1) {
					RepositoryTreeViewer.this.handleDoubleClick(structured);
				}
			}
		});

		addDragSupport(DND.DROP_COPY | DND.DROP_NONE | DND.DROP_MOVE | DND.DROP_LINK,
				new Transfer[] { RemoteResourceTransfer.getInstance() }, new TransferDragSourceListener() {

					@Override
					public void dragFinished(DragSourceEvent event) {
					}

					@Override
					public void dragSetData(DragSourceEvent event) {
						if (RemoteResourceTransfer.getInstance().isSupportedType(event.dataType)) {
							IStructuredSelection selection = (IStructuredSelection) RepositoryTreeViewer.this
									.getSelection();
							ArrayList<IRepositoryResource> resources = new ArrayList<>();
							for (Iterator<?> it = selection.iterator(); it.hasNext();) {
								resources.add(((RepositoryResource) it.next()).getRepositoryResource());
							}
							event.data = new RemoteResourceTransferrable(resources.toArray(new IRepositoryResource[0]),
									0);
						}
					}

					@Override
					public void dragStart(DragSourceEvent event) {
						IStructuredSelection selection = (IStructuredSelection) RepositoryTreeViewer.this
								.getSelection();
						boolean canBeDragged = selection.size() > 0;
						for (Iterator<?> it = selection.iterator(); it.hasNext();) {
							if (!(it.next() instanceof RepositoryResource)) {
								canBeDragged = false;
							}
						}
						event.doit = canBeDragged;
					}

					@Override
					public Transfer getTransfer() {
						return RemoteResourceTransfer.getInstance();
					}

				});

		addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] { RemoteResourceTransfer.getInstance() },
				new DropTargetAdapter() {

					protected int expectedOperation = DND.DROP_MOVE;

					@Override
					public void dragOperationChanged(DropTargetEvent event) {
						expectedOperation = event.detail;
					}

					@Override
					public void dragEnter(DropTargetEvent event) {
						expectedOperation = event.detail;
					}

					@Override
					public void dragOver(DropTargetEvent event) {
						Tree repositoryTree = (Tree) ((DropTarget) event.widget).getControl();
						TreeItem aboveItem = repositoryTree.getItem(repositoryTree.toControl(event.x, event.y));
						if (aboveItem == null) {
							event.detail = DND.DROP_NONE;
							return;
						}
						Object aboveObject = aboveItem.getData();
						if (!(aboveObject instanceof RepositoryResource) || aboveObject instanceof RepositoryFile) {
							event.detail = DND.DROP_NONE;
							return;
						}
						RepositoryResource aboveResource = (RepositoryResource) aboveObject;
						if (aboveResource.getRepositoryResource().getSelectedRevision() != SVNRevision.HEAD) {
							event.detail = DND.DROP_NONE;
							return;
						}
						IStructuredSelection selection = (IStructuredSelection) RepositoryTreeViewer.this
								.getSelection();
						int lastSlashIdx = 0;
						for (Iterator<?> it = selection.iterator(); it.hasNext();) {
							RepositoryResource current = (RepositoryResource) it.next();
							if (lastSlashIdx == 0) {
								lastSlashIdx = current.getRepositoryResource().getUrl().lastIndexOf("/"); //$NON-NLS-1$
							}
							if (current.getRepositoryResource().getUrl().lastIndexOf("/") != lastSlashIdx //$NON-NLS-1$
									|| aboveResource == current || aboveResource == current.getParent()) {
								event.detail = DND.DROP_NONE;
								return;
							}
						}
						event.detail = expectedOperation;
					}

					@Override
					public void drop(DropTargetEvent event) {
						Tree repositoryTree = (Tree) ((DropTarget) event.widget).getControl();
						RepositoryResource aboveResource = (RepositoryResource) repositoryTree
								.getItem(repositoryTree.toControl(event.x, event.y))
								.getData();
						CommentPanel commentPanel = new CommentPanel(event.detail == DND.DROP_MOVE
								? SVNUIMessages.MoveToAction_Select_Title
								: SVNUIMessages.CopyToAction_Select_Title);
						DefaultDialog dialog = new DefaultDialog(UIMonitorUtility.getShell(), commentPanel);
						if (dialog.open() == IDialogConstants.OK_ID) {
							AbstractCopyMoveResourcesOperation mainOp = event.detail == DND.DROP_MOVE
									? new MoveResourcesOperation(aboveResource.getRepositoryResource(),
											((RemoteResourceTransferrable) event.data).resources,
											commentPanel.getMessage(), null)
									: new CopyResourcesOperation(aboveResource.getRepositoryResource(),
											((RemoteResourceTransferrable) event.data).resources,
											commentPanel.getMessage(), null);
							CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
							op.add(mainOp);
							ArrayList<IRepositoryResource> toRefresh = new ArrayList<>();
							toRefresh.add(aboveResource.getRepositoryResource());
							if (event.detail == DND.DROP_MOVE) {
								toRefresh.addAll(Arrays.asList(((RemoteResourceTransferrable) event.data).resources));
							}
							op.add(new RefreshRemoteResourcesOperation(
									SVNUtility.getCommonParents(toRefresh.toArray(new IRepositoryResource[0]))),
									new IActionOperation[] { mainOp });
							ProgressMonitorUtility.doTaskScheduled(op);
						}
					}

				});

		getTree().addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseHover(MouseEvent e) {
				String tooltipText = ""; //$NON-NLS-1$
				Tree tree = RepositoryTreeViewer.this.getTree();
				TreeItem item = tree.getItem(new Point(e.x, e.y));
				if (item != null) {
					Object data = item.getData();
					if (data != null) {
						if (data instanceof IToolTipProvider) {
							tooltipText = ((IToolTipProvider) data)
									.getToolTipMessage(RepositoryTreeViewer.class2Format.get(data.getClass()));
						}
					}
				}
				tree.setToolTipText(tooltipText);
			}

			@Override
			public void mouseExit(MouseEvent e) {
				RepositoryTreeViewer.this.getTree().setToolTipText(""); //$NON-NLS-1$
			}
		});
	}

}
