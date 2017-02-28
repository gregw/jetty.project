//
//  ========================================================================
//  Copyright (c) 1995-2016 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.jsr356.function;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;

import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.common.function.EndpointFunctions;
import org.eclipse.jetty.websocket.common.test.DummyConnection;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.eclipse.jetty.websocket.jsr356.ConfiguredEndpoint;
import org.eclipse.jetty.websocket.jsr356.JsrSession;
import org.eclipse.jetty.websocket.jsr356.client.EmptyClientEndpointConfig;
import org.eclipse.jetty.websocket.jsr356.decoders.AvailableDecoders;
import org.eclipse.jetty.websocket.jsr356.encoders.AvailableEncoders;
import org.eclipse.jetty.websocket.jsr356.endpoints.TrackingSocket;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JsrEndpointFunctions_OnMessage_BinaryStreamTest
{
    private static ClientContainer container;
    
    @BeforeClass
    public static void initContainer()
    {
        container = new ClientContainer();
    }
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    private AvailableEncoders encoders;
    private AvailableDecoders decoders;
    private Map<String, String> uriParams = new HashMap<>();
    private EndpointConfig endpointConfig;
    
    public JsrEndpointFunctions_OnMessage_BinaryStreamTest()
    {
        endpointConfig = new EmptyClientEndpointConfig();
        encoders = new AvailableEncoders(endpointConfig);
        decoders = new AvailableDecoders(endpointConfig);
        uriParams = new HashMap<>();
    }
    
    public JsrSession newSession(Object websocket)
    {
        String id = JsrEndpointFunctions_OnMessage_BinaryStreamTest.class.getSimpleName();
        URI requestURI = URI.create("ws://localhost/" + id);
        WebSocketPolicy policy = WebSocketPolicy.newClientPolicy();
        DummyConnection connection = new DummyConnection(policy);
        ClientEndpointConfig config = new EmptyClientEndpointConfig();
        ConfiguredEndpoint ei = new ConfiguredEndpoint(websocket, config);
        return new JsrSession(container, id, requestURI, ei, connection);
    }
    
    @SuppressWarnings("Duplicates")
    private TrackingSocket performOnMessageInvocation(TrackingSocket socket, Function<EndpointFunctions, Void> func) throws Exception
    {
        // Establish endpoint function
        JsrEndpointFunctions endpointFunctions = new JsrEndpointFunctions(
                socket, container.getPolicy(),
                container.getExecutor(),
                encoders,
                decoders,
                uriParams,
                endpointConfig
        );
        endpointFunctions.start();
        
        // This invocation is the same for all tests
        endpointFunctions.onOpen(newSession(socket));
        
        func.apply(endpointFunctions);
        
        return socket;
    }

    @ClientEndpoint
    public static class MessageStreamSocket extends TrackingSocket
    {
        @OnMessage
        public void onMessage(InputStream stream)
        {
            try
            {
                String msg = IO.toString(stream, StandardCharsets.UTF_8);
                addEvent("onMessage(%s) = \"%s\"", stream.getClass().getSimpleName(), msg);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
    
    @Test
    public void testInvokeMessageText() throws Exception
    {
        TrackingSocket socket = performOnMessageInvocation(new MessageStreamSocket(), (endpoint) ->
        {
            endpoint.onBinary(BufferUtil.toBuffer("Hello World", StandardCharsets.UTF_8), true);
            return null;
        });
        socket.assertEvent("onMessage(MessageInputStream) = \"Hello World\"");
    }
    
}
