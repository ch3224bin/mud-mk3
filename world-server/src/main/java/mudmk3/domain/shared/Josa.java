package mudmk3.domain.shared;

/** 낱말 끝 글자의 받침에 맞는 조사를 고른다 */
public final class Josa {

    private static final char HANGUL_FIRST = '가';
    private static final char HANGUL_LAST = '힣';
    private static final int JONGSEONG_COUNT = 28;

    private Josa() {
    }

    /** 끝 글자가 받침 있는 한글이면 {@code 이}, 아니면 {@code 가} */
    public static String iGa(String word) {
        return endsWithJongseong(word) ? "이" : "가";
    }

    /** 낱말에 {@code 이/가} 를 붙인다: {@code 한백이}, {@code 여소가} */
    public static String withIGa(String word) {
        return word + iGa(word);
    }

    private static boolean endsWithJongseong(String word) {
        if (word.isEmpty()) return false;
        char last = word.charAt(word.length() - 1);
        return last >= HANGUL_FIRST && last <= HANGUL_LAST && (last - HANGUL_FIRST) % JONGSEONG_COUNT != 0;
    }
}
