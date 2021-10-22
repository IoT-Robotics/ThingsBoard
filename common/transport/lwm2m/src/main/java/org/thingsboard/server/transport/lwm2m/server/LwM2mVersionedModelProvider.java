/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.DefaultDDFFileValidator;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.common.data.ResourceType.LWM2M_MODEL;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_KEY;

@Slf4j
@Service
@TbLwM2mTransportComponent
public class LwM2mVersionedModelProvider implements LwM2mModelProvider {

    private final LwM2mClientContext lwM2mClientContext;
    private final LwM2mTransportServerHelper helper;
    private final LwM2mTransportContext context;
    private final ConcurrentMap<TenantId, ConcurrentMap<String, ObjectModel>> models;

    public LwM2mVersionedModelProvider(@Lazy LwM2mClientContext lwM2mClientContext, LwM2mTransportServerHelper helper, LwM2mTransportContext context) {
        this.lwM2mClientContext = lwM2mClientContext;
        this.helper = helper;
        this.context = context;
        this.models = new ConcurrentHashMap<>();
    }

    private String getKeyIdVer(Integer objectId, String version) {
        return objectId != null ? objectId + LWM2M_SEPARATOR_KEY + ((version == null || version.isEmpty()) ? ObjectModel.DEFAULT_VERSION : version) : null;
    }

    @Override
    public LwM2mModel getObjectModel(Registration registration) {
        return new DynamicModel(registration);
    }

    public void evict(TenantId tenantId, String key) {
        if (tenantId.isNullUid()) {
            models.values().forEach(m -> m.remove(key));
        } else {
            models.get(tenantId).remove(key);
        }
    }

    private class DynamicModel implements LwM2mModel {
        private final Registration registration;
        private final TenantId tenantId;
        private final Lock modelsLock;

        public DynamicModel(Registration registration) {
            this.registration = registration;
            this.tenantId = lwM2mClientContext.getClientByEndpoint(registration.getEndpoint()).getTenantId();
            this.modelsLock = new ReentrantLock();
            models.computeIfAbsent(tenantId, t -> new ConcurrentHashMap<>());
        }

        @Override
        public ResourceModel getResourceModel(int objectId, int resourceId) {
            try {
                ObjectModel objectModel = getObjectModel(objectId);
                if (objectModel != null)
                    return objectModel.resources.get(resourceId);
                else
                    log.trace("TbResources (Object model) with id [{}/0/{}] not found on the server", objectId, resourceId);
                return null;
            } catch (Exception e) {
                log.error("", e);
                return null;
            }
        }

        @Override
        public ObjectModel getObjectModel(int objectId) {
            String version = registration.getSupportedVersion(objectId);
            if (version != null) {
                return this.getObjectModelDynamic(objectId, version);
            }
            return null;
        }

        @Override
        public Collection<ObjectModel> getObjectModels() {
            Map<Integer, String> supportedObjects = this.registration.getSupportedObject();
            Collection<ObjectModel> result = new ArrayList<>(supportedObjects.size());
            for (Map.Entry<Integer, String> supportedObject : supportedObjects.entrySet()) {
                ObjectModel objectModel = this.getObjectModelDynamic(supportedObject.getKey(), supportedObject.getValue());
                if (objectModel != null) {
                    result.add(objectModel);
                }
            }
            return result;
        }

        private ObjectModel getObjectModelDynamic(Integer objectId, String version) {
            String key = getKeyIdVer(objectId, version);
            ObjectModel objectModel = models.get(tenantId).get(key);

            if (objectModel == null) {
                modelsLock.lock();
                try {
                    objectModel = models.get(tenantId).get(key);
                    if (objectModel == null) {
                        objectModel = getObjectModel(key);
                        models.get(tenantId).put(key, objectModel);
                    }
                } finally {
                    modelsLock.unlock();
                }
            }

            return objectModel;
        }

        private ObjectModel getObjectModel(String key) {
            Optional<TbResource> tbResource = context.getTransportResourceCache().get(this.tenantId, LWM2M_MODEL, key);
            return tbResource.map(resource -> helper.parseFromXmlToObjectModel(
                    Base64.getDecoder().decode(resource.getData()),
                    key + ".xml",
                    new DefaultDDFFileValidator())).orElse(null);
        }
    }
}
