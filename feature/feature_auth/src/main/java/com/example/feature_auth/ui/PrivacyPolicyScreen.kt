package com.example.feature_auth.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.core_navigation.core.AppNavigator
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_auth.viewmodel.PrivacyPolicyEvent
import com.example.feature_auth.viewmodel.PrivacyPolicyViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * 개인정보 처리방침 화면 (Stateful)
 * @param appNavigator 네비게이션 처리를 위한 AppNavigator
 * @param viewModel PrivacyPolicyViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: PrivacyPolicyViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is PrivacyPolicyEvent.NavigateBack -> appNavigator.navigateBack()
                is PrivacyPolicyEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("(임시)개인정보 처리방침") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackClick() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PrivacyPolicyContent(
            modifier = modifier.padding(paddingValues)
        )
    }
}

/**
 * 개인정보 처리방침 내용 표시 (Stateless)
 */
@Composable
fun PrivacyPolicyContent(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = """
                제1조 (개인정보의 처리 목적)
                [서비스명] (이하 "회사")는 다음의 목적을 위하여 개인정보를 처리합니다. 처리하고 있는 개인정보는 다음의 목적 이외의 용도로는 이용되지 않으며, 이용 목적이 변경되는 경우에는 「개인정보 보호법」 제18조에 따라 별도의 동의를 받는 등 필요한 조치를 이행할 예정입니다.

                회원 가입 및 관리: 회원 식별, 회원제 서비스 제공에 따른 본인 확인, 서비스 부정이용 방지, 각종 고지·통지, 고충처리 등을 목적으로 개인정보를 처리합니다.
                연령 확인: 회원 가입 시 만 14세 이상 여부 확인을 목적으로 개인정보(생년월일)를 처리할 수 있습니다.

                제2조 (처리하는 개인정보의 항목 및 수집 방법)
                회사가 처리하는 개인정보 항목은 다음과 같습니다.
                필수항목: 이메일 주소
                연령 확인 시 수집항목: 생년월일 (단, 이 정보는 만 14세 이상 여부 확인 목적으로만 사용되며, 확인 즉시 파기되어 저장되지 않습니다.)
                수집방법: 회원가입 시 이용자의 직접 입력

                제3조 (개인정보의 처리 및 보유 기간)
                회사는 법령에 따른 개인정보 보유·이용기간 또는 정보주체로부터 개인정보를 수집 시에 동의받은 개인정보 보유·이용기간 내에서 개인정보를 처리·보유합니다.
                각각의 개인정보 처리 및 보유 기간은 다음과 같습니다.
                이메일 주소: 회원 탈퇴 시까지. 다만, 다음의 사유에 해당하는 경우에는 해당 사유 종료 시까지
                관계 법령 위반에 따른 수사·조사 등이 진행 중인 경우에는 해당 수사·조사 종료 시까지
                서비스 이용에 따른 채권·채무관계 잔존 시에는 해당 채권·채무관계 정산 시까지
                생년월일: 만 14세 이상 여부 확인 즉시 파기 (저장하지 않음)

                제4조 (개인정보의 제3자 제공에 관한 사항)
                회사는 정보주체의 개인정보를 제1조(개인정보의 처리 목적)에서 명시한 범위 내에서만 처리하며, 정보주체의 동의, 법률의 특별한 규정 등 「개인정보 보호법」 제17조 및 제18조에 해당하는 경우에만 개인정보를 제3자에게 제공합니다. 현재 회사는 개인정보를 제3자에게 제공하고 있지 않습니다.

                제5조 (개인정보처리의 위탁에 관한 사항)
                회사는 원활한 개인정보 업무처리를 위하여 다음과 같이 개인정보 처리업무를 위탁할 수 있습니다. 현재 개인정보 처리업무를 위탁하고 있지 않습니다.

                제6조 (정보주체와 법정대리인의 권리·의무 및 그 행사방법)
                정보주체는 회사에 대해 언제든지 개인정보 열람·정정·삭제·처리정지 요구 등의 권리를 행사할 수 있습니다.
                제1항에 따른 권리 행사는 회사에 대해 서면, 전자우편 등을 통하여 하실 수 있으며 회사는 이에 대해 지체 없이 조치하겠습니다.
                정보주체가 개인정보의 오류 등에 대한 정정 또는 삭제를 요구한 경우에는 회사는 정정 또는 삭제를 완료할 때까지 당해 개인정보를 이용하거나 제공하지 않습니다.
                만 14세 미만 아동의 경우, 법정대리인이 아동의 개인정보에 대한 열람, 정정·삭제, 처리정지 요구 등의 권리를 행사할 수 있습니다. (본 서비스는 원칙적으로 만 14세 미만 아동의 가입을 허용하지 않으나, 법적 고지 의무에 따라 포함)
                정보주체는 개인정보 보호 관련 법령을 위반하여 회사가 처리하고 있는 정보주체 본인이나 타인의 개인정보 및 사생활을 침해하여서는 아니 됩니다.

                제7조 (개인정보의 파기절차 및 파기방법)
                회사는 개인정보 보유기간의 경과, 처리목적 달성 등 개인정보가 불필요하게 되었을 때에는 지체없이 해당 개인정보를 파기합니다.
                개인정보 파기의 절차 및 방법은 다음과 같습니다.
                파기절차: 파기 사유가 발생한 개인정보를 선정하고, 회사의 개인정보 보호책임자의 승인을 받아 개인정보를 파기합니다.
                파기방법: 전자적 파일 형태로 기록·저장된 개인정보는 기록을 재생할 수 없도록 기술적인 방법을 이용하여 삭제하며, 종이 문서에 기록·저장된 개인정보는 분쇄기로 분쇄하거나 소각하여 파기합니다.

                제8조 (개인정보의 안전성 확보조치에 관한 사항)
                회사는 개인정보의 안전성 확보를 위해 다음과 같은 조치를 취하고 있습니다.
                관리적 조치: 내부관리계획 수립·시행, 정기적 직원 교육 등
                기술적 조치: 개인정보처리시스템 등의 접근권한 관리, 접근통제시스템 설치, 고유식별정보 등의 암호화, 보안프로그램 설치
                물리적 조치: 전산실, 자료보관실 등의 접근통제

                제9조 (개인정보 보호책임자에 관한 사항)
                회사는 개인정보 처리에 관한 업무를 총괄해서 책임지고, 개인정보 처리와 관련한 정보주체의 불만처리 및 피해구제 등을 위하여 아래와 같이 개인정보 보호책임자를 지정하고 있습니다.
                성명(또는 부서명): [담당자명 또는 부서명]
                연락처(이메일): [대표 이메일 주소]
                정보주체께서는 회사의 서비스(또는 사업)을 이용하시면서 발생한 모든 개인정보 보호 관련 문의, 불만처리, 피해구제 등에 관한 사항을 개인정보 보호책임자 및 담당부서로 문의하실 수 있습니다. 회사는 정보주체의 문의에 대해 지체 없이 답변 및 처리해드릴 것입니다.

                제10조 (개인정보 처리방침 변경에 관한 사항)
                이 개인정보 처리방침은 시행일로부터 적용되며, 법령 및 방침에 따른 변경내용의 추가, 삭제 및 정정이 있는 경우에는 변경사항의 시행 7일 전부터 공지사항을 통하여 고지할 것입니다.
                
                부칙
                본 개인정보 처리방침은 [시행일자 예: 2025년 X월 X일]부터 시행됩니다.
            """.trimIndent(),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PrivacyPolicyScreenPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        PrivacyPolicyContent()
    }
} 