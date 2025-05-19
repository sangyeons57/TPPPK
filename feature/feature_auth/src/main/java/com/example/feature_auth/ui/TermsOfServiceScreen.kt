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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.AppNavigator
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_auth.viewmodel.TermsOfServiceEvent
import com.example.feature_auth.viewmodel.TermsOfServiceViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * 서비스 이용약관 화면 (Stateful)
 * @param appNavigator 네비게이션 처리를 위한 AppNavigator
 * @param viewModel TermsOfServiceViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: TermsOfServiceViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is TermsOfServiceEvent.NavigateBack -> appNavigator.navigateBack()
                is TermsOfServiceEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("(임시)서비스 이용약관") },
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
        TermsOfServiceContent(
            modifier = modifier.padding(paddingValues)
        )
    }
}

/**
 * 서비스 이용약관 내용 표시 (Stateless)
 */
@Composable
fun TermsOfServiceContent(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = """
                제1조 (목적)
                본 운영정책은 [서비스명] (이하 "서비스")가 제공하는 모든 서비스의 운영 기준과 회원(이하 "회원")의 서비스 이용 조건 및 절차에 관한 기본적인 사항을 규정함을 목적으로 합니다.

                제2조 (용어의 정의)
                "서비스"라 함은 회사가 제공하는 [간단한 서비스 설명, 예: 이메일 기반의 OOO 정보 제공 서비스]를 의미합니다.
                "회원"이라 함은 본 운영정책 및 개인정보 처리방침에 동의하고 서비스 이용 계약을 체결하여 서비스를 이용하는 자를 의미합니다.
                "계정"이라 함은 회원의 식별과 서비스 이용을 위하여 회원이 선정하고 회사가 부여하는 이메일 주소를 의미합니다.

                제3조 (회원 가입 및 계정)
                회원 가입은 서비스를 이용하고자 하는 자가 본 운영정책과 개인정보 처리방침에 동의한 후, 회사가 정한 가입 양식에 따라 이메일 주소를 제공함으로써 신청하며, 회사가 이를 승낙함으로써 체결됩니다.
                본 서비스는 만 14세 이상인 자에 한하여 회원 가입 및 서비스 이용이 가능합니다. 만 14세 미만의 아동은 본 서비스에 가입할 수 없습니다.
                회원은 계정 생성 시 정확한 정보를 제공해야 하며, 자신의 계정 정보를 안전하게 관리할 책임이 있습니다.
                만약 회원이 만 14세 미만임이 확인되거나, 허위 정보를 통해 가입한 사실이 확인될 경우, 회사는 해당 계정의 이용을 제한하거나 즉시 삭제할 수 있으며, 관련된 모든 개인정보를 파기합니다.

                제4조 (서비스의 제공 및 변경)
                회사는 회원에게 안정적인 서비스를 제공하기 위해 노력합니다.
                서비스의 내용, 운영상 또는 기술상의 필요에 따라 제공하고 있는 서비스의 전부 또는 일부를 변경할 수 있습니다.

                제5조 (회원의 의무)
                회원은 관계 법령, 본 운영정책의 규정, 이용안내 및 서비스와 관련하여 공지한 주의사항, 회사가 통지하는 사항 등을 준수하여야 하며, 기타 회사의 업무에 방해되는 행위를 하여서는 안 됩니다.
                회원은 다음 각 호의 행위를 하여서는 안 됩니다.
                가입 신청 또는 정보 변경 시 허위 내용 등록 행위
                타인의 정보 도용 행위
                회사의 운영을 고의로 방해하는 행위
                기타 불법적이거나 부당한 행위

                제6조 (서비스 이용 제한 및 계약 해지)
                회원이 본 운영정책상의 의무를 위반하거나 서비스의 정상적인 운영을 방해한 경우, 회사는 경고, 일시정지, 영구이용정지 등으로 서비스 이용을 단계적으로 제한할 수 있습니다.
                회원이 다음 각 호에 해당하는 경우, 회사는 사전 통지 없이 회원 자격을 상실시키거나 서비스 이용 계약을 해지하고 계정을 삭제할 수 있습니다.
                만 14세 미만임이 확인된 경우
                허위 정보를 기재하여 가입한 경우
                본 운영정책에서 금지하는 행위를 한 경우

                제7조 (운영정책의 개정)
                회사는 필요한 경우 관련 법령에 위배되지 않는 범위 내에서 본 운영정책을 개정할 수 있습니다. 개정 시에는 적용일자 7일 이전부터 적용일자 전일까지 공지합니다. 다만, 회원에게 불리하게 변경되는 경우에는 최소한 30일 이상의 사전 유예기간을 두고 공지합니다.

                제8조 (문의처)
                서비스 이용과 관련한 모든 문의는 다음 연락처로 하시기 바랍니다.
                이메일: [서비스 대표 이메일 주소]

                부칙
                본 운영정책은 [시행일자 예: 2025년 X월 X일]부터 시행됩니다.
            """.trimIndent(),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun TermsOfServiceScreenPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        TermsOfServiceContent()
    }
} 