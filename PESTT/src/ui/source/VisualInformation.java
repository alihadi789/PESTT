package ui.source;

import static org.eclipse.jdt.core.dom.ASTNode.DO_STATEMENT;
import static org.eclipse.jdt.core.dom.ASTNode.ENHANCED_FOR_STATEMENT;
import static org.eclipse.jdt.core.dom.ASTNode.FOR_STATEMENT;
import static org.eclipse.jdt.core.dom.ASTNode.IF_STATEMENT;
import static org.eclipse.jdt.core.dom.ASTNode.SWITCH_STATEMENT;
import static org.eclipse.jdt.core.dom.ASTNode.TRY_STATEMENT;
import static org.eclipse.jdt.core.dom.ASTNode.WHILE_STATEMENT;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import main.activator.Activator;

import org.eclipse.core.resources.IFile;
import org.eclipse.gef4.zest.core.widgets.GraphConnection;
import org.eclipse.gef4.zest.core.widgets.GraphItem;
import org.eclipse.gef4.zest.core.widgets.GraphNode;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import ui.constants.Colors;
import ui.constants.Description;
import ui.constants.MarkersType;
import ui.controllers.EditorController;
import ui.editor.Line;
import domain.constants.Layer;
import domain.coverage.data.ICoverageData;

public class VisualInformation {

	private static final String ALL = "All";
	private static final String TRUE = "True";
	private static final String FALSE = "False";
	private adt.graph.Graph sourceGraph;
	private ui.source.Graph layoutGraph;
	private ISelectionListener listener;

	public VisualInformation(ui.source.Graph layoutGraph) {
		this.layoutGraph = layoutGraph;
	}

	public void setLayerInformation(Layer layer) {
		sourceGraph = Activator.getDefault().getSourceGraphController()
				.getSourceGraph(); // set the sourceGraph.
		switch (layer) {
		case INSTRUCTIONS:
			addInformationToLayer1();
			break;
		case EMPTY:
			clear();
			break;
		case GUARDS:
			addInformationToLayers2_3_4(ALL);
			break;
		case GUARDS_TRUE:
			addInformationToLayers2_3_4(TRUE);
			break;
		case GUARDS_FALSE:
			addInformationToLayers2_3_4(FALSE);
			break;
		}
	}

	private void clear() {
		sourceGraph.selectMetadataLayer(Layer.EMPTY.getLayer()); // change to the empty layer.
		removeVisualCoverage(); // removes the marks in the editor.
		for (adt.graph.Node node : sourceGraph.getNodes())
			// search in the sourceGraph for all node.
			for (adt.graph.Edge edge : sourceGraph.getNodeEdges(node))
				// search in the sourceGraph for all edges.
				for (GraphConnection gconnection : layoutGraph.getGraphEdges())
					// search in the layoutGraph for all edges.
					if (gconnection.getData().equals(edge)) { // when they match.
						gconnection.setText(Description.EMPTY); // clear the visible information.
						break;
					}
	}

	@SuppressWarnings("unchecked")
	private void addInformationToLayer1() {
		removeVisualCoverage(); // removes the marks in the editor.
		if (!layoutGraph.getSelected().isEmpty()) // verify if there are nodes selected.
			for (GraphItem item : layoutGraph.getSelected())
				// through all graph items.
				if (item instanceof GraphNode) { // verify if is a GraphNode.
					adt.graph.Node node = sourceGraph.getNode(Integer
							.parseInt(item.getText())); // get the node.
					sourceGraph.selectMetadataLayer(Layer.INSTRUCTIONS
							.getLayer()); // select the layer to get the information.
					HashMap<ASTNode, Line> map = (HashMap<ASTNode, Line>) sourceGraph
							.getMetadata(node); // get the information in this layer to this node.
					if (map != null) {
						List<ASTNode> instructions = getASTNodes(map);
						if (instructions != null)
							regionToSelect(instructions,
									MarkersType.LINK_MARKER); // select the area in the editor.
					}
				} else if (item instanceof GraphConnection) {
					adt.graph.Node node = sourceGraph.getNode(Integer
							.parseInt(((GraphConnection) item).getSource()
									.getText()));
					sourceGraph.selectMetadataLayer(Layer.INSTRUCTIONS
							.getLayer()); // select the layer to get the information.
					HashMap<ASTNode, Line> map = (HashMap<ASTNode, Line>) sourceGraph
							.getMetadata(node); // get the information in this layer to this node.
					if (map != null) {
						List<ASTNode> instructions = getASTNodes(map);
						if (instructions != null
								&& isProgramStatement(instructions)) {
							List<ASTNode> exp = getExpression(instructions);
							regionToSelect(exp, MarkersType.LINK_MARKER); // select the area in the editor.
						}
					}
				}
	}

	private List<ASTNode> getExpression(List<ASTNode> instructions) {
		List<ASTNode> exps = new ArrayList<ASTNode>();
		ASTNode instruction = instructions.get(0);
		switch (instruction.getNodeType()) {
		case IF_STATEMENT:
			IfStatement ifExp = (IfStatement) instruction;
			exps.add((ASTNode) ifExp.getExpression());
			break;
		case DO_STATEMENT:
			DoStatement doExp = (DoStatement) instruction;
			exps.add((ASTNode) doExp.getExpression());
			break;
		case FOR_STATEMENT:
			ForStatement forExp = (ForStatement) instruction;
			exps.add((ASTNode) forExp.getExpression());
			break;
		case ENHANCED_FOR_STATEMENT:
			EnhancedForStatement forEachExp = (EnhancedForStatement) instruction;
			exps.add((ASTNode) forEachExp.getExpression());
			break;
		case SWITCH_STATEMENT:
			SwitchStatement switchExp = (SwitchStatement) instruction;
			exps.add((ASTNode) switchExp.getExpression());
			break;
		case WHILE_STATEMENT:
			WhileStatement whileExp = (WhileStatement) instruction;
			exps.add((ASTNode) whileExp.getExpression());
			break;
		}
		return exps;
	}

	private void addInformationToLayers2_3_4(String value) {
		setLayerInformation(Layer.EMPTY); // clean previous informations.
		for (adt.graph.Node node : sourceGraph.getNodes())
			// search in the sourceGraph for all node.
			for (adt.graph.Edge edge : sourceGraph.getNodeEdges(node))
				// search in the sourceGraph for all edges.
				for (GraphConnection gconnection : layoutGraph.getGraphEdges())
					// search in the layoutGraph for all edges.
					if (gconnection.getData().equals(edge)) { // when they match.
						sourceGraph
								.selectMetadataLayer(Layer.GUARDS.getLayer()); // change to the layer with the cycles information.
						String info = (String) sourceGraph.getMetadata(edge); // get the information.
						if (info != null)
							if (!value.equals(ALL)) {
								if (info.equals("break;")
										|| info.equals("continue;")
										|| info.equals("return;"))
									gconnection.setText(info);
								else if (value.equals(TRUE)
										&& !info.substring(0, 1).equals("��"))
									gconnection.setText(info); // set the information to the edge.
								else if (value.equals(FALSE)
										&& info.substring(0, 1).equals("��"))
									gconnection.setText(info); // set the information to the edge.
							} else
								gconnection.setText(info); // set the information to the edge.
						break;
					}
	}

	private List<ASTNode> getASTNodes(HashMap<ASTNode, Line> map) {
		List<ASTNode> nodes = new LinkedList<ASTNode>();
		for (Entry<ASTNode, Line> entry : map.entrySet())
			nodes.add(entry.getKey());
		return nodes;
	}

	private void regionToSelect(List<ASTNode> instructions, String markerType) {
		int start = findStartPosition(instructions); // get the start position.
		int length = 0;
		ASTNode instruction = instructions.get(0); // the first element of the list.
		EditorController ec = Activator.getDefault().getEditorController();
		boolean def = false;
		switch (instruction.getNodeType()) {
		case DO_STATEMENT:
			DoStatement d = (DoStatement) instruction;
			String text = getText(d.getExpression().getStartPosition());
			int w = findWhile(text);
			start = w - ("while".length() - 1);
			length = "while".length();
			break;
		case IF_STATEMENT:
			length = getLength(start, instruction.getStartPosition(),
					"if".length());
			break;
		case FOR_STATEMENT:
		case ENHANCED_FOR_STATEMENT:
			length = getLength(start, instruction.getStartPosition(),
					"for".length());
			break;
		case SWITCH_STATEMENT:
			length = getLength(start, instruction.getStartPosition(),
					"switch".length());
			break;
//		case CATCH_CLAUSE:
//			length = getLength(start, instruction.getStartPosition(),
//					"catch".length());
//			break;
		case TRY_STATEMENT:
			length = getLength(start, instruction.getStartPosition(),
					"try".length());
			break;
		case WHILE_STATEMENT:
			length = getLength(start, instruction.getStartPosition(),
					"while".length());
			break;
		default:
			def = true;
			for (ASTNode instr : instructions)
				ec.createMarker(
						markerType,
						instr.getStartPosition(),
						getLength(instr.getStartPosition(),
								instr.getStartPosition(), instr.getLength()));
			break;
		}
		if (!def)
			ec.createMarker(markerType, start, length);
	}

	/**
	 * Returns the text of the class where the ActiveEditor is, until character
	 * number 'limit'.
	 * 
	 * @param limit
	 * 
	 * @return
	 */
	private String getText(int limit) {
		IFile file = (IFile) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActiveEditor()
				.getEditorInput().getAdapter(IFile.class);
		StringBuilder buf = new StringBuilder();
		try {
			InputStreamReader i = new InputStreamReader(file.getContents());
			int x = 0;
			while (x != -1 && buf.length() <= limit) {
				x = i.read();
				buf.append((char) x);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buf.toString();
	}

	/**
	 * Finds a 'while' (its 'e') going backwards from its condition, which
	 * begins at index 'start'. Returns -1 if the while is not found (should not
	 * happen).
	 * 
	 * @param text
	 *            - the code to search.
	 * @return
	 */
	private int findWhile(String text) {
		for (int i = text.length() - 1; i > 0; i--) {
			char x = text.charAt(i);
			if (x == 'e')
				return i;
		}
		return -1;
	}

	public int findStartPosition(List<ASTNode> info) {
		int start = info.get(0).getStartPosition(); // the start position of the first element in the list.
		for (ASTNode instructions : info)
			// through all.
			if (instructions.getStartPosition() < start) // if the start position of the node begins first than the position stored.
				start = instructions.getStartPosition(); // update the start position.
		return start;
	}

	private int getLength(int start, int nodeStart, int len) {
		return ((nodeStart + len) - start); // calculates the length.
	}

	@SuppressWarnings("unchecked")
	public void addVisualCoverageStatus(ICoverageData data,
			List<GraphItem> items) {
		sourceGraph = Activator.getDefault().getSourceGraphController()
				.getSourceGraph(); // set the sourceGraph.
		removeVisualCoverage(); // removes the marks in the editor.
		for (adt.graph.Node node : sourceGraph.getNodes()) {
			sourceGraph.selectMetadataLayer(Layer.INSTRUCTIONS.getLayer()); // select the layer to get the information.
			HashMap<ASTNode, Line> map = (HashMap<ASTNode, Line>) sourceGraph
					.getMetadata(node); // get the information in this layer to this node.
			if (map != null) {
				List<ASTNode> instructions = getASTNodes(map);
				Entry<ASTNode, Line> entry = map.entrySet().iterator().next();
				String colorStatus = data.getLineStatus(entry.getValue()
						.getStartLine());
				if (colorStatus != null)
					if (colorStatus.equals(Colors.GREEN_ID)
							&& isNodeSelected(items, node)) {
						regionToSelect(instructions,
								MarkersType.FULL_COVERAGE_MARKER); // select the area in the editor.
						if (isProgramStatement(instructions)) {
							List<ASTNode> exp = getExpression(instructions);
							regionToSelect(exp,
									MarkersType.FULL_COVERAGE_MARKER);
						}
					} else if (colorStatus.equals(Colors.RED_ID)
							|| !isNodeSelected(items, node)) {
						regionToSelect(instructions,
								MarkersType.NO_COVERAGE_MARKER); // select the area in the editor.
						if (isProgramStatement(instructions)) {
							List<ASTNode> exp = getExpression(instructions);
							regionToSelect(exp, MarkersType.NO_COVERAGE_MARKER);
						}
					}
			}
		}
	}

	public void removeVisualCoverage() {
		Activator.getDefault().getEditorController().setListenUpdates(false);
		Activator.getDefault().getEditorController().removeALLMarkers(); // removes the marks in the editor.
		Activator.getDefault().getEditorController().setListenUpdates(true);
	}

	private boolean isNodeSelected(List<GraphItem> items,
			adt.graph.Node node) {
		for (GraphNode gnode : layoutGraph.getGraphNodes())
			// through all nodes in the graph.
			if (!gnode.isDisposed() && gnode.getData().equals(node)) // if matches.
				if (items.contains(gnode))
					return items.contains(gnode);
		return false;
	}

	public void createSelectToEditor() {
		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		// adding a listener
		listener = new ISelectionListener() {

			@Override
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (selection instanceof ITextSelection
						&& Activator.getDefault().getEditorController()
								.isEverythingMatching()) {
					ITextSelection textSelected = (ITextSelection) selection; // get the text selected in the editor.
					String currentMethod = getSelectedMethod(textSelected);
					if (currentMethod != null)
						if (textSelected.getLength() != 0
								&& currentMethod.equals(Activator.getDefault()
										.getEditorController()
										.getSelectedMethod())) {
							selectInGraph(textSelected.getOffset());
							return;
						}
					removeVisualCoverage();
				}
			}
		};
		page.addSelectionListener(listener);

	}

	public void removeSelectToEditor() {
		if (listener != null) {
			removeVisualCoverage();
			IWorkbenchPage page = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage(); // get the active page.
			page.removeSelectionListener(listener); // remove the listener.
		}
	}

	private String getSelectedMethod(ITextSelection textSelected) {
		try {
			for (IType type : Activator.getDefault().getEditorController()
					.getCompilationUnit().getAllTypes())
				for (IMethod method : type.getMethods()) {
					int cursorPosition = textSelected.getOffset();
					int methodStart = method.getSourceRange().getOffset();
					int methodEnd = method.getSourceRange().getOffset()
							+ method.getSourceRange().getLength();
					if (methodStart <= cursorPosition
							&& cursorPosition <= methodEnd)
						return method.getElementName();
				}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return null;//!= null checks
	}

	@SuppressWarnings("unchecked")
	private void selectInGraph(int position) {
		sourceGraph = Activator.getDefault().getSourceGraphController()
				.getSourceGraph(); // set the sourceGraph.
		List<GraphItem> aux = new LinkedList<GraphItem>(); // auxiliary list to store selected items.
		sourceGraph.selectMetadataLayer(Layer.INSTRUCTIONS.getLayer()); // select the layer to get the information.
		for (adt.graph.Node node : sourceGraph.getNodes()) { // through all nodes.
			HashMap<ASTNode, Line> map = (HashMap<ASTNode, Line>) sourceGraph
					.getMetadata(node); // get the information in this layer to this node.
			if (map != null) {
				List<ASTNode> instructions = getASTNodes(map);
				if (instructions != null && findNode(instructions, position)) // verify if it is the node.
					for (GraphNode gnode : layoutGraph.getGraphNodes())
						// through all GraphNodes.
						if (gnode.getData().equals(node)) // if matches with the node.
							aux.add(gnode); // adds the GraphNode to the list.
			}
		}
		if (aux.isEmpty()) {
			for (adt.graph.Node node : sourceGraph.getNodes())
				for (adt.graph.Edge edge : sourceGraph
						.getNodeEdges(node)) { // through all nodes.
					HashMap<ASTNode, Line> map = (HashMap<ASTNode, Line>) sourceGraph
							.getMetadata(node); // get the information in this layer to this node.
					if (map != null) {
						List<ASTNode> instructions = getASTNodes(map);
						if (instructions != null) {
							List<ASTNode> exp = getExpression(instructions);
							if (!exp.isEmpty() && findNode(exp, position))
								for (GraphConnection gconnection : layoutGraph
										.getGraphEdges())
									// through all GraphEdges.
									if (gconnection.getData().equals(edge)) // if matches with the edge.
										aux.add(gconnection); // adds the GraphEdge to the list.
						}
					}
				}
		}
		while (!aux.isEmpty() && aux.size() != 1)
			// one word is assigned to only one node.
			if (!(aux.get(0) instanceof GraphConnection))
				aux.remove(0); // remove all the others.
			else
				break;
		GraphItem[] items = Arrays.copyOf(aux.toArray(), aux.toArray().length,
				GraphItem[].class); // convert the aux into an array of GraphItems.
		layoutGraph.setSelected(items); // the list of selected items.
		setLayerInformation(Layer.INSTRUCTIONS); // set the information to the instructions layer.
	}

	private boolean findNode(List<ASTNode> instructions, int position) {
		int startPosition = findStartPosition(instructions); // get the start position.
		int endPosition = 0;
		ASTNode aNode = instructions.get(0); // the first element of the list.
		switch (aNode.getNodeType()) {
		case IF_STATEMENT:
		case DO_STATEMENT:
			endPosition = aNode.getStartPosition() + "if".length();
			break;
		case FOR_STATEMENT:
		case ENHANCED_FOR_STATEMENT:
			endPosition = aNode.getStartPosition() + "for".length();
			break;
		case SWITCH_STATEMENT:
			endPosition = aNode.getStartPosition() + "switch".length();
			break;
		case TRY_STATEMENT:
			endPosition = aNode.getStartPosition() + "try".length();
			break;
//		case CATCH_CLAUSE:
//			endPosition = aNode.getStartPosition() + "catch".length();
//			break;
		case WHILE_STATEMENT:
			endPosition = aNode.getStartPosition() + "while".length();
			break;
		default:
			if (aNode.getStartPosition() <= startPosition)
				endPosition = instructions.get(instructions.size() - 1)
						.getStartPosition()
						+ instructions.get(instructions.size() - 1).getLength(); // select the block of instructions associated to the selected node.
			else
				endPosition = aNode.getStartPosition() + aNode.getLength(); // select the block of instructions associated to the selected node.
			break;
		}
		if (startPosition <= position && position <= endPosition) // if a position is in his node.
			return true;
		return false;
	}

	private boolean isProgramStatement(List<ASTNode> ast) {
		switch (ast.get(0).getNodeType()) {
		case IF_STATEMENT:
		case DO_STATEMENT:
		case FOR_STATEMENT:
		case ENHANCED_FOR_STATEMENT:
		case SWITCH_STATEMENT:
		case WHILE_STATEMENT:
		case TRY_STATEMENT:
//		case CATCH_CLAUSE:
			return true;
		default:
			return false;
		}
	}
}