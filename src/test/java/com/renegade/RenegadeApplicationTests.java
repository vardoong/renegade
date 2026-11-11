package com.renegade;

import com.renegade.global.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RenegadeApplicationTests {

    @Test
    void contextLoads() {
    }

}
