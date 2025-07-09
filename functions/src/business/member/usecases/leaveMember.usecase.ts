import { MemberRepository } from "../../../infrastructure/repositories/member.repository";
import { ProjectRepository } from "../../../infrastructure/repositories/project.repository";
import { ProjectWrapperRepository } from "../../../infrastructure/repositories/projectWrapper.repository";
import { CustomResult } from "../../../core/result/customResult";
import { ConflictError } from "../../../core/errors/conflictError";
import { NotFoundError } from "../../../core/errors/notFoundError";
import { ValidationError } from "../../../core/errors/validationError";
import { injectable } from "inversify";

/**
 * 프로젝트 나가기 UseCase
 * 
 * 현재 사용자가 프로젝트에서 나가는 기능을 제공합니다.
 */
@injectable()
export class LeaveMemberUseCase {
    constructor(
        private memberRepository: MemberRepository,
        private projectRepository: ProjectRepository,
        private projectWrapperRepository: ProjectWrapperRepository
    ) {}

    /**
     * 프로젝트에서 나갑니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 나갈 사용자 ID
     * @returns 나가기 결과
     */
    async execute(projectId: string, userId: string): Promise<CustomResult<void, Error>> {
        try {
            // 1. 프로젝트 존재 확인
            const projectResult = await this.projectRepository.findById(projectId);
            if (!projectResult) {
                return CustomResult.failure(new NotFoundError("프로젝트를 찾을 수 없습니다."));
            }

            // 2. 멤버 존재 확인
            const memberResult = await this.memberRepository.findByUserIdAndProjectId(userId, projectId);
            if (!memberResult) {
                return CustomResult.failure(new NotFoundError("프로젝트 멤버를 찾을 수 없습니다."));
            }

            const member = memberResult;

            // 3. OWNER 역할인지 확인 (OWNER는 나갈 수 없음)
            if (member.roles.includes("OWNER")) {
                return CustomResult.failure(new ConflictError("프로젝트 소유자는 나갈 수 없습니다. 소유권을 다른 멤버에게 전달한 후 나가세요."));
            }

            // 4. 멤버 삭제
            const deleteMemberResult = await this.memberRepository.delete(member.id);
            if (!deleteMemberResult) {
                return CustomResult.failure(new Error("멤버 삭제에 실패했습니다."));
            }

            // 5. 프로젝트 래퍼 삭제
            const deleteWrapperResult = await this.projectWrapperRepository.deleteByUserIdAndProjectId(userId, projectId);
            if (!deleteWrapperResult) {
                // 프로젝트 래퍼 삭제 실패는 치명적이지 않으므로 로그만 남김
                console.warn(`프로젝트 래퍼 삭제 실패: userId=${userId}, projectId=${projectId}`);
            }

            // 6. 프로젝트 멤버 수 업데이트
            const project = projectResult;
            const updatedMemberCount = Math.max(0, project.memberCount - 1);
            await this.projectRepository.updateMemberCount(projectId, updatedMemberCount);

            return CustomResult.success(undefined);

        } catch (error) {
            console.error("LeaveMemberUseCase error:", error);
            return CustomResult.failure(error as Error);
        }
    }
}