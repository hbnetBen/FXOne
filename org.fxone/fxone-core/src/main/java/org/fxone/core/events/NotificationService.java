package org.fxone.core.events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fxone.core.annot.NotificationExtension;
import org.fxone.core.types.AnnotationManager;

public final class NotificationService {

	private Logger LOGGER = Logger.getLogger(NotificationService.class);

	private static final String DEFAULT_TARGET = "_default";

	private static NotificationService INSTANCE = new NotificationService();
	private Map<String, ExecutorService> eventQueues = new ConcurrentHashMap<String, ExecutorService>();
	private Map<String, List<NotificationListener>> listeners = new ConcurrentHashMap<String, List<NotificationListener>>();
	private List<NotificationConsumer> parsers = new ArrayList<NotificationConsumer>();

	private boolean started;

	@SuppressWarnings("rawtypes")
	private Map<Class, NotificationProvider> notificationProvider = new HashMap<Class, NotificationProvider>();

	private NotificationService() {
		// Load notification providers
		Set<String> classNames = AnnotationManager
				.getAnnotatedClasses(NotificationExtension.class.getName());
		for (String className : classNames) {
			try {
				Class<?> clazz = Class.forName(className);
				if (NotificationListener.class.isAssignableFrom(clazz)) {
					NotificationListener l = (NotificationListener) clazz
							.newInstance();
					addListener(l);
				} else if (NotificationConsumer.class.isAssignableFrom(clazz)) {
					NotificationConsumer p = (NotificationConsumer) clazz
							.newInstance();
					addConsumer(p);
				}
			} catch (Exception e) {
				Logger.getLogger(getClass()).error(
						"Failed to load/access NotificationExtension: "
								+ className, e);
			}
		}
		start();
		// init default targets
		Enumeration<NotificationGroup> groups = NotificationGroup.getGroups();
		while (groups.hasMoreElements()) {
			NotificationGroup group = groups.nextElement();
			defineTarget(group.getName());
		}
	}

	public static final NotificationService get() {
		return INSTANCE;
	}

	private ExecutorService createExecutorService() {
		return java.util.concurrent.Executors.newCachedThreadPool();
	}

	public void deleteAllEvents(String target) throws IOException {
		ExecutorService service = eventQueues.put(target,
				createExecutorService());
		if (service != null) {
			service.shutdownNow();
		}
	}

	public String[] getTargets() throws Exception {
		return eventQueues.keySet().toArray(new String[eventQueues.size()]);
	}

	public void removeListener(NotificationListener l, String target) {
		if (l == null) {
			throw new IllegalArgumentException(
					"MessageEvent Listener can not be null.");
		}
		Collection<NotificationListener> found = listeners.get(DEFAULT_TARGET);
		if (found != null) {
			found.remove(l);
		}
		if (target == null) {
			return;
		}
		if (!DEFAULT_TARGET.equals(target)) {
			found = listeners.get(target);
			if (found != null) {
				found.remove(l);
			}
		}
	}

	public void removeListener(NotificationListener l) {
		if (l == null) {
			throw new IllegalArgumentException(
					"MessageEvent Listener can not be null.");
		}
		for (Collection<NotificationListener> list : listeners.values()) {
			list.remove(l);
		}
	}

	public void addListener(NotificationListener l) {
		addListener(l, null);
	}

	public void addListener(NotificationListener l, String target) {
		if (l == null) {
			throw new IllegalArgumentException(
					"MessageEvent Listener can not be null.");
		}
		if (target == null) {
			target = DEFAULT_TARGET;
		}
		List<NotificationListener> found = listeners.get(target);
		if (found == null) {
			found = new ArrayList<NotificationListener>();
			listeners.put(target, found);
		}
		if (!found.contains(l)) {
			found.add(l);
		}
	}

	public void addConsumer(NotificationConsumer p) {
		if (p == null) {
			throw new IllegalArgumentException("Parser can not be null.");
		}
		synchronized (parsers) {
			if (!parsers.contains(p)) {
				parsers.add(p);
				LOGGER.info("Registered AbstractNotification Parser: " + p);
			}
		}
	}

	public void removeParser(NotificationConsumer p) {
		synchronized (parsers) {
			parsers.remove(p);
		}
	}

	public <T extends AbstractNotification> Future<T> publishEvent(final T event,
			Class<T> type) {
		final String groupName = event.getGroup();
		ExecutorService service = this.eventQueues.get(groupName);
		if (service == null) {
			defineTarget(groupName);
			service = this.eventQueues
					.get(groupName);
		}
		return service.submit(new Callable<T>() {
			public T call() {
				try {
					final Collection<NotificationListener> targets = listeners
							.get(groupName);
					if (targets != null) {
						notifyTargets(event, targets);
					}
					final Collection<NotificationListener> defTargets = listeners
							.get(DEFAULT_TARGET);
					if (defTargets != null) {
						notifyTargets(event, defTargets);
					}
				} finally {
					event.setCompleted();
				}
				return event;
			}

			protected void notifyTargets(final AbstractNotification event,
					Collection<NotificationListener> targets) {
				synchronized (targets) {
					for (NotificationListener l : targets) {
						try {
							l.notified(event);
						} catch (Exception e) {
							LOGGER.error("Error providing message to " + l, e);
						}
					}
				}
			}
		});
	}

	public Future<?> submit(Runnable runnable) {
		ExecutorService service = this.eventQueues.get(DEFAULT_TARGET);
		if (service == null) {
			throw new IllegalArgumentException("Undefined target:"
					+ DEFAULT_TARGET);
		}
		return service.submit(runnable);
	}

	public void start() {
		if (!started) {
			LOGGER.info("Starting AbstractNotification Service...");

			// Load notification provider
			Set<String> annotatedClasses = AnnotationManager
					.getAnnotatedClasses(NotificationExtension.class.getName());
			started = true;
			LOGGER.info("AbstractNotification Service started.");
		}
	}

	public void stop() {
		if (started) {
			LOGGER.info("Stopping AbstractNotification Service...");
			String[] keys = this.eventQueues.keySet().toArray(new String[0]);
			for (String target : keys) {
				ExecutorService service = this.eventQueues.remove(target);
				if (service != null) {
					service.shutdown();
					try {
						service.awaitTermination(2L, TimeUnit.MINUTES);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			started = false;
			LOGGER.info("AbstractNotification Service stopped.");
		}
	}

	public synchronized void defineTarget(String target) {
		if (!started) {
			throw new IllegalStateException("MessageBroker is not started.");
		}
		if (this.eventQueues.containsKey(target)) {
			return;
		}
		this.eventQueues.put(target, createExecutorService());
		LOGGER.info("Defined AbstractNotification Service target: " + target);
	}

	public synchronized void undefineTarget(String target) {
		ExecutorService service = this.eventQueues.remove(target);
		if (service != null) {
			service.shutdown();
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends NotificationProvider> T getNotificationProvider(
			Class<T> providerType) {
		return (T) this.notificationProvider.get(providerType);
	}

	public Future<AbstractNotification> send(Object owner, String notification) {
		for (NotificationConsumer parser : this.parsers) {
			try {
				AbstractNotification notif = parser.parseNotification(owner,
						notification);
				if (notif != null) {
					return publishEvent(notif, AbstractNotification.class);
				}
			} catch (Exception e) {
				LOGGER.error("Error parsing notification using: " + parser, e);
			}
		}
		return null;
	}

}
