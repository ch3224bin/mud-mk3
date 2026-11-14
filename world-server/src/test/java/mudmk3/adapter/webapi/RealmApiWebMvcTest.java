package mudmk3.adapter.webapi;

import mudmk3.adapter.security.WebMvcSecurityTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.Map;

import static mudmk3.adapter.security.SecurityFixture.admin;
import static mudmk3.adapter.security.SecurityFixture.player;
import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(RealmApi.class)
@Import(WebMvcSecurityTestConfiguration.class)
record RealmApiWebMvcTest(MockMvcTester mvc) {

    @Test
    void list() {
        var body = assertThat(mvc.get().with(admin()).uri("/api/admin/realms")).hasStatusOk().bodyJson();

        body.extractingPath("$").asArray().hasSize(18);
        body.extractingPath("$[0]").isEqualTo(Map.of("code", "MORTAL", "name", "범인"));
        body.extractingPath("$[1].name").isEqualTo("연기 1층");
        body.extractingPath("$[14].code").isEqualTo("FOUNDATION_EARLY");
        body.extractingPath("$[14].name").isEqualTo("축기 초기");
        body.extractingPath("$[17].code").isEqualTo("FOUNDATION_PEAK");
    }

    @Test
    void playerFail() {
        assertThat(mvc.get().with(player()).uri("/api/admin/realms")).hasStatus(HttpStatus.FORBIDDEN);
    }
}
