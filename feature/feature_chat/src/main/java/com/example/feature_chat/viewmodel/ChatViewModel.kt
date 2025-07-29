package com.example.feature_chat.viewmodel

import androidx.lifecycle.ViewModel
import com.example.core_navigation.core.NavigationManger
import javax.inject.Inject

class ChatViewModel @Inject constructor(
    private val navigationManger: NavigationManger
) : ViewModel() {

    /**
     * ViewModel 에서 하는 일
     *
     * 1. LocalDB 에서 받은 체팅 데이터를 UI 에 그대로 투사
     * 1.1. UpdatedAt 등을 기준으로  변경되거나 추가된 내용 읽어서 Local저장
     * 2. LocalDB 에서 받은 사용자 데이터를 이용해 UI에 투사
     * 3. 메시지 페이징 처리
     * 4. 메시지 전송시 LocalDB에 저장 + websocket 전송
     *
     * # Paging 3 를 이용한 페이징 ROOM Flow 페이징 처리 및
     * Single source of truth
     *
     *
     * 메세지 데이터 : Flow 로 Room 과 연결됨
     * - 아마 repsoitory나 Datasource에서 paging3 처리 될 것 같음, 아니면 Usecase
     * - 해당 메시지 데이터 를 Screen에서 collect해서 사용
     * 2-way Paging3
     * futureMessages - anchor기준으로 최근 데이터 Flow
     * anchorMessage - 기준 데이터
     * pastMessages - anchor 기주으로 과거 데이터
     *
     * anchorMessage기본적으로 가장 최신 데이터 지만
     * 파라미터로 특정 메시지가 제공된 경우  해당 위치로 설정된다.
     */


    /**
     * 화면 들어왔을떄 실행
     *
     * {체널 ID} NaviagetionManager로부터 획득
     * - 채팅방 입장
     * - {체널 ID} 이용해서 LocalDB flow로 ui까지 연결
     *   - 우선 내용 보여주고 싱크는 그다음에 맞추는 방식
     * - `k
     */

    /**
     * 채팅방 나가기 메서드
     */

    /**
     * 채팅 전송 메서드
     */

    /**
     * 특정 메시지로 이동 메서드
     * anchorMessage를 수정하고 그에 맞추서 future와 past Messages다시 설정및 로딩한다.
     *
     * anchor가 설정되는경우
     * - 처음 시작시 어디에서 시작할지
     * - 사용자가 특정 메시지로 이동할때 (맨션, 답장 에대한 원본보기 등)
     * - 메시지가 삭제되거나 유효하지 않는경우
     */

}