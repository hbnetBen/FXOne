package org.fxone.ui.rt.nav;

import org.fxone.core.cdi.Container;
import org.fxone.ui.annot.UINavigationCommand;
import org.fxone.ui.model.Model;
import org.fxone.ui.model.nav.AbstractUIAction;
import org.fxone.ui.model.view.View;

@UINavigationCommand("help/about")
public class GoAbout extends AbstractUIAction {

	@Override
	public void run() {
		View view = Container.getNamedInstance(View.class,  "about");
		if(view!=null){
			Model.Views.openView(view, "default");
		}
	}

}
