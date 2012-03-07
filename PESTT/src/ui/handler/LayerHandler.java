package ui.handler;

import main.activator.Activator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.RadioState;

import domain.constants.Description;
import domain.constants.Messages;

public class LayerHandler extends AbstractHandler {

	String option = Description.EMPTY;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if(Activator.getDefault().getSourceGraphController().isGraphDisplayed()) {
			option = event.getParameter(RadioState.PARAMETER_ID); // get the current selected state.
			if(!option.equals(Description.NONE)) {
				HandlerUtil.updateRadioState(event.getCommand(), option); // update the current state.
				Activator.getDefault().getCFGController().selectLayer(option);
			}
		} else {
			IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			MessageDialog.openInformation(window.getShell(), Messages.DRAW_GRAPH_TITLE, Messages.DRAW_GRAPH_MSG); // message displayed when the graph is not designed.
		}
		return null;
	}
}