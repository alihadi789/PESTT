package domain.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import main.activator.Activator;

import org.eclipse.jdt.core.dom.ASTNode;

import ui.constants.Colors;
import ui.editor.Line;
import ui.events.TourChangeEvent;
import adt.graph.Graph;
import adt.graph.Node;
import adt.graph.Path;
import domain.TestPathSet;
import domain.constants.Layer;
import domain.constants.TestType;
import domain.constants.TourType;
import domain.coverage.data.CoverageData;
import domain.coverage.data.ICoverageData;
import domain.events.TestPathSelectedEvent;

public class TestPathController extends Observable {

	private TestPathSet testPathSet;
	private Set<Path> selectedTestPaths;
	private TourType selectedTourType;
	private Map<Path, String> tooltips;

	public TestPathController(TestPathSet testPathSet) {
		this.testPathSet = testPathSet;
		tooltips = new HashMap<Path, String>();
	}

	public void addObserverTestPath(Observer o) {
		testPathSet.addObserver(o);
	}

	public void deleteObserverTestPath(Observer o) {
		testPathSet.deleteObserver(o);
	}

	public void addTestPath(Path newTestPath, String tooltip) {
		insertTooltip(newTestPath, tooltip);
		testPathSet.add(newTestPath);
		List<ICoverageData> newData = new LinkedList<ICoverageData>();
		newData.add(new CoverageData(newTestPath));
		Activator.getDefault().getCoverageDataController()
				.addCoverageData(newTestPath, newData);
		unSelectTestPaths();
	}

	public void addAutomaticTestPath(Path newTestPath, String tooltip) {
		boolean update = insertTooltip(newTestPath, tooltip);
		if (!update) {
			Activator.getDefault().getEditorController()
					.setListenUpdates(false);
			testPathSet.addAutomatic(newTestPath);
			Activator.getDefault().getEditorController().setListenUpdates(true);
		} else
			testPathSet.addAutomatic(newTestPath);
		List<ICoverageData> newData = new LinkedList<ICoverageData>();
		newData.add(new CoverageData(newTestPath));
		Activator.getDefault().getCoverageDataController()
				.addCoverageData(newTestPath, newData);
		unSelectTestPaths();
	}

	private boolean insertTooltip(Path path, String tooltip) {
		boolean update = false;
		Path remove = null;
		for (Path current : tooltips.keySet())
			if (current.toString().equals(path.toString())) {
				if (!tooltip.equals(TestType.MANUALLY))
					if (testPathSet.isManuallyAdded(current)) {
						Set<Path> set = new TreeSet<Path>();
						set.add(current);
						testPathSet.remove(set);
						remove = current;
						update = true;
						break;
					}
				path = current;
				break;
			}
		if (remove != null)
			tooltips.remove(remove);
		tooltips.put(path, tooltip);
		return update;
	}

	public String getTooltip(Path path) {
		return tooltips.get(path);
	}

	public void removeTestPath() {
		for (Path path : selectedTestPaths) {
			Activator.getDefault().getCoverageDataController()
					.removeSelectedCoverageData(path);
			tooltips.remove(path);
		}
		testPathSet.remove(selectedTestPaths);
		unSelectTestPaths();
	}

	public void clearAutomaticTestPaths() {
		for (Path path : testPathSet.getTestPaths())
			tooltips.remove(path);
		testPathSet.clearAutomatic();
	}

	public void clearManuallyTestPaths() {
		for (Path path : testPathSet.getTestPathsManuallyAdded())
			tooltips.remove(path);
		testPathSet.clearManually();
	}

	public void cleanTestPathSet() {
		testPathSet.clearAll();
		tooltips.clear();
	}

	public boolean isTestPathSelected() {
		return selectedTestPaths != null;
	}

	public Set<Path> getSelectedTestPaths() {
		return selectedTestPaths;
	}

	public void selectTestPath(Set<Path> selectedTestPaths) {
		this.selectedTestPaths = selectedTestPaths;
		setChanged();
		notifyObservers(new TestPathSelectedEvent(selectedTestPaths));
	}

	public void unSelectTestPaths() {
		selectTestPath(null);
	}

	public TourType getSelectedTourType() {
		return selectedTourType;
	}

	public void selectTourType(String selected) {
		if (selected.equals(TourType.DETOUR.toString()))
			this.selectedTourType = TourType.DETOUR;
		else if (selected.equals(TourType.SIDETRIP.toString()))
			this.selectedTourType = TourType.SIDETRIP;
		else
			this.selectedTourType = TourType.TOUR;
		setChanged();
		notifyObservers(new TourChangeEvent(selectedTourType));
	}

	public Iterable<Path> getTestPathsManuallyAdded() {
		return testPathSet.getTestPathsManuallyAdded();
	}

	public Iterable<Path> getTestPaths() {
		return testPathSet.getTestPaths();
	}

	public void getStatistics() {
		Activator.getDefault().getStatisticsController().getStatistics(selectedTestPaths);
	}

	public Set<Path> getTestRequirementCoverage() {
		Set<Path> total = new TreeSet<Path>();
		for (Path path : selectedTestPaths) {
			Set<Path> coveredPaths = Activator.getDefault()
					.getTestRequirementController().getTestPathCoverage(path);
			for (Path p : coveredPaths)
				if (!total.contains(p))
					total.add(p);
		}
		return total;
	}

	@SuppressWarnings("unchecked")
	public ICoverageData getCoverageData() {
		Graph sourceGraph = Activator.getDefault()
				.getSourceGraphController().getSourceGraph();
		LinkedHashMap<Integer, String> coverageData = new LinkedHashMap<Integer, String>();
		List<Integer> lines = new LinkedList<Integer>();
		sourceGraph.selectMetadataLayer(Layer.INSTRUCTIONS.getLayer()); // select the layer to get the information.
		for (Node node : sourceGraph.getNodes()) {
			Map<ASTNode, Line> map = (LinkedHashMap<ASTNode, Line>) sourceGraph
					.getMetadata(node); // get the information in this layer to this node.
			if (map != null)
				for (Entry<ASTNode, Line> entry : map.entrySet()) {
					int line = entry.getValue().getStartLine();
					for (Path path : selectedTestPaths) {
						ICoverageData data = Activator.getDefault()
								.getCoverageDataController()
								.getCoverageData(path);
						if (data.getLineStatus(line).equals(Colors.GREEN_ID))
							if (!lines.contains(line))
								lines.add(line);

						if (!coverageData.containsKey(line))
							coverageData.put(line, Colors.RED_ID);
					}
				}
		}

		for (int line : lines) {
			coverageData.remove(line);
			coverageData.put(line, Colors.GREEN_ID);
		}

		return new CoverageData(coverageData);
	}

	public Path createTestPath(String input) {
		Graph sourceGraph = Activator.getDefault()
				.getSourceGraphController().getSourceGraph();
		boolean validPath = true;
		List<String> insertedNodes = getInsertedNodes(input);
		List<Node> pathNodes = new LinkedList<Node>();
		try {
			List<Node> fromToNodes = new ArrayList<Node>();
			fromToNodes.add(sourceGraph.getNode(Integer.parseInt(insertedNodes
					.get(0))));
			int i = 1;
			while (i < insertedNodes.size() && validPath) {
				fromToNodes.add(sourceGraph.getNode(Integer
						.parseInt(insertedNodes.get(i))));
				if (fromToNodes.get(0) != null && fromToNodes.get(1) != null
						&& sourceGraph.isPath(new Path(fromToNodes))) {
					pathNodes.add(fromToNodes.get(0));
					fromToNodes.remove(0);
				} else
					validPath = false;
				i++;
			}
			if (validPath) {
				pathNodes.add(fromToNodes.get(0));

				if (!sourceGraph.isInitialNode(pathNodes.get(0))
						|| !sourceGraph.isFinalNode(pathNodes.get(pathNodes
								.size() - 1)))
					return null;//!= null check

				return new Path(pathNodes);
			}
		} catch (NumberFormatException e) {
			//ignore
		}
		return null;//!= null check
	}

	private List<String> getInsertedNodes(String input) {
		List<String> aux = new LinkedList<String>();
		StringTokenizer strtok = new StringTokenizer(input, ", ");
		// separate the inserted nodes.
		while (strtok.hasMoreTokens())
			aux.add(strtok.nextToken());
		return aux;
	}
}