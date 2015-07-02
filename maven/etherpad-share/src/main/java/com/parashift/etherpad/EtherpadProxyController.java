package com.parashift.etherpad;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.config.RemoteConfigElement;
import org.springframework.extensions.surf.exception.WebScriptsPlatformException;
import org.springframework.extensions.surf.util.Base64;
import org.springframework.extensions.webscripts.connector.*;
import org.springframework.extensions.webscripts.servlet.mvc.EndPointProxyController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

/**
 * BetterProxyController handles URLs in a less opinionated way
 * Mainly because the URL to socket.io in Etherpad is now /socket.io/?
 * This is stripped to /socket.io? in EndPointProxyController
 * Also indexOf & substring is faster than StringBuffer!
 * Created by cetra on 16/01/15.
 */
public class EtherpadProxyController extends EndPointProxyController {

    private static Log logger = LogFactory.getLog(EtherpadProxyController.class);

    private static final String USER_ID = "_alf_USER_ID";
    private static final String endpointId = "etherpad";

    @Override
    public ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {

        
        // rebuild rest of the URL for the proxy request
        StringBuilder buf = new StringBuilder(64);

        String reqUri = req.getRequestURI();
        buf.append(reqUri.substring(reqUri.indexOf(endpointId) + endpointId.length()));


        try
        {
            // retrieve the endpoint descriptor - do not allow proxy access to unsecure endpoints
            RemoteConfigElement.EndpointDescriptor descriptor = getRemoteConfig().getEndpointDescriptor(endpointId);
            if (descriptor == null || descriptor.getUnsecure())
            {
                // throw an exception if endpoint ID is does not exist or invalid
                throw new WebScriptsPlatformException("Invalid EndPoint Id: " + endpointId);
            }

            // user id from session NOTE: @see org.springframework.extensions.surf.UserFactory
            Connector connector;
            String userId = null;
            HttpSession session = req.getSession(false);
            if (session != null)
            {
                userId = (String)session.getAttribute(USER_ID);
            }
            if (userId != null && this.connectorService.getCredentialVault(req.getSession(), userId).hasCredentials(endpointId))
            {
                // build an authenticated connector - as we have a userId
                connector = this.connectorService.getConnector(endpointId, userId, req.getSession());
            }
            else if (descriptor.getIdentity() == RemoteConfigElement.IdentityType.NONE ||
                    descriptor.getIdentity() == RemoteConfigElement.IdentityType.DECLARED ||
                    descriptor.getExternalAuth())
            {
                // the authentication for this endpoint is either not required, declared in config or
                // managed "externally" (i.e. by a servlet filter such as NTLM) - this means we should
                // proceed on the assumption it will be dealt with later
                connector = this.connectorService.getConnector(endpointId, req.getSession());
            }
            else if (descriptor.getBasicAuth() || this.proxyControllerInterceptor.allowHttpBasicAuthentication(descriptor, reqUri))
            {
                // check for HTTP authorisation request (i.e. RSS feeds, direct links etc.)
                String authorization = req.getHeader("Authorization");
                if (authorization == null || authorization.length() == 0)
                {
                    authorizedResponseStatus(res);

                    // no further processing as authentication is required but not provided
                    // the browser will now prompt the user for appropriate credentials
                    return null;
                }
                else
                {
                    // user has provided authentication details with the request
                    String[] authParts = authorization.split(" ");
                    // test for a "negotiate" header - we will then suggest "basic" as the auth mechanism
                    if (authParts[0].equalsIgnoreCase("negotiate"))
                    {
                        authorizedResponseStatus(res);

                        // no further processing as authentication is required but not provided
                        // the browser will now prompt the user for appropriate credentials
                        return null;
                    }
                    if (!authParts[0].equalsIgnoreCase("basic"))
                    {
                        throw new WebScriptsPlatformException("Authorization '" + authParts[0] + "' not supported.");
                    }

                    String[] values = new String(Base64.decode(authParts[1])).split(":");
                    if (values.length == 2)
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("Authenticating (BASIC HTTP) user " + values[0]);

                        // assume username and password passed as the parts and
                        // build an unauthenticated authentication connector then
                        // apply the supplied credentials to it
                        connector = this.connectorService.getConnector(endpointId, values[0], req.getSession());
                        Credentials credentials = new CredentialsImpl(endpointId);
                        credentials.setProperty(Credentials.CREDENTIAL_USERNAME, values[0]);
                        credentials.setProperty(Credentials.CREDENTIAL_PASSWORD, values[1]);
                        connector.setCredentials(credentials);
                    }
                    else
                    {
                        authorizedResponseStatus(res);

                        // no further processing as authentication is required but not provided
                        // the browser will now prompt the user for appropriate credentials
                        return null;
                    }
                }
            }
            else
            {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "No USER_ID found in session and requested endpoint requires authentication.");

                // no further processing as authentication is required but not provided
                return null;
            }

            // build a connector context, stores information about how we will drive the remote client
            // ensure we don't proxy over any browser to web-tier Authorization headers over to the endpoint
            Map<String, String> headers = new HashMap<>(1, 1.0f);
            headers.put("Authorization", null);
            ConnectorContext context = new ConnectorContext(
                    HttpMethod.valueOf(req.getMethod().toUpperCase()), null, headers);
            context.setExceptionOnError(this.proxyControllerInterceptor.exceptionOnError(descriptor, reqUri));
            context.setContentType(req.getContentType());

            // build proxy URL referencing the endpoint
            final String q = req.getQueryString();
            final String url = buf.toString() + (q != null && q.length() != 0 ? "?" + q : "");

            if (logger.isDebugEnabled())
            {
                logger.debug("EtherpadProxyController preparing to proxy:");
                logger.debug(" - endpointId: " + endpointId);
                logger.debug(" - userId: " + userId);
                logger.debug(" - connector: " + connector);
                logger.debug(" - method: " + context.getMethod());
                logger.debug(" - url: " + url);
            }

            // call through using our connector to proxy
            Response response = connector.call(url, context, req, res);

            if (logger.isDebugEnabled())
            {
                logger.debug("Return code: " + response.getStatus().getCode());
                if (response.getStatus().getCode() == 500)
                {
                    logger.debug("Error detected: " + response.getStatus().getMessage() + "\n" +
                            response.getStatus().getException().toString());
                }
            }
        }
        catch (Throwable err)
        {
            // TODO: trap and handle errors!
            throw new WebScriptsPlatformException("Error during endpoint proxy processing: " + err.getMessage(), err);
        }

        return null;
    }

    private void authorizedResponseStatus(HttpServletResponse res) throws IOException {
        res.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "No USER_ID found in session and requested endpoint requires authentication.");
        res.setHeader("WWW-Authenticate", "Basic realm=\"Alfresco\"");
    }
}
