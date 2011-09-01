package org.icepush;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.client.AddGroupMemberRequest;
import org.icepush.client.CreatePushIdRequest;
import org.icepush.client.HttpClient;
import org.icepush.client.HttpResponse;
import org.icepush.client.ListenRequest;
import org.icepush.client.NotifyRequest;
import org.icepush.client.PushClientException;
import org.icepush.client.RemoveGroupMemberRequest;
import org.icepush.client.HttpRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class PushClient {
    private static final Logger LOGGER = Logger.getLogger(PushClient.class.getName());

    private final HttpClient client = new HttpClient();
    private final Map<String, List<Runnable>> pushIdCallbackMap = new HashMap<String, List<Runnable>>();

    private final ReentrantLock listenRequestLock = new ReentrantLock();
    private ListenRequest listenRequest;

    private final String contextURI;

    /**
     *
     * @param      contextURI
     * @throws     PushClientException
     */
    public PushClient(final String contextURI) {
        this.contextURI = contextURI;
    }

    /**
     * <p>
     *   Adds the specified <code>pushId</code> as a member to the group with the specified <code>groupName</code>.
     * </p>
     *
     * @param      groupName
     *                 The name of the group to add the Push-ID to.
     * @param      pushId
     *                 The Push-ID to be added to the group.
     * @throws     PushClientException
     *                 if the Context-URI contains syntaxial errors or is malformed, or if an I/O error occurred.
     */
    public void addGroupMember(final String groupName, final String pushId)
    throws PushClientException {
        try {
            // throws URISyntaxException, MalformedURLException, IOException
            sendNow(new AddGroupMemberRequest(groupName, pushId, contextURI));
        } catch (URISyntaxException exception) {
            throw new PushClientException(exception);
        }
    }

    /**
     *
     * @return
     * @throws     PushClientException
     *                 if the Context-URI contains syntaxial errors or is malformed, or if an I/O error occurred.
     */
    public String createPushId()
    throws PushClientException {
        try {
            // throws URISyntaxException
            CreatePushIdRequest _request = new CreatePushIdRequest(contextURI);
            // throws MalformedURLException, IOException
            sendNow(_request);
            return _request.getResponse().getEntityBodyAsString();
        } catch (URISyntaxException exception) {
            throw new PushClientException(exception);
        }
    }

    /**
     *
     * @param      pushId
     */
    public void deregister(final String pushId) {
        synchronized (pushIdCallbackMap) {
            pushIdCallbackMap.remove(pushId);
            /*
             * Due to a deregistration of a Push-ID, cancel the pending listen.icepush request.  If there are still more
             * Callbacks registered for any Push-IDs associated with this Push Client, send a new listen.icepush
             * request.
             */
            listenRequestLock.lock();
            try {
                cancelListenRequest();
                if (pushIdCallbackMap.keySet().size() > 0) {
                    listen();
                }
            } finally {
                listenRequestLock.unlock();
            }
//            listen();
        }
    }

    /**
     *
     * @param      groupName
     *                 The name of the group to be notified.
     * @throws     PushClientException
     *                 if the Context-URI contains syntaxial errors or is malformed, or if an I/O error occurred.
     */
    public void notify(final String groupName)
    throws PushClientException {
        try {
            // throws URISyntaxException, MalformedURLException, IOException
            sendNow(new NotifyRequest(groupName, contextURI));
        } catch (URISyntaxException exception) {
            throw new PushClientException(exception);
        }
    }

    /**
     *
     * @param      pushId
     * @param      callback
     */
    public void register(final String pushId, final Runnable callback) {
        synchronized (pushIdCallbackMap) {
            if (pushIdCallbackMap.containsKey(pushId)) {
                pushIdCallbackMap.get(pushId).add(callback);
            } else {
                List<Runnable> _callbackList = new ArrayList<Runnable>();
                _callbackList.add(callback);
                pushIdCallbackMap.put(pushId, _callbackList);
                /*
                 * Due to a new registration of a Push-ID, cancel the pending listen.icepush request and send a new
                 * listen.icepush request.
                 */
                listenRequestLock.lock();
                try {
                    cancelListenRequest();
                    listen();
                } finally {
                    listenRequestLock.unlock();
                }
//                listen();
            }
        }
    }

    /**
     * <p>
     *   Removes the specified <code>pushId</code> as a member from the group with the specified <code>groupName</code>.
     * </p>
     *
     * @param      groupName
     *                 The name of the group to remove the Push-ID from.
     * @param      pushId
     *                 The Push-ID to be added to the group.
     * @throws     PushClientException
     *                 if the Context-URI contains syntaxial errors or is malformed, or if an I/O error occurred.
     */
    public void removeGroupMember(final String groupName, final String pushId)
    throws PushClientException {
        try {
            // throws URISyntaxException, MalformedURLException, IOException
            sendNow(new RemoveGroupMemberRequest(groupName, pushId, contextURI));
        } catch (URISyntaxException exception) {
            throw new PushClientException(exception);
        }
    }

    public void shutdown() {
        listenRequestLock.lock();
        try {
            cancelListenRequest();
            client.shutdown();
        } finally {
            listenRequestLock.unlock();
        }
    }

    private void cancelListenRequest() {
        if (listenRequest != null) {
            listenRequestLock.lock();
            try {
                if (listenRequest != null) {
                    client.cancel(listenRequest);
                    listenRequest = null;
                }
            } finally {
                listenRequestLock.unlock();
            }
        }
    }

    private void listen() {
        listenRequestLock.lock();
        try {
            cancelListenRequest();
            Set<String> _pushIdSet = pushIdCallbackMap.keySet();
            if (!_pushIdSet.isEmpty()) {
                try {
                    listenRequest =
                        // throws URISyntaxException
                        new ListenRequest(_pushIdSet, contextURI) {
                            public void onResponse(final HttpResponse response) {
                                super.onResponse(response);
                                Document _document = response.getEntityBodyAsDocument();
                                if (_document != null) {
                                    Node _currentNode = _document.getFirstChild();
                                    String _currentNodeName = _currentNode.getNodeName();
                                    if (_currentNodeName.equalsIgnoreCase("noop")) {
                                        // Do nothing.
                                    } else if (_currentNodeName.equalsIgnoreCase("notified-pushids")) {
                                        StringTokenizer _pushIds =
                                            new StringTokenizer(_currentNode.getFirstChild().getNodeValue());
                                        synchronized (pushIdCallbackMap) {
                                            while (_pushIds.hasMoreTokens()) {
                                                String _pushId = _pushIds.nextToken();
                                                if (pushIdCallbackMap.containsKey(_pushId)) {
                                                    for (Runnable _callback : pushIdCallbackMap.get(_pushId)) {
                                                        _callback.run();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (LOGGER.isLoggable(Level.INFO)) {
                                        LOGGER.log(Level.INFO, "Initiating listen.icepush...");
                                    }
                                    listen();
                                }
                            }
                        };
                    client.send(listenRequest);
                } catch (URISyntaxException exception) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(
                            Level.WARNING,
                            "",
                            exception);
                    }
                }
            }
        } finally {
            listenRequestLock.unlock();
        }
    }

    private void sendNow(final HttpRequest request)
    throws PushClientException {
        try {
            // throws MalformedURLException, IOException
            client.sendNow(request);
        } catch (MalformedURLException exception) {
            throw new PushClientException(exception);
        } catch (IOException exception) {
            throw new PushClientException(exception);
        }
    }
}
