package com.ssafy.e102.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.ssafy.e102.domain.admin.dto.response.AdminRoutingApplyStateResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoutingApplyStatus;
import com.ssafy.e102.domain.admin.entity.RoutingApplyState;
import com.ssafy.e102.domain.admin.repository.RoutingApplyStateRepository;
import com.ssafy.e102.global.exception.BusinessException;
import com.ssafy.e102.global.exception.CommonErrorCode;
import com.ssafy.e102.global.external.graphhopper.GraphHopperAdminClient;
import com.ssafy.e102.global.external.graphhopper.GraphHopperAdminClient.GraphHopperReloadResult;
import com.ssafy.e102.global.external.graphhopper.GraphHopperAdminClient.GraphHopperReloadStatus;

@ExtendWith(MockitoExtension.class)
class AdminRoutingApplyServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-19T03:00:00Z"), ZoneOffset.UTC);

	@Mock
	private RoutingApplyStateRepository routingApplyStateRepository;

	@Mock
	private GraphHopperAdminClient graphHopperAdminClient;

	private AdminRoutingApplyService adminRoutingApplyService;

	@BeforeEach
	void setUp() {
		when(graphHopperAdminClient.staleLockRecoveryThreshold()).thenReturn(Duration.ofMinutes(2));
		adminRoutingApplyService = new AdminRoutingApplyService(
			routingApplyStateRepository,
			graphHopperAdminClient,
			new NoOpPlatformTransactionManager(),
			FIXED_CLOCK);
	}

	@Test
	@DisplayName("dirty=false면 manual apply는 SKIPPED를 반환한다")
	void applySkipsWhenStateIsClean() {
		RoutingApplyState state = RoutingApplyState.initialize();
		when(routingApplyStateRepository.findForUpdate(RoutingApplyState.STATE_KEY)).thenReturn(Optional.of(state));

		AdminRoutingApplyStateResponse response = adminRoutingApplyService.applyRoutingOverrides();

		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.SKIPPED);
		assertThat(response.dirty()).isFalse();
	}

	@Test
	@DisplayName("dirty=true면 reload 성공 후 dirty=false로 내린다")
	void applyReloadsOnceAndClearsDirtyOnSuccess() {
		RoutingApplyState state = RoutingApplyState.initialize();
		state.markDirty(LocalDateTime.now(FIXED_CLOCK));
		when(routingApplyStateRepository.findForUpdate(RoutingApplyState.STATE_KEY)).thenReturn(Optional.of(state));
		when(graphHopperAdminClient.reloadRoutingOverrides())
			.thenReturn(new GraphHopperReloadResult(GraphHopperReloadStatus.APPLIED, "reloaded"));

		AdminRoutingApplyStateResponse response = adminRoutingApplyService.applyRoutingOverrides();

		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.APPLIED);
		assertThat(response.dirty()).isFalse();
		verify(graphHopperAdminClient).reloadRoutingOverrides();
	}

	@Test
	@DisplayName("reload 실패면 dirty=true를 유지한다")
	void applyKeepsDirtyOnFailure() {
		RoutingApplyState state = RoutingApplyState.initialize();
		state.markDirty(LocalDateTime.now(FIXED_CLOCK));
		when(routingApplyStateRepository.findForUpdate(RoutingApplyState.STATE_KEY)).thenReturn(Optional.of(state));
		when(graphHopperAdminClient.reloadRoutingOverrides())
			.thenReturn(new GraphHopperReloadResult(GraphHopperReloadStatus.FAILED, "reload failed"));

		AdminRoutingApplyStateResponse response = adminRoutingApplyService.applyRoutingOverrides();

		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.FAILED);
		assertThat(response.dirty()).isTrue();
	}

	@Test
	@DisplayName("이미 applying=true면 중복 manual apply를 409로 막는다")
	void applyRejectsConcurrentExecution() {
		RoutingApplyState state = RoutingApplyState.initialize();
		state.markDirty(LocalDateTime.now(FIXED_CLOCK));
		state.startApplying(LocalDateTime.now(FIXED_CLOCK));
		when(routingApplyStateRepository.findForUpdate(RoutingApplyState.STATE_KEY)).thenReturn(Optional.of(state));

		assertThatThrownBy(() -> adminRoutingApplyService.applyRoutingOverrides())
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(CommonErrorCode.CONFLICT);
	}

	@Test
	@DisplayName("stale applying lock??쉶?섏븯怨?manual apply瑜??ㅼ떆 ?ㅽ뻾?쒕떎")
	void applyRecoversStaleApplyingLock() {
		RoutingApplyState state = RoutingApplyState.initialize();
		LocalDateTime staleStart = LocalDateTime.now(FIXED_CLOCK).minusMinutes(30);
		state.markDirty(staleStart.minusMinutes(1));
		state.startApplying(staleStart);
		when(routingApplyStateRepository.findForUpdate(RoutingApplyState.STATE_KEY)).thenReturn(Optional.of(state));
		when(graphHopperAdminClient.reloadRoutingOverrides())
			.thenReturn(new GraphHopperReloadResult(GraphHopperReloadStatus.APPLIED, "reloaded"));

		AdminRoutingApplyStateResponse response = adminRoutingApplyService.applyRoutingOverrides();

		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.APPLIED);
		assertThat(response.applying()).isFalse();
		assertThat(response.dirty()).isFalse();
		verify(graphHopperAdminClient).reloadRoutingOverrides();
	}

	@Test
	@DisplayName("client timeout 湲곗? ?댁쟾 applying lock??洹몃?濡?409???좎???")
	void applyDoesNotRecoverLockWithinClientTimeoutWindow() {
		RoutingApplyState state = RoutingApplyState.initialize();
		LocalDateTime recentStart = LocalDateTime.now(FIXED_CLOCK).minusSeconds(90);
		state.markDirty(recentStart.minusSeconds(10));
		state.startApplying(recentStart);
		when(routingApplyStateRepository.findForUpdate(RoutingApplyState.STATE_KEY)).thenReturn(Optional.of(state));

		assertThatThrownBy(() -> adminRoutingApplyService.applyRoutingOverrides())
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(CommonErrorCode.CONFLICT);
	}

	private static final class NoOpPlatformTransactionManager implements PlatformTransactionManager {

		@Override
		public TransactionStatus getTransaction(TransactionDefinition definition) {
			return new SimpleTransactionStatus();
		}

		@Override
		public void commit(TransactionStatus status) {
		}

		@Override
		public void rollback(TransactionStatus status) {
		}
	}
}
