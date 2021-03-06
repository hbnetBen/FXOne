package org.fxone.ui.model.nav.impl;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Singleton;

import org.apache.log4j.Logger;
import org.fxone.core.events.AbstractNotification;
import org.fxone.core.events.NotificationService;
import org.fxone.ui.model.nav.NavigationHistory;
import org.fxone.ui.model.nav.cmd.ClearNavigationHistory;
import org.fxone.ui.model.nav.cmd.NavigateTo;
import org.fxone.ui.model.nav.cmd.PrintNavigationHistory;

@Singleton
public class NavigationHistoryImpl implements NavigationHistory {

	private static final Logger LOGGER = Logger
			.getLogger(NavigationHistoryImpl.class);

	private List<HistoryItem> history = new LinkedList<HistoryItem>();
	private int pos = 0;

	/**
	 * Got to previous page in history
	 */
	public void back() {
		synchronized (history) {
			if (isBackEnabled()) {
				perform(history.get(--pos));
			} 
		}
	}

	/**
	 * Go to next page in history if there is one
	 */
	public void forward() {
		synchronized (history) {
			if (isForwardEnabled()) {
				perform(history.get(++pos));
			}
		}
	}

	private void perform(HistoryItem cmd) {
		LOGGER.info("Performing command: " + cmd.cmd);
		NotificationService.get().publishEvent(cmd.cmd, NavigateTo.class);
	}

	/**
	 * Utility method for viewing the page history
	 */
	private void printHistory() {
		System.out.println("NAVIGATION HISTORY:");
		synchronized (history) {
			for (HistoryItem histItem : history) {
				System.out.println(histItem.title + "->" + histItem.cmd);
			}
		}
	}

	private static final class HistoryItem {
		String title;
		NavigateTo cmd;
	}

	public NavigateTo removeNavigation(int pos) {
		synchronized (history) {
			NavigateTo cmd = this.history.remove(pos).cmd;
			if (pos >= history.size()) {
				pos = history.size() - 1;
			}
			return cmd;
		}
	}

	@Override
	public boolean isForwardEnabled() {
		synchronized (history) {
			return pos < (history.size() - 1);
		}
	}

	@Override
	public boolean isBackEnabled() {
		synchronized (history) {
			return pos > 0;
		}
	}

	@Override
	public void addNotification(NavigateTo cmd, String title) {
		synchronized (history) {
			if (history.size() > pos) {
				int start = pos + 1;
				for (int i = start; i < history.size(); i++) {
					history.remove(start);
				}
			}
			HistoryItem item = new HistoryItem();
			item.cmd = cmd;
			item.title = title;
			history.add(item);
			pos++;
		}
	}

	@Override
	public void clearHistory() {
		synchronized (history) {
			this.history.clear();
			pos = 0;
		}
	}

	@Override
	public int getSize() {
		return this.history.size();
	}

	@Override
	public int getIndex() {
		return pos;
	}

	@Override
	public String getNotificationTitle(int pos) {
		synchronized (history) {
			return history.get(pos).title;
		}
	}

	@Override
	public NavigateTo getNotification(int pos) {
		synchronized (history) {
			return history.get(pos).cmd;
		}
	}

	public void notified(AbstractNotification evt) {
		if (NavigateTo.NOTIFTYPE_NAVIGATE_TO.isMatching(evt)) {
			NavigateTo nav = (NavigateTo)evt;
			if("_back".equals(nav.getNavigationTarget())){
				back();
			}
			if("_next".equals(nav.getNavigationTarget())){
				forward();
			}
		} else if (ClearNavigationHistory.NOTIFTYPE.isMatching(evt)) {
			clearHistory();
		} else if (PrintNavigationHistory.NOTIFTYPE.isMatching(evt)) {
			printHistory();
		}
	}

}
