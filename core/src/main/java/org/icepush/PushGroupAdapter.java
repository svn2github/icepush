package org.icepush;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class PushGroupAdapter
implements PushGroupListener {
    private static final Logger LOGGER = Logger.getLogger(PushGroupAdapter.class.getName());

    public void groupTouched(final PushGroupEvent event) {
        // Do nothing.
    }

    public void memberAdded(final PushGroupEvent event) {
        // Do nothing.
    }

    public void memberRemoved(final PushGroupEvent event) {
        // Do nothing.
    }

    public void pushed(final PushGroupEvent event) {
        // Do nothing.
    }

    public void pushIDTouched(final PushGroupEvent event) {
        // Do nothing.
    }
}
