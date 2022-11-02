/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.security.auth;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.event.UserAuthDataChangedEvent;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Service
@RequiredArgsConstructor
public class TokenOutdatingService {
    private final TbTransactionalCache<String, Long> cache;
    private final JwtTokenFactory tokenFactory;

    @EventListener(classes = UserAuthDataChangedEvent.class)
    public void onUserAuthDataChanged(UserAuthDataChangedEvent event) {
        if (StringUtils.hasText(event.getId())) {
            cache.put(event.getId(), event.getTs());
        }
    }

    public boolean isOutdated(JwtToken token, UserId userId) {
        Claims claims = tokenFactory.parseTokenClaims(token).getBody();
        long issueTime = claims.getIssuedAt().getTime();
        String sessionId = claims.get("sessionId", String.class) == null ? "" : claims.get("sessionId", String.class);
        return isTokenOutdated(issueTime, userId.toString()) || isTokenOutdated(issueTime, sessionId);
    }

    private Boolean isTokenOutdated(long issueTime, String sessionId) {
        return Optional.ofNullable(cache.get(sessionId)).map(outdatageTime -> isTokenOutdated(issueTime, outdatageTime.get())).orElse(false);
    }

    private boolean isTokenOutdated(long issueTime, Long outdatageTime) {
        return MILLISECONDS.toSeconds(issueTime) < MILLISECONDS.toSeconds(outdatageTime);
    }
}
