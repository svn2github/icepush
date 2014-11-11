package org.icepush.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

public class RequestUtilities {
    private static final Logger LOGGER = Logger.getLogger(RequestUtilities.class.getName());

    public static class Patterns {
        public static final String ADD_GROUP_MEMBER_REQUEST = ".*add-group-member\\.icepush";
        public static final String CREATE_PUSH_ID_REQUEST = ".*create-push-id\\.icepush";
        public static final String LISTEN_REQUEST = ".*listen\\.icepush";
        public static final String NOTIFY_REQUEST = ".*notify\\.icepush";
        public static final String REMOVE_GROUP_MEMBER_REQUEST = ".*remove-group-member\\.icepush";
    }

    private static final Pattern ADD_GROUP_MEMBER_REQUEST_PATTERN =
        Pattern.compile(Patterns.ADD_GROUP_MEMBER_REQUEST);
    private static final Pattern CREATE_PUSH_ID_REQUEST_PATTERN =
        Pattern.compile(Patterns.CREATE_PUSH_ID_REQUEST);
    private static final Pattern LISTEN_REQUEST_PATTERN =
        Pattern.compile(Patterns.LISTEN_REQUEST);
    private static final Pattern NOTIFY_REQUEST_PATTERN =
        Pattern.compile(Patterns.NOTIFY_REQUEST);
    private static final Pattern REMOVE_GROUP_MEMBER_REQUEST_PATTERN =
        Pattern.compile(Patterns.REMOVE_GROUP_MEMBER_REQUEST);

    public static boolean isAddGroupMemberRequest(final HttpServletRequest request) {
        return request != null && ADD_GROUP_MEMBER_REQUEST_PATTERN.matcher(request.getRequestURI()).find();
    }

    public static boolean isCreatePushIDRequest(final HttpServletRequest request) {
        return request != null && CREATE_PUSH_ID_REQUEST_PATTERN.matcher(request.getRequestURI()).find();
    }

    public static boolean isListenRequest(final HttpServletRequest request) {
        return request != null && LISTEN_REQUEST_PATTERN.matcher(request.getRequestURI()).find();
    }

    public static boolean isNotifyRequest(final HttpServletRequest request) {
        return request != null && NOTIFY_REQUEST_PATTERN.matcher(request.getRequestURI()).find();
    }

    public static boolean isRemoveGroupMemberRequest(final HttpServletRequest request) {
        return request != null && REMOVE_GROUP_MEMBER_REQUEST_PATTERN.matcher(request.getRequestURI()).find();
    }
}
