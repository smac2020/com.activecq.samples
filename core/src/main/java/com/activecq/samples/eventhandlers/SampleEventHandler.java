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

import com.day.cq.jcrclustersupport.ClusterAware;
import org.apache.commons.lang.ArrayUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.jobs.JobProcessor;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/**
 * The <code>DropBoxEventHandler</code> moves files posted to /tmp/dropbox to the appropriate locations:
 * images (MIME type: image/png) to /dropbox/images/
 * music (MIME type: audio/mpeg) to /dropbox/music/
 * movies (MIME type: video/x-msvideo) to /dropbox/movies/
 * otherwise to /dropbox/other/
 *
 * @scr.component immediate="true"
 * @scr.service interface="org.osgi.service.event.EventHandler"
 * @scr.property name="event.topics" valueRef="mypackage.DropBoxService.JOB_TOPIC"
 */


@Component(
        label = "Samples - Sling Event Handler",
        description = "",
        immediate = true,
        metatype = false
)
@Properties({
        @Property(
                label = "Vendor",
                name = Constants.SERVICE_VENDOR,
                value = "ActiveCQ",
                propertyPrivate = true
        ),
        @Property(
                label = "Event Topics",
                value = {"samples/events/poked", "samples/events/*"},
                description = "[Required] Event Topics this event handler will to respond to.",
                name = "event.topics",
                propertyPrivate = true
        )
    /*
    @Property(
        label="Event Filter",
        value="(propKey=propValue)",
        description="[Optional] Event Filter further selects Events of interest.",
        name="event.topics",
        propertyPrivate=true
    )
    */
})
@Service
public class SampleEventHandler implements JobProcessor, EventHandler, ClusterAware {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    // EventAdmin is used to manually trigger other events
    @Reference
    private EventAdmin eventAdmin;
    private boolean isMaster;


    @Override
    public void handleEvent(Event event) {
        boolean handleLocally = false;
        boolean handleWithMaster = !handleLocally;

        if (!ArrayUtils.contains(event.getPropertyNames(), EventUtil.PROPERTY_DISTRIBUTE)) {
            // This is the check for a distributed event or not; if this property does not exist, it usually
            // means that this event handler should process the job, as no other event handlers
            // will see this event.

            JobUtil.processJob(event, this);

        } else if (handleLocally && EventUtil.isLocal(event)) {
            // This is a distributed event (first 'if' condition failed)

            // If this server created the event
            // then only this server should process the event

            // This will call this's process(..) method, passing in the event obj
            // JobUtil.processJob(..) sends/checks for an ack for this job

            // Jobs guarantee the event will be processed (though doesnt guarentee the job will be processed SUCCESSFULLY)
            JobUtil.processJob(event, this);

        } else if (handleWithMaster && this.isMaster) {
            // This is a distributed event (first 'if' condition failed)

            // If a event is distributed, you may only want to execute it the Master node in
            // the cluster.

            JobUtil.processJob(event, this);
        } else {
            // DO NOTHING!
        }
    }


    @Override
    public boolean process(Event event) {
        final String path = (String) event.getProperty("resourcePath");

        // Process event logic here

        // Only return false if job processing failed and the job should be rescheduled
        return true;
    }

    /**
     * Cluster Aware Methods *
     */

    @Override
    public void bindRepository(String repositoryId, String clusterId, boolean isMaster) {
        this.isMaster = isMaster;
    }

    @Override
    public void unbindRepository() {
        this.isMaster = false;
    }

    /**
     * OSGi Component Methods *
     */

    protected void activate(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        // Do things like get an admin JCR Session to modify nodes
    }

    protected void deactivate(ComponentContext context) {
        // Close/release any resources before ending the component's lifecycle
    }
}