package mudmk3.domain.cultivation;

import mudmk3.domain.character.CharacterFixture;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.technique.Technique;
import org.springframework.test.util.ReflectionTestUtils;

/** 익힌 공법. 4차에는 단계를 올리는 길이 없어 2성 이상은 필드를 직접 넣어 만든다 */
public final class CharacterTechniqueFixture {

    private CharacterTechniqueFixture() {
    }

    public static CharacterTechnique learned(Technique technique) {
        return learned(CharacterFixture.createCharacter(), technique, 1);
    }

    public static CharacterTechnique learned(Technique technique, int stage) {
        return learned(CharacterFixture.createCharacter(), technique, stage);
    }

    public static CharacterTechnique learned(PlayerCharacter character, Technique technique, int stage) {
        CharacterTechnique learned = CharacterTechnique.learn(character, technique);
        ReflectionTestUtils.setField(learned, "stage", stage);
        return learned;
    }
}
