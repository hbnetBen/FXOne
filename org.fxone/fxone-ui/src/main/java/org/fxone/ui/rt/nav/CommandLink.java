package org.fxone.ui.rt.nav;

import java.util.Locale;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Hyperlink;

import org.fxone.core.cdi.Container;
import org.fxone.ui.model.nav.NavigateableAction;
import org.fxone.ui.model.res.ResourceProvider;

public class CommandLink extends Hyperlink {

	public CommandLink(final NavigateableAction cmd) {
		setText(Container.getInstance(ResourceProvider.class).getName(cmd.getClass(), Locale.ENGLISH)); // TODO i18n
		setUnderline(false);
		setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				cmd.run();
			}
		});
		setUserData(cmd);
	}

}