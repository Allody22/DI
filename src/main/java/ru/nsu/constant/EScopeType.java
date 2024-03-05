package ru.nsu.constant;

public enum EScopeType {
    /**
     * Single instance for the whole lifespan of the app.
     */
    SINGLETON,

    /**
     * New instance for each request to obtain the service.
     */
    PROTOTYPE,

    /**
     * A proxy instance will be created, allowing the user to swap instances for dependencies at runtime
     * without having to reload them.
     */
    PROXY;

    public static final EScopeType DEFAULT_SCOPE = SINGLETON;
}
