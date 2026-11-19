package com.quietplease.platform;

import java.util.ServiceLoader;

public final class Services {

    public static final Platform PLATFORM = ServiceLoader.load(Platform.class, Platform.class.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Quiet Please: no platform implementation found"));

    private Services() {}
}
