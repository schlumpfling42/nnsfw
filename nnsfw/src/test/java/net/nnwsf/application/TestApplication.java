package net.nnwsf.application;

import org.junit.jupiter.api.Test;
import net.nnwsf.application.annotation.ServerConfiguration;

public class TestApplication {
    
    @ServerConfiguration(port=9999, hostname="localhost")
    class Application {

    }

    @Test
    public void testStartApplicationServer() {
        ApplicationServer.start(Application.class);
    }
}
