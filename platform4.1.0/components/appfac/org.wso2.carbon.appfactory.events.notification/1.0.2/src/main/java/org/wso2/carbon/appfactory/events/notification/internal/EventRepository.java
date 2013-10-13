/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package org.wso2.carbon.appfactory.events.notification.internal;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.common.util.AppFactoryUtil;
import org.wso2.carbon.appfactory.events.notification.service.EventBean;
import org.wso2.carbon.appfactory.events.notification.service.UserEventBean;

/**
 * This class Keeps the events in a synchronized circular buffer
 * Events are kept in EventBean format
 */
public class EventRepository {

    private Buffer eventBuffer;
    private Buffer userEventBuffer;
    private static EventRepository instance = new EventRepository();
    private static final Log log = LogFactory.getLog(EventRepository.class);

    private EventRepository(){
        String bufferSize = null;
        try {
            AppFactoryConfiguration config = AppFactoryUtil.getAppfactoryConfiguration();
            bufferSize = config.getFirstProperty("EventBufferSize");
            if(bufferSize != null) {
                eventBuffer = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(Integer.valueOf(bufferSize)));
                userEventBuffer = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(Integer.valueOf(bufferSize)));
            } else {
                eventBuffer = BufferUtils.synchronizedBuffer(new CircularFifoBuffer());
                userEventBuffer = BufferUtils.synchronizedBuffer(new CircularFifoBuffer());
            }
        } catch (Exception e) {
            log.error("Error while creating event buffer with defined size (" + bufferSize + "). Creating the buffer with default size (32)");
            eventBuffer = BufferUtils.synchronizedBuffer(new CircularFifoBuffer());
            userEventBuffer = BufferUtils.synchronizedBuffer(new CircularFifoBuffer());
        }
    }

    public static EventRepository getInstance() {
        return instance;
    }
    public Buffer getEventBuffer() {
        return eventBuffer;
    }

    public void addEvent(EventBean event) {
        eventBuffer.add(event);
    }

    public Buffer getUserEventBuffer() {
        return userEventBuffer;
    }

    public void addUserEvent(UserEventBean event) {
        userEventBuffer.add(event);
    }
}