package com.sap.lsp.cf.ws;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@ClientEndpoint(subprotocols="local_access")
public class WebSocketClient {

    private Session userSession = null;
    private CompletableFuture<String> response;
    private String waitFor;
	private String uri;
    
    private static final Logger LOG = Logger.getLogger(WebSocketClient.class.getName());

    private WebSocketClient() {
    	
    }
    
    public static WebSocketClient getInstance() {
    	return new WebSocketClient();
    }
    
    public void connect(String uri) {
        try {
            WebSocketContainer container = ContainerProvider
                    .getWebSocketContainer();
            container.connectToServer(this, new URI(uri));
            this.setUri(uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public boolean isClosed() {
        return userSession == null || !userSession.isOpen();
    }

    @OnOpen
    public void onOpen(Session userSession) {
        this.userSession = userSession;
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        this.userSession = null;
    }

    @OnMessage
    public void onMessage(String message) {
        if (message.equals(waitFor)) {
            this.response.complete(message);
        }
    }

    @OnError
    public void onError(Session session, Throwable trowable) {
    	if ( trowable != null ) {
    		LOG.severe("SYNC Client error: " + trowable.getMessage());
    	} else {
    		LOG.severe("SYNC Client error: unknown error");
    	}
    	
    }
    
    public CompletableFuture<String> sendRequest(String message, String response) throws RuntimeException {
        this.response = new CompletableFuture<String>();
        waitFor = response;
        sendText(message);
        return this.response;
    }

    public void sendNotification(String message) throws RuntimeException {
        sendText(message);

    }

    private void sendText(String message) {
        if (userSession != null && userSession.isOpen())
            try {
                this.userSession.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        else {
            throw new RuntimeException("Session closed");
        }
    }

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

}
