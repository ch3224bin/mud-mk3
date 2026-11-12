package mudmk3.application.world.required;

import mudmk3.domain.world.WorldSettings;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/** 세계 설정(행 하나)을 저장하고 찾는다 */
public interface WorldSettingsRepository extends Repository<WorldSettings, Long> {

    WorldSettings save(WorldSettings settings);

    Optional<WorldSettings> findFirstByOrderByIdAsc();
}
