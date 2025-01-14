/**
 * The MIT License
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.restapi.controller;

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.config.audit.AuditEventMethod;
import org.niis.xroad.restapi.converter.PublicApiKeyDataConverter;
import org.niis.xroad.restapi.domain.InvalidRoleNameException;
import org.niis.xroad.restapi.domain.PersistentApiKeyType;
import org.niis.xroad.restapi.domain.PublicApiKeyData;
import org.niis.xroad.restapi.dto.PlaintextApiKeyDto;
import org.niis.xroad.restapi.openapi.BadRequestException;
import org.niis.xroad.restapi.openapi.ResourceNotFoundException;
import org.niis.xroad.restapi.service.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

import static org.niis.xroad.restapi.config.audit.RestApiAuditEvent.API_KEY_CREATE;
import static org.niis.xroad.restapi.config.audit.RestApiAuditEvent.API_KEY_REMOVE;
import static org.niis.xroad.restapi.config.audit.RestApiAuditEvent.API_KEY_UPDATE;
import static org.niis.xroad.restapi.openapi.ApiUtil.API_V1_PREFIX;

/**
 * Controller for rest apis for api key operations
 */
@RestController
@RequestMapping(ApiKeysController.API_KEYS_V1_PATH)
@Slf4j
@PreAuthorize("denyAll")
public class ApiKeysController {

    public static final String API_KEYS_V1_PATH = API_V1_PREFIX + "/api-keys";

    private final ApiKeyService apiKeyService;
    private final PublicApiKeyDataConverter publicApiKeyDataConverter;

    @Autowired
    public ApiKeysController(ApiKeyService apiKeyService, PublicApiKeyDataConverter publicApiKeyDataConverter) {
        this.apiKeyService = apiKeyService;
        this.publicApiKeyDataConverter = publicApiKeyDataConverter;
    }

    /**
     * create a new api key
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @AuditEventMethod(event = API_KEY_CREATE)
    @PreAuthorize("hasAuthority('CREATE_API_KEY')")
    public ResponseEntity<PublicApiKeyData> createKey(@RequestBody List<String> roles) {
        try {
            PlaintextApiKeyDto createdKeyData = apiKeyService.create(roles);
            return new ResponseEntity<>(publicApiKeyDataConverter.convert(createdKeyData), HttpStatus.OK);
        } catch (InvalidRoleNameException e) {
            throw new BadRequestException(e);
        }
    }

    /**
     * update an existing api key
     */
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @AuditEventMethod(event = API_KEY_UPDATE)
    @PreAuthorize("hasAuthority('UPDATE_API_KEY')")
    public ResponseEntity<PublicApiKeyData> updateKey(@PathVariable("id") long id,
            @RequestBody List<String> roles) {
        try {
            PersistentApiKeyType key = apiKeyService.update(id, roles);
            return new ResponseEntity<>(publicApiKeyDataConverter.convert(key), HttpStatus.OK);
        } catch (InvalidRoleNameException e) {
            throw new BadRequestException(e);
        } catch (ApiKeyService.ApiKeyNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }
    }

    /**
     * get an existing api key
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('VIEW_API_KEYS')")
    public ResponseEntity<PublicApiKeyData> getKey(@PathVariable("id") long id) {
        try {
            PersistentApiKeyType key = apiKeyService.getForId(id);
            return new ResponseEntity<>(publicApiKeyDataConverter.convert(key), HttpStatus.OK);
        } catch (ApiKeyService.ApiKeyNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }
    }

    /**
     * list api keys from db
     */
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('VIEW_API_KEYS')")
    public ResponseEntity<Collection<PublicApiKeyData>> list() {
        Collection<PersistentApiKeyType> keys = apiKeyService.listAll();
        return new ResponseEntity<>(publicApiKeyDataConverter.convert(keys), HttpStatus.OK);
    }

    /**
     * revoke key
     * @param id
     * @return
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @AuditEventMethod(event = API_KEY_REMOVE)
    @PreAuthorize("hasAuthority('REVOKE_API_KEY')")
    public ResponseEntity revoke(@PathVariable("id") long id) {
        try {
            apiKeyService.removeForId(id);
        } catch (ApiKeyService.ApiKeyNotFoundException e) {
            throw new ResourceNotFoundException(e);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

}
