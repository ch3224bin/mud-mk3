package mudmk3.domain.technique;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 신통 이름으로 쓸 수 없는 낱말: 룰북 명령어의 명령·줄임말·방위 이름(첫 낱말). 아직 없는 명령도 넣어
 * 그 명령이 생기는 순간 신통을 쓸 수 없게 되지 않도록 한다. 비교는 영문 대소문자를 가리지 않는다
 */
public final class ReservedWords {

    public static final List<String> ALL = List.of(
            "보기", "ㅂ", "동", "서", "남", "북", "북동", "북서", "남동", "남서", "위", "아래",
            "e", "w", "s", "n", "ne", "nw", "se", "sw", "u", "d",
            "ㄷ", "ㄴ", "ㅂㄷ", "ㅂㅅ", "ㄴㄷ", "ㄴㅅ", "ㅇ", "ㅇㄹ", "봐", "보", "ㅅㅌ",
            "들어가기", "살피기", "만지기", "탐지", "은신", "전송", "상태", "ㅅ", "소지품", "ㅈ",
            "신통", "공법", "서재", "제작법", "시간", "파티", "누구", "도움말", "대상", "중지",
            "도주", "막기", "추격", "정혈", "명상", "ㅁ", "일어서기", "영석", "복용", "수련",
            "본명패", "읽기", "참오", "산공", "돌파", "수습", "줍기", "버리기", "부적", "진법",
            "꺼내기", "거두기", "수리", "본명", "온양", "제작", "채집", "채굴", "대화", "목록",
            "사기", "팔기", "값", "확인", "취소", "주기", "교환", "거래소", "경매", "찾기",
            "가입", "탈퇴", "공적", "동부", "맡기기", "보관함", "비경", "주입", "나가기", "퀘스트",
            "영수", "계약", "먹이기", "되살리기", "말하기", "'", "귓속말", "외치기", "종료", "뒤로",
            "고르기", "튜토리얼", "공격");

    private static final Set<String> LOWER = ALL.stream()
            .map(word -> word.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

    private ReservedWords() {
    }

    /** 예약어인지(영문 대소문자 무시) */
    public static boolean contains(String word) {
        return LOWER.contains(word.toLowerCase(Locale.ROOT));
    }
}
