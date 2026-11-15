package mudmk3;

import mudmk3.application.game.required.CorpseRepository;
import mudmk3.application.game.required.MobInstanceRepository;
import mudmk3.application.game.required.PlayerCombatRepository;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 테스트마다 시작 전에 몹·교전·시체 메모리를 비우고 {@link ScriptedRandom} 에 넣은 값을 지운다(D93).
 * 메모리 저장소와 난수는 스프링 싱글턴이라 테스트 롤백으로 지워지지 않는다
 */
public class CombatMemoryReset implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        applicationContext.getBean(MobInstanceRepository.class).deleteAll();
        applicationContext.getBean(PlayerCombatRepository.class).deleteAll();
        applicationContext.getBean(CorpseRepository.class).deleteAll();
        applicationContext.getBean(ScriptedRandom.class).reset();
    }
}
