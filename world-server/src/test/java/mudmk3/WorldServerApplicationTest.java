package mudmk3;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestFakesConfiguration.class)
class WorldServerApplicationTest {

    @Test
    void contextLoads() {
    }
}
