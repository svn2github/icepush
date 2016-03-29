package org.icepush.util;

public interface DatabaseEntity {
    String getDatabaseID();

    String getKey();

    void save();
}
