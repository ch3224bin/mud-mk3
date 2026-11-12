package mudmk3.domain.room;

import org.junit.jupiter.api.Test;

import static mudmk3.domain.room.Direction.*;
import static org.assertj.core.api.Assertions.assertThat;

class DirectionTest {

    @Test
    void opposite() {
        assertThat(NORTH.opposite()).isEqualTo(SOUTH);
        assertThat(EAST.opposite()).isEqualTo(WEST);
        assertThat(NORTHEAST.opposite()).isEqualTo(SOUTHWEST);
        assertThat(NORTHWEST.opposite()).isEqualTo(SOUTHEAST);
        assertThat(UP.opposite()).isEqualTo(DOWN);

        for (Direction direction : Direction.values()) {
            assertThat(direction.opposite()).isNotEqualTo(direction);
            assertThat(direction.opposite().opposite()).isEqualTo(direction);
        }
    }

    @Test
    void fromCommand() {
        assertThat(Direction.fromCommand("북")).contains(NORTH);
        assertThat(Direction.fromCommand("N")).contains(NORTH);
        assertThat(Direction.fromCommand("ne")).contains(NORTHEAST);
        assertThat(Direction.fromCommand("아래")).contains(DOWN);
    }

    @Test
    void fromCommandFail() {
        assertThat(Direction.fromCommand("북쪽")).isEmpty();
        assertThat(Direction.fromCommand("말")).isEmpty();
        assertThat(Direction.fromCommand("")).isEmpty();
    }

    @Test
    void towardPhrase() {
        assertThat(NORTH.towardPhrase()).isEqualTo("북쪽으로");
        assertThat(NORTHEAST.towardPhrase()).isEqualTo("북동쪽으로");
        assertThat(UP.towardPhrase()).isEqualTo("위로");
        assertThat(DOWN.towardPhrase()).isEqualTo("아래로");
    }

    @Test
    void fromPhrase() {
        assertThat(SOUTH.fromPhrase()).isEqualTo("남쪽에서");
        assertThat(SOUTHWEST.fromPhrase()).isEqualTo("남서쪽에서");
        assertThat(UP.fromPhrase()).isEqualTo("위에서");
        assertThat(DOWN.fromPhrase()).isEqualTo("아래에서");
    }
}
