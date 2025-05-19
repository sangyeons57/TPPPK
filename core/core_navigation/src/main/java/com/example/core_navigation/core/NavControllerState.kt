package com.example.core_navigation.core

import android.os.Bundle
import android.os.Parcelable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.parcelize.Parcelize

/**
 * 네비게이션 컨트롤러의 상태를 저장하는 데이터 클래스
 * 
 * @property backStackState 현재 백스택 상태를 나타내는 문자열 (nullable)
 * @property screenState 화면의 UI 상태를 저장하는 Bundle (nullable)
 */
@Parcelize
data class NavControllerState(
    var backStackState: String? = null,
    var screenState: Bundle? = null
) : Parcelable

/**
 * NavControllerState의 맵을 저장하고 복원하기 위한 Saver 구현체
 */
object NavControllerSaver : Saver<SnapshotStateMap<String, NavControllerState>, HashMap<String, NavControllerState>> {
    override fun SaverScope.save(value: SnapshotStateMap<String, NavControllerState>): HashMap<String, NavControllerState> {
        return HashMap(value)
    }
    
    override fun restore(value: HashMap<String, NavControllerState>): SnapshotStateMap<String, NavControllerState>? {
        return mutableStateMapOf<String, NavControllerState>().apply { 
            putAll(value)
        }
    }
} 