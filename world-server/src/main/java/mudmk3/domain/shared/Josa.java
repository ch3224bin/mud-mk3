package mudmk3.domain.shared;

/**
 * 낱말 끝 글자의 받침에 맞는 조사를 고른다. 끝의 닫는 괄호·문장 부호·공백은 건너뛰고 본 끝 글자가
 * 받침 있는 한글이면 받침 있음, 받침 없는 한글·숫자·영문이면 받침 없음으로 본다
 * ({@code 회춘단(연기 초)는}, {@code 회춘단123456는})
 */
public final class Josa {

    private static final char HANGUL_FIRST = '가';
    private static final char HANGUL_LAST = '힣';
    private static final int JONGSEONG_COUNT = 28;

    private Josa() {
    }

    /** 끝 글자에 받침이 있으면 {@code 이}, 아니면 {@code 가} */
    public static String iGa(String word) {
        return endsWithJongseong(word) ? "이" : "가";
    }

    /** 낱말에 {@code 이/가} 를 붙인다: {@code 한백이}, {@code 여소가} */
    public static String withIGa(String word) {
        return word + iGa(word);
    }

    /** 끝 글자에 받침이 있으면 {@code 은}, 아니면 {@code 는} */
    public static String eunNeun(String word) {
        return endsWithJongseong(word) ? "은" : "는";
    }

    /** 낱말에 {@code 은/는} 을 붙인다: {@code 회춘단은}, {@code 회춘단(연기 초)는} */
    public static String withEunNeun(String word) {
        return word + eunNeun(word);
    }

    /** 끝 글자에 받침이 있으면 {@code 을}, 아니면 {@code 를} */
    public static String eulReul(String word) {
        return endsWithJongseong(word) ? "을" : "를";
    }

    /** 낱말에 {@code 을/를} 을 붙인다: {@code 회춘단을}, {@code 법기를} */
    public static String withEulReul(String word) {
        return word + eulReul(word);
    }

    /** 끝 글자에 받침이 있으면 {@code 과}, 아니면 {@code 와} */
    public static String gwaWa(String word) {
        return endsWithJongseong(word) ? "과" : "와";
    }

    /** 낱말에 {@code 과/와} 를 붙인다: {@code 비검과}, {@code 영수 먹이와} */
    public static String withGwaWa(String word) {
        return word + gwaWa(word);
    }

    private static boolean endsWithJongseong(String word) {
        for (int i = word.length() - 1; i >= 0; i--) {
            char c = word.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                return c >= HANGUL_FIRST && c <= HANGUL_LAST && (c - HANGUL_FIRST) % JONGSEONG_COUNT != 0;
            }
        }
        return false;
    }
}
