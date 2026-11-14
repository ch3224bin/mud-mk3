package mudmk3.adapter.webapi;

import mudmk3.adapter.webapi.dto.RealmResponse;
import mudmk3.domain.shared.Realm;
import mudmk3.support.stereotype.WebApiAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.List;

/** 관리: 경지 목록(낮은 경지부터). 엔진이 정한 값이라 읽기만 한다 */
@WebApiAdapter
@RequestMapping("/api/admin/realms")
public class RealmApi {

    @GetMapping
    public List<RealmResponse> list() {
        return Arrays.stream(Realm.values()).map(RealmResponse::of).toList();
    }
}
