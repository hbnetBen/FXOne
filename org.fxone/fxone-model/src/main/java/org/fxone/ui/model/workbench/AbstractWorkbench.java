package org.fxone.ui.model.workbench;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.Instance;
import javax.inject.Named;

import org.apache.log4j.Logger;

public abstract class AbstractWorkbench implements Workbench {

	protected static final String DEFAULT_STYLE_ID = "default";
	@SuppressWarnings("rawtypes")
	protected final Map<String, Perspective> perspectives = new ConcurrentHashMap<String, Perspective>();
	private Perspective currentPerspective;
	private final Logger log = Logger.getLogger(getClass());

	public AbstractWorkbench(Instance<Perspective> perspectives) {
		for (Perspective perspective : perspectives) {
			Named annot = perspective.getClass().getAnnotation(
					Named.class);
			String perspectiveName = perspective.getClass().getSimpleName();
			if (annot != null) {
				perspectiveName = annot.value();
			}
			this.perspectives.put(perspectiveName, perspective);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration<Perspective> getPerspectives() {
		return Collections.enumeration(perspectives.values());
	}

	@Override
	public Perspective getCurrentPerspective() {
		return currentPerspective;
	}

	@Override
	public boolean setCurrentPerspective(String persepectiveID) {
		Perspective perspectiveInstance = perspectives.get(persepectiveID);
		if (perspectiveInstance == getCurrentPerspective()) {
			return true;
		}
		if (perspectiveInstance != null) {
			Perspective current = getCurrentPerspective();
			this.currentPerspective = perspectiveInstance;
			applyPerspective(perspectiveInstance);
			if (current != null) {
				current.deactivated(this);
			}
			perspectiveInstance.activated(this);
		} else {
			log.warn("Failed to apply style (not found): " + persepectiveID);
		}
		return false;
	}


	@Override
	public void setDefaultPerspective() {
		setCurrentPerspective(DEFAULT_STYLE_ID);
	}

	protected abstract void applyPerspective(Perspective perspective);

}
