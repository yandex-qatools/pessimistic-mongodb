package ru.qatools.mongodb;

import java.util.function.Consumer;

/**
 * @author Ilya Sadykov
 */
public interface TailingQueue<T> {
    /**
     * Initialize the queue
     */
    default void init() {
    }

    /**
     * Drop the queue
     */
    default void drop() {
    }

    /**
     * Stop the processing of the messages
     * This method makes sense only when there is an
     * active poll process
     */
    void stop();

    /**
     * Polls the queue and gives the control to consumer upom each incoming item within
     */
    void poll(Consumer<T> consumer);

    /**
     * Appends the new item to the queue
     */
    void add(T object);

    /**
     * Returns the size of the queue (number of documents currently enqueued)
     */
    long size();
}
