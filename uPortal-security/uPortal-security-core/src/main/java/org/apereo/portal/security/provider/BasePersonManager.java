/**
 * Licensed to Apereo under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership. Apereo
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at the
 * following location:
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apereo.portal.security.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apereo.portal.IUserIdentityStore;
import org.apereo.portal.security.IPerson;
import org.apereo.portal.security.IPersonManager;
import org.apereo.portal.security.InitialSecurityContextFactory;
import org.apereo.portal.security.PersonFactory;
import org.apereo.portal.security.PortalSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class BasePersonManager implements IPersonManager {

    private final Map<String, Integer> guestUserIds = new HashMap<>();

    @Autowired(required = false)
    private List<IGuestUsernameSelector> guestUsernameSelectors;

    @Autowired private IUserIdentityStore userIdentityStore;

    @Autowired private InitialSecurityContextFactory initialSecurityContextFactory;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @PostConstruct
    public void init() {
        // Make sure we have a guestUsernameSelectors collection & sort it
        if (guestUsernameSelectors == null) {
            guestUsernameSelectors = Collections.emptyList();
        }
        Collections.sort(guestUsernameSelectors);
    }

    /**
     * This is a basic implementation of <code>getPerson</code> that formerly appeared in <code>
     * SimplePersonManager</code>. For uPortal 5, it's better to avoid unnecessary bean tweaking on
     * the part of deployers, so the various flavors of PersonManager were combined in a manner
     * where the appropriate behavior triggers automatically (based on AuthN settings).
     *
     * @param request the servlet request object
     * @return the IPerson object for the incoming request
     */
    @Override
    public IPerson getPerson(HttpServletRequest request) throws PortalSecurityException {
        HttpSession session = request.getSession(false);
        IPerson person = null;

        // Return the person object if it exists in the user's session
        if (session != null) {
            person = (IPerson) session.getAttribute(PERSON_SESSION_KEY);
            logger.debug("getPerson -- person object retrieved from session is [{}]", person);
        }

        if (person == null) {
            try {
                // Create a guest person
                person = createGuestPerson(request);
                logger.debug("getPerson -- created a new guest person [{}]", person);
            } catch (Exception e) {
                // Log the exception
                logger.error("Exception creating guest person.", e);
            }
            // Add this person object to the user's session
            if (person != null && session != null) {
                session.setAttribute(PERSON_SESSION_KEY, person);
            }
        }

        return person;
    }

    /**
     * Creates a new <i>guest</i> user based on the value of the <code>
     * org.apereo.portal.security.PersonFactory.guest_user_names</code> property in
     * portal.properties and (optionally) any beans that implement {@link IGuestUsernameSelector}.
     * This approach supports pluggable, open-ended strategies for multiple guest users who may have
     * different content.
     *
     * @since 5.0
     */
    protected IPerson createGuestPerson(HttpServletRequest request) throws Exception {

        // First we need to know the guest username
        String username = PersonFactory.GUEST_USERNAMES.get(0); // First item is the default

        // Pluggable strategy for supporting multiple guest users
        for (IGuestUsernameSelector selector : guestUsernameSelectors) {
            final String s = selector.selectGuestUsername(request);
            if (s != null) {
                username = s;
                break;
            }
        }

        // Sanity check...
        if (!PersonFactory.GUEST_USERNAMES.contains(username)) {
            final String msg =
                    "The specified guest username is not in the configured list:  " + username;
            throw new IllegalStateException(msg);
        }

        Integer guestUserId = guestUserIds.get(username);
        if (guestUserId == null) {
            // Not yet looked up
            loadGuestUserId(username, guestUserIds);
            guestUserId = guestUserIds.get(username);
        }

        final IPerson rslt = PersonFactory.createPerson();
        rslt.setAttribute(IPerson.USERNAME, username);
        rslt.setID(guestUserId);
        rslt.setSecurityContext(initialSecurityContextFactory.getInitialContext());

        return rslt;
    }

    private synchronized void loadGuestUserId(String username, Map<String, Integer> map) {
        if (map.containsKey(username)) {
            // Already have it
            return;
        }
        final Integer userId = userIdentityStore.getPortalUserId(username);
        if (userId == null) {
            final String msg =
                    "The specified guest user account does not exist in the portal database:  "
                            + username;
            throw new IllegalStateException(msg);
        }
        map.put(username, userId);
    }
}
