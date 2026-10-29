package com.audition;

import static org.assertj.core.api.Assertions.assertThat;

import com.audition.web.AuditionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class AuditionApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsAndWiresTheController() {
        assertThat(context.getBean(AuditionController.class)).isNotNull();
    }

}
