/*
 * Copyright 2012 david gonzalez.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.activecq.samples.eventhandlers;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.apache.sling.event.EventUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;


@Component(
        label = "Samples - JCR Event Listener",
        description = "Sample implementation of a low-level JCR Event Listner.",
        metatype = false,
        immediate = true)
@Properties({
        @Property(
                label = "Vendor",
                name = Constants.SERVICE_VENDOR,
                value = "ActiveCQ",
                propertyPrivate = true)
})
@Service
public class SampleJcrEventListener implements EventListener {
    /*
     * A combination of one or more event type constants encoded as a bitmask
     *
     * Available JCR Events:
     *
     * Event.NODE_ADDED
     * Event.NODE_MOVED
     * Event.NODE_REMOVED
     * Event.PERSIST
     * Event.PROPERTY_ADDED
     * Event.PROPERTY_REMOVED
     * Event.PROPERTY_CHANGED
    */
    private final int events = Event.PROPERTY_ADDED | Event.NODE_ADDED;

    // Only events whose associated node is at absPath (or within its subtree, if isDeep is true) will be received. It is permissible to register a listener for a path where no node currently exists.
    private final String absPath = "/content/samples";
    private final boolean isDeep = true;

    // Additionally, if noLocal is true, then events generated by the session through which the listener was registered are ignored. Otherwise, they are not ignored.
    private final boolean noLocal = false;
    private final String[] uuids = null;

    // Only events whose associated node has one of the node types (or a subtype of one of the node types) in this list will be received. If his parameter is null then no node type-related restriction is placed on events received.
    private final String[] nodeTypes = new String[]{"nt:unstructured", "nt:folder"};


    // Is this OK? JCR Sessions are not threadsafe?
    private Session adminSession;
    private ObservationManager observationManager;

    @Reference
    private SlingRepository repository;

    @Reference
    private EventAdmin eventAdmin;

    @Override
    public void onEvent(EventIterator events) {
        // Handle events
        while (events.hasNext()) {
            try {
                Event event = events.nextEvent();

                // IMPORTANT!
                //
                // JCR Events are NOT cluster-aware and this event listener will be invoked on every node in the cluster.

                // Check if this event was spawned from the server this event handler is running on or from another
                if (event instanceof JackrabbitEvent && ((JackrabbitEvent) event).isExternal()) {
                    // Event did NOT originate from this server

                    // Skip, Let only the originator process;

                    // This is usual to avoid having the same processing happening for every node in a cluster. This
                    // is almost always the case when the EventListener modifies the JCR.

                    // A possible use-case for handling the event on EVERY member of a cluster would be clearing out an
                    // in memory (Service-level) cache.

                    return;
                } else {
                    // Event originated from THIS server
                    // Continue processing this Event
                }

                final String path = event.getPath();

                if (Event.NODE_ADDED == event.getType()) {
                    // Node added!
                } else if (Event.PROPERTY_ADDED == event.getType()) {
                    // Property added!
                }

                boolean handleInSlingEvent = true;

                if (!handleInSlingEvent) {
                    // Execute handler logic
                    Node node = adminSession.getNode(path);
                } else {
                    // Or fire off a specific Sling Event
                    final Dictionary<String, Object> eventProperties = new Hashtable<String, Object>();
                    eventProperties.put("resourcePath", path);
                    eventAdmin.postEvent(new org.osgi.service.event.Event(EventUtil.TOPIC_JOB, eventProperties));
                }

            } catch (RepositoryException ex) {
                Logger.getLogger(SampleJcrEventListener.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Activate
    public void activate(ComponentContext context) throws RepositoryException {
        this.adminSession = repository.loginAdministrative(null);
        // Get JCR ObservationManager from Workspace
        this.observationManager =
                this.adminSession.getWorkspace().getObservationManager();

        // Register the JCR Listener
        this.observationManager.addEventListener(this, events, absPath, isDeep,
                uuids, nodeTypes, noLocal);

    }

    @Deactivate
    public void deactivate() throws RepositoryException {
        try {
            if (this.observationManager != null) {
                this.observationManager.removeEventListener(this);
            }
        } finally {
            if (adminSession != null) {
                adminSession.logout();
            }
        }
    }
}