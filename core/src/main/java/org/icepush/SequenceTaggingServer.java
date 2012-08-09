package org.icepush;

import org.icepush.http.Request;
import org.icepush.http.Response;
import org.icepush.http.ResponseHandler;
import org.icepush.http.Server;
import org.icepush.http.standard.RequestProxy;
import org.icepush.util.Slot;

public class SequenceTaggingServer implements Server {
    private Server server;
    private Slot sequenceNo;

    public SequenceTaggingServer(Slot sequenceNo, Server server) {
        this.sequenceNo = sequenceNo;
        this.server = server;
    }

    public void service(Request request) throws Exception {
        server.service(new TaggingRequest(request));
    }

    public void shutdown() {
        server.shutdown();
    }

    public class TaggingRequest extends RequestProxy {
        public TaggingRequest(Request request) {
            super(request);
        }

        public void respondWith(final ResponseHandler handler) throws Exception {
            request.respondWith(new TaggingResponseHandler(handler));
        }

        private class TaggingResponseHandler implements ResponseHandler {
            private final ResponseHandler handler;

            public TaggingResponseHandler(ResponseHandler handler) {
                this.handler = handler;
            }

            public void respond(Response response) throws Exception {
                try {
                    long previousSequenceNo;
                    try {
                        previousSequenceNo = request.getHeaderAsInteger("ice.push.sequence");
                    } catch (RuntimeException e) {
                        previousSequenceNo = 0;
                    }
                    sequenceNo.setLongValue(sequenceNo.getIntegerValue() + 1);
                    response.setHeader("ice.push.sequence", sequenceNo.getIntegerValue());
                } finally {
                    handler.respond(response);
                }
            }
        }
    }
}
