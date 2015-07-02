package com.parashift.etherpad;

import org.springframework.extensions.config.RemoteConfigElement;
import org.springframework.extensions.webscripts.connector.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Transparently adds the API KEY to requests to Etherpad
 *
 * Uses the <password></password> directive for the API key.
 *
 * This should be used in conjunction with <unsecure>true</unsecure> within
 * the endpoint configuration to prevent end users proxying this connection.
 *
 *
 * @author cetra3
 */
public class EtherpadConnector extends HttpConnector {

    public EtherpadConnector(RemoteConfigElement.ConnectorDescriptor descriptor, String endpoint)
    {
        super(descriptor, endpoint);
    }

    @Override
    public Response call(String uri, ConnectorContext context) {
        uri = setAPIKey(uri);
        return super.call(uri, context);
    }

    @Override
    public Response call(String uri, ConnectorContext context, InputStream in) {
        uri = setAPIKey(uri);
        return super.call(uri, context, in);
    }

    @Override
    public Response call(String uri, ConnectorContext context, InputStream in, OutputStream out) {
        uri = setAPIKey(uri);
        return super.call(uri, context, in, out);
    }

    @Override
    public Response call(String uri, ConnectorContext context, HttpServletRequest req, HttpServletResponse res) {
        uri = setAPIKey(uri);
        return super.call(uri, context, req, res);
    }

    private String setAPIKey(String uri) {

        String pass = (String) getCredentials().getProperty(Credentials.CREDENTIAL_PASSWORD);
        return uri + "&apikey=" + pass;
    }



}