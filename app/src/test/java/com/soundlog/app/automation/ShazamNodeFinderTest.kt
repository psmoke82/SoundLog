package com.soundlog.app.automation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShazamNodeFinderTest {

    @Test
    fun testSongWithDayOfWeekIsNotFiltered() {
        // "Sunday Morning", "Yesterday", "Today" 등 영어 명곡 제목이 필터링되지 않아야 함
        assertFalse(ShazamNodeFinder.isDateTimeOrIgnoredText("Sunday Morning"))
        assertFalse(ShazamNodeFinder.isDateTimeOrIgnoredText("Yesterday"))
        assertFalse(ShazamNodeFinder.isDateTimeOrIgnoredText("Today"))
        assertFalse(ShazamNodeFinder.isDateTimeOrIgnoredText("Friday I'm in love"))
        assertFalse(ShazamNodeFinder.isDateTimeOrIgnoredText("Blue Monday"))
        assertFalse(ShazamNodeFinder.isDateTimeOrIgnoredText("Saturday Night Fever"))
        assertFalse(ShazamNodeFinder.isDateTimeOrIgnoredText("Today Was a Fairytale"))
        assertFalse(ShazamNodeFinder.isDateTimeOrIgnoredText("Margo Guryan"))
        assertFalse(ShazamNodeFinder.isDateTimeOrIgnoredText("The Beatles"))
    }

    @Test
    fun testKoreanSongWithDayOfWeekIsNotFiltered() {
        // "금요일에 만나요" (아이유) 등 요일이 포함된 한국어 노래 제목이 정상 통과해야 함
        assertFalse(ShazamNodeFinder.isDateTimeOrIgnoredText("금요일에 만나요"))
        assertFalse(ShazamNodeFinder.isDateTimeOrIgnoredText("월요일에 만나요"))
        assertFalse(ShazamNodeFinder.isDateTimeOrIgnoredText("일요일의 노래"))
        assertFalse(ShazamNodeFinder.isDateTimeOrIgnoredText("아이유"))

        assertTrue(ShazamNodeFinder.isValidViewIdText("금요일에 만나요"))
        assertTrue(ShazamNodeFinder.isValidViewIdText("아이유"))
        assertTrue(ShazamNodeFinder.isValidArtistTitlePair("금요일에 만나요", "아이유"))
    }

    @Test
    fun testIsValidViewIdText() {
        // View ID 추출 시 유효성 검사: 정상 곡명 및 아티스트명 통과
        assertTrue(ShazamNodeFinder.isValidViewIdText("Yesterday"))
        assertTrue(ShazamNodeFinder.isValidViewIdText("The Beatles"))
        assertTrue(ShazamNodeFinder.isValidViewIdText("Today"))
        assertTrue(ShazamNodeFinder.isValidViewIdText("Sunday Morning"))
        assertTrue(ShazamNodeFinder.isValidViewIdText("1999"))
        assertTrue(ShazamNodeFinder.isValidViewIdText("금요일에 만나요"))

        // View ID 추출 시 시스템 상태 안내문이나 통계 숫자, UI 버튼은 차단
        assertFalse(ShazamNodeFinder.isValidViewIdText("듣는 중..."))
        assertFalse(ShazamNodeFinder.isValidViewIdText("수음 중"))
        assertFalse(ShazamNodeFinder.isValidViewIdText("Shazam하려면 탭하세요"))
        assertFalse(ShazamNodeFinder.isValidViewIdText("음악 감상"))
        assertFalse(ShazamNodeFinder.isValidViewIdText("곡 공유하기"))
        assertFalse(ShazamNodeFinder.isValidViewIdText("공유하기"))
        assertFalse(ShazamNodeFinder.isValidViewIdText("37,110"))
        assertFalse(ShazamNodeFinder.isValidViewIdText("1,387,352"))
    }

    @Test
    fun testShazamCountAndStatisticTextIsFiltered() {
        // 샤잠 횟수, 콤마 포함 수치, 단위 숫자는 무시 텍스트로 필터링되어야 함
        assertTrue(ShazamNodeFinder.isCountOrStatisticText("37,110"))
        assertTrue(ShazamNodeFinder.isCountOrStatisticText("37,111"))
        assertTrue(ShazamNodeFinder.isCountOrStatisticText("1,234,567"))
        assertTrue(ShazamNodeFinder.isCountOrStatisticText("1,387,352"))
        assertTrue(ShazamNodeFinder.isCountOrStatisticText("5.2K"))
        assertTrue(ShazamNodeFinder.isCountOrStatisticText("1.2M"))
        assertTrue(ShazamNodeFinder.isCountOrStatisticText("100만 회"))
        assertTrue(ShazamNodeFinder.isCountOrStatisticText("500 shazams"))

        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("37,110"))
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("37,111"))
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("1,387,352"))
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("5.2K"))
    }

    @Test
    fun testShazamUIKeywordsAreFiltered() {
        // 한국어/영문 UI 버튼 및 가이드 텍스트 필터링 확인
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("가사"))
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("상위 곡"))
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("상위곡"))
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("곡 공유하기"))
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("공유하기"))
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("YouTube Music"))
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("Apple Music"))
    }

    @Test
    fun testStandaloneDateKeywordsAreFiltered() {
        // 한국어 단독 날짜/요일 헤더 텍스트 및 정규식 날짜는 필터링되어야 함
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("일요일"))
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("어제"))
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("오늘"))
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("2026.08.15"))
        assertTrue(ShazamNodeFinder.isDateTimeOrIgnoredText("8월 15일"))
    }

    @Test
    fun testIsValidArtistTitlePair() {
        // 1. 정상 곡명/아티스트 쌍
        assertTrue(ShazamNodeFinder.isValidArtistTitlePair("Yesterday", "The Beatles"))
        assertTrue(ShazamNodeFinder.isValidArtistTitlePair("Sunday Morning", "Margo Guryan"))
        assertTrue(ShazamNodeFinder.isValidArtistTitlePair("Why Do I Cry", "Margo Guryan"))
        assertTrue(ShazamNodeFinder.isValidArtistTitlePair("1999", "Prince"))
        assertTrue(ShazamNodeFinder.isValidArtistTitlePair("금요일에 만나요", "아이유"))

        // 2. 숫자가 아티스트나 곡명으로 잘못 들어간 경우 기각
        assertFalse(ShazamNodeFinder.isValidArtistTitlePair("Margo Guryan", "37,110"))
        assertFalse(ShazamNodeFinder.isValidArtistTitlePair("37,110", "Margo Guryan"))
        assertFalse(ShazamNodeFinder.isValidArtistTitlePair("The Beatles", "1,387,352"))
        assertFalse(ShazamNodeFinder.isValidArtistTitlePair("Sunday Morning", "12345"))
        assertFalse(ShazamNodeFinder.isValidArtistTitlePair("37,110", "37,110"))

        // 3. UI 문구가 포함된 경우 기각
        assertFalse(ShazamNodeFinder.isValidArtistTitlePair("음악 감상", "Margo Guryan"))
        assertFalse(ShazamNodeFinder.isValidArtistTitlePair("Sunday Morning", "YouTube Music"))
        assertFalse(ShazamNodeFinder.isValidArtistTitlePair("곡 공유하기", "아이유"))
        assertFalse(ShazamNodeFinder.isValidArtistTitlePair("금요일에 만나요", "공유하기"))
    }
}
