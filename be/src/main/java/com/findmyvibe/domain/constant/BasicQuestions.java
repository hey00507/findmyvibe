package com.findmyvibe.domain.constant;

import java.util.List;

public final class BasicQuestions {

    private BasicQuestions() {
    }

    public static final List<String> CONTENTS = List.of(
            "혼자 시간을 보낼 때 주로 뭘 하나요?",
            "새로운 걸 배울 때 어떤 방식을 선호하나요? (영상 / 책 / 직접 해보기)",
            "실내 vs 야외, 어디서 활동하는 게 더 편한가요?",
            "사람들과 함께하는 활동 vs 혼자 집중하는 활동, 어느 쪽을 선호하나요?",
            "손으로 만드는 걸 좋아하나요? (요리, 공예, 그림 등)",
            "운동이나 신체 활동에 관심이 있나요? 있다면 어떤 종류?",
            "최근에 \"이거 해보고 싶다\"고 떠오른 게 있나요?"
    );

    public static final int COUNT = CONTENTS.size();
}
