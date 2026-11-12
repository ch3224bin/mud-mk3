package mudmk3;

import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Import(TestFakesConfiguration.class)
class WorldServerApplicationTest {

    @Test
    void contextLoads() {
    }
}
